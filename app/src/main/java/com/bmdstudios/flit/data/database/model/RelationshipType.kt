package com.bmdstudios.flit.data.database.model

/**
 * Relationship types for knowledge graph connections between notes.
 */
enum class RelationshipType {
    FOLLOWS_ON,
    SIMILAR_TO,
    CONTRADICTS,
    REFERENCES,
    RELATED_TO
}
