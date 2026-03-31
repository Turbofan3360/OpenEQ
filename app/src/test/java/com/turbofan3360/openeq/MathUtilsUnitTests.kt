package com.turbofan3360.openeq

import androidx.compose.ui.geometry.Offset
import com.turbofan3360.openeq.ui.utils.generateSplineControlPoints
import com.turbofan3360.openeq.ui.utils.roundOneDP
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Local unit tests, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

class MathUtilsUnitTests {
    @Test
    fun uiRounding_isCorrect() {
        // Testing the ui/utils function for rounding to one decimal place
        assertEquals(3.1f, roundOneDP(3.14159f))
        assertEquals(1.6f, roundOneDP(1.58284f))
        assertEquals(-2.4f, roundOneDP(-2.4378f))
        assertEquals(-1.2f, roundOneDP(-1.182f))
        assertEquals(0f, roundOneDP(0.047f))
    }

    @Test
    fun uiCurveControlPoint_isCorrect() {
        // Testing the ui/utils function for generating control points for a spline
        // Testing all points are horizontal (important base case for the EQ)
        val (p1, p2) = generateSplineControlPoints(
            Offset(x = 0f, y = 5f),
            Offset(x = 6f, y = 5f),
            Offset(x = 12f, y = 5f),
            Offset(x = 18f, y = 5f)
        )

        assertOffsetEquals(Offset(x = 8f, y = 5f), p1)
        assertOffsetEquals(Offset(x = 10f, y = 5f), p2)

        // Testing an actual curve
        val (p3, p4) = generateSplineControlPoints(
            Offset(x = 0f, y = 12f),
            Offset(x = 6f, y = 6f),
            Offset(x = 12f, y = 9f),
            Offset(x = 18f, y = 3f)
        )

        assertOffsetEquals(Offset(x = 8f, y = 5.5f), p3)
        assertOffsetEquals(Offset(x = 10f, y = 9.5f), p4)
    }

    fun assertOffsetEquals(
        computedVal: Offset,
        trueVal: Offset
    ) {
        assertEquals(trueVal.x, computedVal.x)
        assertEquals(trueVal.y, computedVal.y)
    }
}
