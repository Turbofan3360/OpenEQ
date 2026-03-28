package com.turbofan3360.openeq.appdata

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// Class for the user to easily handle the database, serializing and de-serializing data
object RoomDatabaseHandler {
    private var db: EqPresetDatabase? = null

    var dbInitialized = false
    var idStrings = mutableListOf<String>()

    // Function to build the database instance
    // Only lets you run the function once
    fun buildDatabase(context: Context) {
        if (dbInitialized) {
            return
        }

        // Building the database instance
        db = Room.databaseBuilder(
            context.applicationContext,
            EqPresetDatabase::class.java, "preset-database"
        ).build()

        dbInitialized = true

        // Finds what's in the database (blocking to ensure other code doesn't break
        getAllPresetIds()
    }

    fun addPreset(
        stringPresetId: String,
        eqLevels: List<Float>,
        myScope: CoroutineScope
    ) {
        // Checks ID is valid
        if (idStrings.contains(stringPresetId)) {
            return
        }

        // Launches a coroutine to save the current EQ levels as a new preset in the database
        myScope.launch {
            val serializedEqLevels = Gson().toJson(eqLevels)
            db?.userDao()?.addPreset(Preset(presetId = stringPresetId, eqLevels = serializedEqLevels))

            // Once preset added to database, adds the new preset ID to the list of preset ID string
            idStrings += stringPresetId
        }
    }

    fun updatePreset(
        stringPresetId: String,
        eqLevels: List<Float>,
        myScope: CoroutineScope
    ) {
        if (!idStrings.contains(stringPresetId)) {
            return
        }

        // Launches coroutine to update preset in database to current EQ levels
        myScope.launch {
            val serializedEqLevels = Gson().toJson(eqLevels)

            db?.userDao()?.updatePreset(Preset(presetId = stringPresetId, eqLevels = serializedEqLevels))
        }
    }

    fun updatePresetBlocking(
        stringPresetId: String,
        eqLevels: List<Float>,
    ) {
        if (!idStrings.contains(stringPresetId)) {
            return
        }

        // Sometimes preset update needs to be blocking to ensure completion, so have this option
        runBlocking {
            val serializedEqLevels = Gson().toJson(eqLevels)

            db?.userDao()?.updatePreset(Preset(presetId = stringPresetId, eqLevels = serializedEqLevels))
        }
    }

    fun getPreset(
        stringPresetId: String,
        myScope: CoroutineScope,
        onPresetRetrieved: (List<Float>) -> Unit
    ): Boolean {
        // Checking whether the desired preset exists or not
        if (!idStrings.contains(stringPresetId)) {
            return false
        }

        // Launches a coroutine to find the wanted preset and hand it back
        myScope.launch {
            val presetLevelsJson: Preset? = db?.userDao()?.getPreset(stringPresetId)

            if (presetLevelsJson != null) {
                val presetLevelsList = Gson().fromJson(presetLevelsJson.eqLevels, Array<Float>::class.java).toList()

                onPresetRetrieved(presetLevelsList)
            }
        }

        return true
    }

    fun deletePreset(
        stringPresetId: String,
        myScope: CoroutineScope,
        onPresetDelete: () -> Unit
    ) {
        // Checking whether the desired preset exists or not
        if (!idStrings.contains(stringPresetId)) {
            return
        }

        // Launches coroutine to remove preset from database
        myScope.launch {
            db?.userDao()?.deletePreset(stringPresetId)
            idStrings.remove(stringPresetId)

            onPresetDelete()
        }
    }

    private fun getAllPresetIds() {
        // Gets all preset IDs from the database and stores them in a variable
        runBlocking {
            val presetIds = db?.userDao()?.getPresetIds()?.toMutableList()

            if (presetIds != null) {
                // Removes this preset ID which is just used for storing the most recent EQ levels
                idStrings.clear()
                idStrings.addAll(presetIds)
            }
        }
    }
}
