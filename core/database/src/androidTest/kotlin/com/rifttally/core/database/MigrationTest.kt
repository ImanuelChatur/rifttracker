package com.rifttally.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** One test per schema version. Add a `migrate_N_to_N+1` test with every new version. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = RiftDatabase::class.java,
    )

    @Test
    fun createVersion1_hasAllTables() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            val tables = buildSet {
                db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
                    while (cursor.moveToNext()) add(cursor.getString(0))
                }
            }
            assertTrue(tables.toString(), tables.containsAll(listOf("game", "game_event", "player_preset")))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
