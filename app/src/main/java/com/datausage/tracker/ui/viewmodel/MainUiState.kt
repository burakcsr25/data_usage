package com.datausage.tracker.ui.viewmodel

import com.datausage.tracker.data.model.AppDataUsage
import com.datausage.tracker.data.model.NetworkTypeFilter
import com.datausage.tracker.data.model.OverallStats
import com.datausage.tracker.data.model.SortOption
import com.datausage.tracker.data.model.TimeRange

data class MainUiState(
    val isLoading: Boolean = true,
    val isPermissionGranted: Boolean = false,
    val allApps: List<AppDataUsage> = emptyList(),
    val filteredApps: List<AppDataUsage> = emptyList(),
    val overallStats: OverallStats = OverallStats(),
    val selectedTimeRange: TimeRange = TimeRange.Today,
    val selectedNetworkFilter: NetworkTypeFilter = NetworkTypeFilter.ALL,
    val selectedSortOption: SortOption = SortOption.USAGE_DESC,
    val searchQuery: String = "",
    val selectedAppForDetail: AppDataUsage? = null,
    val showCustomDateDialog: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)
