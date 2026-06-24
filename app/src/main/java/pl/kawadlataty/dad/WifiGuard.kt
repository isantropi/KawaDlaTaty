package pl.kawadlataty.dad

import android.Manifest
import android.content.Context
import android.net.wifi.WifiManager
import androidx.annotation.RequiresPermission

object WifiGuard {
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun isHomeWifi(context: Context): Boolean {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ssid = wifi.connectionInfo?.ssid?.removeSurrounding("\"") ?: return false
        return ssid == Config.HOME_WIFI_SSID
    }
}
