package com.bmdstudios.flit.data.database

import androidx.room.TypeConverter
import com.bmdstudios.flit.data.database.model.NoteStatus
import com.bmdstudios.flit.data.database.model.RelationshipType

/**
 * Room type converters for custom types.
 */
class Converters {

    @TypeConverter
    fun fromNoteStatus(status: NoteStatus): String {
        return status.name
    }

    @TypeConverter
    fun toNoteStatus(status: String): NoteStatus {
        return NoteStatus.valueOf(status)
    }

    @TypeConverter
    fun fromRelationshipType(type: RelationshipType): String {
        return type.name
    }

    @TypeConverter
    fun toRelationshipType(type: String): RelationshipType {
        return RelationshipType.valueOf(type)
    }
}
