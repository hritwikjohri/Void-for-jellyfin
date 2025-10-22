package com.hritwik.avoid.utils.helpers

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

object FileHelper {
    fun getContentUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun getMimeType(file: File): String? {
        val extension = file.extension.lowercase()
        return when (extension) {
            "ass", "ssa" -> "text/x-ssa"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }    }
}