package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Foreground Service that enforces CPU wake lock, manages the multi-threaded
 * floating-point math engine, and broadcasts real-time thermal throttling telemetry.
 */
class ExtremePerformanceService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var wakeLock: PowerManager.WakeLock? = null
    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null

    private val workerJobs = mutableListOf<Job>()
    private val totalOpsCounter = AtomicLong(0L)
    private var lastRecordedOps = 0L
    private var lastMetricsTimestamp = System.currentTimeMillis()
    private var currentMode = StressMode.GAMING_LOCK
    private var targetThreads = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupThermalMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopPerformanceEngine()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_CONFIG -> {
                val modeOrdinal = intent?.getIntExtra(EXTRA_STRESS_MODE, currentMode.ordinal) ?: currentMode.ordinal
                currentMode = StressMode.entries.getOrElse(modeOrdinal) { StressMode.GAMING_LOCK }
                targetThreads = (intent?.getIntExtra(EXTRA_THREAD_COUNT, targetThreads) ?: targetThreads)
                    .coerceIn(1, Runtime.getRuntime().availableProcessors().coerceAtLeast(1))

                restartMathEngine()
                updateNotification()
            }
            ACTION_START -> {
                val modeOrdinal = intent?.getIntExtra(EXTRA_STRESS_MODE, currentMode.ordinal) ?: currentMode.ordinal
                currentMode = StressMode.entries.getOrElse(modeOrdinal) { StressMode.GAMING_LOCK }
                targetThreads = (intent?.getIntExtra(EXTRA_THREAD_COUNT, targetThreads) ?: targetThreads)
                    .coerceIn(1, Runtime.getRuntime().availableProcessors().coerceAtLeast(1))

                startPerformanceEngine()
            }
        }

        return START_STICKY
    }

    private fun startPerformanceEngine() {
        acquireWakeLock()

        val notification = buildNotification(
            title = getString(R.string.notification_title),
            contentText = "Active on $targetThreads cores [${currentMode.title}]"
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

        restartMathEngine()
        startTelemetryLoop()

        _metricsState.update {
            it.copy(
                isActive = true,
                stressMode = currentMode,
                activeThreads = targetThreads,
                wakeLockHeld = wakeLock?.isHeld == true
            )
        }
    }

    private fun stopPerformanceEngine() {
        releaseWakeLock()
        stopWorkers()
        serviceScope.coroutineContext.cancel()

        _metricsState.update {
            it.copy(
                isActive = false,
                activeThreads = 0,
                opsPerSecond = 0.0,
                wakeLockHeld = false
            )
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null || wakeLock?.isHeld == false) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RaunakBooster::ExtremePerformanceWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12h safety ceiling
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        } finally {
            wakeLock = null
        }
    }

    private fun restartMathEngine() {
        stopWorkers()

        val coresToSpawn = targetThreads.coerceAtLeast(1)
        for (i in 0 until coresToSpawn) {
            val workerJob = serviceScope.launch(Dispatchers.Default) {
                runMathWorker(workerIndex = i, mode = currentMode)
            }
            workerJobs.add(workerJob)
        }

        _metricsState.update {
            it.copy(
                activeThreads = coresToSpawn,
                stressMode = currentMode
            )
        }
    }

    private fun stopWorkers() {
        workerJobs.forEach { it.cancel() }
        workerJobs.clear()
    }

    /**
     * Math stress engine: continuously executes floating-point & vector arithmetic
     * to keep CPU cores active at high frequency governors.
     */
    private suspend fun runMathWorker(workerIndex: Int, mode: StressMode) {
        var acc = 1.0001
        var iter = 0L

        while (currentCoroutineContext().isActive) {
            // Unrolled vector and trigonometric math block
            for (step in 0 until 5_000) {
                val seed = (iter + step + workerIndex).toDouble()
                acc = sqrt(acc * acc + sin(seed) * cos(seed) + 0.00001)
                if (acc > 10_000.0) acc = 1.0001
            }
            iter += 5_000
            totalOpsCounter.addAndGet(5_000)

            when (mode) {
                StressMode.EXTREME_THROTTLE_TEST -> {
                    // Maximum load: Cooperative yield every 50k ops to prevent thread starvations
                    if (iter % 50_000L == 0L) {
                        yield()
                    }
                }
                StressMode.GAMING_LOCK -> {
                    // Governor lock: 20ms work, 4ms micro-delay to maintain peak clock state
                    if (iter % 40_000L == 0L) {
                        delay(3)
                    }
                }
                StressMode.ECO_TURBO -> {
                    // Eco pacing: ~30% duty cycle
                    if (iter % 20_000L == 0L) {
                        delay(12)
                    }
                }
            }
        }
    }

    private fun startTelemetryLoop() {
        serviceScope.launch {
            var uptime = 0L
            val historyList = mutableListOf<Float>()

            while (isActive) {
                delay(1000)
                uptime++

                val now = System.currentTimeMillis()
                val currentTotalOps = totalOpsCounter.get()
                val deltaOps = currentTotalOps - lastRecordedOps
                val deltaTimeSec = (now - lastMetricsTimestamp).coerceAtLeast(1) / 1000.0

                val opsPerSec = (deltaOps / deltaTimeSec)
                val mflops = opsPerSec / 1_000_000.0

                lastRecordedOps = currentTotalOps
                lastMetricsTimestamp = now

                // Read battery telemetry
                val batteryData = readBatteryStatus()

                // Keep last 20 throughput data points
                historyList.add(mflops.toFloat())
                if (historyList.size > 20) {
                    historyList.removeAt(0)
                }

                _metricsState.update { current ->
                    current.copy(
                        isActive = true,
                        totalOpsCount = currentTotalOps,
                        opsPerSecond = mflops,
                        uptimeSeconds = uptime,
                        wakeLockHeld = wakeLock?.isHeld == true,
                        batteryLevel = batteryData.first,
                        batteryTempCelsius = batteryData.second,
                        batteryPlugged = batteryData.third,
                        throughputHistory = historyList.toList()
                    )
                }

                // Periodically update foreground notification
                if (uptime % 3L == 0L) {
                    updateNotification(
                        "Active: $targetThreads Cores | %.1f Mops/s | %.1f°C"
                            .format(mflops, batteryData.second)
                    )
                }
            }
        }
    }

    private fun readBatteryStatus(): Triple<Int, Float, Boolean> {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (scale > 0) ((level / scale.toFloat()) * 100).toInt() else level

        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300) ?: 300
        val tempCelsius = rawTemp / 10.0f

        val plugged = (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        return Triple(batteryPct, tempCelsius, plugged)
    }

    private fun setupThermalMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
                val (name, code) = parseThermalStatus(status)
                _metricsState.update {
                    it.copy(
                        thermalStatusCode = code,
                        thermalStatusName = name
                    )
                }
            }
            thermalListener?.let { powerManager?.addThermalStatusListener(it) }
        }
    }

    private fun parseThermalStatus(status: Int): Pair<String, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (status) {
                PowerManager.THERMAL_STATUS_NONE -> "Nominal (Cool)" to status
                PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth" to status
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Throttling" to status
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe Throttling!" to status
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Thermal Alert!" to status
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency Cooling!" to status
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Hardware Shutdown!" to status
                else -> "Status: $status" to status
            }
        } else {
            "Hardware Normal" to 0
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc)
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

        val stopIntent = Intent(this, ExtremePerformanceService::class.java).apply {
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
                getString(R.string.action_stop),
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification(text: String? = null) {
        val content = text ?: "Active on $targetThreads Cores [${currentMode.title}]"
        val notification = buildNotification(getString(R.string.notification_title), content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalListener != null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            thermalListener?.let { powerManager?.removeThermalStatusListener(it) }
        }
        stopPerformanceEngine()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "performance_engine_channel"
        const val NOTIFICATION_ID = 7710

        const val ACTION_START = "com.example.action.START"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_UPDATE_CONFIG = "com.example.action.UPDATE_CONFIG"

        const val EXTRA_STRESS_MODE = "extra_stress_mode"
        const val EXTRA_THREAD_COUNT = "extra_thread_count"

        private val _metricsState = MutableStateFlow(PerformanceMetrics())
        val metricsState: StateFlow<PerformanceMetrics> = _metricsState.asStateFlow()

        fun startService(context: Context, mode: StressMode, threads: Int) {
            val intent = Intent(context, ExtremePerformanceService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_STRESS_MODE, mode.ordinal)
                putExtra(EXTRA_THREAD_COUNT, threads)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateConfig(context: Context, mode: StressMode, threads: Int) {
            val intent = Intent(context, ExtremePerformanceService::class.java).apply {
                action = ACTION_UPDATE_CONFIG
                putExtra(EXTRA_STRESS_MODE, mode.ordinal)
                putExtra(EXTRA_THREAD_COUNT, threads)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ExtremePerformanceService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
