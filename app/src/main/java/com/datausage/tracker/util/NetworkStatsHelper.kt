package com.datausage.tracker.util

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log

class NetworkStatsHelper(private val context: Context) {

    private val networkStatsManager: NetworkStatsManager? =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager

    data class RawUidTraffic(
        val uid: Int,
        var mobileRx: Long = 0L,
        var mobileTx: Long = 0L,
        var wifiRx: Long = 0L,
        var wifiTx: Long = 0L,
        var foregroundRx: Long = 0L,
        var foregroundTx: Long = 0L,
        var backgroundRx: Long = 0L,
        var backgroundTx: Long = 0L
    )

    /**
     * Belirtilen zaman aralığında tüm UID'lerin Mobil ve Wi-Fi veri tüketimini
     * (Rx, Tx, Foreground, Background) eksiksiz ve doğru ayrıştırarak çeker.
     */
    fun fetchTrafficForInterval(startTime: Long, endTime: Long): Map<Int, RawUidTraffic> {
        val trafficMap = mutableMapOf<Int, RawUidTraffic>()

        if (networkStatsManager == null) {
            Log.e(TAG, "NetworkStatsManager servisi bulunamadı!")
            return trafficMap
        }

        // 1. ADIM: querySummary ile tüm UID'lerin genel Mobil ve Wi-Fi toplamlarını çek
        // (querySummary tek sorguda tüm sistem UID'lerini getiren en kararlı ve hızlı API'dır)
        collectNetworkSummary(
            networkType = ConnectivityManager.TYPE_MOBILE,
            startTime = startTime,
            endTime = endTime,
            isMobile = true,
            trafficMap = trafficMap
        )

        collectNetworkSummary(
            networkType = ConnectivityManager.TYPE_WIFI,
            startTime = startTime,
            endTime = endTime,
            isMobile = false,
            trafficMap = trafficMap
        )

        // 2. ADIM: Trafiği olan her UID için Ön Plan (Foreground) ve Arka Plan (Background) dökümünü çıkar
        // Android'de genel parametresiz queryDetails API'ı state ayrımı vermez (STATE_ALL döndürür).
        // Ancak UID bazında queryDetailsForUid veya queryDetailsForUidTagState çağrıldığında
        // STATE_FOREGROUND (2) ve STATE_DEFAULT (1 - Arka Plan) verileri tam olarak çekilir!
        for ((uid, traffic) in trafficMap) {
            val totalBytes = traffic.mobileRx + traffic.mobileTx + traffic.wifiRx + traffic.wifiTx
            if (totalBytes <= 0L) continue

            // Negatif sistem UID'leri (tethering/removed vb.) detay sorgulanmaz, arka plan sayılır
            if (uid < 0) {
                traffic.backgroundRx = traffic.mobileRx + traffic.wifiRx
                traffic.backgroundTx = traffic.mobileTx + traffic.wifiTx
                continue
            }

            var uidFgRx = 0L
            var uidFgTx = 0L

            // Mobil için ön plan verisini sorgula
            if (traffic.mobileRx + traffic.mobileTx > 0L) {
                val (fgRx, fgTx) = queryUidStateDetails(
                    networkType = ConnectivityManager.TYPE_MOBILE,
                    startTime = startTime,
                    endTime = endTime,
                    uid = uid
                )
                uidFgRx += fgRx
                uidFgTx += fgTx
            }

            // Wi-Fi için ön plan verisini sorgula
            if (traffic.wifiRx + traffic.wifiTx > 0L) {
                val (fgRx, fgTx) = queryUidStateDetails(
                    networkType = ConnectivityManager.TYPE_WIFI,
                    startTime = startTime,
                    endTime = endTime,
                    uid = uid
                )
                uidFgRx += fgRx
                uidFgTx += fgTx
            }

            val totalRx = traffic.mobileRx + traffic.wifiRx
            val totalTx = traffic.mobileTx + traffic.wifiTx

            // Ön plan toplamı genel toplamı geçemez
            traffic.foregroundRx = uidFgRx.coerceAtMost(totalRx)
            traffic.foregroundTx = uidFgTx.coerceAtMost(totalTx)

            // Arka plan = Toplam - Ön Plan (Tam matematiksel tutarlılık)
            traffic.backgroundRx = (totalRx - traffic.foregroundRx).coerceAtLeast(0L)
            traffic.backgroundTx = (totalTx - traffic.foregroundTx).coerceAtLeast(0L)
        }

        return trafficMap
    }

