# Veri Kullanımı Takibi (Android / Jetpack Compose)

Honor (Android 14 / MagicOS) ve tüm modern Android cihazları için geliştirilmiş, yüklü uygulamaların detaylı **Mobil** ve **Wi-Fi** veri tüketimini (Download/Rx, Upload/Tx, Ön Plan/Arka Plan) listeleyen bağımsız bir Android uygulamasıdır.

---

## 🚀 Temel Özellikler

1. **İzin Yönetimi & Güvenlik:**
   - Android sisteminin `PACKAGE_USAGE_STATS` (Kullanım Erişimi) iznini `AppOpsManager` üzerinden dinamik olarak doğrular.
   - İzin verilmemişse kullanıcı dostu bir yönlendirme kartı ve açıklama diyalogu sunar; doğrudan `Settings.ACTION_USAGE_ACCESS_SETTINGS` ekranına yönlendirir.
   - Kullanıcı sistem ayarlarından izin verip uygulamaya döndüğünde `LifecycleEventObserver (ON_RESUME)` ile izni otomatik algılar ve UI'ı anında günceller.

2. **NetworkStats & Veri Okuma Motoru:**
   - Android `NetworkStatsManager` API'ını kullanarak `queryDetails` ile her bir uygulamanın (UID):
     - **İndirilen Veri (Rx Bytes)**
     - **Yüklenen Veri (Tx Bytes)**
     - **Ön Plan Verisi (`NetworkStats.Bucket.STATE_FOREGROUND`)**
     - **Arka Plan Verisi (`NetworkStats.Bucket.STATE_DEFAULT`)**
     ayrımını hassasiyetle hesaplar.
   - `ConnectivityManager.TYPE_MOBILE` ve `ConnectivityManager.TYPE_WIFI` verilerini ayrı ayrı ve birleşik analiz eder.
   - Honor MagicOS ve Android 14 üzerindeki `SecurityException` ve `RemoteException` kısıtlamalarını ele alan dayanıklı hata yönetimi.

3. **Yüklü Uygulama & Sistem UID Eşleme:**
   - `PackageManager` üzerinden cihazdaki tüm uygulamaların isim, paket adı ve ikonlarını çeker.
   - Listeleme performansını 120 FPS akıcılığında tutmak için `Drawable` nesnelerini arka planda (`Dispatchers.IO`) Compose `ImageBitmap` formatına dönüştürüp önbelleğe alır.
   - Özel UID'leri akıllıca etiketler:
     - UID 1000: *Android Sistemi*
     - UID 1001: *Telefon & SIM Servisleri*
     - UID -4 / 100000+: *Kaldırılmış Uygulamalar*
     - UID -5: *Kişisel Erişim Noktası (Hotspot)*

4. **Kullanıcı Arayüzü (Jetpack Compose & Material 3):**
   - **Özet Kartı:** Toplam tüketim (Mobil + Wi-Fi), Mobil ve Wi-Fi ayrı kutucukları, Ön Plan vs Arka Plan oranı.
   - **Zaman Aralığı Seçici:** *Bugün*, *Son 7 Gün*, *Bu Ay* ve Material 3 `DateRangePicker` destekli *Özel Aralık*.
   - **Arama & Filtreleme:** Uygulama veya paket adına göre anlık filtreleme, *Tümü / Mobil / Wi-Fi* seçimi ve *En Çok Tüketen*, *Download (Rx)*, *Upload (Tx)*, *İsme Göre (A-Z)* sıralama seçenekleri.
   - **Detay BottomSheet:** Seçilen uygulamanın tüm Mobil/Wi-Fi ve Ön/Arka plan dökümünü, grafiksel çift renkli oran çubuğunu ve tek tıkla sistemin uygulama veri ayarlarını açan butonu içerir.

---

## 📂 Proje Dizin Yapısı

```
app/src/main/
├── AndroidManifest.xml
├── java/com/datausage/tracker/
│   ├── MainActivity.kt
│   ├── DataUsageApp.kt
│   ├── data/
│   │   ├── model/
│   │   │   ├── AppDataUsage.kt
│   │   │   ├── NetworkTypeFilter.kt
│   │   │   ├── OverallStats.kt
│   │   │   ├── SortOption.kt
│   │   │   └── TimeRange.kt
│   │   └── repository/
│   │       └── DataUsageRepository.kt
│   ├── util/
│   │   ├── FormatUtils.kt
│   │   ├── NetworkStatsHelper.kt
│   │   └── PermissionUtils.kt
│   └── ui/
│       ├── theme/
│       │   ├── Color.kt
│       │   ├── Theme.kt
│       │   └── Type.kt
│       ├── viewmodel/
│       │   ├── MainViewModel.kt
│       │   └── MainUiState.kt
│       ├── components/
│       │   ├── AppDetailBottomSheet.kt
│       │   ├── AppUsageListItem.kt
│       │   ├── CustomDateRangeDialog.kt
│       │   ├── FilterAndSearchBar.kt
│       │   ├── PermissionDialog.kt
│       │   ├── SummaryHeaderCard.kt
│       │   └── TimeRangeChips.kt
│       └── screen/
│           └── MainScreen.kt
└── res/
    ├── values/ (strings.xml, colors.xml, themes.xml)
    ├── drawable/
    └── mipmap-anydpi-v26/
```

---

## 🛠️ Nasıl Çalıştırılır?

1. **Android Studio** (Hedgehog, Iguana, Koala veya daha yeni sürüm) açın.
2. `Open` seçeneğiyle bu proje klasörünü (`android veri kullanımı`) seçin.
3. Gradle senkronizasyonunun (Sync Project with Gradle Files) tamamlanmasını bekleyin.
4. Honor cihazınızı USB Hata Ayıklama (USB Debugging) modunda bilgisayara bağlayın veya bir Android 14 emülatörü başlatın.
5. `Run 'app'` butonuna basarak uygulamayı yükleyin.
6. Uygulama ilk açıldığında gösterilen "Ayarları Aç ve İzin Ver" butonuna dokunarak **Kullanım Erişimi (Usage Access)** iznini etkinleştirin.
