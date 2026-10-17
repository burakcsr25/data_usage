package com.datausage.tracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.datausage.tracker.data.model.AppDataUsage
import com.datausage.tracker.data.model.NetworkTypeFilter
import com.datausage.tracker.data.model.SortOption
import com.datausage.tracker.data.model.TimeRange
import com.datausage.tracker.data.repository.DataUsageRepository
import com.datausage.tracker.util.FormatUtils
import com.datausage.tracker.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DataUsageRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoadData()
    }

    /**
     * İzin durumunu kontrol eder ve verilmişse verileri arka planda yükler.
     */
    fun checkPermissionAndLoadData(forceRefresh: Boolean = false) {
        val context = getApplication<Application>().applicationContext
        val hasPermission = PermissionUtils.hasUsageStatsPermission(context)

        if (!hasPermission) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isPermissionGranted = false,
                    isRefreshing = false
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isPermissionGranted = true,
                isLoading = if (forceRefresh) it.isLoading else it.allApps.isEmpty(),
                isRefreshing = forceRefresh,
                errorMessage = null
            )
        }

        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentRange = _uiState.value.selectedTimeRange
                val result = repository.getUsageData(currentRange)

                val filtered = applyFilterAndSort(
                    list = result.apps,
                    networkFilter = _uiState.value.selectedNetworkFilter,
                    sortOption = _uiState.value.selectedSortOption,
                    query = _uiState.value.searchQuery
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        allApps = result.apps,
                        filteredApps = filtered,
                        overallStats = result.overallStats,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Veriler alınırken hata oluştu: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }

    fun setTimeRange(timeRange: TimeRange) {
        if (_uiState.value.selectedTimeRange == timeRange) return
        _uiState.update { it.copy(selectedTimeRange = timeRange, isLoading = true) }
        loadData()
    }

    fun setNetworkFilter(filter: NetworkTypeFilter) {
        val currentState = _uiState.value
        if (currentState.selectedNetworkFilter == filter) return

        val filtered = applyFilterAndSort(
            list = currentState.allApps,
            networkFilter = filter,
            sortOption = currentState.selectedSortOption,
            query = currentState.searchQuery
        )

        _uiState.update {
            it.copy(
                selectedNetworkFilter = filter,
                filteredApps = filtered
            )
        }
    }

    fun setSortOption(sortOption: SortOption) {
        val currentState = _uiState.value
        if (currentState.selectedSortOption == sortOption) return

        val filtered = applyFilterAndSort(
            list = currentState.allApps,
            networkFilter = currentState.selectedNetworkFilter,
            sortOption = sortOption,
            query = currentState.searchQuery
        )

        _uiState.update {
            it.copy(
                selectedSortOption = sortOption,
                filteredApps = filtered
            )
        }
    }

    fun setSearchQuery(query: String) {
        val currentState = _uiState.value
        val filtered = applyFilterAndSort(
            list = currentState.allApps,
            networkFilter = currentState.selectedNetworkFilter,
            sortOption = currentState.selectedSortOption,
            query = query
        )

        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredApps = filtered
            )
        }
    }

    fun selectAppForDetail(app: AppDataUsage?) {
        _uiState.update { it.copy(selectedAppForDetail = app) }
    }

    fun setShowCustomDateDialog(show: Boolean) {
        _uiState.update { it.copy(showCustomDateDialog = show) }
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        val label = FormatUtils.formatDateRange(startMillis, endMillis)
        val customRange = TimeRange.Custom(startMillis, endMillis, label)
        setShowCustomDateDialog(false)
        setTimeRange(customRange)
    }

    fun refresh() {
        repository.clearCache()
        checkPermissionAndLoadData(forceRefresh = true)
    }

    private fun applyFilterAndSort(
        list: List<AppDataUsage>,
        networkFilter: NetworkTypeFilter,
        sortOption: SortOption,
        query: String
    ): List<AppDataUsage> {
        return list.asSequence()
            .filter { app ->
                // Ağ filtresine göre 0 bayttan büyük olanlar
                when (networkFilter) {
                    NetworkTypeFilter.ALL -> app.totalBytes > 0
                    NetworkTypeFilter.MOBILE_ONLY -> app.totalMobileBytes > 0
                    NetworkTypeFilter.WIFI_ONLY -> app.totalWifiBytes > 0
                }
            }
            .filter { app ->
                // Arama sorgusu filtresi
                if (query.isBlank()) true
                else {
                    app.appName.contains(query, ignoreCase = true) ||
                            app.packageName.contains(query, ignoreCase = true)
                }
            }
            .sortedWith { a, b ->
                when (sortOption) {
                    SortOption.USAGE_DESC -> {
                        b.getUsageForFilter(networkFilter).compareTo(a.getUsageForFilter(networkFilter))
                    }
                    SortOption.DOWNLOAD_DESC -> {
                        b.getRxForFilter(networkFilter).compareTo(a.getRxForFilter(networkFilter))
                    }
                    SortOption.UPLOAD_DESC -> {
                        b.getTxForFilter(networkFilter).compareTo(a.getTxForFilter(networkFilter))
                    }
                    SortOption.NAME_ASC -> {
                        a.appName.lowercase(Locale.getDefault()).compareTo(b.appName.lowercase(Locale.getDefault()))
                    }
                }
            }
            .toList()
    }
}
