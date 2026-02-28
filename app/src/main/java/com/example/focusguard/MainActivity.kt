package com.example.focusguard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.example.focusguard.data.entity.User
import com.example.focusguard.ui.screens.DashboardScreen
import com.example.focusguard.ui.screens.OnboardingScreen
import com.example.focusguard.viewmodel.MainViewModel
import com.example.focusguard.service.MonitoringWorker

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scheduleMonitoring()

        setContent {
            val userState by viewModel.user.observeAsState()
            var showSettings by remember { mutableStateOf(false) }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (userState == null) {
                        OnboardingScreen(onSetupComplete = { newUser ->
                            viewModel.saveUser(newUser)
                            checkPermissions()
                        })
                    } else {
                        if (showSettings) {
                            com.example.focusguard.ui.screens.SettingsScreen(
                                viewModel = viewModel,
                                onBack = { showSettings = false }
                            )
                        } else {
                            DashboardScreen(
                                user = userState!!,
                                viewModel = viewModel,
                                onOpenSettings = { showSettings = true }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        // Direct user to Usage Stats permission if needed
        // Direct user to Accessibility Service if needed
        // Direct user to Device Admin if needed
        // This should be done in a guided flow. For now, we assume user does it via system settings or we launch intents.
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun scheduleMonitoring() {
        val workRequest = PeriodicWorkRequestBuilder<MonitoringWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "FocusGuardMonitoring",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
