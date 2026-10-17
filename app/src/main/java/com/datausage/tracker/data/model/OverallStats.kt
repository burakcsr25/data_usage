package com.datausage.tracker.data.model

data class OverallStats(
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L,
    val foregroundRxBytes: Long = 0L,
    val foregroundTxBytes: Long = 0L,
    val backgroundRxBytes: Long = 0L,
    val backgroundTxBytes: Long = 0L
) {
    val mobileTotalBytes: Long
        get() = mobileRxBytes + mobileTxBytes

    val wifiTotalBytes: Long
        get() = wifiRxBytes + wifiTxBytes

    val totalBytes: Long
        get() = mobileTotalBytes + wifiTotalBytes

    val totalRxBytes: Long
        get() = mobileRxBytes + wifiRxBytes

    val totalTxBytes: Long
        get() = mobileTxBytes + wifiTxBytes

    val totalForegroundBytes: Long
        get() = foregroundRxBytes + foregroundTxBytes

    val totalBackgroundBytes: Long
        get() = backgroundRxBytes + backgroundTxBytes
}
