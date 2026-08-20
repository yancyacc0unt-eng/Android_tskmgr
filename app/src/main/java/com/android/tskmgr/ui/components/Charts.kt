package com.android.tskmgr.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * A minimal Material-style line chart drawn on Canvas.
 * [values] are the samples, newest last. Y axis is normalized to [maxValue].
 */
@Composable
fun SparklineChart(
    values: List<Float>,
    color: Color,
    maxValue: Float = values.maxOrNull() ?: 1f,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color.Transparent)
            .padding(4.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (values.size < 2) return@Canvas
            val stepX = size.width / (values.size - 1)
            val m = if (maxValue <= 0) 1f else maxValue

            // Grid lines (horizontal)
            val gridColor = color.copy(alpha = 0.15f)
            for (i in 0..4) {
                val y = size.height * i / 4f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }

            // Area fill under the line
            val linePath = Path()
            values.forEachIndexed { i, v ->
                val x = i * stepX
                val y = size.height - (v.coerceIn(0f, m) / m) * size.height
                if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }
            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.02f)),
                    endY = size.height,
                ),
            )

            // Line stroke
            drawPath(
                path = linePath,
                color = color,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

/** A progress bar with rounded corners, e.g. for used memory / storage. */
@Composable
fun PercentBar(
    percent: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val radius = size.height / 2
        val bgColor = color.copy(alpha = 0.15f)
        drawLine(
            color = bgColor,
            start = Offset(radius, size.height / 2),
            end = Offset(size.width - radius, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Round,
        )
        val clamped = percent.coerceIn(0.0, 100.0) / 100.0
        val endX = radius + (size.width - 2 * radius) * clamped.toFloat()
        if (endX > radius) {
            drawLine(
                color = color,
                start = Offset(radius, size.height / 2),
                end = Offset(endX, size.height / 2),
                strokeWidth = size.height,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Binds the history list of doubles/longs into floats and forwards to [SparklineChart]. */
@Composable
fun <T : Number> HistoryChart(
    values: List<T>,
    color: Color,
    maxValue: Float,
    modifier: Modifier = Modifier,
) {
    val floats = values.map { it.toFloat() }
    SparklineChart(values = floats, color = color, maxValue = max(1f, maxValue), modifier = modifier)
}
