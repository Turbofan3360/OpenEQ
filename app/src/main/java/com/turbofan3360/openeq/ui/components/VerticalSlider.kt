package com.turbofan3360.openeq.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

private const val HEIGHT_TO_WIDTH = 0.02f
private const val BAR_WIDTH_TO_BOX_WIDTH = 0.5f
private const val BOX_WIDTH_TO_BAR_WIDTH = 1f / BAR_WIDTH_TO_BOX_WIDTH
private const val SLIDER_BAR_START_X_OFFSET_TO_WIDTH = 0.5f * (BOX_WIDTH_TO_BAR_WIDTH - BAR_WIDTH_TO_BOX_WIDTH)

@Composable
fun VerticalSlider(
    height: Dp,
    value: Float,
    onValueChange: (Float) -> Unit,
    updateThumbPosition: (Offset) -> Unit,
    colors: ColorScheme,
    valueRange: List<Float>
) {
    // Draws and handles a vertical slider component

    // Calculates the position offsets for the thumb circle
    val sliderRange = valueRange[1] - valueRange[0]
    val heightPx = with(LocalDensity.current) { height.toPx() }
    val circleOffsets = Offset(
        x = HEIGHT_TO_WIDTH * heightPx,
        y = (heightPx / sliderRange) * (valueRange[1] - value)
    )
    var canvasPos by remember { mutableStateOf(Offset.Zero) }
    var thumbPos by remember { mutableStateOf(Offset.Zero) }

    // Creates the canvas
    Canvas(
        modifier = Modifier
            .height(height)
            .width(height * HEIGHT_TO_WIDTH * BOX_WIDTH_TO_BAR_WIDTH)
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
                        val eqValue = valueRange[1] - (thumbPos.y / heightPx) * sliderRange
                        onValueChange(eqValue.coerceIn(valueRange[0], valueRange[1]))
                    }
                )
            }
    ) {
        // Draws the central bar of the slider (with rounded ends)
        drawRoundRect(
            color = colors.tertiary,
            style = Fill,
            alpha = 0.75f,
            size = Size(BAR_WIDTH_TO_BOX_WIDTH * HEIGHT_TO_WIDTH * heightPx, heightPx),
            cornerRadius = CornerRadius(BOX_WIDTH_TO_BAR_WIDTH * HEIGHT_TO_WIDTH * heightPx),
            topLeft = Offset(x = SLIDER_BAR_START_X_OFFSET_TO_WIDTH * HEIGHT_TO_WIDTH * heightPx, y = 0f)
        )
        // Draws the circle
        drawCircle(
            color = colors.primary,
            style = Fill,
            radius = HEIGHT_TO_WIDTH * heightPx,
            // Calculating where the center should be based on the range and value of the slider
            center = circleOffsets
        )
    }
}
