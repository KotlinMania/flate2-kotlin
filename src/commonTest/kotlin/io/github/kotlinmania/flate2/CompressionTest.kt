// port-lint: tests lib.rs
package io.github.kotlinmania.flate2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CompressionTest {
    @Test
    fun newReturnsExplicitLevel() {
        assertEquals(3u, Compression.new(3u).level())
    }

    @Test
    fun noneIsZero() {
        assertEquals(0u, Compression.none().level())
    }

    @Test
    fun fastIsOne() {
        assertEquals(1u, Compression.fast().level())
    }

    @Test
    fun bestIsNine() {
        assertEquals(9u, Compression.best().level())
    }

    @Test
    fun defaultIsSix() {
        assertEquals(6u, Compression.default().level())
    }

    @Test
    fun equalityIsByLevel() {
        assertEquals(Compression.new(5u), Compression.new(5u))
        assertNotEquals(Compression.new(5u), Compression.new(4u))
        assertEquals(Compression.new(5u).hashCode(), Compression.new(5u).hashCode())
    }

    @Test
    fun toStringContainsLevel() {
        assertTrue(Compression.new(7u).toString().contains("7"))
    }
}
