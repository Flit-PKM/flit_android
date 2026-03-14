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
        Index(value = ["core_id"], unique = true),
        Index(value = ["note_id"]),
        Index(value = ["is_deleted"])
    ]
)
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val core_id: Long? = null,
    val ver: Int = 1,
    val is_deleted: Boolean = false,
    val note_id: Long,
    val position_start: Int,
    val position_end: Int,
    val embedding_vector: ByteArray,
    val text: String? = null,
    val updated_at: Long
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
        if (core_id != other.core_id) return false
        if (ver != other.ver) return false
        if (is_deleted != other.is_deleted) return false
        if (note_id != other.note_id) return false
        if (position_start != other.position_start) return false
        if (position_end != other.position_end) return false
        if (!embedding_vector.contentEquals(other.embedding_vector)) return false
        if (text != other.text) return false
        if (updated_at != other.updated_at) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (core_id?.hashCode() ?: 0)
        result = 31 * result + ver
        result = 31 * result + is_deleted.hashCode()
        result = 31 * result + note_id.hashCode()
        result = 31 * result + position_start
        result = 31 * result + position_end
        result = 31 * result + embedding_vector.contentHashCode()
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + updated_at.hashCode()
        return result
    }
}
