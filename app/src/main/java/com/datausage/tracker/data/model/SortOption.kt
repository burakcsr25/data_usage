package com.datausage.tracker.data.model

enum class SortOption(val title: String) {
    USAGE_DESC("En Çok Tüketen"),
    DOWNLOAD_DESC("En Çok İndirme (Rx)"),
    UPLOAD_DESC("En Çok Yükleme (Tx)"),
    NAME_ASC("Uygulama Adı (A-Z)")
}
