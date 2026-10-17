package com.datausage.tracker.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.datausage.tracker.data.model.AppDataUsage
import com.datausage.tracker.ui.components.AppDetailBottomSheet
import com.datausage.tracker.ui.components.AppUsageListItem
import com.datausage.tracker.ui.components.CustomDateRangeDialog
import com.datausage.tracker.ui.components.FilterAndSearchBar
import com.datausage.tracker.ui.components.PermissionRequiredDialog
import com.datausage.tracker.ui.components.SummaryHeaderCard
import com.datausage.tracker.ui.components.TimeRangeChips
import com.datausage.tracker.ui.viewmodel.MainViewModel
import com.datausage.tracker.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showPermissionDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Kullanıcı ayarlardan döndüğünde izni ve verileri otomatik güncelle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionAndLoadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Veri Kullanımı Takibi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !uiState.isLoading && uiState.isPermissionGranted
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Yenile"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // 1. İZİN VERİLMEMİŞSE
                !uiState.isPermissionGranted -> {
                    PermissionBlockedContent(
                        onOpenSettings = {
                            PermissionUtils.openUsageAccessSettings(context)
                        },
                        onShowDialog = { showPermissionDialog = true }
                    )
                }

                // 2. İLK YÜKLEME SIRASINDA
                uiState.isLoading && uiState.allApps.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ağ istatistikleri ve uygulamalar taranıyor…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 3. VERİ LİSTELEME
                else -> {
                    val maxUsageBytes = remember(uiState.filteredApps, uiState.selectedNetworkFilter) {
                        uiState.filteredApps.maxOfOrNull { it.getUsageForFilter(uiState.selectedNetworkFilter) } ?: 1L
                    }

                    val listState = rememberLazyListState()

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // En Üst: Özet Kartı
                        item(key = "summary_header") {
                            SummaryHeaderCard(
                                overallStats = uiState.overallStats,
                                timeRange = uiState.selectedTimeRange
                            )
                        }

                        // Zaman Aralığı Seçici
                        item(key = "time_range") {
                            TimeRangeChips(
                                selectedRange = uiState.selectedTimeRange,
                                onRangeSelected = { viewModel.setTimeRange(it) },
                                onCustomRangeClicked = { viewModel.setShowCustomDateDialog(true) }
                            )
                        }

                        // Arama ve Ağ / Sıralama Filtresi
                        item(key = "filters_bar") {
                            FilterAndSearchBar(
                                searchQuery = uiState.searchQuery,
                                onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                                selectedNetworkFilter = uiState.selectedNetworkFilter,
                                onNetworkFilterChanged = { viewModel.setNetworkFilter(it) },
                                selectedSortOption = uiState.selectedSortOption,
                                onSortOptionChanged = { viewModel.setSortOption(it) }
                            )
                        }

                        // Uygulama Sayısı Bilgisi
                        item(key = "app_count") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${uiState.filteredApps.size} Uygulama Listelendi",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (uiState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                }
                            }
                        }

                        // Boş Durum
                        if (uiState.filteredApps.isEmpty()) {
                            item(key = "empty_state") {
                                EmptyDataContent()
                            }
                        } else {
                            // Uygulama Listesi
                            items(
                                items = uiState.filteredApps,
                                key = { it.uid }
                            ) { app ->
                                AppUsageListItem(
                                    app = app,
                                    networkFilter = uiState.selectedNetworkFilter,
                                    maxUsageBytes = maxUsageBytes,
                                    onClick = { viewModel.selectAppForDetail(app) }
                                )
                            }
                        }
                    }
                }
            }

            // Hata Mesajı Banner'ı
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                uiState.errorMessage?.let { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    // Detay BottomSheet
    if (uiState.selectedAppForDetail != null) {
        AppDetailBottomSheet(
            app = uiState.selectedAppForDetail,
            sheetState = sheetState,
            onDismissRequest = { viewModel.selectAppForDetail(null) }
        )
    }

    // Özel Tarih Seçici Dialog
    if (uiState.showCustomDateDialog) {
        CustomDateRangeDialog(
            onDismissRequest = { viewModel.setShowCustomDateDialog(false) },
            onDateRangeSelected = { start, end ->
                viewModel.setCustomDateRange(start, end)
            }
        )
    }

    // İzin Açıklama Dialog'u
    if (showPermissionDialog) {
        PermissionRequiredDialog(
            onGrantClicked = {
                showPermissionDialog = false
                PermissionUtils.openUsageAccessSettings(context)
            },
            onDismissClicked = { showPermissionDialog = false }
        )
    }
}

@Composable
private fun PermissionBlockedContent(
    onOpenSettings: () -> Unit,
    onShowDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Kullanım Erişimi İzni Gerekiyor",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Honor (MagicOS) cihazınızdaki yüklü uygulamaların detaylı Mobil ve Wi-Fi veri tüketimini listeleyebilmek için 'Kullanım Erişimi' iznini açmanız gerekmektedir.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Ayarları Aç ve İzin Ver", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onShowDialog,
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("Neden bu izin gerekli?")
        }
    }
}

@Composable
private fun EmptyDataContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Veri Tüketimi Bulunamadı",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Seçilen filtre ve zaman aralığı için herhangi bir ağ aktivitesi kaydedilmemiş.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
