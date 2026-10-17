package com.datausage.tracker.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log

object PermissionUtils {
    private const val TAG = "PermissionUtils"

    /**
     * PACKAGE_USAGE_STATS izninin AppOps üzerinden verilip verilmediğini doğrular.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "İzin kontrol hatası: ${e.message}", e)
            return false
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Kullanıcıyı Kullanım Erişimi (Usage Access) izin sayfasına yönlendirir.
     */
    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Usage settings açılamadı, genel ayarlar açılıyor: ${e.message}", e)
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Genel ayarlar da açılamadı: ${ex.message}", ex)
            }
        }
    }

    /**
     * Belirli bir uygulamanın sistem detay sayfasını (Arka plan veri kısıtlama vb.) açar.
     */
    fun openAppSettings(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Uygulama ayarları açılamadı: ${e.message}", e)
        }
    }
}
