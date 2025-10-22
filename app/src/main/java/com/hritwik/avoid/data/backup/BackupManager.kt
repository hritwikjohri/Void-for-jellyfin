package com.hritwik.avoid.data.backup

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.hritwik.avoid.utils.constants.AppConstants
import com.hritwik.avoid.utils.constants.PreferenceConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val backupDir: File by lazy {
        File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
    }

    @Serializable
    private data class BackupMetadata(val version: Int)

    suspend fun createBackup(): File = withContext(Dispatchers.IO) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val backupFile = File(backupDir, "backup_${System.currentTimeMillis()}.bak")
        val encryptedFile = EncryptedFile.Builder(
            context,
            backupFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { outputStream ->
            ZipOutputStream(outputStream).use { zip ->
                val metadata = BackupMetadata(AppConstants.DATABASE_VERSION)
                zip.putNextEntry(ZipEntry("meta.json"))
                zip.write(Json.encodeToString(metadata).toByteArray())
                zip.closeEntry()

                val prefsFile = File(context.filesDir, "datastore/${PreferenceConstants.DATASTORE_NAME}.preferences_pb")
                if (prefsFile.exists()) {
                    zip.putNextEntry(ZipEntry("prefs.pb"))
                    prefsFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }

                val authPrefsFile = File(context.filesDir, "datastore/auth_preferences.preferences_pb")
                if (authPrefsFile.exists()) {
                    zip.putNextEntry(ZipEntry("auth_prefs.pb"))
                    authPrefsFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }

                val dbFile = context.getDatabasePath(AppConstants.DATABASE_NAME)
                if (dbFile.exists()) {
                    zip.putNextEntry(ZipEntry("database.db"))
                    dbFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }

        backupFile
    }

    suspend fun restoreLatestBackup(): Boolean = withContext(Dispatchers.IO) {
        val latest = backupDir.listFiles()?.maxByOrNull { it.lastModified() } ?: return@withContext false
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val encryptedFile = EncryptedFile.Builder(
            context,
            latest,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        var metadata: BackupMetadata? = null
        val tempDir = File(context.cacheDir, "restore_temp").apply {
            deleteRecursively()
            mkdirs()
        }

        encryptedFile.openFileInput().use { input ->
            ZipInputStream(input).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "meta.json" -> {
                            val bytes = zip.readBytes()
                            metadata = runCatching { Json.decodeFromString<BackupMetadata>(String(bytes)) }.getOrNull()
                        }
                        "prefs.pb" -> {
                            val dest = File(tempDir, "prefs.pb")
                            dest.outputStream().use { zip.copyTo(it) }
                        }
                        "auth_prefs.pb" -> {
                            val dest = File(tempDir, "auth_prefs.pb")
                            dest.outputStream().use { zip.copyTo(it) }
                        }
                        "database.db" -> {
                            val dest = File(tempDir, "database.db")
                            dest.outputStream().use { zip.copyTo(it) }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        val meta = metadata ?: return@withContext false
        if (meta.version != AppConstants.DATABASE_VERSION) {
            tempDir.deleteRecursively()
            return@withContext false
        }

        val prefsDest = File(context.filesDir, "datastore/${PreferenceConstants.DATASTORE_NAME}.preferences_pb")
        val authPrefsDest = File(context.filesDir, "datastore/auth_preferences.preferences_pb")
        val dbDest = context.getDatabasePath(AppConstants.DATABASE_NAME)

        File(tempDir, "prefs.pb").takeIf { it.exists() }?.copyTo(prefsDest, overwrite = true)
        File(tempDir, "auth_prefs.pb").takeIf { it.exists() }?.copyTo(authPrefsDest, overwrite = true)
        File(tempDir, "database.db").takeIf { it.exists() }?.copyTo(dbDest, overwrite = true)

        tempDir.deleteRecursively()
        true
    }
}

