package com.datausage.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.datausage.tracker.ui.screen.MainScreen
import com.datausage.tracker.ui.theme.DataUsageTrackerTheme
import com.datausage.tracker.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DataUsageTrackerTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
