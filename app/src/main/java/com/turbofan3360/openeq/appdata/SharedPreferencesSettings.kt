package com.turbofan3360.openeq.appdata

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit

// Handles storing basic app settings in shared preferences (much simpler than a data store, or room database)
// Currently only setting stored this way is whether attached to global audio mix or not

class SharedPreferencesSettings(
    context: Context,
    name: String
) {
    val sharedPref: SharedPreferences by lazy { context.getSharedPreferences(name, MODE_PRIVATE) }

    fun getAppSettingBoolean(
        key: String,
        default: Boolean
    ): Boolean {
        val setting = sharedPref.getBoolean(key, default)

        return setting
    }

    fun appSaveBoolean(
        key: String,
        value: Boolean
    ) {
        // Saves app settings to the shared preferences
        sharedPref.edit {
            putBoolean(key, value)
        }
    }

    fun appDeleteBoolean(
        key: String
    ) {
        // Deletes a preset from the shared preferences
        sharedPref.edit {
            remove(key)
        }
    }

    fun appClearSharedPreferences() {
        // Clears all data
        sharedPref.edit {
            clear()
        }
    }
}
