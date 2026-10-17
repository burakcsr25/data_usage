package com.datausage.tracker.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val decimalFormatOne = DecimalFormat("#,##0.0")
    private val decimalFormatTwo = DecimalFormat("#,##0.00")
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("tr", "TR"))
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr", "TR"))

    /**
     * Bayt cinsinden veriyi okunabilir birime (B, KB, MB, GB, TB) dönüştürür.
     */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        val tb = gb * 1024

        val doubleBytes = bytes.toDouble()
        return when {
            doubleBytes >= tb -> "${decimalFormatTwo.format(doubleBytes / tb)} TB"
            doubleBytes >= gb -> "${decimalFormatTwo.format(doubleBytes / gb)} GB"
            doubleBytes >= mb -> "${decimalFormatOne.format(doubleBytes / mb)} MB"
            doubleBytes >= kb -> "${decimalFormatOne.format(doubleBytes / kb)} KB"
            else -> "$bytes B"
        }
    }

    /**
     * İki değer arasındaki yüzdeyi hesaplar.
     */
    fun formatPercentage(part: Long, total: Long): String {
        if (total <= 0L || part <= 0L) return "0%"
        val percent = (part.toDouble() / total.toDouble()) * 100.0
        return if (percent < 0.1) "<0.1%" else "${decimalFormatOne.format(percent)}%"
    }

    /**
     * Oranı 0.0f ile 1.0f arasında float olarak döndürür (Progress bar için).
     */
    fun getProgressFraction(part: Long, total: Long): Float {
        if (total <= 0L || part <= 0L) return 0f
        return (part.toDouble() / total.toDouble()).coerceIn(0.0, 1.0).toFloat()
    }

    /**
     * Tarih aralığını metin olarak biçimlendirir.
     */
    fun formatDateRange(startMillis: Long, endMillis: Long): String {
        val startStr = dateFormat.format(Date(startMillis))
        val endStr = dateFormat.format(Date(endMillis))
        return "$startStr - $endStr"
    }

    fun formatDate(millis: Long): String {
        return dateFormat.format(Date(millis))
    }
}
