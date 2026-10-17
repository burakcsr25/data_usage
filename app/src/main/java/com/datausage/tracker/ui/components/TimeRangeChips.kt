package com.datausage.tracker.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.datausage.tracker.data.model.TimeRange

@Composable
fun TimeRangeChips(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    onCustomRangeClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Bugün
        FilterChip(
            selected = selectedRange is TimeRange.Today,
            onClick = { onRangeSelected(TimeRange.Today) },
            label = { Text("Bugün") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Son 7 Gün
        FilterChip(
            selected = selectedRange is TimeRange.Last7Days,
            onClick = { onRangeSelected(TimeRange.Last7Days) },
            label = { Text("Son 7 Gün") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Bu Ay
        FilterChip(
            selected = selectedRange is TimeRange.ThisMonth,
            onClick = { onRangeSelected(TimeRange.ThisMonth) },
            label = { Text("Bu Ay") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Özel Tarih Aralığı
        FilterChip(
            selected = selectedRange is TimeRange.Custom,
            onClick = {
                onCustomRangeClicked()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            label = {
                Text(
                    text = if (selectedRange is TimeRange.Custom) selectedRange.displayName else "Özel Aralık"
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}
