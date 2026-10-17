package com.datausage.tracker.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.datausage.tracker.data.model.AppDataUsage
import com.datausage.tracker.data.model.OverallStats
import com.datausage.tracker.data.model.TimeRange
import com.datausage.tracker.util.NetworkStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DataUsageResult(
    val apps: List<AppDataUsage>,
    val overallStats: OverallStats
)

class DataUsageRepository(private val context: Context) {

    private val networkStatsHelper = NetworkStatsHelper(context)
    private val packageManager = context.packageManager

    // Performans için paket bilgilerini önbelleğe al
    private var cachedAppInfoMap: Map<Int, CachedAppMetadata>? = null

    private data class CachedAppMetadata(
        val uid: Int,
        val packageName: String,
        val appName: String,
        val iconBitmap: ImageBitmap?,
        val isSystemApp: Boolean
    )

    /**
     * Belirtilen zaman aralığı için tüm uygulamaların veri kullanımını hesaplar.
     */
    suspend fun getUsageData(timeRange: TimeRange): DataUsageResult = withContext(Dispatchers.IO) {
        val (startTime, endTime) = timeRange.getTimeSpan()

        // 1. Ağ istatistiklerini çek
        val trafficMap = networkStatsHelper.fetchTrafficForInterval(startTime, endTime)

        // 2. Yüklü uygulamaları getir (önbellekten veya PackageManager'dan)
        val appInfoMap = getOrLoadAppMetadata()

        // 3. Verileri eşleştir
        val resultList = mutableListOf<AppDataUsage>()

        var totalMobileRx = 0L
        var totalMobileTx = 0L
        var totalWifiRx = 0L
        var totalWifiTx = 0L
        var totalFgRx = 0L
        var totalFgTx = 0L
        var totalBgRx = 0L
        var totalBgTx = 0L

        // Ağ trafiği olan her UID için model oluştur
        for ((uid, traffic) in trafficMap) {
            val totalBytesForUid = traffic.mobileRx + traffic.mobileTx + traffic.wifiRx + traffic.wifiTx
            if (totalBytesForUid <= 0L) continue

            // Genel toplamlara ekle
            totalMobileRx += traffic.mobileRx
            totalMobileTx += traffic.mobileTx
            totalWifiRx += traffic.wifiRx
            totalWifiTx += traffic.wifiTx
            totalFgRx += traffic.foregroundRx
            totalFgTx += traffic.foregroundTx
            totalBgRx += traffic.backgroundRx
            totalBgTx += traffic.backgroundTx

            val metadata = appInfoMap[uid]
            val (appName, packageName, isSystem, iconBitmap) = if (metadata != null) {
                Quadruple(metadata.appName, metadata.packageName, metadata.isSystemApp, metadata.iconBitmap)
            } else {
                resolveSpecialUid(uid)
            }

            resultList.add(
                AppDataUsage(
                    uid = uid,
                    packageName = packageName,
                    appName = appName,
                    iconBitmap = iconBitmap,
                    isSystemApp = isSystem,
                    mobileRxBytes = traffic.mobileRx,
                    mobileTxBytes = traffic.mobileTx,
                    wifiRxBytes = traffic.wifiRx,
                    wifiTxBytes = traffic.wifiTx,
                    foregroundRxBytes = traffic.foregroundRx,
                    foregroundTxBytes = traffic.foregroundTx,
                    backgroundRxBytes = traffic.backgroundRx,
                    backgroundTxBytes = traffic.backgroundTx
                )
            )
        }

        // Genel istatistikleri oluştur
        val overallStats = OverallStats(
            mobileRxBytes = totalMobileRx,
            mobileTxBytes = totalMobileTx,
            wifiRxBytes = totalWifiRx,
            wifiTxBytes = totalWifiTx,
            foregroundRxBytes = totalFgRx,
            foregroundTxBytes = totalFgTx,
            backgroundRxBytes = totalBgRx,
            backgroundTxBytes = totalBgTx
        )

        DataUsageResult(
            apps = resultList.sortedByDescending { it.totalBytes },
            overallStats = overallStats
        )
    }

    /**
     * PackageManager'dan yüklü uygulamaları tarar ve UI için hazır bitmap'lerle önbelleğe alır.
     */
    private fun getOrLoadAppMetadata(): Map<Int, CachedAppMetadata> {
        cachedAppInfoMap?.let { return it }

        val map = mutableMapOf<Int, CachedAppMetadata>()
        try {
            val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(0)
            }

            for (pkg in packages) {
                val appInfo = pkg.applicationInfo ?: continue
                val uid = appInfo.uid

                // Eğer birden fazla paket aynı UID'yi paylaşıyorsa (sharedUserId), ilki veya birleşik isim kullanılabilir
                if (!map.containsKey(uid)) {
                    val appName = try {
                        appInfo.loadLabel(packageManager).toString()
                    } catch (_: Exception) {
                        pkg.packageName
                    }

                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    val iconBitmap = try {
                        val drawable = appInfo.loadIcon(packageManager)
                        drawableToImageBitmap(drawable)
                    } catch (e: Exception) {
                        null
                    }

                    map[uid] = CachedAppMetadata(
                        uid = uid,
                        packageName = pkg.packageName,
                        appName = appName,
                        iconBitmap = iconBitmap,
                        isSystemApp = isSystem
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "PackageManager paket okuma hatası: ${e.message}", e)
        }

        cachedAppInfoMap = map
        return map
    }

    /**
     * Sistem UID'lerini ve kaldırılmış uygulamaları kullanıcı dostu isimlendirir.
     */
    private fun resolveSpecialUid(uid: Int): Quadruple<String, String, Boolean, ImageBitmap?> {
        return when (uid) {
            0 -> Quadruple("Root Sistemi", "android.root", true, null)
            1000 -> Quadruple("Android Sistemi", "android.uid.system", true, null)
            1001 -> Quadruple("Telefon & SIM Servisleri", "android.uid.phone", true, null)
            1013 -> Quadruple("Medya Sunucusu (MediaServer)", "android.uid.media", true, null)
            1020 -> Quadruple("mDNS / Ağ Keşif Servisi", "android.uid.mdns", true, null)
            1051 -> Quadruple("Ağ Yığını (NetworkStack)", "com.android.networkstack", true, null)
            1052 -> Quadruple("Esir Ağ Girişi (CaptivePortal)", "com.android.captiveportallogin", true, null)
            -4 -> Quadruple("Kaldırılmış Uygulamalar", "android.uid.removed", false, null)
            -5 -> Quadruple("Kişisel Erişim Noktası (Hotspot)", "android.uid.tethering", true, null)
            else -> {
                if (uid >= 100000) {
                    Quadruple("Kaldırılmış Uygulama (UID: $uid)", "removed.app.$uid", false, null)
                } else {
                    Quadruple("Sistem Servisi (UID: $uid)", "android.service.$uid", true, null)
                }
            }
        }
    }

    /**
     * Drawable nesnesini Jetpack Compose için ImageBitmap formatına dönüştürür.
     */
    private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap.asImageBitmap()
            } else {
                val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
                val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap.asImageBitmap()
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun clearCache() {
        cachedAppInfoMap = null
    }

    companion object {
        private const val TAG = "DataUsageRepository"
    }
}

// 4 elemanlı yardımcı sınıf
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
