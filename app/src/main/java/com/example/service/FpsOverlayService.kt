package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.engine.EngineMode
import com.example.engine.RaunakExploitsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Floating Screen Overlay HUD displaying real-time FPS, Temperature, Ping (<50ms),
 * and Session Timer directly on top of games.
 */
class FpsOverlayService : Service() {

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var windowManager: WindowManager? = null
    private var overlayView: LinearLayout? = null
    private var tvFps: TextView? = null
    private var tvTemp: TextView? = null
    private var tvPing: TextView? = null
    private var tvTimer: TextView? = null
    private var tvStatus: TextView? = null

    private var observerJob: Job? = null

    companion object {
        const val ACTION_START_OVERLAY = "com.example.action.START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.example.action.STOP_OVERLAY"
        private const val CHANNEL_ID = "raunak_overlay_channel"
        private const val NOTIFICATION_ID = 9922

        fun startOverlay(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            val intent = Intent(context, FpsOverlayService::class.java).apply {
                action = ACTION_START_OVERLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopOverlay(context: Context) {
            val intent = Intent(context, FpsOverlayService::class.java).apply {
                action = ACTION_STOP_OVERLAY
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                removeOverlayView()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, createNotification())
                showOverlayView()
            }
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlayView() {
        if (overlayView != null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 100
        }

        val pillLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            gravity = Gravity.CENTER_VERTICAL

            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#EE060A14"))
                setStroke(dp(1), Color.parseColor("#6600F0FF"))
                cornerRadius = dp(24).toFloat()
            }
            background = bg
        }

        val brandTag = TextView(this).apply {
            text = "⚡ RAUNAK"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, dp(8), 0)
        }

        tvFps = TextView(this).apply {
            text = "60 FPS"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, dp(8), 0)
        }

        tvTemp = TextView(this).apply {
            text = "36°C"
            setTextColor(Color.parseColor("#00F59B"))
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, dp(8), 0)
        }

        tvPing = TextView(this).apply {
            text = "28ms"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, dp(8), 0)
        }

        tvTimer = TextView(this).apply {
            text = "15:00"
            setTextColor(Color.parseColor("#FFB703"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, dp(8), 0)
        }

        tvStatus = TextView(this).apply {
            text = "GPU 100%"
            setTextColor(Color.parseColor("#FF007F"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
        }

        pillLayout.addView(brandTag)
        pillLayout.addView(tvFps)
        pillLayout.addView(tvTemp)
        pillLayout.addView(tvPing)
        pillLayout.addView(tvTimer)
        pillLayout.addView(tvStatus)

        // Draggable HUD
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        pillLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(pillLayout, params)
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(pillLayout, params)
            overlayView = pillLayout
            observeEngineTelemetry()
        } catch (_: Exception) {}
    }

    private fun observeEngineTelemetry() {
        observerJob?.cancel()
        observerJob = scope.launch {
            RaunakExploitsEngine.engineState.collectLatest { state ->
                tvFps?.text = "%.0f FPS".format(state.liveFps)
                tvTemp?.text = "%.1f°C".format(state.temperatureCelsius)
                tvPing?.text = "${state.livePingMs}ms"

                if (state.timerSecondsRemaining < 0) {
                    tvTimer?.text = "∞ NON-STOP"
                } else {
                    val mins = state.timerSecondsRemaining / 60
                    val secs = state.timerSecondsRemaining % 60
                    tvTimer?.text = "%02d:%02d".format(mins, secs)
                }

                if (state.livePingMs < 50) {
                    tvPing?.setTextColor(Color.parseColor("#00F59B")) // Ultra low ping locked
                } else {
                    tvPing?.setTextColor(Color.parseColor("#FFB703"))
                }

                if (state.temperatureCelsius >= 45f) {
                    tvTemp?.setTextColor(Color.parseColor("#FFFF1744"))
                    tvStatus?.text = "THERMAL GUARD"
                    tvStatus?.setTextColor(Color.parseColor("#FFFFB703"))
                } else {
                    tvTemp?.setTextColor(Color.parseColor("#00F59B"))
                    when (state.selectedMode) {
                        EngineMode.GAMING_MODE -> {
                            tvStatus?.text = if (state.gpuGovernorLocked) "GPU 100%" else "STANDBY"
                            tvStatus?.setTextColor(Color.parseColor("#FF007F"))
                        }
                        EngineMode.THERMAL_TEST_MODE -> {
                            tvStatus?.text = if (state.cpu100PercentLocked) "CPU 100%" else "STANDBY"
                            tvStatus?.setTextColor(Color.parseColor("#00F0FF"))
                        }
                    }
                }
            }
        }
    }

    private fun removeOverlayView() {
        observerJob?.cancel()
        observerJob = null
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) {}
            overlayView = null
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RAUNAK EXPLOITS In-Game HUD",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("RAUNAK EXPLOITS HUD")
            .setContentText("Real-Time FPS, Ping & Temperature In-Game Overlay Active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlayView()
        scope.coroutineContext.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
