package com.bmdstudios.flit.data.export

import com.bmdstudios.flit.data.database.model.RelationshipType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteMarkdownRelationshipImportTest {

    @Test
    fun followsTo_orientsCurrentAsNoteA() {
        val r = resolveRelationshipLabel("Follows to")!!
        assertEquals(RelationshipType.FOLLOWS_ON, r.type)
        assertTrue(r.noteAIsCurrent)
    }

    @Test
    fun followsFrom_swapsNoteA() {
        val r = resolveRelationshipLabel("Follows from")!!
        assertEquals(RelationshipType.FOLLOWS_ON, r.type)
        assertFalse(r.noteAIsCurrent)
    }

    @Test
    fun similarTo_byEnumName() {
        val r = resolveRelationshipLabel("SIMILAR_TO")!!
        assertEquals(RelationshipType.SIMILAR_TO, r.type)
    }

    @Test
    fun symmetricTypes_flagged() {
        assertTrue(RelationshipType.SIMILAR_TO.isSymmetricForImportDedupe())
        assertTrue(RelationshipType.RELATED_TO.isSymmetricForImportDedupe())
        assertFalse(RelationshipType.REFERENCES.isSymmetricForImportDedupe())
    }
}
