package com.turbofan3360.openeq.appdata

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// Class for the user to easily handle the database, serializing and de-serializing data
// "object" enforces this as a singleton
object RoomDatabaseHandler {
    private var db: EqPresetDatabase? = null

    var dbInitialized = false
    var idStrings = mutableListOf<String>()

    // Function to build the database instance
    // Only lets you run the function once
    fun buildDatabase(dbId: String, context: Context) {
        if (db != null) {
            return
        }

        // Building the database instance
        db = Room.databaseBuilder(
            context.applicationContext,
            EqPresetDatabase::class.java,
            dbId
        ).build()

        dbInitialized = true

        // Finds what's in the database (blocking to ensure other code doesn't break
        getAllPresetIds()
    }

    // Builds a database in memory (to be used for testing the database handler)
    fun buildDatabaseInMemory(context: Context) {
        if (db != null) {
            return
        }

        db = Room.inMemoryDatabaseBuilder(
            context.applicationContext,
            EqPresetDatabase::class.java
        ).build()

        dbInitialized = true
        idStrings.clear()
    }

    // Function to close down the database safely
    fun closeDatabase() {
        db?.close()
        db = null
        dbInitialized = false
    }

    fun addPreset(
        stringPresetId: String,
        eqLevels: List<Float>,
        myScope: CoroutineScope
    ): Job? {
        // Checks ID is valid
        if (idStrings.contains(stringPresetId)) {
            return null
        }

        // Launches a coroutine to save the current EQ levels as a new preset in the database
        val retJob = myScope.launch {
            val serializedEqLevels = Gson().toJson(eqLevels)
            db?.userDao()?.addPreset(Preset(presetId = stringPresetId, eqLevels = serializedEqLevels))

            // Once preset added to database, adds the new preset ID to the list of preset ID string
            idStrings += stringPresetId
        }

        return retJob
    }

    fun updatePreset(
        stringPresetId: String,
        eqLevels: List<Float>,
        myScope: CoroutineScope
    ): Job? {
        if (!idStrings.contains(stringPresetId)) {
            return null
        }

        // Launches coroutine to update preset in database to current EQ levels
        val retJob = myScope.launch {
            val serializedEqLevels = Gson().toJson(eqLevels)

            db?.userDao()?.updatePreset(Preset(presetId = stringPresetId, eqLevels = serializedEqLevels))
        }

        return retJob
    }

    fun getPreset(
        stringPresetId: String,
        myScope: CoroutineScope,
        onPresetRetrieved: (List<Float>) -> Unit
    ): Job? {
        // Checking whether the desired preset exists or not
        if (!idStrings.contains(stringPresetId)) {
            return null
        }

        // Launches a coroutine to find the wanted preset and hand it back
        val retJob = myScope.launch {
            val presetLevelsJson: Preset? = db?.userDao()?.getPreset(stringPresetId)

            if (presetLevelsJson != null) {
                val presetLevelsList = Gson().fromJson(presetLevelsJson.eqLevels, Array<Float>::class.java).toList()

                onPresetRetrieved(presetLevelsList)
            }
        }

        return retJob
    }

    fun deletePreset(
        stringPresetId: String,
        myScope: CoroutineScope,
        onPresetDelete: () -> Unit
    ): Job? {
        // Checking whether the desired preset exists or not
        if (!idStrings.contains(stringPresetId)) {
            return null
        }

        // Launches coroutine to remove preset from database
        val retJob = myScope.launch {
            db?.userDao()?.deletePreset(stringPresetId)
            idStrings.remove(stringPresetId)

            onPresetDelete()
        }

        return retJob
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
