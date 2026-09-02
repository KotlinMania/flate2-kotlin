// port-lint: tests tests/early-flush.rs
package io.github.kotlinmania.flate2

import io.github.kotlinmania.flate2.gz.GzDecoder
import io.github.kotlinmania.flate2.gz.GzWriteEncoder
import kotlin.test.Test
import kotlin.test.assertEquals

class EarlyFlushTest {
    private class ByteArraySink : OutputSink {
        private val buffer = mutableListOf<Byte>()

        override fun write(source: ByteArray, offset: Int, length: Int): Int {
            for (i in offset until offset + length) {
                buffer.add(source[i])
            }
            return length
        }

        override fun flush() {}

        fun toByteArray(): ByteArray = buffer.toByteArray()
    }

    private class ByteArraySource(private val bytes: ByteArray) : BufferedSource {
        private var pos = 0

        override fun read(sink: ByteArray, offset: Int, length: Int): Int {
            if (pos >= bytes.size) return 0
            val count = minOf(length, bytes.size - pos)
            bytes.copyInto(sink, offset, pos, pos + count)
            pos += count
            return count
        }

        override fun fillBuffer(): ByteArray =
            if (pos < bytes.size) bytes.copyOfRange(pos, bytes.size) else ByteArray(0)

        override fun consume(amount: Int) {
            pos = minOf(bytes.size, pos + amount)
        }
    }

    @Test
    fun smoke() {
        val sink = ByteArraySink()
        val encoder = GzWriteEncoder.new(sink, Compression.default())
        encoder.flush()
        val hello = "hello".encodeToByteArray()
        encoder.write(hello)
        encoder.finish()

        val compressed = sink.toByteArray()
        val decoder = GzDecoder.new(ByteArraySource(compressed))
        val output = ByteArray(64)
        val readCount = decoder.read(output)
        val result = output.decodeToString(0, readCount)
        assertEquals("hello", result)
    }
}
