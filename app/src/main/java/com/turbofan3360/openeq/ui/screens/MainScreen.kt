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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import android.content.res.Configuration

import com.turbofan3360.openeq.R
import com.turbofan3360.openeq.ui.components.VerticalSlider
import com.turbofan3360.openeq.ui.utils.roundOneDP
import com.turbofan3360.openeq.ui.utils.generateSplineControlPoint

@Composable
fun MainScreen(
    eqEnabled: Boolean,
    eqToggle: () -> Unit,
    eqLevels: MutableList<Float>,
    updateEqLevel: (Int, Float) -> Unit,
    frequencyBands: List<String>,
    eqRange: List<Float>,

    presetIds: List<String>,
    onPresetSelect: (String) -> Unit,
    onPresetSave: (String) -> Unit,
    onPresetDelete: (String) -> Unit,
    onPresetUpdate: (String) -> Unit
) {
    // Saving state of thumb positions on sliders
    val thumbPositions = remember { mutableStateListOf(*MutableList(frequencyBands.size) {Offset.Zero}.toTypedArray()) }
    // Grabbing screen orientation and setting it as boolean
    val isPortrait = (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT)

    Scaffold(
        // Creating the "OpenEQ" app bar at the top of the main screen
        topBar = {AppTitle(presetIds, onPresetSelect, onPresetSave, onPresetDelete, onPresetUpdate)},
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
        floatingActionButton = {PowerButton(eqEnabled, eqToggle)},
        // Centering the EQ on/off toggle button
        floatingActionButtonPosition = if (isPortrait) FabPosition.Center else FabPosition.End
    ) {
        // Defining content: Draws a colored background that fills the page, draws the EQ Sliders on it, and then a curve between the EQ sliders
        innerPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(color=MaterialTheme.colorScheme.background).padding(paddingValues=innerPadding),
        ) {
            // Grabbing scope (i.e. box size) parameters
            val scope = this

            val topPadding = with(LocalDensity.current) {innerPadding.calculateTopPadding().toPx()}
            val sidePadding = with(LocalDensity.current) {innerPadding.calculateLeftPadding(LayoutDirection.Ltr).toPx()}

            EQSliders(scope.maxHeight, scope.maxWidth, isPortrait, frequencyBands, eqRange[0], eqRange[1], eqLevels, updateEqLevel, thumbPositions)
            // Drawing the curve on top of the EQ sliders
            EQCurve(MaterialTheme.colorScheme.primary, thumbPositions, topPadding, sidePadding)
        }
    }
}

