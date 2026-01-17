package com.bmdstudios.flit.ui.util

import com.bmdstudios.flit.data.database.model.RelationshipType

/**
 * Extension functions for RelationshipType to provide display names.
 */
fun RelationshipType.displayName(): String {
    return when (this) {
        RelationshipType.FOLLOWS_ON -> "Follows On"
        RelationshipType.SIMILAR_TO -> "Similar To"
        RelationshipType.CONTRADICTS -> "Contradicts"
        RelationshipType.REFERENCES -> "References"
        RelationshipType.RELATED_TO -> "Related To"
    }
}
