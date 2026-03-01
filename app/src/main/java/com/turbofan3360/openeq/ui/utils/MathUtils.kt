package com.turbofan3360.openeq.ui.utils

import androidx.compose.ui.geometry.Offset

fun roundOneDP(floatValue: Float): Float {
    // Utility to round floats to one decimal place
    return ((floatValue * 10).toInt()).toFloat() / 10
}

fun generateSplineControlPoint(
    prevPoint: Offset,
    point: Offset,
    nextPoint: Offset,
    nextnextPoint: Offset
    ): Pair<Offset, Offset> {
    // Uses the Catmull-Rom algorithm to generate control points for a spline
    val controlPoint1 = point + (nextPoint-prevPoint)/6f
    val controlPoint2 = nextPoint - (nextnextPoint-point)/6f

    return Pair(controlPoint1, controlPoint2)
}