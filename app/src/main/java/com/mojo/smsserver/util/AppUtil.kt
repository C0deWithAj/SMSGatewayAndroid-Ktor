package com.mojo.smsserver.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import androidx.activity.ComponentActivity

class AppUtil {

    companion object {

        fun getIpAddress(context: Context): String {
            try {
                val wm = context.getSystemService(ComponentActivity.WIFI_SERVICE) as WifiManager
                return Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
            } catch (ex: java.lang.Exception) {

            }
            return ""
        }

        fun isWifiConnected(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            } else {
                val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
                activeNetwork?.typeName?.contains("wifi", ignoreCase = true) ?: false
            }
        }

    }


}