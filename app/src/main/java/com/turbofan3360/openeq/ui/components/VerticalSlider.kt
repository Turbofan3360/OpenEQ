package com.turbofan3360.openeq.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity

private const val heightToWidth: Float = 0.02f

@Composable
fun VerticalSlider(
    height: Dp,
    value: Float,
    onValueChange: (Float) -> Unit,
    updateThumbPosition: (Offset) -> Unit,
    trackColor: Color,
    thumbColor: Color,
    valueRange: ClosedFloatingPointRange<Float>
    ) {
    // Draws and handles a vertical slider component

    // Calculates the position offsets for the thumb circle
    val heightPx = with(LocalDensity.current) {height.toPx()}
    val circleOffsets = Offset(x=heightToWidth*heightPx,
        y=(heightPx/(valueRange.endInclusive - valueRange.start))*(valueRange.endInclusive - value)
    )
    var canvasPos by remember{ mutableStateOf(Offset.Zero) }
    var thumbPos by remember{ mutableStateOf(Offset.Zero) }

    // Creates the canvas
    Canvas(
        modifier = Modifier
            .height(height)
            .width(height*heightToWidth*2f)
            // Hands back coordinates of thumb circle on slider
            .onGloballyPositioned { coordinates ->
                canvasPos = coordinates.positionOnScreen()

                updateThumbPosition(canvasPos + circleOffsets)
            }
            // Making sliders draggable
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startPos: Offset -> thumbPos = startPos },
                    onDragEnd = { thumbPos = Offset.Zero },
                    onDragCancel = { thumbPos = Offset.Zero },
                    onDrag = { _, dragDelta ->
                        thumbPos += dragDelta
                        // Computing the slider value from the pixel positions
                        onValueChange((valueRange.endInclusive - (thumbPos.y)/(heightPx)*(valueRange.endInclusive - valueRange.start)).coerceIn(valueRange.start, valueRange.endInclusive))
                    }
                )
            }
    ) {
        // Draws the central bar of the slider (with rounded ends)
        drawRoundRect(
            color = trackColor,
            style = Fill,
            alpha = 0.75f,
            size = Size(0.5f*heightToWidth*heightPx, heightPx),
            cornerRadius = CornerRadius(2f*heightToWidth*heightPx),
            topLeft = Offset(x=0.75f*heightToWidth*heightPx, y=0f)
        )
        // Draws the circle
        drawCircle(
            color = thumbColor,
            style = Fill,
            radius = heightToWidth*heightPx,
            // Calculating where the center should be based on the range and value of the slider
            center = circleOffsets
        )
    }
}