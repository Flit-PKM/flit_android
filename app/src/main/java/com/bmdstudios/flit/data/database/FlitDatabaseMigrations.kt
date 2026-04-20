package com.bmdstudios.flit.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val VERSION_3 = 3
private const val VERSION_4 = 4
private const val VERSION_5 = 5

/**
 * Migration from database version 3 to 4: add notesearch table.
 * Content is populated by app code (one-time rebuild) after migration.
 */
val MIGRATION_3_4 = object : Migration(VERSION_3, VERSION_4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS notesearch (
                note_id INTEGER PRIMARY KEY NOT NULL,
                content TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

/**
 * Migration from database version 4 to 5: remove deprecated chunks table.
 */
val MIGRATION_4_5 = object : Migration(VERSION_4, VERSION_5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS chunks")
    }
}
