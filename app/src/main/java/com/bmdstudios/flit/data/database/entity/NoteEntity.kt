package com.bmdstudios.flit.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bmdstudios.flit.data.database.model.NoteStatus

/**
 * Note entity for transcribed notes.
 */
@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["status"]),
        Index(value = ["created_at"]),
        Index(value = ["updated_at"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val text: String,
    val recording: String? = null,
    val embedding_vector: ByteArray? = null,
    val created_at: Long,
    val updated_at: Long,
    val status: NoteStatus
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NoteEntity

        if (id != other.id) return false
        if (title != other.title) return false
        if (text != other.text) return false
        if (recording != other.recording) return false
        if (embedding_vector != null) {
            if (other.embedding_vector == null) return false
            if (!embedding_vector.contentEquals(other.embedding_vector)) return false
        } else if (other.embedding_vector != null) return false
        if (created_at != other.created_at) return false
        if (updated_at != other.updated_at) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + (recording?.hashCode() ?: 0)
        result = 31 * result + (embedding_vector?.contentHashCode() ?: 0)
        result = 31 * result + created_at.hashCode()
        result = 31 * result + updated_at.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }
}
