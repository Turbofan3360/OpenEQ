package com.turbofan3360.openeq.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.turbofan3360.openeq.R
import com.turbofan3360.openeq.ui.components.DeletePresetDialog
import com.turbofan3360.openeq.ui.components.LoadPresetDialog
import com.turbofan3360.openeq.ui.components.SavePresetDialog
import com.turbofan3360.openeq.ui.components.UpdatePresetDialog
import com.turbofan3360.openeq.ui.components.VerticalSlider
import com.turbofan3360.openeq.ui.utils.generateSplineControlPoints
import com.turbofan3360.openeq.ui.utils.roundOneDP

private const val SLIDER_HEIGHT_SCALAR_PORTRAIT = 0.625f
private const val SLIDER_HEIGHT_SCALAR_LANDSCAPE = 0.8f
private const val SPACER_HEIGHT_SCALAR = 10
private const val LANDSCAPE_SLIDERS_WIDTH_SCALAR = 0.85f
private const val LANDSCAPE_PADDING_SIZE_SCALAR = 0.025f

@Composable
fun MainScreen(
    eqEnabled: Boolean,
    eqToggle: () -> Unit,
    tryGlobal: Boolean,
    toggleGlobal: () -> Unit,
    eqLevels: List<Float>,
    updateEqLevel: (Int, Float) -> Unit,
    frequencyBands: List<String>,
    eqRange: List<Float>,

    onPresetSelect: (String) -> Unit,
    onPresetSave: (String) -> Unit,
    onPresetDelete: (String) -> Unit,
    onPresetUpdate: (String) -> Unit
) {
    // Saving state of thumb positions on sliders
    val thumbPositions = remember { List(frequencyBands.size) { Offset.Zero }.toMutableStateList() }
    // Grabbing screen orientation and setting it as boolean
    val isPortrait = (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT)

    Scaffold(
        // Creating the "OpenEQ" app bar at the top of the main screen
        topBar = {
            AppTitleBar(
                tryGlobal,
                toggleGlobal,
                onPresetSelect,
                onPresetSave,
                onPresetDelete,
                onPresetUpdate
            )
        },
        // Creating the bottom app bar with the reset sliders button
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.secondary,
            ) {
                // Creating a row of items at the bottom of the screen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    // Shifting everything to the right first
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Adding the button to zero all sliders
                    ResetButton(updateEqLevel, frequencyBands.size)
                }
            }
        },
        // Creating the EQ on/off toggle button at the bottom
        floatingActionButton = { PowerButton(eqEnabled, eqToggle) },
        // Centering the EQ on/off toggle button
        floatingActionButtonPosition = if (isPortrait) FabPosition.Center else FabPosition.End

        // Defining content: Draws a colored background that fills the page
        // Then draws the EQ Sliders on it, and then a curve between the EQ sliders
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
                .padding(paddingValues = innerPadding),
        ) {
            // Grabbing scope (i.e. box size) parameters
            val scope = this

            val topPadding = with(LocalDensity.current) { innerPadding.calculateTopPadding().toPx() }
            val sidePadding =
                with(LocalDensity.current) { innerPadding.calculateLeftPadding(LayoutDirection.Ltr).toPx() }

            EQSliders(
                listOf(scope.maxWidth, scope.maxHeight),
                isPortrait,
                frequencyBands,
                eqRange,
                eqLevels,
                updateEqLevel,
                thumbPositions
            )

            // Drawing the curve on top of the EQ sliders
            EQCurve(MaterialTheme.colorScheme.primary, thumbPositions, topPadding, sidePadding)
        }
    }
}

