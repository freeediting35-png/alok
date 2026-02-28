package com.example.focusguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Restart monitoring or any service that needs manual start
            // Accessibility service starts automatically if enabled by user
            // WorkManager jobs persist across reboots automatically
        }
    }
}
