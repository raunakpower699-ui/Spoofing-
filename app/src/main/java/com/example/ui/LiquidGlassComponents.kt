package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassBorderTint
import com.example.ui.theme.GlassDeepBlue
import com.example.ui.theme.GlassHighlight
import com.example.ui.theme.GlassNavyDark
import com.example.ui.theme.GlassWhiteHigh
import com.example.ui.theme.GlassWhiteLow
import com.example.ui.theme.GlassWhiteMedium
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.VoidBlack
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Liquid Glass Canvas Backdrop with fluid luminous plasma orbs.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    isBoosterActive: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glass_fluid_orb")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isBoosterActive) 6000 else 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isBoosterActive) 1200 else 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Deep background gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(VoidBlack, GlassNavyDark, GlassDeepBlue, VoidBlack)
                )
            )

            // Dynamic fluid luminous plasma orbs
            val orb1X = width * 0.3f + cos(phase) * 120f
            val orb1Y = height * 0.25f + sin(phase) * 100f
            val orb1Radius = width * 0.6f * pulseScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (isBoosterActive) NeonCyan else Color(0xFF1E3A8A)).copy(alpha = if (isBoosterActive) 0.32f else 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = orbRadiusBound(orb1Radius)
                ),
                radius = orb1Radius,
                center = Offset(orb1X, orb1Y)
            )

            val orb2X = width * 0.75f - sin(phase * 0.8f) * 140f
            val orb2Y = height * 0.65f + cos(phase * 0.8f) * 110f
            val orb2Radius = width * 0.7f * pulseScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (isBoosterActive) NeonPurple else Color(0xFF3B0764)).copy(alpha = if (isBoosterActive) 0.35f else 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = orbRadiusBound(orb2Radius)
                ),
                radius = orb2Radius,
                center = Offset(orb2X, orb2Y)
            )

            if (isBoosterActive) {
                val orb3X = width * 0.5f + cos(phase * 1.5f) * 90f
                val orb3Y = height * 0.45f - sin(phase * 1.5f) * 90f
                val orb3Radius = width * 0.45f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonMagenta.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(orb3X, orb3Y),
                        radius = orbRadiusBound(orb3Radius)
                    ),
                    radius = orb3Radius,
                    center = Offset(orb3X, orb3Y)
                )
            }
        }

        content()
    }
}

private fun orbRadiusBound(value: Float): Float = if (value <= 0f) 1f else value

/**
 * Translucent Glassmorphic Container with multi-tone frosted gradient and refractive specular rim.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderGlowColor: Color = GlassBorderTint,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val glassBrush = Brush.linearGradient(
        colors = listOf(
            GlassWhiteHigh,
            GlassWhiteMedium,
            GlassWhiteLow,
            Color(0x05FFFFFF)
        ),
        start = Offset(0f, 0f),
        end = Offset(400f, 600f)
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            borderGlowColor,
            GlassHighlight.copy(alpha = 0.4f),
            GlassBorderTint,
            Color(0x10FFFFFF)
        ),
        start = Offset(0f, 0f),
        end = Offset(400f, 600f)
    )

    Column(
        modifier = modifier
            .clip(shape)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .background(brush = glassBrush, shape = shape)
            .padding(18.dp)
    ) {
        content()
    }
}

/**
 * Interactive Liquid Glass Button with pulsating plasma borders and tactile glow.
 */
@Composable
fun LiquidGlassButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "boost_performance_button"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "btn_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val shape = RoundedCornerShape(32.dp)

    val activeGradient = Brush.horizontalGradient(
        colors = listOf(
            NeonCyan,
            NeonPurple,
            NeonMagenta
        )
    )

    val inactiveGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF1E293B),
            Color(0xFF334155),
            Color(0xFF1E293B)
        )
    )

    val borderBrush = if (isActive) {
        Brush.horizontalGradient(
            colors = listOf(
                NeonCyan.copy(alpha = glowAlpha),
                NeonMagenta.copy(alpha = glowAlpha),
                NeonCyan.copy(alpha = glowAlpha)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                GlassHighlight,
                GlassBorderTint,
                Color(0x22FFFFFF)
            )
        )
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .height(64.dp)
            .clip(shape)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                brush = borderBrush,
                shape = shape
            )
            .background(
                brush = if (isActive) activeGradient else inactiveGradient,
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = if (isActive) Color.White else NeonCyan),
                onClick = onClick
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Filled.Pause else Icons.Filled.Bolt,
                contentDescription = if (isActive) "Stop Boost" else "Ignite Boost",
                tint = if (isActive) Color.White else NeonCyan,
                modifier = Modifier
                    .size(28.dp)
                    .then(if (isActive) Modifier.scale(glowAlpha * 0.2f + 0.9f) else Modifier)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isActive) "DISENGAGE BOOSTER" else "IGNITE GOVERNOR LOCK",
                color = if (isActive) Color.White else Color(0xFFF1F5F9),
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )
        }
    }
}

