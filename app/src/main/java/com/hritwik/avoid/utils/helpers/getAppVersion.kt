package com.hritwik.avoid.utils.helpers

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

fun Context.getAppVersionName(): String {
    return try {
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        pInfo.versionName ?: "Unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}

fun Context.getAppVersionCode(): Long {
    return try {
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            pInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pInfo.versionCode.toLong()
        }
    } catch (e: PackageManager.NameNotFoundException) {
        -1L
    }
}

fun getDeviceName(context: Context): String {
    val userName = Settings.Global.getString(
        context.contentResolver,
        Settings.Global.DEVICE_NAME
    )

    if (!userName.isNullOrBlank()) return userName
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    return if (model.startsWith(manufacturer, ignoreCase = true)) {
        model.replaceFirstChar { it.uppercase() }
    } else {
        "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
    }
}