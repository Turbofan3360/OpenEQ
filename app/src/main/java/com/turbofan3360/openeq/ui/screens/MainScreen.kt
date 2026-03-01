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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Info
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.Alignment

import com.turbofan3360.openeq.ui.components.VerticalSlider

// TODO: Make layout adaptive to different screen orientations

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
                contentColor = MaterialTheme.colorScheme.secondary,
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
        // Creating the EQ on/off toggle button at the bottom
        floatingActionButton = {PowerButton(eqEnabled, eqToggle)},
        // Centering the EQ on/off toggle button
        floatingActionButtonPosition = FabPosition.Center
    ) {
        // Defining content: Draws a colored background that fills the page, and then draws the EQ Sliders on it
        innerPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(color=MaterialTheme.colorScheme.background).padding(paddingValues=innerPadding),
        ) {
            val scope = this
            EQSliders(scope.maxHeight, frequencyBands, eqLevels, updateEqLevel)
        }
    }
}

@Composable
private fun EQSliders(
    boxHeight: Dp,
    frequencyBands: List<String>,
    eqLevels: MutableList<Float>,
    updateEqLevel: (Int, Float) -> Unit
    ) {
    val sliderHeight = 0.625f*boxHeight
    val spacerHeight = (boxHeight-sliderHeight)/10

    Row(
        modifier = Modifier.fillMaxWidth(),
        // Evenly spacing the 10 EQ sliders across the screen
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Repeating the slider 10 times across the screen
        repeat(10) { sliderNo ->
            // Generates 1 EQ slider with labels
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Defines the column:
                // Adding spacing
                Spacer(modifier=Modifier.height(spacerHeight))
                // EQ dB level text
                Text(
                    "${roundOneDP(eqLevels[sliderNo])}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                // Adding spacing
                Spacer(modifier=Modifier.height(spacerHeight))
                // EQ level slider
                VerticalSlider(
                    height = sliderHeight,
                    // Setting slider value
                    value = eqLevels[sliderNo],
                    // Modifying state variable when slider moved
                    onValueChange = { newValue -> updateEqLevel(sliderNo, roundOneDP(newValue)) },
                    trackColor = MaterialTheme.colorScheme.tertiary,
                    thumbColor = MaterialTheme.colorScheme.primary,
                    // Slider can go from -18dB to +18dB
                    valueRange = -18f..18f
                )
                // Adding spacing
                Spacer(modifier=Modifier.height(spacerHeight))
                // Frequency band text
                Text(
                    // Displaying frequency band in Hz
                    frequencyBands[sliderNo],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTitle() {
    var menuOpen by remember { mutableStateOf(false) }
    // Handles the app bar at the top of the UI
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
        },
        // Menu handling
        actions = {
            // Creating menu button
            IconButton(onClick={menuOpen = !menuOpen}) {
                Icon(
                    imageVector=Icons.Rounded.Menu,
                    contentDescription = "Options Menu",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            // Creating items in menu
            DropdownMenu(
                // Handling whether it's expanded or not, and what happens when it's closed
                expanded = menuOpen,
                onDismissRequest = {menuOpen = false}
            ) {
                // Menu items
                DropdownMenuItem(
                    // Menu item text
                    text = {
                        Text(
                            "About",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    },
                    // Icon at start of menu item
                    leadingIcon = {
                        Icon(
                            imageVector=Icons.Rounded.Info,
                            contentDescription = "Information about the app",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    onClick = {} // TODO
                )
                // TODO: Complete menu
            }
        }
    )
}

@Composable
private fun PowerButton(eqEnabled: Boolean, eqToggle: () -> Unit) {
    // Handles the button to toggle the EQ on or off
    LargeFloatingActionButton(
        onClick = {eqToggle()},
        shape= CircleShape,
        // Changing button color depending on whether EQ is enabled or not
        containerColor = if (eqEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.secondary
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