@Composable
private fun EQSliders(
    boxHeight: Dp,
    boxWidth: Dp,
    isPortrait: Boolean,
    frequencyBands: List<String>,
    eqMin: Float,
    eqMax: Float,
    eqLevels: MutableList<Float>,
    updateEqLevel: (Int, Float) -> Unit,
    thumbPositions: MutableList<Offset>
    ) {
    // Simple scaling of sliders and spacers to adapt to the screen size - changes scaling depending on screen orientation
    val sliderHeight = if (isPortrait) 0.625f*boxHeight else 0.8f*boxHeight
    val spacerHeight = (boxHeight-sliderHeight)/10

    Row(
        // Tweaks positioning of sliders depending on screen orientation
        modifier = if (isPortrait) Modifier.fillMaxWidth() else Modifier.width(0.85f*boxWidth),
        // Evenly spacing the EQ sliders across the screen
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // If in landscape: spacing things in from the screen edge a little bit
        if (!isPortrait) {
            Spacer(modifier=Modifier.width(0.025f*boxWidth))
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
                    updateThumbPosition = {coordinates -> thumbPositions[sliderNo] = coordinates},
                    trackColor = MaterialTheme.colorScheme.tertiary,
                    thumbColor = MaterialTheme.colorScheme.primary,
                    // Adapting slider range to be whatever the system supports
                    valueRange = eqMin..eqMax
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


@Composable
private fun EQCurve(
    pathColor: Color,
    thumbPositions: List<Offset>,
    topPadding: Float,
    sidePadding: Float,
    ) {
    // Generates the curve between each of the EQ points
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val path = Path()

        // Moving to the starting point (need to adjust for padding from other components)
        path.moveTo(thumbPositions[0].x - sidePadding, thumbPositions[0].y - topPadding)

        // Iterating through terms to add curves between thumb points on sliders to the path
        for (i in 0..(thumbPositions.size-2)) {
            // Finding curve control points
            val (point1, point2) = generateSplineControlPoint(
                // Handling edge case with first point
                if (i!=0) thumbPositions[i-1] else Offset(x=thumbPositions[0].x*0.5f, y=thumbPositions[0].y),
                thumbPositions[i],
                thumbPositions[i+1],
                // Handling edge case with final point
                if (i!=thumbPositions.size-2) thumbPositions[i+2] else Offset(x=thumbPositions[thumbPositions.size-1].x+20,
                                                                                y=thumbPositions[thumbPositions.size-1].y)
            )
            // Adding another curve to the spline
            path.cubicTo(
                // Control point 1
                x1 = point1.x-sidePadding,
                y1 = point1.y-topPadding,
                // Control point 2
                x2 = point2.x-sidePadding,
                y2 = point2.y-topPadding,
                // Destination point
                x3 = thumbPositions[i+1].x-sidePadding,
                y3 = thumbPositions[i+1].y-topPadding
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
private fun AppTitle(
    presetIds: List<String>,
    onPresetSelect: (String) -> Unit,
    onPresetSave: (String) -> Unit,
    onPresetDelete: (String) -> Unit,
    onPresetUpdate: (String) -> Unit
    ) {
    val uriHandler = LocalUriHandler.current

    var menuOpen by remember { mutableStateOf(false) }
    var savePresetDialogOpen by remember { mutableStateOf(false) }
    var updatePresetDialogOpen by remember { mutableStateOf(false) }
    var loadPresetDialogOpen by remember { mutableStateOf(false) }
    var deletePresetDialogOpen by remember { mutableStateOf(false) }

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
            IconButton(onClick={menuOpen = !menuOpen}) {
                Icon(
                    imageVector=Icons.Rounded.Menu,
                    contentDescription = stringResource(R.string.menu_icon_description),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            // Creating items in menu
            DropdownMenu(
                // Handling whether it's expanded or not, and what happens when it's closed
                expanded = menuOpen,
                onDismissRequest = {menuOpen = false},
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                // Menu items
                DropdownMenuItem(
                    // Menu item text
                    text = {
                        Text(
                            stringResource(R.string.menu_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    },
                    // Icon at start of menu item
                    leadingIcon = {
                        Icon(
                            imageVector=Icons.Rounded.Info,
                            contentDescription = stringResource(R.string.menu_info_icon_description),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    onClick = { uriHandler.openUri("https://github.com/Turbofan3360/OpenEQ?tab=readme-ov-file#openeq") }
                )

                // --------------------------------------------------------------
                // Placing a line between the other things and the preset controls
                HorizontalDivider()

                // Button to save current EQ values as a new preset to the database
                DropdownMenuItem(
                    onClick = {
                        menuOpen = false
                        savePresetDialogOpen = true
                    },
                    text = {
                        Text(
                            stringResource(R.string.menu_save_new_preset),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    },
                    // Icon at start of menu item
                    leadingIcon = {
                        Icon(
                            imageVector=Icons.Rounded.Save,
                            contentDescription = stringResource(R.string.menu_save_as_icon_description),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                )

                // Button to load a preset from the database
                DropdownMenuItem(
                    onClick = {
                        menuOpen = false
                        loadPresetDialogOpen = true
                    },
                    text = {
                        Text(
                            stringResource(R.string.menu_load_preset),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    },
                    // Icon at start of menu item
                    leadingIcon = {
                        Icon(
                            imageVector=Icons.Rounded.Cached,
                            contentDescription = stringResource(R.string.menu_load_preset_icon_description),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                )

                // Button to update a preset in the database
                DropdownMenuItem(
                    onClick = {
                        menuOpen = false
                        updatePresetDialogOpen = true
                    },
                    text = {
                        Text(
                            stringResource(R.string.menu_update_preset),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    },
                    // Icon at start of menu item
                    leadingIcon = {
                        Icon(
                            imageVector=Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.menu_update_preset_icon_description),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                )

                // Button to delete a preset from the database
                DropdownMenuItem(
                    onClick = {
                        menuOpen = false
                        deletePresetDialogOpen = true
                    },
                    text = {
                        Text(
                            stringResource(R.string.menu_delete_preset),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    },
                    // Icon at start of menu item
                    leadingIcon = {
                        Icon(
                            imageVector=Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.menu_delete_preset_icon_description),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                )
            }
        }
    )

    // Save preset dialog
    PresetSaveDialog(
        savePresetDialogOpen,
        onPresetSave,
        onDismiss = { savePresetDialogOpen = false }
    )

    // Update preset dialog
    PresetDialogStructure(
        updatePresetDialogOpen,
        presetIds,
        onPresetUpdate,
        onDismiss = { updatePresetDialogOpen = false },
        Icons.Rounded.Edit,
        stringResource(R.string.update_preset_dialog_icon_description),
        stringResource(R.string.update_preset_dialog_title),
        stringResource(R.string.update_preset_dialog_text)
    )

    // Load preset dialog
    PresetDialogStructure(
        loadPresetDialogOpen,
        presetIds,
        onPresetSelect,
        onDismiss = { loadPresetDialogOpen = false },
        Icons.Rounded.Cached,
        stringResource(R.string.load_preset_dialog_icon_description),
        stringResource(R.string.load_preset_dialog_title),
        stringResource(R.string.load_preset_dialog_text)
    )

    // Delete preset dialog
    PresetDialogStructure(
        deletePresetDialogOpen,
        presetIds,
        onPresetDelete,
        onDismiss = { deletePresetDialogOpen = false },
        Icons.Rounded.Delete,
        stringResource(R.string.delete_preset_dialog_icon_description),
        stringResource(R.string.delete_preset_dialog_title),
        stringResource(R.string.delete_preset_dialog_text)
    )
}

@Composable
private fun PresetDialogStructure(
    showDialog: Boolean,
    presetIds: List<String>,
    presetAction: (String) -> Unit,
    onDismiss: () -> Unit,

    iconImageVector: ImageVector,
    iconDescription: String,
    title: String,
    bodyText: String
) {
    var selectedPreset by remember{ mutableStateOf("") }

    // A pop-up dialog to request the user input a preset ID to save the current EQ levels to
    if (showDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            iconContentColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.tertiary,
            textContentColor = MaterialTheme.colorScheme.tertiary,

            icon = { Icon(
                imageVector=iconImageVector,
                contentDescription=iconDescription
            ) },
            title = { Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
            ) },
            onDismissRequest =  {
                onDismiss()
            },
            confirmButton = { TextButton(
                onClick = {
                    presetAction(selectedPreset)
                    onDismiss()
                }
            ) {
                Text(
                    stringResource(R.string.dialog_confirm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            } },
            dismissButton = { TextButton(
                onClick = {
                    onDismiss()
                }
            ) {
                Text(
                    stringResource(R.string.dialog_dismiss),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            } },
            text = {
                Column {
                    Text(
                        text = bodyText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PresetIdsDropDown(
                        presetIds,
                        onSelect = { id -> selectedPreset = id }
                    )
                }
            }
        )
    }
}

@Composable
private fun PresetSaveDialog(
    showDialog: Boolean,
    onPresetSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val presetInputState = rememberTextFieldState("")

    // A pop-up dialog to request the user input a preset ID to save the current EQ levels to
    if (showDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            iconContentColor = MaterialTheme.colorScheme.secondary,
            titleContentColor = MaterialTheme.colorScheme.tertiary,
            textContentColor = MaterialTheme.colorScheme.tertiary,

            icon = { Icon(
                imageVector=Icons.Rounded.Save,
                contentDescription=stringResource(R.string.save_preset_dialog_icon_description)
            ) },
            title = { Text(
                stringResource(R.string.save_preset_dialog_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
            ) },
            onDismissRequest =  {
                onDismiss()
                // Resetting text field state
                presetInputState.setTextAndPlaceCursorAtEnd("")
            },
            confirmButton = { TextButton(
                onClick = {
                    // If preset ID entered + user confirms - save the preset ID to the database, then dismiss the dialog
                    onPresetSave(presetInputState.text.toString())
                    // Resetting text field state
                    presetInputState.setTextAndPlaceCursorAtEnd("")
                    onDismiss()
                }
            ) {
                Text(
                    stringResource(R.string.dialog_confirm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } },
            dismissButton = { TextButton(
                onClick = {
                    onDismiss()
                    // Resetting text field state
                    presetInputState.setTextAndPlaceCursorAtEnd("")
                }
            ) {
                Text(
                    stringResource(R.string.dialog_dismiss),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } },
            // Actual input box for the user to enter their preset name
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.save_preset_dialog_text),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        state = presetInputState,
                        label = { Text(stringResource(R.string.save_preset_dialog_input_box_label)) },
                        textStyle = MaterialTheme.typography.bodySmall,

                        colors = TextFieldDefaults.colors(
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetIdsDropDown(
    presetIds: List<String>,
    onSelect: (String) -> Unit
    ) {
    var dropdownOpen by remember{ mutableStateOf(false) }
    val textFieldState = rememberTextFieldState(stringResource(R.string.preset_dropdown_field_default))

    // Dropdown box to select a preset you want to do something to
    ExposedDropdownMenuBox(
        expanded = dropdownOpen,
        onExpandedChange = { dropdownOpen = it }
    ) {
        OutlinedTextField(
            readOnly = true,
            state = textFieldState,
            label = { stringResource(R.string.preset_dropdown_field_label) },
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),

            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                focusedTextColor = MaterialTheme.colorScheme.secondary,
                unfocusedTextColor = MaterialTheme.colorScheme.tertiary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.primary
            )
        )
        ExposedDropdownMenu(
            expanded = dropdownOpen,
            onDismissRequest = { dropdownOpen = false }) {
            presetIds.forEach { id ->
                DropdownMenuItem(
                    onClick = {
                        textFieldState.setTextAndPlaceCursorAtEnd(id)
                        onSelect(id)
                        dropdownOpen = false
                    },
                    text = {
                        Text(
                            text = id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                )
            }
        }
    }
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
            contentDescription=stringResource(R.string.eq_toggle_button_description)
        )
    }
}

@Composable
private fun ResetButton(
    updateEqLevel: (Int, Float) -> Unit,
    numEqBands: Int
    ) {
    // Handles the button that resets all the sliders to 0 dB
    IconButton(onClick = {for (i in 0..<numEqBands) updateEqLevel(i, 0.0f)}) {
        Icon(
            imageVector = Icons.Rounded.SettingsBackupRestore,
            contentDescription=stringResource(R.string.eq_reset_button_description)
        )
    }
}