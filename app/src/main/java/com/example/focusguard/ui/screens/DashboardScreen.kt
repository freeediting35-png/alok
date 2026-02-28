package com.example.focusguard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.Icons
import com.example.focusguard.data.entity.User
import com.example.focusguard.data.entity.UserRole
import com.example.focusguard.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: User,
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FocusGuard Dashboard") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(text = "Hello, ${user.name}!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (user.role == UserRole.CHILD || user.role == UserRole.SELF) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Current Balance", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "₹${user.balance}",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Text("Manage your children's profiles below.")
                // Parent view logic here
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            if (user.role == UserRole.PARENT || user.role == UserRole.SELF) {
                StudyScheduleCard(user, onSaveSchedule = { start, end ->
                    val updatedUser = user.copy(studyStartHour = start, studyEndHour = end)
                    viewModel.saveUser(updatedUser)
                })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Recent Activity", style = MaterialTheme.typography.titleMedium)
            // List violations or transactions here

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { /* Check permissions status */ }) {
                Text("Verify Permissions")
            }
        }
    }
}

@Composable
fun StudyScheduleCard(user: User, onSaveSchedule: (Int, Int) -> Unit) {
    var startHour by remember(user) { mutableStateOf(if (user.studyStartHour != -1) user.studyStartHour.toString() else "") }
    var endHour by remember(user) { mutableStateOf(if (user.studyEndHour != -1) user.studyEndHour.toString() else "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Study Schedule (24h)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startHour,
                    onValueChange = { startHour = it },
                    label = { Text("Start") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endHour,
                    onValueChange = { endHour = it },
                    label = { Text("End") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val s = startHour.toIntOrNull() ?: -1
                    val e = endHour.toIntOrNull() ?: -1
                    onSaveSchedule(s, e)
                },
                modifier = Modifier.align(androidx.compose.ui.Alignment.End)
            ) {
                Text("Save Schedule")
            }
        }
    }
}
