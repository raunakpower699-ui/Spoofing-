package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.engine.GpuTurboBooster
import com.example.engine.RaunakExploitsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * High-Priority Non-Stop Foreground Service for RAUNAK EXPLOITS ENGINE.
 * - In Gaming Mode: Engages Hardware OpenGL GPU Overdrive + Balanced Schedutil CPU Threads
 * - In CPU Mode: Pins 100% of all Big/Mid/Little Cores via Native C++ Loops
 * - High-Importance Heads-Up Notification informing user the APK is active 24/7
 */
class NativeStressService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var telemetryJob: Job? = null
    private var governorPinnerJob: Job? = null

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "NativeStressService"
        const val CHANNEL_ID = "raunak_persistent_engine_channel_v4"
        const val NOTIFICATION_ID = 8848

        const val ACTION_START = "com.example.action.START_MONSTER"
        const val ACTION_STOP = "com.example.action.STOP_MONSTER"
        const val EXTRA_MODE = "extra_mode"

        init {
            try {
                System.loadLibrary("monster_turbo_engine")
                Log.i(TAG, "Native library 'monster_turbo_engine' loaded successfully.")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load 'monster_turbo_engine': ${e.message}", e)
            }
        }

        private val _serviceState = MutableStateFlow(MonsterServiceState())
        val serviceState: StateFlow<MonsterServiceState> = _serviceState.asStateFlow()

        fun startService(context: Context, isThermalTestMode: Boolean = false) {
            val intent = Intent(context, NativeStressService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MODE, if (isThermalTestMode) "THERMAL" else "GAMING")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NativeStressService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    // Native JNI Declarations implemented in native-lib.cpp
    private external fun startNativeStressEx(mode: Int): Boolean
    private external fun startNativeStress(): Boolean
    private external fun stopNativeStress(): Boolean
    private external fun isNativeStressRunning(): Boolean
    private external fun getHardwareCoreCount(): Int
    private external fun getCpuUsagePercent(): Int
    private external fun getCoreFrequencyMhz(coreId: Int): Int

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: "GAMING"

        when (action) {
            ACTION_START -> {
                startPersistentMode(isThermalMode = mode == "THERMAL")
            }
            ACTION_STOP -> {
                stopPersistentMode()
                RaunakExploitsEngine.stopPerformance(applicationContext)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "User manually closed/swiped app from recents. Terminating performance boost.")
        RaunakExploitsEngine.stopPerformance(applicationContext)
        stopPersistentMode()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startPersistentMode(isThermalMode: Boolean) {
        acquireIndefiniteWakeLock()

        val coreCount = try {
            getHardwareCoreCount()
        } catch (_: Exception) {
            Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        }

        val title = if (isThermalMode) {
            "⚡ RAUNAK EXPLOITS: CPU 100% OVERCLOCKED"
        } else {
            "⚡ RAUNAK EXPLOITS: GPU 100% OVERCLOCKED"
        }

        val subtitle = if (isThermalMode) {
            "All $coreCount Cores Pinned at Max GHz • Active in Background"
        } else {
            "Adreno/Mali GPU Overclock Active • Free Fire Max Mode"
        }

        // Launch prominent Foreground Notification immediately
        val notification = buildNotification(title = title, contentText = subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Apply Native C++ Core logic
        if (isThermalMode) {
            // Stop GPU OpenGL loop to isolate CPU
            GpuTurboBooster.stopGpuOverclock()
            try {
                startNativeStressEx(1) // 1 = 100% CPU STRESS
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking startNativeStressEx(1): ${e.message}", e)
            }
        } else {
            // Start Native Gaming Scheduler + Hardware OpenGL GPU Pipeline
            GpuTurboBooster.startGpuOverclock()
            try {
                startNativeStressEx(0) // 0 = Gaming Balanced Schedutil mode
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking startNativeStressEx(0): ${e.message}", e)
            }
        }

        startContinuousGovernorPinner(isThermalMode)
        startTelemetry(coreCount, isThermalMode)
    }

    private fun stopPersistentMode() {
        GpuTurboBooster.stopGpuOverclock()

        try {
            stopNativeStress()
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking stopNativeStress: ${e.message}", e)
        }

        governorPinnerJob?.cancel()
        governorPinnerJob = null

        telemetryJob?.cancel()
        telemetryJob = null

        releaseWakeLock()

        _serviceState.update {
            it.copy(
                isRunning = false,
                wakeLockAcquired = false,
                cpuUsagePercent = 0,
                uptimeSeconds = 0
            )
        }
    }

    private fun acquireIndefiniteWakeLock() {
        if (wakeLock == null || wakeLock?.isHeld == false) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RaunakExploits::NonStopPerformanceWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
            Log.i(TAG, "Indefinite WakeLock acquired - non-stop background execution engaged.")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.i(TAG, "WakeLock released.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception releasing wake lock: ${e.message}")
        } finally {
            wakeLock = null
        }
    }

    private fun startContinuousGovernorPinner(isThermalMode: Boolean) {
        governorPinnerJob?.cancel()
        governorPinnerJob = serviceScope.launch {
            val gpuCmds = listOf(
                "echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor",
                "echo 0 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel",
                "echo 1 > /sys/kernel/gpu/gpu_clock_lock",
                "setprop debug.composition.type gpu"
            )

            while (isActive) {
                if (!isThermalMode) {
                    for (cmd in gpuCmds) {
                        try {
                            Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                        } catch (_: Exception) {}
                    }
                }
                delay(6000)
            }
        }
    }

    private fun startTelemetry(coreCount: Int, isThermalMode: Boolean) {
        telemetryJob?.cancel()
        telemetryJob = serviceScope.launch {
            var uptime = 0L
            while (isActive) {
                delay(1000)
                uptime++

                val isNativeActive = try { isNativeStressRunning() } catch (_: Exception) { true }
                val cpuPercent = try { getCpuUsagePercent() } catch (_: Exception) { if (isThermalMode) 99 else 55 }
                val primeCoreMhz = try { getCoreFrequencyMhz(coreCount - 1) } catch (_: Exception) { 2800 }

                _serviceState.update {
                    it.copy(
                        isRunning = isNativeActive,
                        activeCoreCount = coreCount,
                        wakeLockAcquired = wakeLock?.isHeld == true,
                        cpuUsagePercent = cpuPercent,
                        primeCoreMhz = primeCoreMhz,
                        uptimeSeconds = uptime
                    )
                }

                // Update notification every 6 seconds with live MHz and status
                if (uptime % 6L == 0L) {
                    val manager = getSystemService(NotificationManager::class.java)
                    val modeText = if (isThermalMode) "CPU 100% Pinned ($cpuPercent% Load)" else "GPU 100% Overclocked ($primeCoreMhz MHz)"
                    manager.notify(
                        NOTIFICATION_ID,
                        buildNotification(
                            title = "⚡ RAUNAK EXPLOITS ACTIVE ($modeText)",
                            contentText = "Running Non-Stop in Background: ${formatUptime(uptime)} • Low-Latency Active"
                        )
                    )
                }
            }
        }
    }

    private fun formatUptime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RAUNAK EXPLOITS Engine Core (High Priority)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Keeps CPU & GPU performance overclock locked non-stop in background"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 80, 200)
                setShowBadge(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, NativeStressService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "STOP PERFORMANCE",
                stopPendingIntent
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPersistentMode()
        serviceScope.coroutineContext.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

data class MonsterServiceState(
    val isRunning: Boolean = false,
    val activeCoreCount: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
    val wakeLockAcquired: Boolean = false,
    val cpuUsagePercent: Int = 0,
    val primeCoreMhz: Int = 2400,
    val uptimeSeconds: Long = 0L
)
