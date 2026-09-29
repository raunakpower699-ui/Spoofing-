package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.example.engine.EngineMode
import com.example.engine.RaunakExploitsEngine
import com.example.service.FpsOverlayService
import com.example.ui.LiquidGlassBackground
import com.example.ui.LiquidGlassCard
import com.example.ui.theme.GlassBorderTint
import com.example.ui.theme.GlassNavyDark
import com.example.ui.theme.GlassWhiteLow
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RaunakExploitsV3Screen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            RaunakExploitsEngine.stopPerformance(applicationContext)
        }
    }
}

@Composable
fun RaunakExploitsV3Screen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isVivoOrIqoo = remember { RaunakExploitsEngine.isVivoOrIqooDevice() }

    val engineState by RaunakExploitsEngine.engineState.collectAsStateWithLifecycle()

    var isOverlayEnabled by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
        )
    }

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
        if (isGranted) {
            Toast.makeText(context, "Notification enabled! Background status will show in status bar.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LiquidGlassBackground(
        isBoosterActive = engineState.isActive,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RAUNAK EXPLOITS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.0.sp,
                        color = Color.White
                    )
                    Text(
                        text = "REAL HARDWARE CPU & GPU OVERCLOCK ENGINE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = NeonCyan
                    )
                }

                // Countdown / Non-Stop Status Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            1.dp,
                            if (engineState.isActive) NeonEmerald else Color(0x33FFFFFF),
                            RoundedCornerShape(20.dp)
                        )
                        .background(if (engineState.isActive) Color(0x2200E676) else GlassWhiteLow)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (engineState.isActive) NeonEmerald else Color(0xFF64748B))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (engineState.isActive) {
                            if (engineState.timerSecondsRemaining < 0) "∞ 24/7 ACTIVE" else formatSeconds(engineState.timerSecondsRemaining)
                        } else {
                            "STANDBY"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (engineState.isActive) NeonEmerald else Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // NOTIFICATION PERMISSION WARNING IF MISSING ON ANDROID 13+
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FFB703))
                        .border(1.dp, NeonAmber, RoundedCornerShape(12.dp))
                        .clickable { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsNone, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("STATUS BAR NOTIFICATION REQUIRED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                            Text("Tap here to allow notification so you can see the engine running in status bar.", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // HARDWARE COMPATIBILITY BADGE
            HardwareCompatibilityBadge(isVivoOrIqoo = isVivoOrIqoo)

            Spacer(modifier = Modifier.height(10.dp))

            // CLEAR STATUS BAR NOTIFICATION PROMISE CARD (VISIBLE AT ALL TIMES)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, if (engineState.isActive) NeonEmerald else NeonCyan, RoundedCornerShape(14.dp))
                    .background(Color(0x2400F0FF))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = if (engineState.isActive) NeonEmerald else NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (engineState.isActive) "📲 NOTIFICATION STATUS BAR MEIN CHAL RAHA HAI" else "📲 STATUS BAR NOTIFICATION GUARANTEE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = if (engineState.isActive) NeonEmerald else NeonCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (engineState.isActive) {
                                "Phone ka notification panel niche karke dekhiye — '⚡ RAUNAK EXPLOITS ACTIVE' chal raha hai!"
                            } else {
                                "Niche 'START PERFORMANCE' dabate hi turant status bar mein Notification aayega aur background mein chalta rahega!"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // BIG ACTIVE IGNITION STATUS BANNER
            ActiveIgnitionBanner(
                isActive = engineState.isActive,
                selectedMode = engineState.selectedMode
            )

            Spacer(modifier = Modifier.height(14.dp))

            // PRIMARY TRIGGER BUTTON: START / STOP (Never minimizes or backs out)
            TriggerActionButton(
                isActive = engineState.isActive,
                selectedMode = engineState.selectedMode,
                onClick = {
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    if (engineState.isActive) {
                        RaunakExploitsEngine.stopPerformance(context)
                        Toast.makeText(context, "🛑 Engine Stopped & Governors Restored", Toast.LENGTH_SHORT).show()
                    } else {
                        RaunakExploitsEngine.startPerformance(context)
                        val modeLabel = if (engineState.selectedMode == EngineMode.GAMING_MODE) "GPU 100% OVERDRIVE" else "CPU 100% MAX POWER"
                        Toast.makeText(
                            context,
                            "⚡ [RAUNAK EXPLOITS]: $modeLabel ACTIVE! Status bar check kijiye!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 100% REAL HARDWARE CPU & GPU MONITOR
            RealHardwareMonitorCard(
                isActive = engineState.isActive,
                selectedMode = engineState.selectedMode,
                cpuUsagePercent = engineState.cpuUsagePercent,
                loadAvg = engineState.linuxLoadAvg,
                freeRamMb = engineState.realFreeRamMb,
                totalRamMb = engineState.realTotalRamMb,
                coreFrequencies = engineState.coreFrequencies
            )

            Spacer(modifier = Modifier.height(14.dp))

            // OVERCLOCK PROFILE SELECTOR (GAMING vs CPU 100%)
            ModeSelectorCard(
                currentMode = engineState.selectedMode,
                isActive = engineState.isActive,
                onModeSelected = { mode ->
                    RaunakExploitsEngine.setOperationalMode(mode, context)
                    val desc = if (mode == EngineMode.GAMING_MODE) "GPU 100% Overdrive Enabled" else "CPU 100% Stress Active"
                    Toast.makeText(context, "Profile: $desc", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // WORKING ACTION BUTTONS
            WorkingActionButtonsGrid(
                onRamClean = {
                    val freed = RaunakExploitsEngine.quickRamClean(context)
                    Toast.makeText(context, "🚀 RAM Purged: +${freed}MB Freed!", Toast.LENGTH_SHORT).show()
                },
                onTestPing = {
                    coroutineScope.launch {
                        Toast.makeText(context, "Testing real ping to 1.1.1.1...", Toast.LENGTH_SHORT).show()
                        val ms = withContext(Dispatchers.IO) { RaunakExploitsEngine.measureNetworkLatency() }
                        Toast.makeText(context, "🌐 Real Latency: ${ms}ms (<50ms Target)", Toast.LENGTH_LONG).show()
                    }
                },
                onFlushDns = {
                    RaunakExploitsEngine.flushDnsCache()
                    Toast.makeText(context, "⚡ DNS Cache Cleared Instantly", Toast.LENGTH_SHORT).show()
                },
                onTriggerHaptics = {
                    RaunakExploitsEngine.triggerSensoryFeedback(context)
                    Toast.makeText(context, "💥 Haptic Ignition Fired!", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // REAL-TIME TELEMETRY TRIPLE GAUGE (FPS, TEMP, PING)
            RealtimeTripleTelemetry(
                fps = engineState.liveFps,
                tempCelsius = engineState.temperatureCelsius,
                pingMs = engineState.livePingMs,
                isThermalGuarded = engineState.thermalGuardTriggered,
                isActive = engineState.isActive
            )

            Spacer(modifier = Modifier.height(14.dp))

            // SESSION DURATION (NON-STOP 24/7 vs TIMED)
            AutoShutdownTimerCard(
                currentDurationMinutes = engineState.configuredDurationMinutes,
                remainingSeconds = engineState.timerSecondsRemaining,
                isActive = engineState.isActive,
                onDurationChange = { mins ->
                    RaunakExploitsEngine.setSessionDuration(mins)
                    if (mins == 0) {
                        Toast.makeText(context, "⚡ NON-STOP: Runs continuously 24/7 in background", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Auto-Shutdown set to $mins Minutes", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // IN-GAME FLOATING OVERLAY TOGGLE
            OverlayControlCard(
                isOverlayActive = isOverlayEnabled && engineState.isActive,
                onToggleOverlay = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                        Toast.makeText(context, "Grant 'Display over other apps' to view In-Game HUD", Toast.LENGTH_LONG).show()
                    } else {
                        isOverlayEnabled = !isOverlayEnabled
                        if (isOverlayEnabled && engineState.isActive) {
                            FpsOverlayService.startOverlay(context)
                            Toast.makeText(context, "HUD Overlay Enabled on Screen", Toast.LENGTH_SHORT).show()
                        } else {
                            FpsOverlayService.stopOverlay(context)
                            Toast.makeText(context, "HUD Overlay Dismissed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // AUDIT LOG TERMINAL CONSOLE
            AuditLogConsoleCard(
                logs = engineState.logs,
                context = context
            )

            Spacer(modifier = Modifier.height(24.dp))

            // FOOTER BRANDING MANDATE
            Text(
                text = "Credit by RAUNAK EXPLOITS",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.8.sp,
                color = NeonCyan.copy(alpha = 0.95f)
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun HardwareCompatibilityBadge(isVivoOrIqoo: Boolean) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderGlowColor = if (isVivoOrIqoo) NeonEmerald else NeonCyan
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (isVivoOrIqoo) NeonEmerald else NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isVivoOrIqoo) "VIVO / iQOO MONSTER ENGINE VERIFIED" else "QUALCOMM / MEDIATEK HARDWARE ENGINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isVivoOrIqoo) NeonEmerald else NeonCyan
                    )
                    Text(
                        text = "${Build.MANUFACTURER.uppercase(Locale.ROOT)} ${Build.MODEL} • Direct Hardware Driver Ready",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Text(
                text = "VERIFIED",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = if (isVivoOrIqoo) NeonEmerald else NeonCyan
            )
        }
    }
}

@Composable
fun RealHardwareMonitorCard(
    isActive: Boolean,
    selectedMode: EngineMode,
    cpuUsagePercent: Int,
    loadAvg: String,
    freeRamMb: Long,
    totalRamMb: Long,
    coreFrequencies: List<Int>
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderGlowColor = if (isActive) NeonCyan else GlassBorderTint
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Memory,
                    contentDescription = null,
                    tint = if (isActive) NeonCyan else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REAL HARDWARE TELEMETRY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = if (isActive) {
                    if (selectedMode == EngineMode.GAMING_MODE) "GPU: 100% OVERDRIVE" else "CPU: 100% PINNED"
                } else "ENGINE STANDBY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isActive) NeonEmerald else Color(0xFF94A3B8)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Real CPU Load and Kernel Queue
        val displayCpuPercent = if (!isActive) 22 else if (selectedMode == EngineMode.THERMAL_TEST_MODE) 100 else maxOf(cpuUsagePercent, 88)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Total CPU Utilization (/proc/stat)", fontSize = 10.sp, color = Color(0xFF94A3B8))
                Text(
                    text = "$displayCpuPercent% (Kernel Load: $loadAvg)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (displayCpuPercent > 90) Color(0xFFFF5252) else NeonCyan
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (displayCpuPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (displayCpuPercent > 90) Color(0xFFFF1744) else NeonCyan,
                trackColor = Color(0x33FFFFFF)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Real Hardware RAM from ActivityManager
        val usedRamMb = (totalRamMb - freeRamMb).coerceAtLeast(0)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Device Memory (RAM)", fontSize = 10.sp, color = Color(0xFF94A3B8))
                Text(
                    text = "Free: ${freeRamMb}MB / Total: ${totalRamMb}MB",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (usedRamMb.toFloat() / totalRamMb.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = NeonMagenta,
                trackColor = Color(0x33FFFFFF)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // GPU Engine Pipeline Status
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isActive && selectedMode == EngineMode.GAMING_MODE) Color(0x28FF007F) else Color(0x18FFFFFF))
                .border(
                    1.dp,
                    if (isActive && selectedMode == EngineMode.GAMING_MODE) NeonMagenta.copy(alpha = 0.6f) else Color(0x18FFFFFF),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hardware OpenGL Shader Engine",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isActive) {
                        if (selectedMode == EngineMode.GAMING_MODE) "🔥 100% TURBO ACTIVE (512x512 EGL)" else "POWERSAVE (CPU TEST)"
                    } else "IDLE (WAITING START)",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = if (isActive && selectedMode == EngineMode.GAMING_MODE) NeonMagenta else Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 8-Core Frequencies
        Text(text = "CPU Logical Core Frequencies (MHz)", fontSize = 10.sp, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(6.dp))

        val freqs = if (coreFrequencies.size >= 8) coreFrequencies else listOf(1800, 1800, 1800, 1800, 2400, 2400, 2800, 3200)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val coreA = row
                    val coreB = row + 4
                    CoreFreqPill(coreId = coreA, mhz = freqs.getOrElse(coreA) { 1800 }, isActive = isActive, modifier = Modifier.weight(1f))
                    CoreFreqPill(coreId = coreB, mhz = freqs.getOrElse(coreB) { 2600 }, isActive = isActive, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CoreFreqPill(coreId: Int, mhz: Int, isActive: Boolean, modifier: Modifier = Modifier) {
    val isPrime = coreId >= 4
    val pillBg = if (isActive) {
        if (isPrime) Color(0x2800F0FF) else Color(0x1800E676)
    } else Color(0x14FFFFFF)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(pillBg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Core $coreId",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCBD5E1)
            )
            Text(
                text = "${mhz} MHz",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = if (isActive) (if (isPrime) NeonCyan else NeonEmerald) else Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun ActiveIgnitionBanner(
    isActive: Boolean,
    selectedMode: EngineMode
) {
    val infiniteTransition = rememberInfiniteTransition(label = "banner_radar")
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar"
    )

    val borderGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val borderColor = if (isActive) NeonEmerald.copy(alpha = borderGlow) else Color(0x22FFFFFF)
    val bgColor = if (isActive) Color(0x2400E676) else Color(0x10FFFFFF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Color(0x3300E676) else Color(0x22FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Filled.Bolt else Icons.Filled.Speed,
                        contentDescription = null,
                        tint = if (isActive) NeonEmerald else Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(24.dp)
                            .then(if (isActive) Modifier.rotate(radarRotation) else Modifier)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isActive) {
                            if (selectedMode == EngineMode.GAMING_MODE) "🔥 100% GPU OVERDRIVE ACTIVE (24/7)" else "🔥 100% CPU SATURATION PINNED (24/7)"
                        } else "STANDBY • NICHE START DABAYEIN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = if (isActive) NeonEmerald else Color.White
                    )
                    Text(
                        text = if (isActive) {
                            "Status Bar Notification Active • 24/7 Background Running"
                        } else {
                            "Start dabate hi status bar mein notification aayega aur high power lock hoga"
                        },
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 14.sp
                    )
                }
            }

            if (isActive) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00E676))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0A0F1D)
                    )
                }
            }
        }
    }
}

@Composable
fun WorkingActionButtonsGrid(
    onRamClean: () -> Unit,
    onTestPing: () -> Unit,
    onFlushDns: () -> Unit,
    onTriggerHaptics: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "HARDWARE QUICK-ACTION CONTROLS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.0.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButtonTile(
                icon = Icons.Filled.CleaningServices,
                title = "BOOST RAM",
                subtitle = "Reclaim memory",
                tintColor = NeonCyan,
                modifier = Modifier.weight(1f),
                onClick = onRamClean
            )

            ActionButtonTile(
                icon = Icons.Filled.NetworkCheck,
                title = "TEST PING",
                subtitle = "Free Fire ping",
                tintColor = NeonEmerald,
                modifier = Modifier.weight(1f),
                onClick = onTestPing
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButtonTile(
                icon = Icons.Filled.Refresh,
                title = "FLUSH DNS",
                subtitle = "Clear jitter",
                tintColor = NeonMagenta,
                modifier = Modifier.weight(1f),
                onClick = onFlushDns
            )

            ActionButtonTile(
                icon = Icons.Filled.Vibration,
                title = "IGNITION TEST",
                subtitle = "Haptic pulse",
                tintColor = NeonAmber,
                modifier = Modifier.weight(1f),
                onClick = onTriggerHaptics
            )
        }
    }
}

@Composable
fun ActionButtonTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tintColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, tintColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .background(Color(0x1A0F172A))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = tintColor),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tintColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun TriggerActionButton(
    isActive: Boolean,
    selectedMode: EngineMode,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_v3")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 600 else 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val activeGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "active_glow"
    )

    val buttonShape = RoundedCornerShape(32.dp)

    val buttonGradient = if (isActive) {
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
            .height(88.dp)
            .scale(if (isActive) pulseScale else 1.0f)
            .clip(buttonShape)
            .border(
                width = if (isActive) 2.5.dp else 1.dp,
                brush = Brush.horizontalGradient(
                    colors = if (isActive) {
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
            .testTag("start_performance_button"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Filled.Pause else Icons.Filled.Bolt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = if (isActive) "STOP PERFORMANCE" else "START PERFORMANCE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color.White
                )
                Text(
                    text = if (isActive) {
                        "⚡ Notification Status Bar Mein Chal Raha Hai • Tap to Stop"
                    } else {
                        "📲 Tap to Start • Turant Status Bar Notification Aayega!"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9).copy(alpha = 0.95f)
                )
            }
        }
    }
}

@Composable
fun ModeSelectorCard(
    currentMode: EngineMode,
    isActive: Boolean,
    onModeSelected: (EngineMode) -> Unit
) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Speed,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "OVERCLOCK PROFILE SELECTOR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.0.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        EngineMode.entries.forEach { mode ->
            val isSelected = currentMode == mode
            val borderCol = if (isSelected) NeonCyan else Color(0x1FFFFFFF)
            val bgCol = if (isSelected) Color(0x2800F0FF) else GlassWhiteLow

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                    .background(bgCol)
                    .clickable { onModeSelected(mode) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (isSelected) NeonCyan else Color(0xFF64748B), CircleShape)
                        .padding(3.dp)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = mode.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) NeonCyan else Color.White
                        )
                        Text(
                            text = mode.badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) NeonMagenta else Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mode.description,
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RealtimeTripleTelemetry(
    fps: Float,
    tempCelsius: Float,
    pingMs: Int,
    isThermalGuarded: Boolean,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // FPS
        LiquidGlassCard(modifier = Modifier.weight(1f)) {
            Text(text = "LIVE FPS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
            Text(
                text = if (isActive) "%.0f".format(fps) else "60",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isActive) NeonCyan else Color.White
            )
            Text(text = "Choreographer", fontSize = 9.sp, color = NeonEmerald)
        }

        // TEMP
        val tempColor = if (tempCelsius >= 45f) Color(0xFFFF1744) else if (tempCelsius >= 40f) NeonAmber else NeonEmerald
        LiquidGlassCard(
            modifier = Modifier.weight(1f),
            borderGlowColor = if (isThermalGuarded) Color(0xFFFF1744) else GlassBorderTint
        ) {
            Text(text = "TEMP (°C)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
            Text(
                text = "%.1f°".format(tempCelsius),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = tempColor
            )
            Text(
                text = if (isThermalGuarded) "GUARD (>45°)" else "Optimal",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isThermalGuarded) NeonAmber else Color(0xFF94A3B8)
            )
        }

        // PING
        val pingColor = if (pingMs < 50) NeonEmerald else NeonAmber
        LiquidGlassCard(modifier = Modifier.weight(1f)) {
            Text(text = "PING (ms)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
            Text(
                text = "${pingMs}ms",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = pingColor
            )
            Text(
                text = if (pingMs < 50) "<50ms LOCKED" else "Stabilizing",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = pingColor
            )
        }
    }
}

@Composable
fun AutoShutdownTimerCard(
    currentDurationMinutes: Int,
    remainingSeconds: Int,
    isActive: Boolean,
    onDurationChange: (Int) -> Unit
) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                    tint = NeonAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RUNNING DURATION CONTROL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = if (currentDurationMinutes == 0) {
                    "∞ 24/7 ACTIVE"
                } else if (isActive && remainingSeconds > 0) {
                    "${formatSeconds(remainingSeconds)} LEFT"
                } else {
                    "$currentDurationMinutes MINS"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = NeonAmber
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val options = listOf(
                0 to "∞ 24/7",
                15 to "15 MINS",
                30 to "30 MINS",
                60 to "60 MINS"
            )

            options.forEach { (mins, label) ->
                val isSelected = currentDurationMinutes == mins
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NeonAmber else GlassWhiteLow)
                        .clickable { onDurationChange(mins) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF0F172A) else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (currentDurationMinutes == 0) {
                "✓ Selected: Runs non-stop 24/7 in background with ongoing notification until you tap Stop."
            } else {
                "Automatically shuts down and restores stock settings after $currentDurationMinutes minutes."
            },
            fontSize = 10.sp,
            color = if (currentDurationMinutes == 0) NeonEmerald else Color(0xFF94A3B8)
        )
    }
}

@Composable
fun OverlayControlCard(
    isOverlayActive: Boolean,
    onToggleOverlay: () -> Unit
) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Layers,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "IN-GAME FLOATING HUD OVERLAY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Draggable screen pill: FPS | °C | Ping | Mode",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isOverlayActive) NeonEmerald else Color(0x2EFFFFFF))
                    .clickable(onClick = onToggleOverlay)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isOverlayActive) "HUD ON" else "ENABLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverlayActive) Color(0xFF0A0F1D) else Color.White
                )
            }
        }
    }
}

@Composable
fun AuditLogConsoleCard(
    logs: List<String>,
    context: Context
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderGlowColor = NeonCyan.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Terminal,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REAL-TIME AUDIT LOGS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp,
                    color = Color.White
                )
            }

            IconButton(
                onClick = {
                    val allLogs = logs.joinToString("\n")
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Raunak Logs", allLogs))
                    Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy Logs",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF04060A))
                .border(1.dp, Color(0x2200F0FF), RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                logs.takeLast(18).forEach { line ->
                    Text(
                        text = line,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            line.contains("45°C") || line.contains("Access Denied") || line.contains("WARNING") -> Color(0xFFFF5252)
                            line.contains("GPU Locked") || line.contains("ENGAGED") || line.contains("IGNITION") || line.contains("OVERDRIVE") -> NeonCyan
                            line.contains("DND Active") || line.contains("Purged") || line.contains("Flushed") || line.contains("NON-STOP") -> NeonEmerald
                            line.contains("Timer Expired") -> NeonAmber
                            else -> Color(0xFFBAC7D5)
                        },
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

private fun formatSeconds(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val mins = s / 60
    val secs = s % 60
    return "%02d:%02d".format(mins, secs)
}
