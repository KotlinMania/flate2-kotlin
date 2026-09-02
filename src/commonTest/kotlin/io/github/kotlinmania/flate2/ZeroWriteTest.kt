// port-lint: tests zero-write.rs
package io.github.kotlinmania.flate2

import io.github.kotlinmania.flate2.deflate.DeflateWriteEncoder
import kotlin.test.Test
import kotlin.test.assertFails

class ZeroWriteTest {
    private class FixedBufferSink(private val capacity: Int) : OutputSink {
        private var pos = 0

        override fun write(source: ByteArray, offset: Int, length: Int): Int {
            if (pos + length > capacity) {
                throw IllegalStateException("Buffer overflow: write exceeds fixed capacity of $capacity")
            }
            pos += length
            return length
        }

        override fun flush() {}
    }

    @Test
    fun zeroWriteIsError() {
        val sink = FixedBufferSink(1)
        val writer = DeflateWriteEncoder(sink, Compression.default())
        assertFails {
            writer.finish()
        }
    }
}
