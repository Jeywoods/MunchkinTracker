package com.munchkin.tracker.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.munchkin.tracker.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MagicCircleBackground(
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val maxRadius = minOf(size.width, size.height) * 0.5f

            // Внешний круг
            drawCircle(
                color = Primary.copy(alpha = 0.04f * alpha),
                radius = maxRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.dp.toPx())
            )

            // Средний круг
            drawCircle(
                color = Secondary.copy(alpha = 0.05f * alpha),
                radius = maxRadius * 0.75f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Внутренний круг
            drawCircle(
                color = Primary.copy(alpha = 0.06f * alpha),
                radius = maxRadius * 0.5f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )

            // Центральная точка
            drawCircle(
                color = GoldGlow.copy(alpha = 0.1f * alpha),
                radius = 4.dp.toPx(),
                center = Offset(centerX, centerY)
            )

            // Лучи
            val angles = 8
            for (i in 0 until angles) {
                val angle = (i.toFloat() / angles) * 360f + 22.5f
                val rad = Math.toRadians(angle.toDouble())
                val endX = centerX + (maxRadius * 0.9f * cos(rad)).toFloat()
                val endY = centerY + (maxRadius * 0.9f * sin(rad)).toFloat()

                drawLine(
                    color = Primary.copy(alpha = 0.03f * alpha),
                    start = Offset(centerX, centerY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Точки на концах лучей
            for (i in 0 until angles) {
                val angle = (i.toFloat() / angles) * 360f + 22.5f
                val rad = Math.toRadians(angle.toDouble())
                val cx = centerX + (maxRadius * 0.9f * cos(rad)).toFloat()
                val cy = centerY + (maxRadius * 0.9f * sin(rad)).toFloat()

                drawCircle(
                    color = GoldGlow.copy(alpha = 0.08f * alpha),
                    radius = 3.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }
    }
}