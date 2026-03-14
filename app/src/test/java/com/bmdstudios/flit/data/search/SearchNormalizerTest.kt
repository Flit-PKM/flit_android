package com.bmdstudios.flit.data.search

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [SearchNormalizer].
 */
class SearchNormalizerTest {

    @Test
    fun normalize_empty_returnsEmpty() {
        assertEquals("", SearchNormalizer.normalize(""))
        assertEquals("", SearchNormalizer.normalize("   "))
    }

    @Test
    fun normalize_lowercases() {
        assertEquals("hello world", SearchNormalizer.normalize("Hello World"))
    }

    @Test
    fun normalize_removesStopWords() {
        assertEquals("hello world", SearchNormalizer.normalize("hello and the world"))
        assertEquals("", SearchNormalizer.normalize("and the then yes"))
    }

    @Test
    fun normalize_singleWord_notStopWord() {
        assertEquals("hello", SearchNormalizer.normalize("hello"))
    }

    @Test
    fun normalize_preservesNumbers() {
        assertEquals("123 456", SearchNormalizer.normalize("123 and 456"))
    }

    @Test
    fun queryWords_empty_returnsEmpty() {
        assertEquals(emptyList<String>(), SearchNormalizer.queryWords(""))
        assertEquals(emptyList<String>(), SearchNormalizer.queryWords("   "))
    }

    @Test
    fun queryWords_removesStopWords() {
        assertEquals(listOf("hello", "world"), SearchNormalizer.queryWords("hello and world"))
    }

    @Test
    fun queryWords_lowercases() {
        assertEquals(listOf("hello"), SearchNormalizer.queryWords("HELLO"))
    }
}