    /**
     * querySummary çağrısı yaparak tüm UID'lerin toplam Rx/Tx değerlerini toplar.
     */
    private fun collectNetworkSummary(
        networkType: Int,
        startTime: Long,
        endTime: Long,
        isMobile: Boolean,
        trafficMap: MutableMap<Int, RawUidTraffic>
    ) {
        var networkStats: NetworkStats? = null
        try {
            networkStats = networkStatsManager?.querySummary(networkType, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (networkStats?.hasNextBucket() == true) {
                networkStats.getNextBucket(bucket)
                val uid = bucket.uid
                val rx = bucket.rxBytes.coerceAtLeast(0L)
                val tx = bucket.txBytes.coerceAtLeast(0L)

                if (rx == 0L && tx == 0L) continue

                val item = trafficMap.getOrPut(uid) { RawUidTraffic(uid) }
                if (isMobile) {
                    item.mobileRx += rx
                    item.mobileTx += tx
                } else {
                    item.wifiRx += rx
                    item.wifiTx += tx
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "querySummary sorgu hatası (networkType: $networkType): ${e.message}", e)
        } finally {
            try {
                networkStats?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Belirli bir UID için Ön Plan (STATE_FOREGROUND) tüketimini ayrıntılı sorgular.
     */
    private fun queryUidStateDetails(
        networkType: Int,
        startTime: Long,
        endTime: Long,
        uid: Int
    ): Pair<Long, Long> {
        var fgRx = 0L
        var fgTx = 0L
        var foundForeground = false

        // 1. Yöntem: queryDetailsForUid (Tüm soketleri kapsayan detaylı döküm)
        var stats: NetworkStats? = null
        try {
            stats = networkStatsManager?.queryDetailsForUid(
                networkType,
                null,
                startTime,
                endTime,
                uid
            )
            if (stats != null) {
                val bucket = NetworkStats.Bucket()
                while (stats.hasNextBucket()) {
                    stats.getNextBucket(bucket)
                    if (bucket.state == NetworkStats.Bucket.STATE_FOREGROUND) {
                        fgRx += bucket.rxBytes.coerceAtLeast(0L)
                        fgTx += bucket.txBytes.coerceAtLeast(0L)
                        foundForeground = true
                    }
                }
            }
        } catch (_: Exception) {
            // Güvenlik veya OEM kısıtlaması durumunda devam et
        } finally {
            try { stats?.close() } catch (_: Exception) {}
        }

        // 2. Yöntem: Eğer ilk yöntem ön plan verisi vermediyse doğrudan STATE_FOREGROUND etiketli sorguyu dene
        if (!foundForeground) {
            var tagStats: NetworkStats? = null
            try {
                tagStats = networkStatsManager?.queryDetailsForUidTagState(
                    networkType,
                    null,
                    startTime,
                    endTime,
                    uid,
                    NetworkStats.Bucket.TAG_NONE,
                    NetworkStats.Bucket.STATE_FOREGROUND
                )
                if (tagStats != null) {
                    val bucket = NetworkStats.Bucket()
                    while (tagStats.hasNextBucket()) {
                        tagStats.getNextBucket(bucket)
                        fgRx += bucket.rxBytes.coerceAtLeast(0L)
                        fgTx += bucket.txBytes.coerceAtLeast(0L)
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { tagStats?.close() } catch (_: Exception) {}
            }
        }

        return Pair(fgRx, fgTx)
    }

    companion object {
        private const val TAG = "NetworkStatsHelper"
    }
}