@Composable
private fun EQSliders(
    boxSize: List<Dp>,
    isPortrait: Boolean,
    frequencyBands: List<String>,
    eqRange: List<Float>,
    eqLevels: List<Float>,
    updateEqLevel: (Int, Float) -> Unit,
    thumbPositions: MutableList<Offset>
) {
    // Simple scaling of sliders and spacers to adapt to the screen size
    // Changes scaling depending on screen orientation
    val sliderHeight = if (isPortrait) {
        SLIDER_HEIGHT_SCALAR_PORTRAIT * boxSize[1]
    } else {
        SLIDER_HEIGHT_SCALAR_LANDSCAPE * boxSize[1]
    }

    val spacerHeight = (boxSize[1] - sliderHeight) / SPACER_HEIGHT_SCALAR

    Row(
        // Tweaks width of sliders depending on screen orientation
        modifier = if (isPortrait) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.width(LANDSCAPE_SLIDERS_WIDTH_SCALAR * boxSize[0])
        },

        // Evenly spacing the EQ sliders across the screen
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // If in landscape: spacing things in from the screen edge a little bit
        if (!isPortrait) {
            Spacer(modifier = Modifier.width(LANDSCAPE_PADDING_SIZE_SCALAR * boxSize[0]))
        }
        // Repeating the slider the right number of times across the screen
        repeat(frequencyBands.size) { sliderNo ->
            // Generates 1 EQ slider with labels
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Defines the column:
                // Adding spacing
                Spacer(modifier = Modifier.height(spacerHeight))
                // EQ dB level text
                Text(
                    "${roundOneDP(eqLevels[sliderNo])}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                // Adding spacing
                Spacer(modifier = Modifier.height(spacerHeight))
                // EQ level slider
                VerticalSlider(
                    height = sliderHeight,
                    // Setting slider value
                    value = eqLevels[sliderNo],
                    // Modifying state variable when slider moved
                    onValueChange = { newValue -> updateEqLevel(sliderNo, roundOneDP(newValue)) },
                    updateThumbPosition = { coordinates -> thumbPositions[sliderNo] = coordinates },
                    colors = MaterialTheme.colorScheme,
                    // Adapting slider range to be whatever the system supports
                    valueRange = eqRange
                )
                // Adding spacing
                Spacer(modifier = Modifier.height(spacerHeight))
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

@Composable
private fun EQCurve(
    pathColor: Color,
    thumbPositions: List<Offset>,
    topPadding: Float,
    sidePadding: Float
) {
    // Generates the curve between each of the EQ points
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val path = Path()
        var pointOne: Offset
        var pointTwo: Offset

        // Moving to the starting point (need to adjust for padding from other components)
        path.moveTo(thumbPositions[0].x - sidePadding, thumbPositions[0].y - topPadding)

        // Iterating through terms to add curves between thumb points on sliders to the path
        for (i in 0..(thumbPositions.size - 2)) {
            // Handling edge case with first point
            pointOne = if (i == 0) {
                Offset(x = thumbPositions[0].x * 0.5f, y = thumbPositions[0].y)
            } else {
                thumbPositions[i - 1]
            }

            // Handling edge case with final point
            pointTwo = if (i == thumbPositions.size - 2) {
                Offset(
                    x = thumbPositions[thumbPositions.size - 1].x + 20,
                    y = thumbPositions[thumbPositions.size - 1].y
                )
            } else {
                thumbPositions[i + 2]
            }

            // Finding curve control points
            val (point1, point2) = generateSplineControlPoints(
                pointOne,
                thumbPositions[i],
                thumbPositions[i + 1],
                pointTwo
            )

            // Adding another curve to the spline
            path.cubicTo(
                // Control point 1
                x1 = point1.x - sidePadding,
                y1 = point1.y - topPadding,
                // Control point 2
                x2 = point2.x - sidePadding,
                y2 = point2.y - topPadding,
                // Destination point
                x3 = thumbPositions[i + 1].x - sidePadding,
                y3 = thumbPositions[i + 1].y - topPadding
            )
        }
        // Drawing the path
        drawPath(
            color = pathColor,
            alpha = 0.5f,
            style = Stroke(width = 10f),
            path = path
        )
    }
}

// CenterAlignedTopAppBar is an experimental API so need to allow it
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTitleBar(
    tryGlobal: Boolean,
    toggleGlobal: () -> Unit,
    onPresetSelect: (String) -> Unit,
    onPresetSave: (String) -> Unit,
    onPresetDelete: (String) -> Unit,
    onPresetUpdate: (String) -> Unit
) {
    var optionSelected by remember { mutableStateOf("") }
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
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge
            )
        },

        // Menu handling
        actions = {
            // Creating menu button
            IconButton(onClick = { menuOpen = !menuOpen }) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = stringResource(R.string.menu_icon_description),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            // Creating items in menu
            DropdownMenu(
                // Handling whether it's expanded or not, and what happens when it's closed
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                // Function to handle menu options relating to the app configuration
                TopBarMenuConfigItems(tryGlobal, toggleGlobal)

                // Placing a line between the other things and the preset controls
                HorizontalDivider()

                // Function to handle menu options relating to user-defined presets
                TopBarMenuPresetItems(
                    closeMenu = { menuOpen = false },
                    setOption = { value -> optionSelected = value }
                )
            }
        }
    )

    // Calling the correct dialog composables depending on what the user chose to do
    when (optionSelected) {
        "save_preset" -> SavePresetDialog(onPresetSave) { optionSelected = "" }
        "update_preset" -> UpdatePresetDialog(onPresetUpdate) { optionSelected = "" }
        "load_preset" -> LoadPresetDialog(onPresetSelect) { optionSelected = "" }
        "delete_preset" -> DeletePresetDialog(onPresetDelete) { optionSelected = "" }
    }
}