/**
 * Circular Tachometer Gauge representing live calculation throughput and Governor stress.
 */
@Composable
fun LiquidTachometer(
    opsPerSec: Double,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gauge_ring")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 4000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawTachometerArc(
                opsPerSec = opsPerSec,
                isActive = isActive,
                rotationAngle = ringRotation
            )
        }

        // Center Readout Typography
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isActive) "%.1f".format(opsPerSec) else "0.0",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isActive) NeonCyan else Color(0xFF94A3B8)
            )
            Text(
                text = "MEGA-OPS / S",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = if (isActive) NeonPurple else Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isActive) "GOVERNOR PINNED" else "IDLE STANDBY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.0.sp,
                color = if (isActive) NeonEmerald else Color(0xFF475569)
            )
        }
    }
}

private fun DrawScope.drawTachometerArc(
    opsPerSec: Double,
    isActive: Boolean,
    rotationAngle: Float
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension / 2f - 14f

    // Background track ring
    drawCircle(
        color = Color(0x1AFFFFFF),
        radius = radius,
        center = center,
        style = Stroke(width = 12f)
    )

    // Outer subtle glowing aura when active
    if (isActive) {
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(NeonCyan, NeonPurple, NeonMagenta, NeonCyan),
                center = center
            ),
            radius = radius + 6f,
            center = center,
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
    }

    // Dynamic sweep arc proportional to throughput (0 to 100+ Mops/s)
    val maxExpectedMflops = 120.0
    val fillRatio = (opsPerSec / maxExpectedMflops).coerceIn(0.05, 1.0).toFloat()
    val sweepAngle = if (isActive) (fillRatio * 260f) else 10f
    val startAngle = 140f

    val arcBrush = Brush.sweepGradient(
        colors = listOf(
            NeonCyan,
            NeonPurple,
            NeonMagenta,
            NeonAmber
        ),
        center = center
    )

    drawArc(
        brush = if (isActive) arcBrush else Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF475569))),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = 14f, cap = StrokeCap.Round)
    )
}

/**
 * Metric badge pill with glowing status indicator dot.
 */
@Composable
fun LiquidMetricPill(
    label: String,
    value: String,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .border(0.8.dp, GlassBorderTint, shape)
            .background(GlassWhiteLow, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Real-time Throughput Area Sparkline Chart.
 */
@Composable
fun LiquidThroughputChart(
    data: List<Float>,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Engage booster to stream real-time throughput",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
        return
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {
        val maxVal = (data.maxOrNull() ?: 1f).coerceAtLeast(10f)
        val minVal = 0f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val w = size.width
        val h = size.height
        val stepX = w / (data.size - 1).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()

        data.forEachIndexed { i, value ->
            val normY = 1f - ((value - minVal) / range)
            val x = i * stepX
            val y = (normY * (h - 12f)) + 6f

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevNormY = 1f - ((data[i - 1] - minVal) / range)
                val prevY = (prevNormY * (h - 12f)) + 6f
                val controlX = (prevX + x) / 2f

                path.cubicTo(controlX, prevY, controlX, y, x, y)
                fillPath.cubicTo(controlX, prevY, controlX, y, x, y)
            }
        }

        fillPath.lineTo((data.size - 1) * stepX, h)
        fillPath.close()

        // Gradient under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    NeonCyanGlow.copy(alpha = 0.45f),
                    NeonPurpleGlow.copy(alpha = 0.15f),
                    Color.Transparent
                )
            )
        )

        // Line stroke
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                colors = listOf(NeonCyan, NeonPurple, NeonMagenta)
            ),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )
    }
}
