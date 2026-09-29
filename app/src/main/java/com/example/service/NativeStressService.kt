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
 * Foreground Service hosting the Native C++ POSIX thread stress engine.
 * Holds a 120-minute Partial WakeLock to lock CPU governors during gaming or thermal tests.
 */
class NativeStressService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var telemetryJob: Job? = null

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "NativeStressService"
        const val CHANNEL_ID = "monster_engine_channel"
        const val NOTIFICATION_ID = 8848

        const val ACTION_START = "com.example.action.START_MONSTER"
        const val ACTION_STOP = "com.example.action.STOP_MONSTER"

        // Max wake lock: 120 minutes (2 hours)
        private const val WAKELOCK_DURATION_MS = 120 * 60 * 1000L

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

        fun startService(context: Context) {
            val intent = Intent(context, NativeStressService::class.java).apply {
                action = ACTION_START
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

        when (action) {
            ACTION_START -> {
                startMonsterMode()
            }
            ACTION_STOP -> {
                stopMonsterMode()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_STICKY
    }

    private fun startMonsterMode() {
        acquireWakeLock()

        val coreCount = try {
            getHardwareCoreCount()
        } catch (_: Exception) {
            Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        }

        val notification = buildNotification(
            title = "Monster Engine Active",
            contentText = "Locking CPU governor across $coreCount native cores"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            startNativeStress()
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking startNativeStress: ${e.message}", e)
        }

        startTelemetry(coreCount)
    }

    private fun stopMonsterMode() {
        try {
            stopNativeStress()
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking stopNativeStress: ${e.message}", e)
        }

        releaseWakeLock()
        telemetryJob?.cancel()
        telemetryJob = null

        _serviceState.update {
            it.copy(
                isRunning = false,
                wakeLockAcquired = false,
                uptimeSeconds = 0
            )
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null || wakeLock?.isHeld == false) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MonsterTurbo::NativeCpuLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(WAKELOCK_DURATION_MS)
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception releasing wake lock: ${e.message}")
        } finally {
            wakeLock = null
        }
    }

    private fun startTelemetry(coreCount: Int) {
        telemetryJob?.cancel()
        telemetryJob = serviceScope.launch {
            var uptime = 0L
            while (isActive) {
                delay(1000)
                uptime++

                val isNativeActive = try {
                    isNativeStressRunning()
                } catch (_: Exception) {
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

                // Update notification text with live seconds
                if (uptime % 5L == 0L) {
                    val manager = getSystemService(NotificationManager::class.java)
                    manager.notify(
                        NOTIFICATION_ID,
                        buildNotification(
                            title = "Monster Engine Active",
                            contentText = "Locked across $coreCount cores • Active: ${formatUptime(uptime)}"
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
                "Monster Performance Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps CPU frequency governors locked during gaming and benchmarks"
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
                "STOP MONSTER MODE",
                stopPendingIntent
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonsterMode()
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
