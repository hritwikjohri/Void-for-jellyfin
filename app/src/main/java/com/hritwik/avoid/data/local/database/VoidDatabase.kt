package com.hritwik.avoid.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.database.SQLiteDatabase
import com.hritwik.avoid.data.local.database.migrations.MIGRATION_4_5
import com.hritwik.avoid.data.local.database.migrations.MIGRATION_6_7
import com.hritwik.avoid.data.local.database.migrations.MIGRATION_7_8
import com.hritwik.avoid.data.local.database.migrations.MIGRATION_8_9
import com.hritwik.avoid.data.local.database.convertors.DatabaseConverters
import com.hritwik.avoid.data.local.database.dao.DownloadDao
import com.hritwik.avoid.data.local.database.dao.LibraryDao
import com.hritwik.avoid.data.local.database.dao.MediaItemDao
import com.hritwik.avoid.data.local.database.dao.PendingActionDao
import com.hritwik.avoid.data.local.database.dao.PlaybackLogDao
import com.hritwik.avoid.data.local.database.dao.SearchResultDao
import com.hritwik.avoid.data.local.database.dao.UserDao
import com.hritwik.avoid.data.local.database.entities.DownloadEntity
import com.hritwik.avoid.data.local.database.entities.LibraryEntity
import com.hritwik.avoid.data.local.database.entities.MediaItemEntity
import com.hritwik.avoid.data.local.database.entities.PendingActionEntity
import com.hritwik.avoid.data.local.database.entities.PlaybackLogEntity
import com.hritwik.avoid.data.local.database.entities.SearchResultEntity
import com.hritwik.avoid.data.local.database.entities.UserEntity
import com.hritwik.avoid.data.local.database.migrations.MIGRATION_9_10
import com.hritwik.avoid.data.local.database.migrations.MIGRATION_10_11
import com.hritwik.avoid.data.local.database.migrations.MIGRATION_11_12
import com.hritwik.avoid.data.local.database.migrations.MIGRATION_12_13
import com.hritwik.avoid.data.local.database.migrations.MIGRATION_13_14
import com.hritwik.avoid.data.local.database.migrations.MIGRATION_14_15
import com.hritwik.avoid.data.local.database.DatabasePassphraseProvider
import com.hritwik.avoid.utils.constants.AppConstants
import com.hritwik.avoid.utils.CrashReporter

@Database(
    entities = [
        MediaItemEntity::class,
        LibraryEntity::class,
        UserEntity::class,
        DownloadEntity::class,
        PlaybackLogEntity::class,
        SearchResultEntity::class,
        PendingActionEntity::class
    ],
    version = AppConstants.DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class VoidDatabase : RoomDatabase() {

    abstract fun mediaItemDao(): MediaItemDao
    abstract fun libraryDao(): LibraryDao
    abstract fun userDao(): UserDao
    abstract fun downloadDao(): DownloadDao
    abstract fun playbackLogDao(): PlaybackLogDao
    abstract fun searchResultDao(): SearchResultDao
    abstract fun pendingActionDao(): PendingActionDao

    suspend fun clearDatabase() {
        mediaItemDao().closeOpenCursors()
        libraryDao().closeOpenCursors()
        downloadDao().closeOpenCursors()
        playbackLogDao().closeOpenCursors()
        searchResultDao().closeOpenCursors()
        pendingActionDao().closeOpenCursors()
        clearAllTables()
    }

    companion object {
        @Volatile
        private var INSTANCE: VoidDatabase? = null

        fun getDatabase(context: Context): VoidDatabase {
            return INSTANCE ?: synchronized(this) {
                SQLiteDatabase.loadLibs(context)
                val passphrase = DatabasePassphraseProvider.getPassphrase(context)
                val factory = SupportFactory(passphrase)
                try {
                    val instance = buildDatabase(context, factory)
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    context.deleteDatabase(AppConstants.DATABASE_NAME)
                    CrashReporter.report(e)
                    val instance = buildDatabase(context, factory)
                    INSTANCE = instance
                    instance
                }
            }
        }

        private fun buildDatabase(context: Context, factory: SupportFactory): VoidDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                VoidDatabase::class.java,
                AppConstants.DATABASE_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(
                    MIGRATION_4_5,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15
                )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}