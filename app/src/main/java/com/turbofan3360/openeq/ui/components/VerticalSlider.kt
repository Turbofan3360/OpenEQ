package com.turbofan3360.openeq.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
    val thumbCircleOffsets = Offset(x=heightToWidth*heightPx,
        y=(heightPx/(valueRange.endInclusive - valueRange.start))*(valueRange.endInclusive - value)
    )

    // Creates the canvas
    Canvas(
        modifier = Modifier
            .height(height)
            .width(height*heightToWidth*2f)
            // Making sliders draggable
            .draggable(
                // Which orientation to drag them in
                orientation = Orientation.Vertical,
                // What to do when dragged
                state = rememberDraggableState { dragChange ->
                    // TODO: Make the sensitivity change value here adaptive
                    onValueChange((value-0.04f*dragChange).coerceIn(valueRange.start, valueRange.endInclusive))
                }
            )
            // Hands back coordinates of thumb circle on slider
            .onGloballyPositioned { coordinates ->
                val canvasPos = coordinates.positionOnScreen()

                updateThumbPosition(canvasPos + thumbCircleOffsets)
            }
    ) {
        // Draws the central bar of the slider (with rounded ends)
        drawRoundRect(
            color = trackColor,
            style = Fill,
            alpha = 0.75f,
            size = Size(0.5f*heightToWidth*height.toPx(), height.toPx()),
            cornerRadius = CornerRadius(2f*heightToWidth*height.toPx()),
            topLeft = Offset(x=0.75f*heightToWidth*height.toPx(), y=0f)
        )
        // Draws the circle
        drawCircle(
            color = thumbColor,
            style = Fill,
            radius = heightToWidth*height.toPx(),
            // Calculating where the center should be based on the range and value of the slider
            center = thumbCircleOffsets
        )
    }
}