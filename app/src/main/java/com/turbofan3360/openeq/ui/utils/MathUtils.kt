package com.turbofan3360.openeq.ui.utils

import androidx.compose.ui.geometry.Offset

fun roundOneDP(floatValue: Float): Float {
    // Utility to round floats to one decimal place
    return (floatValue * 10f).toInt() / 10f
}

fun generateSplineControlPoint(
    prevPoint: Offset,
    point: Offset,
    nextPoint: Offset,
    nextNextPoint: Offset
): Pair<Offset, Offset> {
    // Uses the Catmull-Rom algorithm to generate control points for a spline
    val controlPoint1 = point + (nextPoint - prevPoint) / 6f
    val controlPoint2 = nextPoint - (nextNextPoint - point) / 6f

    return Pair(controlPoint1, controlPoint2)
}
