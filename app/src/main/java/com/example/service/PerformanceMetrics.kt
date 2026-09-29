package com.example.service

/**
 * Workload intensity modes for CPU governor locking & thermal stress analysis.
 */
enum class StressMode(val title: String, val description: String, val targetLoad: String) {
    ECO_TURBO(
        title = "Eco Governor Lock",
        description = "Light burst pacing (~30% duty cycle). Keeps CPU frequency governors awake without rapid battery drain.",
        targetLoad = "~30% Load"
    ),
    GAMING_LOCK(
        title = "Gaming Turbo Lock",
        description = "Balanced floating-point calculations to eliminate OEM downclocking and micro-stutters during heavy gaming.",
        targetLoad = "~65% Load"
    ),
    EXTREME_THROTTLE_TEST(
        title = "Extreme Thermal Stress",
        description = "100% duty cycle multi-core matrix and FMA arithmetic to benchmark thermal throttling dissipation curve.",
        targetLoad = "100% Core Load"
    )
}

/**
 * Snapshot of real-time hardware telemetry and background engine state.
 */
data class PerformanceMetrics(
    val isActive: Boolean = false,
    val activeThreads: Int = 0,
    val maxCores: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
    val stressMode: StressMode = StressMode.GAMING_LOCK,
    val totalOpsCount: Long = 0L,
    val opsPerSecond: Double = 0.0,
    val uptimeSeconds: Long = 0L,
    val wakeLockHeld: Boolean = false,
    val thermalStatusCode: Int = 0,
    val thermalStatusName: String = "Nominal (Cool)",
    val batteryLevel: Int = 100,
    val batteryTempCelsius: Float = 30.0f,
    val batteryPlugged: Boolean = false,
    val throughputHistory: List<Float> = emptyList()
)
