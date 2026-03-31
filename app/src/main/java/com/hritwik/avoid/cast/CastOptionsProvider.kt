package com.hritwik.avoid.cast

import android.content.Context
import com.google.android.gms.cast.LaunchOptions
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions

class CastOptionsProvider : OptionsProvider {

    companion object {
        const val CAST_APP_ID = "C94B3EEA"
    }

    override fun getCastOptions(context: Context): CastOptions {
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName("com.hritwik.avoid.MainActivity")
            .build()

        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .build()

        val launchOptions = LaunchOptions.Builder()
            .setRelaunchIfRunning(false)
            .build()

        return CastOptions.Builder()
            .setReceiverApplicationId(CAST_APP_ID)
            .setCastMediaOptions(mediaOptions)
            .setLaunchOptions(launchOptions)
            .setStopReceiverApplicationWhenEndingSession(true)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
