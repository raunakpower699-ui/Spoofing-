package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.PerformanceMetrics
import com.example.service.StressMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RAUNAK EXPLOITS", appName)
    }

    @Test
    fun `verify default raunak engine state`() {
        val state = com.example.engine.RaunakExploitsEngine.engineState.value
        assertFalse(state.isActive)
        assertFalse(state.gpuGovernorLocked)
        assertFalse(state.cpuGovernorBalanced)
        assertEquals(com.example.engine.EngineMode.GAMING_MODE, state.selectedMode)
        assertEquals(0, state.configuredDurationMinutes)
        assertNotNull(state.logs)
    }

    @Test
    fun `verify default performance metrics initialization`() {
        val defaultMetrics = PerformanceMetrics()
        assertFalse(defaultMetrics.isActive)
        assertEquals(0, defaultMetrics.activeThreads)
        assertEquals(StressMode.GAMING_LOCK, defaultMetrics.stressMode)
        assertFalse(defaultMetrics.wakeLockHeld)
        assertNotNull(defaultMetrics.maxCores)
    }

    @Test
    fun `verify default monster service state`() {
        val state = com.example.service.MonsterServiceState()
        assertFalse(state.isRunning)
        assertFalse(state.wakeLockAcquired)
        assert(state.activeCoreCount >= 1)
    }
}
