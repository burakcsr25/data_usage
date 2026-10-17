package com.datausage.tracker.data.model

import androidx.compose.ui.graphics.ImageBitmap

data class AppDataUsage(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val iconBitmap: ImageBitmap? = null,
    val isSystemApp: Boolean = false,
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L,
    val foregroundRxBytes: Long = 0L,
    val foregroundTxBytes: Long = 0L,
    val backgroundRxBytes: Long = 0L,
    val backgroundTxBytes: Long = 0L
) {
    val totalMobileBytes: Long
        get() = mobileRxBytes + mobileTxBytes

    val totalWifiBytes: Long
        get() = wifiRxBytes + wifiTxBytes

    val totalBytes: Long
        get() = totalMobileBytes + totalWifiBytes

    val totalRxBytes: Long
        get() = mobileRxBytes + wifiRxBytes

    val totalTxBytes: Long
        get() = mobileTxBytes + wifiTxBytes

    val totalForegroundBytes: Long
        get() = foregroundRxBytes + foregroundTxBytes

    val totalBackgroundBytes: Long
        get() = backgroundRxBytes + backgroundTxBytes

    fun getUsageForFilter(filter: NetworkTypeFilter): Long {
        return when (filter) {
            NetworkTypeFilter.ALL -> totalBytes
            NetworkTypeFilter.MOBILE_ONLY -> totalMobileBytes
            NetworkTypeFilter.WIFI_ONLY -> totalWifiBytes
        }
    }

    fun getRxForFilter(filter: NetworkTypeFilter): Long {
        return when (filter) {
            NetworkTypeFilter.ALL -> totalRxBytes
            NetworkTypeFilter.MOBILE_ONLY -> mobileRxBytes
            NetworkTypeFilter.WIFI_ONLY -> wifiRxBytes
        }
    }

    fun getTxForFilter(filter: NetworkTypeFilter): Long {
        return when (filter) {
            NetworkTypeFilter.ALL -> totalTxBytes
            NetworkTypeFilter.MOBILE_ONLY -> mobileTxBytes
            NetworkTypeFilter.WIFI_ONLY -> wifiTxBytes
        }
    }
}
