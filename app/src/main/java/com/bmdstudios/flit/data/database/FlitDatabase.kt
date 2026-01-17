package com.bmdstudios.flit.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bmdstudios.flit.data.database.dao.CategoryDao
import com.bmdstudios.flit.data.database.dao.ChunkDao
import com.bmdstudios.flit.data.database.dao.NoteCategoryDao
import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.RelationshipDao
import com.bmdstudios.flit.data.database.dao.UserDao
import com.bmdstudios.flit.data.database.entity.CategoryEntity
import com.bmdstudios.flit.data.database.entity.ChunkEntity
import com.bmdstudios.flit.data.database.entity.NoteCategoryCrossRef
import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.data.database.entity.RelationshipEntity
import com.bmdstudios.flit.data.database.entity.UserEntity

/**
 * Room database for Flit app.
 * Contains all entities and DAOs for user settings, notes, chunks, relationships, and categories.
 */
@Database(
    entities = [
        UserEntity::class,
        NoteEntity::class,
        ChunkEntity::class,
        RelationshipEntity::class,
        CategoryEntity::class,
        NoteCategoryCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FlitDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun noteDao(): NoteDao
    abstract fun chunkDao(): ChunkDao
    abstract fun relationshipDao(): RelationshipDao
    abstract fun categoryDao(): CategoryDao
    abstract fun noteCategoryDao(): NoteCategoryDao
}
