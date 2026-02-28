package com.example.focusguard.ui.screens

import android.content.pm.ApplicationInfo
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.focusguard.data.entity.AppRule
import com.example.focusguard.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val installedApps by viewModel.installedApps.observeAsState(emptyList())
    val rules by viewModel.rules.observeAsState(emptyList())

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage App Rules") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(installedApps, key = { it.packageName }) { appInfo ->
                AppRuleItem(
                    appInfo = appInfo,
                    existingRule = rules.find { it.packageName == appInfo.packageName },
                    onSaveRule = { rule -> viewModel.saveRule(rule) }
                )
            }
        }
    }
}

@Composable
fun AppRuleItem(
    appInfo: ApplicationInfo,
    existingRule: AppRule?,
    onSaveRule: (AppRule) -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val appName = remember(appInfo) { packageManager.getApplicationLabel(appInfo).toString() }
    val icon = remember(appInfo) { packageManager.getApplicationIcon(appInfo).toBitmap().asImageBitmap() }

    var isDoomscrollMonitored by remember(existingRule) { mutableStateOf(existingRule?.isDoomscrollMonitored ?: false) }
    var dailyLimit by remember(existingRule) { mutableStateOf(existingRule?.dailyLimitMinutes?.toString() ?: "0") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    bitmap = icon,
                    contentDescription = appName,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = appName, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isDoomscrollMonitored,
                    onCheckedChange = { checked ->
                        isDoomscrollMonitored = checked
                        val newRule = existingRule?.copy(isDoomscrollMonitored = checked)
                            ?: AppRule(
                                packageName = appInfo.packageName,
                                appName = appName,
                                isDoomscrollMonitored = checked,
                                dailyLimitMinutes = dailyLimit.toIntOrNull() ?: 0
                            )
                        onSaveRule(newRule)
                    }
                )
                Text("Monitor Scrolling")
            }

            OutlinedTextField(
                value = dailyLimit,
                onValueChange = {
                    dailyLimit = it
                    val limit = it.toIntOrNull()
                    if (limit != null) {
                         val newRule = existingRule?.copy(dailyLimitMinutes = limit)
                            ?: AppRule(
                                packageName = appInfo.packageName,
                                appName = appName,
                                isDoomscrollMonitored = isDoomscrollMonitored,
                                dailyLimitMinutes = limit
                            )
                         onSaveRule(newRule)
                    }
                },
                label = { Text("Daily Limit (mins)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
