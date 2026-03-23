package com.turbofan3360.openeq.appdata

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.google.gson.Gson

// Class for the user to easily handle the database, serializing and de-serializing data
class DatabaseHandler {
    private var db: EqPresetDatabase? = null
    private var dbInitialized = false

    // Function to build the database instance
    // Only lets you run the function once, and not concurrently
    @Synchronized
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
    }

    suspend fun addPreset(stringPresetId: String, eqLevels: List<Float>) {
        val serializedEqLevels = Gson().toJson(eqLevels)

        db?.userDao()?.addPreset(Preset(presetId = stringPresetId, eqLevels = serializedEqLevels))
    }

    suspend fun updatePreset(stringPresetId: String, eqLevels: List<Float>) {
        val serializedEqLevels = Gson().toJson(eqLevels)

        db?.userDao()?.updatePreset(Preset(presetId = stringPresetId, eqLevels = serializedEqLevels))
    }

    suspend fun getPreset(stringPresetId: String): List<Float>? {
        val presetLevelsJson: Preset = db?.userDao()?.getPreset(stringPresetId) ?: return null

        return Gson().fromJson(presetLevelsJson.eqLevels, Array<Float>::class.java).toList()
    }

    suspend fun getAllPresetIds(): List<String>? {
        val presetIds = db?.userDao()?.getPresetIds()?.toMutableList() ?: return null

        // Removes this preset ID which is just used for storing the most recent EQ levels
        presetIds.remove("latest_eq_levels")

        return presetIds.toList()
    }

    suspend fun deletePreset(presetId: String) {
        db?.userDao()?.deletePreset(presetId)
    }
}

// -----------------------------------------------

// Defining the actual database
@Database(entities = [Preset::class], version = 1, exportSchema = true)
abstract class EqPresetDatabase : RoomDatabase() {
    abstract fun userDao(): PresetDao
}

// Defining what a row in the preset database looks like
@Entity
data class Preset(
    @PrimaryKey val presetId: String,
    // Stores the JSON serialized EQ levels list
    @ColumnInfo(name = "eq_levels") val eqLevels: String
)

// Defining the functions to interact with the database
@Dao
interface PresetDao {
    // Lets you select a certain preset from the database
    @Query("SELECT * FROM preset WHERE presetId = :wantedPresetId")
    suspend fun getPreset(wantedPresetId: String): Preset?

    // Lets you grab all the preset ID strings
    @Query("SELECT presetId FROM Preset")
    suspend fun getPresetIds(): List<String>

    // Lets you add a preset to the database
    @Insert
    suspend fun addPreset(preset: Preset)

    // Lets you update a preset in the database
    @Update
    suspend fun updatePreset(preset: Preset)

    // Lets you remove preset from the database
    @Query("DELETE FROM Preset WHERE presetId = :wantedPresetId")
    suspend fun deletePreset(wantedPresetId: String)
}
