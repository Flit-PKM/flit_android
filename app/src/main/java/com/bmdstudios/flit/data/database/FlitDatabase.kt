package com.bmdstudios.flit.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bmdstudios.flit.data.database.dao.CategoryDao
import com.bmdstudios.flit.data.database.dao.ChunkDao
import com.bmdstudios.flit.data.database.dao.NoteCategoryDao
import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.NotesearchDao
import com.bmdstudios.flit.data.database.dao.RelationshipDao
import com.bmdstudios.flit.data.database.entity.CategoryEntity
import com.bmdstudios.flit.data.database.entity.ChunkEntity
import com.bmdstudios.flit.data.database.entity.NoteCategoryCrossRef
import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.data.database.entity.NoteSearchEntity
import com.bmdstudios.flit.data.database.entity.RelationshipEntity

/**
 * Room database for Flit app.
 * Contains all entities and DAOs for notes, chunks, relationships, categories, and notesearch.
 */
@Database(
    entities = [
        NoteEntity::class,
        ChunkEntity::class,
        RelationshipEntity::class,
        CategoryEntity::class,
        NoteCategoryCrossRef::class,
        NoteSearchEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FlitDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun chunkDao(): ChunkDao
    abstract fun relationshipDao(): RelationshipDao
    abstract fun categoryDao(): CategoryDao
    abstract fun noteCategoryDao(): NoteCategoryDao
    abstract fun notesearchDao(): NotesearchDao
}