@Composable
private fun TopBarMenuConfigItems(
    tryGlobal: Boolean,
    toggleGlobal: () -> Unit
) {
    // Handles the menu items used to configure the equalizer and provide general info

    val uriHandler = LocalUriHandler.current

    // About app item
    DropdownMenuItem(
        // Menu item text
        text = { SmallSecondaryText(stringResource(R.string.menu_info)) },
        // Icon at start of menu item
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = stringResource(R.string.menu_info_icon_description),
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        onClick = { uriHandler.openUri("https://github.com/Turbofan3360/OpenEQ?tab=readme-ov-file#openeq") }
    )

    // Checkbox selector to try attaching the EQ to the global mix
    DropdownMenuItem(
        text = {
            SmallSecondaryText(stringResource(R.string.menu_attach_global_mix))
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = stringResource(R.string.menu_attach_global_mix_icon_description),
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        trailingIcon = {
            Checkbox(
                checked = tryGlobal,
                onCheckedChange = { toggleGlobal() },
                modifier = Modifier.padding(0.dp)
            )
        },
        onClick = { toggleGlobal() }
    )
}

@Composable
private fun TopBarMenuPresetItems(
    closeMenu: () -> Unit,
    setOption: (String) -> Unit
) {
    // Button to save current EQ values as a new preset to the database
    DropdownMenuItem(
        onClick = {
            closeMenu()
            setOption("save_preset")
        },
        text = { SmallSecondaryText(stringResource(R.string.menu_save_new_preset)) },
        // Icon at start of menu item
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Save,
                contentDescription = stringResource(R.string.menu_save_as_icon_description),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    )

    // Button to load a preset from the database
    DropdownMenuItem(
        onClick = {
            closeMenu()
            setOption("load_preset")
        },
        text = { SmallSecondaryText(stringResource(R.string.menu_load_preset)) },
        // Icon at start of menu item
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Cached,
                contentDescription = stringResource(R.string.menu_load_preset_icon_description),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    )

    // Button to update a preset in the database
    DropdownMenuItem(
        onClick = {
            closeMenu()
            setOption("update_preset")
        },
        text = { SmallSecondaryText(stringResource(R.string.menu_update_preset)) },
        // Icon at start of menu item
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = stringResource(R.string.menu_update_preset_icon_description),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    )

    // Button to delete a preset from the database
    DropdownMenuItem(
        onClick = {
            closeMenu()
            setOption("delete_preset")
        },
        text = { SmallSecondaryText(stringResource(R.string.menu_delete_preset)) },
        // Icon at start of menu item
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.menu_delete_preset_icon_description),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    )
}

@Composable
fun SmallSecondaryText(inputText: String) {
    Text(
        text = inputText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun PowerButton(eqEnabled: Boolean, eqToggle: () -> Unit) {
    // Handles the button to toggle the EQ on or off
    LargeFloatingActionButton(
        onClick = { eqToggle() },
        shape = CircleShape,
        // Changing button color depending on whether EQ is enabled or not
        containerColor = if (eqEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.secondary
    ) {
        // Setting the button icon
        Icon(
            imageVector = Icons.Rounded.PowerSettingsNew,
            contentDescription = stringResource(R.string.eq_toggle_button_description)
        )
    }
}

@Composable
private fun ResetButton(
    updateEqLevel: (Int, Float) -> Unit,
    numEqBands: Int
) {
    // Handles the button that resets all the sliders to 0 dB
    IconButton(onClick = { for (i in 0..<numEqBands) updateEqLevel(i, 0.0f) }) {
        Icon(
            imageVector = Icons.Rounded.SettingsBackupRestore,
            contentDescription = stringResource(R.string.eq_reset_button_description)
        )
    }
}
