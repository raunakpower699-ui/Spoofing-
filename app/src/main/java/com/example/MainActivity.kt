package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.NativeStressService
import com.example.ui.LiquidGlassBackground
import com.example.ui.LiquidGlassCard
import com.example.ui.LiquidMetricPill
import com.example.ui.theme.GlassBorderTint
import com.example.ui.theme.GlassHighlight
import com.example.ui.theme.GlassNavyDark
import com.example.ui.theme.GlassWhiteHigh
import com.example.ui.theme.GlassWhiteLow
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MonsterAppScreen(onSelfTerminate = { finishAffinity() })
            }
        }
    }
}

@Composable
fun MonsterAppScreen(onSelfTerminate: () -> Unit) {
    val context = LocalContext.current
    val manufacturer = remember { Build.MANUFACTURER.lowercase(Locale.ROOT) }
    val isGenuineTargetDevice = remember {
        manufacturer.contains("vivo") || manufacturer.contains("iqoo")
    }

    var isBypassedForDev by remember { mutableStateOf(false) }
    var showRestrictionDialog by remember { mutableStateOf(!isGenuineTargetDevice) }

    // Service state from NativeStressService
    val monsterState by NativeStressService.serviceState.collectAsStateWithLifecycle()

    // Notification permission for Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                "Notification permission required for background governor lock.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Hardware Guardrail Alert Dialog
    if (showRestrictionDialog && !isBypassedForDev) {
        AlertDialog(
            onDismissRequest = { onSelfTerminate() },
            containerColor = GlassNavyDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = NeonAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device Incompatible",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "This application strictly requires Vivo or iQOO hardware to trigger OEM-level kernel turbo drivers.",
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Detected Manufacturer: \"${Build.MANUFACTURER}\"",
                        fontFamily = FontFamily.Monospace,
                        color = NeonAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Expected: \"vivo\" or \"iqoo\". The app will now terminate.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onSelfTerminate() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                    modifier = Modifier.testTag("terminate_button")
                ) {
                    Text("TERMINATE APP", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isBypassedForDev = true
                        showRestrictionDialog = false
                    },
                    modifier = Modifier.testTag("simulate_bypass_button")
                ) {
                    Text("SIMULATE (DEV ONLY)", color = NeonCyan, fontSize = 12.sp)
                }
            }
        )
    }

    LiquidGlassBackground(
        isBoosterActive = monsterState.isRunning,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MONSTER TURBO",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.0.sp,
                        color = Color.White
                    )
                    Text(
                        text = "VIVO / iQOO GOVERNOR PINNER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = NeonCyan
                    )
                }

                // Status Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            1.dp,
                            if (monsterState.isRunning) NeonEmerald else Color(0x33FFFFFF),
                            RoundedCornerShape(20.dp)
                        )
                        .background(if (monsterState.isRunning) Color(0x2200E676) else GlassWhiteLow)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (monsterState.isRunning) NeonEmerald else Color(0xFF64748B))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (monsterState.isRunning) formatTime(monsterState.uptimeSeconds) else "STANDBY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (monsterState.isRunning) NeonEmerald else Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hardware Verification Banner
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderGlowColor = if (isGenuineTargetDevice) NeonEmerald else NeonAmber
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGenuineTargetDevice) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (isGenuineTargetDevice) NeonEmerald else NeonAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isGenuineTargetDevice) "VIVO / iQOO HARDWARE VERIFIED" else "SIMULATED / EMULATOR ENVIRONMENT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGenuineTargetDevice) NeonEmerald else NeonAmber
                        )
                        Text(
                            text = "Brand: ${Build.MANUFACTURER.uppercase(Locale.ROOT)} | Model: ${Build.MODEL}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Central Pulsing Monster Action Button
            MonsterToggleButton(
                isRunning = monsterState.isRunning,
                onClick = {
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    if (monsterState.isRunning) {
                        NativeStressService.stopService(context)
                    } else {
                        NativeStressService.startService(context)
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Real-Time Hardware Status Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LiquidMetricPill(
                        label = "Native Engine",
                        value = if (monsterState.isRunning) "C++ POSIX Active" else "Engine Idle",
                        indicatorColor = if (monsterState.isRunning) NeonCyan else Color(0xFF64748B),
                        modifier = Modifier.weight(1f)
                    )

                    LiquidMetricPill(
                        label = "Locked Cores",
                        value = if (monsterState.isRunning) "${monsterState.activeCoreCount} Threads Active" else "${monsterState.activeCoreCount} Cores Ready",
                        indicatorColor = if (monsterState.isRunning) NeonPurple else Color(0xFF64748B),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LiquidMetricPill(
                        label = "WakeLock Guard",
                        value = if (monsterState.wakeLockAcquired) "Active (120 min max)" else "Released",
                        indicatorColor = if (monsterState.wakeLockAcquired) NeonEmerald else Color(0xFF64748B),
                        modifier = Modifier.weight(1f)
                    )

                    LiquidMetricPill(
                        label = "Driver Spoof",
                        value = "com.antutu.ABenchMark",
                        indicatorColor = NeonMagenta,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Technical Architecture Explainer Card
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SYSTEM ENGINE HIGHLIGHTS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.0.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                ArchitectureBullet(
                    title = "Native POSIX Threads (C++)",
                    desc = "Direct std::thread deployment querying std::thread::hardware_concurrency() with unrolled math loops to lock core clocks."
                )
                Spacer(modifier = Modifier.height(8.dp))
                ArchitectureBullet(
                    title = "120-Min Partial WakeLock",
                    desc = "PowerManager.PARTIAL_WAKE_LOCK ensures SoC prime and performance clusters never sleep during gaming sessions."
                )
                Spacer(modifier = Modifier.height(8.dp))
                ArchitectureBullet(
                    title = "AnTuTu Package Signature",
                    desc = "applicationId = 'com.antutu.ABenchMark' triggers Vivo/iQOO driver thermal relaxation and maximum governor burst."
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun MonsterToggleButton(
    isRunning: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "monster_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRunning) 700 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val activeGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "active_glow"
    )

    val buttonShape = RoundedCornerShape(32.dp)

    val buttonGradient = if (isRunning) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFF1744),
                Color(0xFFFF5252),
                Color(0xFFFF1744)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                NeonCyan,
                NeonPurple,
                NeonMagenta
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .scale(if (isRunning) pulseScale else 1.0f)
            .clip(buttonShape)
            .border(
                width = if (isRunning) 2.dp else 1.dp,
                brush = Brush.horizontalGradient(
                    colors = if (isRunning) {
                        listOf(Color(0xFFFF1744).copy(alpha = activeGlowAlpha), Color.White)
                    } else {
                        listOf(NeonCyan, NeonMagenta)
                    }
                ),
                shape = buttonShape
            )
            .background(brush = buttonGradient, shape = buttonShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .padding(horizontal = 24.dp)
            .testTag("monster_toggle_button"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.Bolt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = if (isRunning) "STOP MONSTER MODE" else "START MONSTER MODE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = Color.White
                )
                Text(
                    text = if (isRunning) "Tap to release CPU locks & native threads" else "Tap to trigger C++ multi-core stress",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF1F5F9).copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun ArchitectureBullet(title: String, desc: String) {
    Column {
        Text(
            text = "• $title",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan
        )
        Text(
            text = desc,
            fontSize = 11.sp,
            color = Color(0xFFCBD5E1),
            lineHeight = 15.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
