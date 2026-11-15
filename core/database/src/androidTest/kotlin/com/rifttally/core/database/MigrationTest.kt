package com.rifttally.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rifttally.core.database.model.GameTimerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            assertTrue(db.tables().toString(), db.tables().containsAll(listOf("game", "game_event", "player_preset")))
        }
    }

    /** 1 → 2 adds `game_timer` (validated against 2.json) and keeps every existing row. */
    @Test
    fun migrate1To2_addsGameTimer_andKeepsData() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO game (id, created_at, updated_at, config_json, ended_at) VALUES ('g1', 1, 2, '{}', NULL)",
            )
            db.execSQL(
                "INSERT INTO game_event (id, game_id, seq, timestamp, payload) VALUES ('e1', 'g1', 1, 2, '{}')",
            )
            db.execSQL(
                "INSERT INTO player_preset (id, name, color_id, legend_id) VALUES ('p1', 'Ana', 'orange', NULL)",
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true).use { db ->
            assertTrue(db.tables().contains("game_timer"))
            assertEquals(1, db.count("SELECT COUNT(*) FROM game WHERE id = 'g1'"))
            assertEquals(1, db.count("SELECT COUNT(*) FROM game_event WHERE game_id = 'g1'"))
            assertEquals(1, db.count("SELECT COUNT(*) FROM player_preset WHERE id = 'p1'"))
            assertEquals(0, db.count("SELECT COUNT(*) FROM game_timer"))
        }
    }

    /** After the migration the app's DAOs work on the old data, including the cascade on delete. */
    @Test
    fun migrate1To2_timerWorksOnMigratedGame() = runTest {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO game (id, created_at, updated_at, config_json, ended_at) VALUES ('g1', 1, 2, '{}', NULL)",
            )
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, RiftDatabase::class.java, TEST_DB).build()
        try {
            val timerDao = database.gameTimerDao()
            timerDao.upsert(GameTimerEntity(gameId = "g1", durationMs = 60_000))
            assertEquals(GameTimerEntity("g1", 60_000), timerDao.observe("g1").first())

            database.gameDao().delete("g1")
            assertNull(timerDao.get("g1"))
        } finally {
            database.close()
        }
    }

    private fun SupportSQLiteDatabase.tables(): Set<String> = buildSet {
        query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            while (cursor.moveToNext()) add(cursor.getString(0))
        }
    }

    private fun SupportSQLiteDatabase.count(sql: String): Int = query(sql).use { cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
