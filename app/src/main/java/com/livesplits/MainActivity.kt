package com.livesplits

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.livesplits.data.settings.SettingsRepository
import com.livesplits.service.TimerOverlayService
import com.livesplits.ui.navigation.NavigationGraph
import com.livesplits.ui.theme.LiveSplitsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Overlay permission is required for the timer",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Notification permission is required for the timer",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            LiveSplitsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavigationGraph(navController = navController)
                }
            }
        }

        // Check if timer service is running and show dialog
        checkRunningTimer()
    }

    private fun checkRunningTimer() {
        lifecycleScope.launch {
            val (categoryId, segments, startTime) = settingsRepository.getCurrentTimerInfo()
            if (categoryId != null) {
                // Timer is running, show dialog to close it
                showTimerRunningDialog(categoryId)
            }
        }
    }

    private fun showTimerRunningDialog(categoryId: Long) {
        // Show dialog asking if user wants to close the timer
        // This would be implemented with a Compose dialog
        Toast.makeText(this, "Timer is still running", Toast.LENGTH_SHORT).show()
    }

    fun requestOverlayPermissionAndStartTimer(categoryId: Long, segments: List<Long>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                startTimerService(categoryId, segments)
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(Manifest.permission.SYSTEM_ALERT_WINDOW)
                startActivity(intent)
            }
        } else {
            startTimerService(categoryId, segments)
        }
    }

    private fun startTimerService(categoryId: Long? = null, segments: List<Long>? = null) {
        // The service will be started by the ViewModel when launching timer
        Toast.makeText(this, "Starting timer...", Toast.LENGTH_SHORT).show()
    }
}
