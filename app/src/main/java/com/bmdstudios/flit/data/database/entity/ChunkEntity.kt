package com.bmdstudios.flit.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Chunk entity for semantic atomic chunks of notes.
 */
@Entity(
    tableName = "chunks",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["note_id"])
    ]
)
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val note_id: Long,
    val position_start: Int,
    val position_end: Int,
    val embedding_vector: ByteArray,
    val text: String? = null
) {
    init {
        require(position_end >= position_start) {
            "ChunkEntity position_end ($position_end) must be >= position_start ($position_start)"
        }
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChunkEntity

        if (id != other.id) return false
        if (note_id != other.note_id) return false
        if (position_start != other.position_start) return false
        if (position_end != other.position_end) return false
        if (!embedding_vector.contentEquals(other.embedding_vector)) return false
        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + note_id.hashCode()
        result = 31 * result + position_start
        result = 31 * result + position_end
        result = 31 * result + embedding_vector.contentHashCode()
        result = 31 * result + (text?.hashCode() ?: 0)
        return result
    }
}
