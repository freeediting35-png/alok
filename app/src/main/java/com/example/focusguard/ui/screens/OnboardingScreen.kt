package com.example.focusguard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.focusguard.data.entity.User
import com.example.focusguard.data.entity.UserRole

@Composable
fun OnboardingScreen(onSetupComplete: (User) -> Unit) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.PARENT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to FocusGuard", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Enter your name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("Select Mode:")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            UserRole.values().forEach { role ->
                RadioButton(
                    selected = (role == selectedRole),
                    onClick = { selectedRole = role }
                )
                Text(
                    text = role.name,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("Set 4-digit PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val newUser = User(
                    name = name,
                    role = selectedRole,
                    pinHash = pin,
                    balance = if (selectedRole == UserRole.PARENT) 500.0 else 0.0 // Parent starts with balance? No, balance is for child. Parent sets it.
                    // If Self mode, balance starts at 0 or user sets it.
                    // If Parent mode, we create parent user. Then parent adds child.
                    // For simplicity, we create the primary user profile here.
                )
                onSetupComplete(newUser)
            },
            enabled = name.isNotBlank() && pin.length >= 4,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Complete Setup")
        }
    }
}
