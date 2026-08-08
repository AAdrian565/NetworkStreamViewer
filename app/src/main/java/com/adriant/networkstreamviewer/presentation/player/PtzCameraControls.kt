package com.adriant.networkstreamviewer.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

@Composable
internal fun PtzCameraControls(
    isSupported: Boolean,
    onPanTiltSpeed: (Float, Float) -> Unit,
    onZoomSpeed: (Float) -> Unit,
    onFocusSpeed: (Float) -> Unit,
    onStop: () -> Unit,
    onAutoFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isSupported) return

    var movementSpeed by remember { mutableStateOf(DEFAULT_MOVEMENT_SPEED) }
    var driveSpeed by remember { mutableStateOf(DriveSpeed.MEDIUM) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "PTZ manual controls" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1.05f)
            ) {
                PanelLabel("PAN / TILT")
                PanTiltJoystick(
                    speed = movementSpeed,
                    onPanTiltSpeed = onPanTiltSpeed,
                    onStop = onStop,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PanelLabel("ZOOM")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PtzHoldButton(
                        label = "−",
                        contentDescription = "Zoom out",
                        onPressed = { onZoomSpeed(-MAX_ZOOM_SPEED) },
                        onReleased = onStop,
                        modifier = Modifier.weight(1f),
                        size = 38.dp
                    )
                    PtzHoldButton(
                        label = "+",
                        contentDescription = "Zoom in",
                        onPressed = { onZoomSpeed(MAX_ZOOM_SPEED) },
                        onReleased = onStop,
                        modifier = Modifier.weight(1f),
                        size = 38.dp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    PanelLabel("FOCUS", modifier = Modifier.weight(1f))
                    Button(
                        onClick = onAutoFocus,
                        modifier = Modifier
                            .height(28.dp)
                            .semantics { contentDescription = "Autofocus" },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 8.dp,
                            vertical = 0.dp
                        )
                    ) { Text("AUTO") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PtzHoldButton(
                        label = "−",
                        contentDescription = "Focus farther",
                        onPressed = { onFocusSpeed(-MAX_FOCUS_SPEED) },
                        onReleased = onStop,
                        modifier = Modifier.weight(1f),
                        size = 38.dp
                    )
                    PtzHoldButton(
                        label = "+",
                        contentDescription = "Focus nearer",
                        onPressed = { onFocusSpeed(MAX_FOCUS_SPEED) },
                        onReleased = onStop,
                        modifier = Modifier.weight(1f),
                        size = 38.dp
                    )
                }

                PanelLabel("DRIVE SPEED")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.24f), RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    DriveSpeed.entries.forEach { option ->
                        FilledTonalButton(
                            onClick = {
                                driveSpeed = option
                                movementSpeed = option.value
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (driveSpeed == option) {
                                    Color(0xFF075C65)
                                } else {
                                    Color.Transparent
                                },
                                contentColor = Color.White
                            )
                        ) {
                            Text(option.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanTiltJoystick(
    speed: Float,
    onPanTiltSpeed: (Float, Float) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val currentOnPanTiltSpeed by rememberUpdatedState(onPanTiltSpeed)
    val currentOnStop by rememberUpdatedState(onStop)

    Box(
        modifier = modifier
            .size(124.dp)
            .background(Color(0xFF17191D), CircleShape)
            .border(1.dp, Color(0xFF30343A), CircleShape)
            .pointerInput(speed) {
                val joystickCenter = Offset(size.width / 2f, size.height / 2f)
                val maxDistance = minOf(size.width, size.height) * 0.24f
                detectDragGestures(
                    onDragStart = { position ->
                        knobOffset = limitedJoystickOffset(position - joystickCenter, maxDistance)
                        sendJoystickSpeed(
                            knobOffset,
                            maxDistance,
                            speed,
                            currentOnPanTiltSpeed
                        )
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        knobOffset = limitedJoystickOffset(
                            knobOffset + dragAmount,
                            maxDistance
                        )
                        sendJoystickSpeed(
                            knobOffset,
                            maxDistance,
                            speed,
                            currentOnPanTiltSpeed
                        )
                    },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        currentOnStop()
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        currentOnStop()
                    }
                )
            }
            .semantics { contentDescription = "Pan tilt joystick" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val arrowColor = Color.White.copy(alpha = 0.9f)
            drawLine(arrowColor, center.copy(y = 19.dp.toPx()), center.copy(y = 34.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
            drawLine(arrowColor, center.copy(y = size.height - 19.dp.toPx()), center.copy(y = size.height - 34.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
            drawLine(arrowColor, center.copy(x = 19.dp.toPx()), center.copy(x = 34.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
            drawLine(arrowColor, center.copy(x = size.width - 19.dp.toPx()), center.copy(x = size.width - 34.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
            drawCircle(
                color = Color(0xFF262A30),
                radius = 20.dp.toPx(),
                center = center + knobOffset,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF262A30),
                radius = 19.dp.toPx(),
                center = center + knobOffset
            )
        }
    }
}

private fun sendJoystickSpeed(
    offset: Offset,
    maxDistance: Float,
    speed: Float,
    onPanTiltSpeed: (Float, Float) -> Unit
) {
    onPanTiltSpeed(
        -offset.x / maxDistance * speed,
        -offset.y / maxDistance * speed
    )
}

private fun limitedJoystickOffset(offset: Offset, maxDistance: Float): Offset {
    val distance = hypot(offset.x.toDouble(), offset.y.toDouble()).toFloat()
    if (distance <= maxDistance || distance == 0f) return offset
    val scale = maxDistance / distance
    return Offset(offset.x * scale, offset.y * scale)
}

@Composable
private fun PanelLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.72f),
        modifier = modifier
    )
}

@Composable
internal fun PtzHoldButton(
    label: String,
    contentDescription: String,
    onPressed: () -> Unit,
    onReleased: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 42.dp
) {
    val currentOnPressed by rememberUpdatedState(onPressed)
    val currentOnReleased by rememberUpdatedState(onReleased)

    Box(
        modifier = modifier
            .size(size)
            .background(Color(0xFF202329), RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    currentOnPressed()
                    try {
                        waitForUpOrCancellation()?.consume()
                    } finally {
                        currentOnReleased()
                    }
                }
            }
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White)
    }
}

private enum class DriveSpeed(val label: String, val value: Float) {
    LOW("LO", 0.25f),
    MEDIUM("MED", 0.5f),
    HIGH("HI", 1.0f)
}

private const val DEFAULT_MOVEMENT_SPEED = 0.5f
private const val MAX_ZOOM_SPEED = 0.5f
private const val MAX_FOCUS_SPEED = 0.5f
