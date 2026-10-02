package com.poolaimguide.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poolaimguide.app.service.FloatingOverlayService
import com.poolaimguide.app.ui.theme.PoolAimGuideTheme

/**
 * Main Activity for Pool Aim Guide.
 * Handles overlay permissions and controls the floating overlay service lifecycle.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PoolAimGuideTheme {
                MainScreen(
                    onStartService = { startOverlayService() },
                    onStopService = { stopOverlayService() },
                    onRequestPermission = { requestOverlayPermission() }
                )
            }
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        if (!hasOverlayPermission()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        } else {
            Toast.makeText(this, "Overlay permission already granted!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startOverlayService() {
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
            return
        }
        val intent = Intent(this, FloatingOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Aim Guide Overlay Started!", Toast.LENGTH_SHORT).show()
    }

    private fun stopOverlayService() {
        val intent = Intent(this, FloatingOverlayService::class.java)
        stopService(intent)
        Toast.makeText(this, "Aim Guide Overlay Stopped", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQ_CODE = 1001
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    var isPermissionGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎱 Pool Aim Guide", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPermissionGranted) Color(0xFF1E3A2F) else Color(0xFF3E2723)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isPermissionGranted) "✓ Overlay Permission Granted" else "⚠ Permission Required",
                        color = if (isPermissionGranted) Color(0xFF4ADE80) else Color(0xFFFF8A80),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Allows displaying floating aim lines above 8 Ball Pool and other billiards games.",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                    if (!isPermissionGranted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onRequestPermission) {
                            Text("Grant Permission in Settings")
                        }
                    }
                }
            }

            // Service Launch Buttons
            Button(
                onClick = onStartService,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Start Floating Aim Guide", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onStopService,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Stop Overlay Service", fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Instructions Guide
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("How to Use (Phase 1)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Tap 'Start Floating Aim Guide' to show floating bubble above your games.")
                    Text("2. Open 8 Ball Pool or your favorite pool game.")
                    Text("3. Tap the floating bubble to expand the Aim Guide overlay.")
                    Text("4. Manual 2-Point Mode: Tap the Cue Ball, then tap the Target Ball.")
                    Text("5. Three accurate geometry lines will instantly project:")
                    Text("   • Line 1: Long aim line through target ball")
                    Text("   • Line 2: Object ball path along center line")
                    Text("   • Line 3: Cue ball 90° tangent deflection line")
                    Text("6. Tap 'Pass-Through' on the panel to let your shots pass to the game!")
                }
            }
        }
    }
}