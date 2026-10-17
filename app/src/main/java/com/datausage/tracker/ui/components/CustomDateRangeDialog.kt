package com.datausage.tracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangeDialog(
    onDismissRequest: () -> Unit,
    onDateRangeSelected: (startMillis: Long, endMillis: Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis ?: start
                    if (start != null && end != null) {
                        // Seçilen bitiş gününün sonuna kadar (23:59:59) kapsayacak şekilde
                        val endOfDay = end + (24 * 60 * 60 * 1000L) - 1L
                        onDateRangeSelected(start, endOfDay)
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null
            ) {
                Text("Uygula")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("İptal")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "Tarih Aralığı Seçiniz",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            },
            headline = {
                Text(
                    text = "Ağ verisi başlangıç ve bitiş tarihi",
                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
                )
            },
            showModeToggle = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
        )
    }
}
