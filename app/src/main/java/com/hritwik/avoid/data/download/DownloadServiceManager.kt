package com.hritwik.avoid.data.download

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

object DownloadServiceManager {
    @OptIn(UnstableApi::class)
    fun withService(
        context: Context,
        startIfNeeded: Boolean = false,
        action: (DownloadService) -> Unit
    ) {
        val instance = DownloadService.getInstance()
        if (instance != null) {
            action(instance)
            return
        }
        if (!startIfNeeded) return

        
        DownloadService.start(context)

        val appContext = context.applicationContext
        val intent = Intent(appContext, DownloadService::class.java)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val service = (binder as DownloadService.DownloadBinder).getService()
                action(service)
                appContext.unbindService(this)
            }

            override fun onServiceDisconnected(name: ComponentName) {}
        }
        appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
}