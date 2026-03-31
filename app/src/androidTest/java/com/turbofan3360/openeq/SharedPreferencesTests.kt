package com.turbofan3360.openeq

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.turbofan3360.openeq.appdata.SharedPreferencesSettings
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

@RunWith(AndroidJUnit4::class)
class SharedPreferencesTests {
    lateinit var sharedPreferencesHandler: SharedPreferencesSettings

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        sharedPreferencesHandler = SharedPreferencesSettings(appContext, "testing_preferences")
        sharedPreferencesHandler.appClearSharedPreferences()
    }

    @Test
    fun getValue_isCorrect() {
        // Testing getting values from the data store
        // Adding values to use for testing
        sharedPreferencesHandler.appSaveBoolean(key = "test_get", value = true)

        // Checking it returns right value
        assertTrue(sharedPreferencesHandler.getAppSettingBoolean(key = "test_get", default = false))

        // Checking that when an invalid key is passed, the default is returned
        assertTrue(sharedPreferencesHandler.getAppSettingBoolean(key = "some_invalid_key", default = true))
    }

    @Test
    fun overwriteValue_isCorrect() {
        // Adding value to use for testing
        sharedPreferencesHandler.appSaveBoolean(key = "test_overwrite", value = false)

        // Overwriting the boolean
        sharedPreferencesHandler.appSaveBoolean(key = "test_overwrite", value = true)
        assertTrue(sharedPreferencesHandler.getAppSettingBoolean(key = "test_overwrite", default = false))
    }

    @Test
    fun deleteValue_isCorrect() {
        // Adding value to use for testing
        sharedPreferencesHandler.appSaveBoolean(key = "test_delete", value = false)

        // Deleting and checking it was deleted
        sharedPreferencesHandler.appDeleteBoolean(key = "test_delete")
        assertTrue(sharedPreferencesHandler.getAppSettingBoolean(key = "test_delete", default = true))

        // Testing deleting invalid key
        sharedPreferencesHandler.appDeleteBoolean("some_invalid_key")
        assertTrue(sharedPreferencesHandler.getAppSettingBoolean(key = "some_invalid_key", default = true))
    }

    @After
    fun teardown() {
        // Clearing all data used for testing
        sharedPreferencesHandler.appClearSharedPreferences()
    }
}
