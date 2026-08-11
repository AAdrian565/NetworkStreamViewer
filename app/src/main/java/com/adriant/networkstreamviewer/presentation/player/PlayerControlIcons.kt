package com.adriant.networkstreamviewer.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun PlaybackControlsIcon(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val knobRadius = 3.dp.toPx()
        val startX = size.width * 0.14f
        val endX = size.width * 0.86f
        val rows =
            listOf(
                size.height * 0.27f to size.width * 0.36f,
                size.height * 0.50f to size.width * 0.68f,
                size.height * 0.73f to size.width * 0.46f,
            )

        rows.forEach { (y, knobX) ->
            drawLine(
                color = color,
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawCircle(color = color, radius = knobRadius, center = Offset(knobX, y))
        }
    }
}

@Composable
internal fun PtzControlsIcon(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier = modifier) {
        val strokeWidth = 1.8.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val left = size.width * 0.12f
        val right = size.width * 0.88f
        val top = size.height * 0.12f
        val bottom = size.height * 0.88f
        val arrowInset = size.minDimension * 0.13f

        drawLine(color, Offset(left, center.y), Offset(right, center.y), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(center.x, top), Offset(center.x, bottom), strokeWidth, StrokeCap.Round)

        drawLine(color, Offset(left, center.y), Offset(left + arrowInset, center.y - arrowInset), strokeWidth)
        drawLine(color, Offset(left, center.y), Offset(left + arrowInset, center.y + arrowInset), strokeWidth)
        drawLine(color, Offset(right, center.y), Offset(right - arrowInset, center.y - arrowInset), strokeWidth)
        drawLine(color, Offset(right, center.y), Offset(right - arrowInset, center.y + arrowInset), strokeWidth)
        drawLine(color, Offset(center.x, top), Offset(center.x - arrowInset, top + arrowInset), strokeWidth)
        drawLine(color, Offset(center.x, top), Offset(center.x + arrowInset, top + arrowInset), strokeWidth)
        drawLine(color, Offset(center.x, bottom), Offset(center.x - arrowInset, bottom - arrowInset), strokeWidth)
        drawLine(color, Offset(center.x, bottom), Offset(center.x + arrowInset, bottom - arrowInset), strokeWidth)

        drawCircle(
            color = color,
            radius = size.minDimension * 0.14f,
            center = center,
            style = Stroke(width = strokeWidth),
        )
    }
}
