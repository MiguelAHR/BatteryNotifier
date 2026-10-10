package com.batterynotify.battery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.batterynotify.battery.ui.main.DashboardRoute
import com.batterynotify.battery.ui.permissions.PermissionsHelper
import com.batterynotify.battery.ui.settings.SettingsRoute
import com.batterynotify.battery.ui.theme.BatteryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var showSettings = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // No se bloquea la UI: si el usuario deniega, verá el aviso
        // en el dashboard y podrá concederlo desde Ajustes
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()
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

    private fun requestRequiredPermissions() {
        val missing = PermissionsHelper.requiredRuntimePermissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                it
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
