package com.bmdstudios.flit.data.export

import com.bmdstudios.flit.data.database.model.RelationshipType

/**
 * Maps exported relationship labels to [RelationshipType] and whether the row is
 * `(current, target)` or `(target, current)` for [RelationshipType.FOLLOWS_ON].
 */
data class ResolvedRelationship(
    val type: RelationshipType,
    /** If true, store note_a = currentNoteId, note_b = targetNoteId; else swap for FOLLOWS_ON only. */
    val noteAIsCurrent: Boolean
)

fun resolveRelationshipLabel(label: String): ResolvedRelationship? {
    val n = label.trim().lowercase()
    return when (n) {
        "follows to" -> ResolvedRelationship(RelationshipType.FOLLOWS_ON, noteAIsCurrent = true)
        "follows from" -> ResolvedRelationship(RelationshipType.FOLLOWS_ON, noteAIsCurrent = false)
        "similar to" -> ResolvedRelationship(RelationshipType.SIMILAR_TO, noteAIsCurrent = true)
        "contradicts" -> ResolvedRelationship(RelationshipType.CONTRADICTS, noteAIsCurrent = true)
        "references" -> ResolvedRelationship(RelationshipType.REFERENCES, noteAIsCurrent = true)
        "related to" -> ResolvedRelationship(RelationshipType.RELATED_TO, noteAIsCurrent = true)
        "follows on" -> ResolvedRelationship(RelationshipType.FOLLOWS_ON, noteAIsCurrent = true)
        else -> {
            RelationshipType.entries.firstOrNull { it.name.equals(label.trim(), ignoreCase = true) }
                ?.let { ResolvedRelationship(it, noteAIsCurrent = true) }
        }
    }
}

fun RelationshipType.isSymmetricForImportDedupe(): Boolean =
    this == RelationshipType.SIMILAR_TO || this == RelationshipType.RELATED_TO
