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
 * Persistent Non-Stop Foreground Service for RAUNAK EXPLOITS ENGINE.
 * Holds an indefinite PARTIAL_WAKE_LOCK so CPU & GPU locks NEVER sleep
 * in the background until the user stops or clears the app from recents.
 */
class NativeStressService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var telemetryJob: Job? = null
    private var governorPinnerJob: Job? = null

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "NativeStressService"
        const val CHANNEL_ID = "raunak_persistent_engine_channel"
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
    private external fun startNativeStress(): Boolean
    private external fun stopNativeStress(): Boolean
    private external fun isNativeStressRunning(): Boolean
    private external fun getHardwareCoreCount(): Int

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
                // User pressed Stop on notification or in app
                stopPersistentMode()
                RaunakExploitsEngine.stopPerformance(applicationContext)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // Return START_STICKY to guarantee Android will restart the service if killed by memory pressure
        return START_STICKY
    }

    /**
     * Called when the user removes the app task from recent apps (swiping away).
     * Automatically and cleanly shuts down the performance boost as requested.
     */
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
            "⚡ RAUNAK EXPLOITS: 100% CPU TEST ACTIVE"
        } else {
            "⚡ RAUNAK EXPLOITS: GPU 100% LOCKED (BACKGROUND)"
        }

        val subtitle = if (isThermalMode) {
            "C++ threads stressing $coreCount cores • WakeLock held non-stop"
        } else {
            "Performance boost active in background • Free Fire Mode"
        }

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

        // In Thermal Mode, invoke heavy C++ POSIX thread math loop
        if (isThermalMode) {
            try {
                startNativeStress()
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking startNativeStress: ${e.message}", e)
            }
        } else {
            try {
                stopNativeStress()
            } catch (_: Exception) {}
        }

        startContinuousGovernorPinner(isThermalMode)
        startTelemetry(coreCount, isThermalMode)
    }

    private fun stopPersistentMode() {
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
                uptimeSeconds = 0
            )
        }
    }

    /**
     * Indefinite WakeLock ensures Vivo / iQOO / Android OS never puts CPU into deep sleep
     * until the app is explicitly stopped or swiped from recent apps.
     */
    private fun acquireIndefiniteWakeLock() {
        if (wakeLock == null || wakeLock?.isHeld == false) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RaunakExploits::NonStopPerformanceWakeLock"
            )?.apply {
                setReferenceCounted(false)
                // Indefinite acquire without timeout - runs non-stop in background
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

    /**
     * Re-applies GPU 100% and governor locks continuously every 8 seconds
     * so aggressive OEM battery managers never throttle clocks while in background.
     */
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
                delay(8000)
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

                val isNativeActive = if (isThermalMode) {
                    try { isNativeStressRunning() } catch (_: Exception) { true }
                } else {
                    true
                }

                _serviceState.update {
                    it.copy(
                        isRunning = isNativeActive,
                        activeCoreCount = coreCount,
                        wakeLockAcquired = wakeLock?.isHeld == true,
                        uptimeSeconds = uptime
                    )
                }

                // Update notification every 10 seconds to maintain foreground priority
                if (uptime % 10L == 0L) {
                    val manager = getSystemService(NotificationManager::class.java)
                    val modeText = if (isThermalMode) "CPU 100% Test" else "GPU 100% Locked"
                    manager.notify(
                        NOTIFICATION_ID,
                        buildNotification(
                            title = "⚡ RAUNAK EXPLOITS ACTIVE ($modeText)",
                            contentText = "Active in Background: ${formatUptime(uptime)} • WakeLock Guarded"
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
                "RAUNAK EXPLOITS Engine Core",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps CPU & GPU performance governors locked non-stop in background"
                setShowBadge(false)
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
    val uptimeSeconds: Long = 0L
)
