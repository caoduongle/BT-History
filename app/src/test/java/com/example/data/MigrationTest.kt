package com.example.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.AppDatabase.Companion.MIGRATION_1_2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class MigrationTest {

    @Test
    fun testMigration1To2_versionsAreCorrect() {
        assertEquals(1, MIGRATION_1_2.startVersion)
        assertEquals(2, MIGRATION_1_2.endVersion)
    }

    @Test
    fun testMigration1To2_executesCreateIndexStatement() {
        val executedStatements = mutableListOf<String>()
        val proxyDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedStatements.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        MIGRATION_1_2.migrate(proxyDb)

        assertEquals(1, executedStatements.size)
        assertTrue(
            "Executed statement must create index on device_id and timestamp",
            executedStatements[0].contains("CREATE INDEX IF NOT EXISTS `index_events_device_id_timestamp` ON `events` (`device_id`, `timestamp`)")
        )
    }

    @Test
    fun testAllMigrationsArray_containsMigration1To2() {
        assertNotNull(AppDatabase.ALL_MIGRATIONS)
        assertEquals(1, AppDatabase.ALL_MIGRATIONS.size)
        assertEquals(MIGRATION_1_2, AppDatabase.ALL_MIGRATIONS[0])
    }
}
