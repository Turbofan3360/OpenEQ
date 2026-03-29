package com.turbofan3360.openeq

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.turbofan3360.openeq.appdata.SharedPreferencesSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesTests {
    lateinit var sharedPreferencesHandler: SharedPreferencesSettings

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        sharedPreferencesHandler = SharedPreferencesSettings(appContext)
    }

    @Test
    fun sharedPreferencesBoolean_isCorrect() {
        // Testing adding/setting/removing boolean from shared preferences is good
        sharedPreferencesHandler.appSaveBoolean(key = "testing", value = false)

        // Checking it returns right value
        assertFalse(sharedPreferencesHandler.getAppSettingBoolean(key = "testing", default = true))

        // Checking that when an invalid key is passed, the default is returned
        assertTrue(sharedPreferencesHandler.getAppSettingBoolean(key = "some_invalid_key", default = true))

        // Deleting and checking it was deleted
        sharedPreferencesHandler.appDeleteBoolean(key = "testing")
        assertTrue(sharedPreferencesHandler.getAppSettingBoolean(key = "testing", default = true))
    }
}
