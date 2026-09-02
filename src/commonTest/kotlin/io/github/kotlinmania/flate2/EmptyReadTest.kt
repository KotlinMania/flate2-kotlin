// port-lint: tests empty-read.rs
package io.github.kotlinmania.flate2

import io.github.kotlinmania.flate2.deflate.DeflateDecoder
import io.github.kotlinmania.flate2.deflate.DeflateEncoder
import io.github.kotlinmania.flate2.deflate.DeflateWriteEncoder
import io.github.kotlinmania.flate2.gz.GzDecoder
import io.github.kotlinmania.flate2.gz.GzEncoder
import io.github.kotlinmania.flate2.gz.GzWriteEncoder
import io.github.kotlinmania.flate2.zlib.ZlibDecoder
import io.github.kotlinmania.flate2.zlib.ZlibEncoder
import io.github.kotlinmania.flate2.zlib.ZlibWriteEncoder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class EmptyReadTest {
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

    private fun readToEnd(readFn: (ByteArray) -> Int): ByteArray {
        val result = mutableListOf<Byte>()
        val buf = ByteArray(1024)
        while (true) {
            val count = readFn(buf)
            if (count <= 0) break
            for (i in 0 until count) {
                result.add(buf[i])
            }
        }
        return result.toByteArray()
    }

    @Test
    fun deflateDecoderEmptyRead() {
        val original = "Lorem ipsum dolor sit amet.".encodeToByteArray()
        val sink = ByteArraySink()
        val encoder = DeflateWriteEncoder(sink, Compression.default())
        encoder.write(original)
        encoder.finish()
        val encoded = sink.toByteArray()

        val decoder = DeflateDecoder(ByteArraySource(encoded))
        assertEquals(0, decoder.read(ByteArray(0)))
        val decoded = readToEnd { decoder.read(it) }
        assertContentEquals(original, decoded)
    }

    @Test
    fun deflateEncoderEmptyRead() {
        val original = "Lorem ipsum dolor sit amet.".encodeToByteArray()
        val encoder = DeflateEncoder(ByteArraySource(original), Compression.default())
        assertEquals(0, encoder.read(ByteArray(0)))
        val encoded = readToEnd { encoder.read(it) }

        val decoder = DeflateDecoder(ByteArraySource(encoded))
        val decoded = readToEnd { decoder.read(it) }
        assertContentEquals(original, decoded)
    }

    @Test
    fun gzipDecoderEmptyRead() {
        val original = "Lorem ipsum dolor sit amet.".encodeToByteArray()
        val sink = ByteArraySink()
        val encoder = GzWriteEncoder.new(sink, Compression.default())
        encoder.write(original)
        encoder.finish()
        val encoded = sink.toByteArray()

        val decoder = GzDecoder.new(ByteArraySource(encoded))
        assertEquals(0, decoder.read(ByteArray(0)))
        val decoded = readToEnd { decoder.read(it) }
        assertContentEquals(original, decoded)
    }

    @Test
    fun gzipEncoderEmptyRead() {
        val original = "Lorem ipsum dolor sit amet.".encodeToByteArray()
        val encoder = GzEncoder(ByteArraySource(original), Compression.default())
        assertEquals(0, encoder.read(ByteArray(0)))
        val encoded = readToEnd { encoder.read(it) }

        val decoder = GzDecoder.new(ByteArraySource(encoded))
        val decoded = readToEnd { decoder.read(it) }
        assertContentEquals(original, decoded)
    }

    @Test
    fun zlibDecoderEmptyRead() {
        val original = "Lorem ipsum dolor sit amet.".encodeToByteArray()
        val sink = ByteArraySink()
        val encoder = ZlibWriteEncoder(sink, Compression.default())
        encoder.write(original)
        encoder.finish()
        val encoded = sink.toByteArray()

        val decoder = ZlibDecoder(ByteArraySource(encoded))
        assertEquals(0, decoder.read(ByteArray(0)))
        val decoded = readToEnd { decoder.read(it) }
        assertContentEquals(original, decoded)
    }

    @Test
    fun zlibEncoderEmptyRead() {
        val original = "Lorem ipsum dolor sit amet.".encodeToByteArray()
        val encoder = ZlibEncoder(ByteArraySource(original), Compression.default())
        assertEquals(0, encoder.read(ByteArray(0)))
        val encoded = readToEnd { encoder.read(it) }

        val decoder = ZlibDecoder(ByteArraySource(encoded))
        val decoded = readToEnd { decoder.read(it) }
        assertContentEquals(original, decoded)
    }
}
