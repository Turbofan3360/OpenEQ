package com.turbofan3360.openeq.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable

// CenterAlignedTopAppBar is an experimental API so need to allow it
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun MainScreen() {
    Scaffold(
        // Creating the "OpenEQ" app bar at the top of the main screen
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text(
                        "OpenEQ",
                        style = MaterialTheme.typography.titleLarge
                        )
                }
            )
        },
        // Creating the power button at the bottom
        floatingActionButton = {
            LargeFloatingActionButton(onClick = {updateEQState()}, shape= CircleShape) {
                Icon(
                    imageVector=Icons.Rounded.PowerSettingsNew,
                    contentDescription="Toggle equalizer on/off"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) {
        // Defining content: Currently just a blank background colored, set to fill the page
        innerPadding -> Box(modifier =
            Modifier.fillMaxSize().background(color=MaterialTheme.colorScheme.background).padding(paddingValues=innerPadding)
        ) {}
    }
}

fun updateEQState() {

}