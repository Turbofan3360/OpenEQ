package com.turbofan3360.openeq.audioprocessing

import android.media.audiofx.Equalizer
import kotlin.math.round

// A series of utility functions to help the code manage equalizer instances

fun addEqualizer(
    audioSession: Int
    ): Equalizer {
    // Adds an equalizer session to a given audio stream and returns the EQ object
    val eqObject = Equalizer(0, audioSession)
    eqObject.setEnabled(true)

    return eqObject
}

fun delEqualizer(
    eq: Equalizer
    ) {
    // Tidies up and deletes an EQ instance
    eq.release()
}

fun setEqualizer(
    eq: Equalizer,
    levels: List<Float>
    ) {
    // Sets levels of an equalizer instance to the given values
    for (i in 0..<levels.size) {
        eq.setBandLevel(i.toShort(), round(levels[i] * 100).toInt().toShort())
    }
}

fun getEqBands(): List<Float> {
    // Returns a list of the center frequencies of the EQ bands available in Hz
    val eqObj = addEqualizer(0)
    val frequencies = mutableListOf<Float>()

    for (i in 0..<eqObj.numberOfBands) {
        frequencies += eqObj.getCenterFreq(i.toShort())/1000f
    }

    delEqualizer(eqObj)

    return frequencies.toList()
}

fun getEqRange(): List<Float> {
    // Returns a [min, max] float value range that the EQ lets you select on this device
    val eqObj = addEqualizer(0)
    val eqRange = eqObj.getBandLevelRange()
    delEqualizer(eqObj)

    return listOf(eqRange[0]/100f, eqRange[1]/100f)
}

fun eqFrequenciesToLabels(
    frequencyBands: List<Float>
    ): List<String> {
    // Takes in the list of frequency bands (in Hz) and generates nice labels for them
    val labels = mutableListOf<String>()
    var baseStr: String

    for (freq in frequencyBands) {
        if (freq < 1000) {
            // For frequencies below 1KHz - can just convert straight to string and add to list
            labels += freq.toString().dropLastWhile { it == '0' }.dropLastWhile { it == '.' }
        }
        else {
            // Generates nice "1K"/"2K"/e.t.c labels for frequencies above 1KHz
            baseStr = (freq/1000f).toString().dropLastWhile { it == '0' }.dropLastWhile { it == '.' }
            labels += "${baseStr}K"
        }
    }

    return labels.toList()
}