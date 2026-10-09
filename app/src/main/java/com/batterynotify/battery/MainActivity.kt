package com.batterynotify.battery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.batterynotify.battery.ui.main.DashboardRoute
import com.batterynotify.battery.ui.settings.SettingsRoute
import com.batterynotify.battery.ui.theme.BatteryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var showSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BatteryTheme {
                if (showSettings) {
                    SettingsRoute(onBack = { showSettings = false })
                } else {
                    DashboardRoute(onOpenSettings = { showSettings = true })
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}