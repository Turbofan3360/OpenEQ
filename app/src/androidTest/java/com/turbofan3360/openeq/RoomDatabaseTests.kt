package com.turbofan3360.openeq

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.turbofan3360.openeq.appdata.RoomDatabaseHandler
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class RoomDatabaseTests {
    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        // Ensuring any database instance is closed before building a new one for testing
        RoomDatabaseHandler.closeDatabase()
        RoomDatabaseHandler.buildDatabaseInMemory(appContext)
    }

    @Test
    fun addToDatabase_isCorrect() = runTest {
        var returnedValues = listOf<Float>()
        // Adding a preset to the database (thing that's being testing)
        var job = RoomDatabaseHandler.addPreset("testing_db_add_item", listOf(1.1f, 2.0f, -9.8f, -5.0f, 0.0f), this)

        // Checking returned job object is valid + waiting for DB op to complete
        assertNotNull(job)
        job?.join()

        // Checking the ID string was properly added to the cache of ID strings
        assertTrue(RoomDatabaseHandler.idStrings.contains("testing_db_add_item"))

        // Getting the values back that I just wrote to the DB
        job = RoomDatabaseHandler.getPreset("testing_db_add_item", this) { returnedValues = it }

        // Waiting for DB op to complete
        job?.join()

        // Checking values
        assertEquals(listOf(1.1f, 2.0f, -9.8f, -5.0f, 0.0f), returnedValues)

        // Checking that adding another preset with same ID doesn't do anything
        job = RoomDatabaseHandler.addPreset("testing_db_add_item", listOf(1f, 2f, 3f, 4f, 5f), this)
        assertNull(job)

        // Deleting testing preset from DB
        job = RoomDatabaseHandler.deletePreset("testing_db_add_item", this) {}
        job?.join()
    }

    @Test
    fun updateDatabaseItem_isCorrect() = runTest {
        var returnedValues = listOf<Float>()
        // Adding a value that can then be edited, waiting for the DB op to complete
        var job = RoomDatabaseHandler.addPreset("testing_db_update_item", listOf(1.0f, 2.0f, 0.0f, -1.0f, -2.0f), this)
        job?.join()

        // Updating the preset ID in the database (thing that's being testing)
        job = RoomDatabaseHandler.updatePreset("testing_db_update_item", listOf(2.4f, 4.1f, 0.0f, -2.2f, -4.5f), this)

        // Checking returned job object is valid + waiting for DB op to complete
        assertNotNull(job)
        job?.join()

        // Getting the values back that I just wrote to the DB, waiting for op to complete
        job = RoomDatabaseHandler.getPreset("testing_db_update_item", this) { returnedValues = it }
        job?.join()

        // Checking values
        assertEquals(listOf(2.4f, 4.1f, 0.0f, -2.2f, -4.5f), returnedValues)

        // Checking that updating non-existing item doesn't do anything
        job = RoomDatabaseHandler.updatePreset("some_invalid_id", listOf(1f, 2f, 3f, 4f, 5f), this)
        assertNull(job)

        // Deleting testing preset from DB
        job = RoomDatabaseHandler.deletePreset("testing_db_update_item", this) {}
        job?.join()
    }

    @Test
    fun getDatabaseItem_isCorrect() = runTest {
        var returnedValues = listOf<Float>()
        // Adding a value that can then be retrieved, waiting for the DB op to complete
        var job = RoomDatabaseHandler.addPreset("testing_db_get_item", listOf(1.2f, 2.8f, 0.0f, -1.4f, -2.5f), this)
        job?.join()

        // Getting the values back that I just wrote to the DB (thing that's being tested)
        job = RoomDatabaseHandler.getPreset("testing_db_get_item", this) { returnedValues = it }

        // Checking returned job object is valid + waiting for DB op to complete
        assertNotNull(job)
        job?.join()

        // Checking values
        assertEquals(listOf(1.2f, 2.8f, 0.0f, -1.4f, -2.5f), returnedValues)

        // Checking that getting non-existent item doesn't do anything
        job = RoomDatabaseHandler.getPreset("some_invalid_id", this) {}
        assertNull(job)

        // Deleting testing preset from DB
        job = RoomDatabaseHandler.deletePreset("testing_db_get_item", this) {}
        job?.join()
    }

    @Test
    fun deleteDatabaseItem_isCorrect() = runTest {
        var valuesWereReturned = false
        // Adding a value that can then be retrieved, waiting for the DB op to complete
        var job = RoomDatabaseHandler.addPreset("testing_db_delete_item", listOf(1.2f, 2.8f, 0.0f, -1.4f, -2.5f), this)
        job?.join()

        // Deleting testing preset from DB (thing that's being tested)
        job = RoomDatabaseHandler.deletePreset("testing_db_delete_item", this) {}

        // Checking returned job object is valid + waiting for DB op to complete
        assertNotNull(job)
        job?.join()

        // Checking ID string properly removed from ID string cache
        assertFalse(RoomDatabaseHandler.idStrings.contains("testing_db_delete_item"))

        // Trying to get item from DB - should return null
        job = RoomDatabaseHandler.getPreset("testing_db_delete_item", this) { valuesWereReturned = true }

        // Checking that deleting non-existent item doesn't do anything
        job = RoomDatabaseHandler.deletePreset("some_invalid_id", this) {}
        assertNull(job)

        assertNull(job)
        assertFalse(valuesWereReturned)
    }

    @After
    fun teardown() {
        RoomDatabaseHandler.closeDatabase()
    }
}
