package com.hritwik.avoid.utils.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    fun isNetworkAvailable(): Boolean {
        return checkNetworkCapabilities()
    }

    fun isNetworkConnected(): Boolean {
        return getActiveNetworkCapabilities()?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    fun isWifiConnected(): Boolean {
        return getActiveNetworkCapabilities()?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    fun isCellularConnected(): Boolean {
        return getActiveNetworkCapabilities()?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    }

    fun getNetworkType(): String {
        val capabilities = getActiveNetworkCapabilities() ?: return "No Connection"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }
    }

    private fun getActiveNetworkCapabilities(): NetworkCapabilities? {
        val network = connectivityManager.activeNetwork ?: return null
        return connectivityManager.getNetworkCapabilities(network)
    }

    private fun checkNetworkCapabilities(): Boolean {
        val capabilities = getActiveNetworkCapabilities() ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun checkLegacyNetworkWithTest(): Boolean {
        @Suppress("DEPRECATION")
        if (connectivityManager.activeNetworkInfo?.isConnected != true) {
            return false
        }

        return try {
            val url = URL("https://clients3.google.com/generate_204")
            (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "Android")
                setRequestProperty("Connection", "close")
                connectTimeout = 1500  
                readTimeout = 1500
                connect()
            }.run {
                responseCode == 204 && contentLength == 0
            }
        } catch (e: Exception) {
            false
        }
    }
}