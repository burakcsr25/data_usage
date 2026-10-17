package com.datausage.tracker.data.model

import java.util.Calendar

sealed class TimeRange(val displayName: String) {
    abstract fun getTimeSpan(): Pair<Long, Long>

    object Today : TimeRange("Bugün") {
        override fun getTimeSpan(): Pair<Long, Long> {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = calendar.timeInMillis
            val end = System.currentTimeMillis()
            return Pair(start, end)
        }
    }

    object Last7Days : TimeRange("Son 7 Gün") {
        override fun getTimeSpan(): Pair<Long, Long> {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -7)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = calendar.timeInMillis
            val end = System.currentTimeMillis()
            return Pair(start, end)
        }
    }

    object ThisMonth : TimeRange("Bu Ay") {
        override fun getTimeSpan(): Pair<Long, Long> {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = calendar.timeInMillis
            val end = System.currentTimeMillis()
            return Pair(start, end)
        }
    }

    data class Custom(
        val startMillis: Long,
        val endMillis: Long,
        val formattedLabel: String = "Özel Aralık"
    ) : TimeRange(formattedLabel) {
        override fun getTimeSpan(): Pair<Long, Long> {
            val safeEnd = if (endMillis <= startMillis) startMillis + (24 * 60 * 60 * 1000L) else endMillis
            return Pair(startMillis, safeEnd)
        }
    }
}
