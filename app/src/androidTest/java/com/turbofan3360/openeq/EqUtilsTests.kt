package com.turbofan3360.openeq

import android.media.MediaPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.turbofan3360.openeq.audioprocessing.addEqualizer
import com.turbofan3360.openeq.audioprocessing.delEqualizer
import com.turbofan3360.openeq.audioprocessing.eqFrequenciesToLabels
import com.turbofan3360.openeq.audioprocessing.getEqBands
import com.turbofan3360.openeq.audioprocessing.getEqRange
import com.turbofan3360.openeq.audioprocessing.globalEqAllowed
import com.turbofan3360.openeq.audioprocessing.setEqualizer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.min

/**
 * Instrumented tests, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

private const val DECIBEL_TO_MILLIBEL = 100f

@RunWith(AndroidJUnit4::class)
class EqUtilsTests {
    lateinit var testingMediaPlayer: MediaPlayer

    @Before
    fun setup() {
        testingMediaPlayer = MediaPlayer()
    }

    @Test
    fun frequencyBandsToLabels_isCorrect() {
        // Testing the function to generate labels for the EQ bands
        assertEquals("900", eqFrequenciesToLabels(listOf(900f))[0])
        assertEquals("900.5", eqFrequenciesToLabels(listOf(900.5f))[0])

        assertEquals("2K", eqFrequenciesToLabels(listOf(2000f))[0])
        assertEquals("2.5K", eqFrequenciesToLabels(listOf(2500f))[0])
    }

    @Test
    fun addSetDelEqualizer_isCorrect() {
        // Needs to be in one function so they run in sequence
        // Testing the function to add an EQ object to an audio stream, using a testing media player
        val eqObj = addEqualizer(testingMediaPlayer.audioSessionId)
        assertTrue(eqObj.enabled)

        // Testing the function to set the equalizer levels
        val eqValsSet = listOf(-1f, 1f, 0f, 1f, -1f, 0f, 1f) // dB
        setEqualizer(eqObj, eqValsSet)

        // Have to adapt to different devices with potentially different numbers of EQ bands
        val itemsSet = min(eqValsSet.size, eqObj.numberOfBands.toInt())

        for (i in 0..<itemsSet) {
            assertEquals(eqValsSet[i], eqObj.getBandLevel(i.toShort()) / DECIBEL_TO_MILLIBEL)
        }

        // Testing the function to release the equalizer object
        delEqualizer(eqObj)
        assertThrows(IllegalStateException::class.java) { eqObj.enabled }
    }

    @Test
    fun getEqRange_isCorrect() {
        // Testing the function to ge the device EQ range
        val range = getEqRange()

        assertTrue(range.size == 2)
        assertTrue(range[0] < range[1])
        assertTrue(range[0] > -50f && range[1] < 50f) // Reasonable max EQ range - normally small, e.g. +-15db
    }

    @Test
    fun getEqBands_isCorrect() {
        // Testing the function to return device EQ bands
        val bands = getEqBands()

        // Checking values actually returned
        assertTrue(bands.isNotEmpty())

        // Checking they're in size order
        for (i in 0..<(bands.size - 1)) {
            assertTrue(bands[i] < bands[i + 1])
        }

        // Checking range is valid correct
        assertTrue(bands[0] > 0)
        assertTrue(bands.last() < 20000)
    }

    @Test
    fun globalEqAllowed_isCorrect() {
        // Testing the function to determine whether global EQ is allowed on a device or not
        val globalAllowed = globalEqAllowed()

        assertTrue(globalAllowed == true || globalAllowed == false)
    }

    @After
    fun teardown() {
        testingMediaPlayer.release()
    }
}
