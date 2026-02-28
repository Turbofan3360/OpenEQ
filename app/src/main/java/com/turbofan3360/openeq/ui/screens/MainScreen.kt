package com.turbofan3360.openeq.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable

// CenterAlignedTopAppBar is an experimental API so need to allow it
@Composable
fun MainScreen(
    eqEnabled: Boolean,
    eqToggle: () -> Unit,
    eqLevels: MutableList<Float>,
    updateEqLevel: (Int, Float) -> Unit,
    frequencyBands: List<String>
) {
    Scaffold(
        // Creating the "OpenEQ" app bar at the top of the main screen
        topBar = {AppTitle()},
        // Creating the bottom app bar with the reset sliders button
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.tertiary,
            ) {
                // Creating a row of items at the bottom of the screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    // Shifting everything to the right first
                    horizontalArrangement = Arrangement.End
                ) {
                    // Adding the button to zero all sliders
                    ResetButton(updateEqLevel)
                }
            }
        },
        // Creating the power button at the bottom
        floatingActionButton = {PowerButton(eqEnabled, eqToggle)},
        // Centering the EQ on/off toggle button
        floatingActionButtonPosition = FabPosition.Center
    ) {
        // Defining content: Draws a colored background that fills the page, and then draws the EQ Sliders on it
        innerPadding -> Box(modifier =
            Modifier.fillMaxSize().background(color=MaterialTheme.colorScheme.background).padding(paddingValues=innerPadding)
        ) {EQSliders(frequencyBands, eqLevels, updateEqLevel)}
    }
}

@Composable
private fun EQSliders(
    frequencyBands: List<String>,
    eqLevels: MutableList<Float>,
    updateEqLevel: (Int, Float) -> Unit
    ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        // Evenly spacing the 10 EQ sliders across the screen
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        // Repeating the slider 10 times across the screen
        repeat(10) { sliderNo ->
            // Generates 1 EQ slider with labels
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                // Defines the column:
                // EQ dB level text
                Text(
                    "${roundOneDP(eqLevels[sliderNo])}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                // EQ level slider
                Slider(
                    // Modifier to re-orient the slider visually, and re-size its footprint
                    modifier = Modifier
                        .size(width = 40.dp, height = 500.dp)
                        .rotate(-90f),

                    // Setting slider value
                    value = eqLevels[sliderNo],
                    // Modifying state variable when slider moved
                    onValueChange = { newValue -> updateEqLevel(sliderNo, roundOneDP(newValue)) },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.tertiary
                    ),
                    // Slider can go from -18dB to +18dB
                    valueRange = -18f..18f,
                )
                // Frequency band text
                Text(
                    // Displaying frequency band in Hz
                    frequencyBands[sliderNo],
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTitle() {
    CenterAlignedTopAppBar(
        // Making it look pretty
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.primary
        ),
        // App title text
        title = {
            Text(
                "OpenEQ",
                style = MaterialTheme.typography.titleLarge
            )
        }
    )
}

@Composable
private fun PowerButton(eqEnabled: Boolean, eqToggle: () -> Unit) {
    LargeFloatingActionButton(
        onClick = {eqToggle()},
        shape= CircleShape,
        // Changing button color depending on whether EQ is enabled or not
        containerColor = if (eqEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.tertiary
    )
    {
        // Setting the button icon
        Icon(
            imageVector=Icons.Rounded.PowerSettingsNew,
            contentDescription="Toggle Equalizer On or Off"
        )
    }
}

@Composable
private fun ResetButton(
    updateEqLevel: (Int, Float) -> Unit
    ) {
    // Handles the button that resets all the sliders to 0 dB
    IconButton(onClick = {for (i in 0..9) updateEqLevel(i, 0.0f)}) {
        Icon(
            imageVector = Icons.Rounded.SettingsBackupRestore,
            contentDescription="Set all EQ channels back to 0"
        )
    }
}

private fun roundOneDP(floatValue: Float): Float {
    // Utility to round floats to one decimal place
    return ((floatValue * 10).toInt()).toFloat() / 10
}