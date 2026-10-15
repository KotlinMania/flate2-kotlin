// port-lint: tests tests/gunzip.rs
package io.github.kotlinmania.flate2

import io.github.kotlinmania.flate2.gz.GzDecoder
import io.github.kotlinmania.flate2.gz.GzWriteEncoder
import io.github.kotlinmania.flate2.gz.MultiGzDecoder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class GunzipTest {
    private class ByteArraySink : OutputSink {
        private val buffer = mutableListOf<Byte>()

        override fun write(
            source: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            for (i in offset until offset + length) {
                buffer.add(source[i])
            }
            return length
        }

        override fun flush() {}

        fun toByteArray(): ByteArray = buffer.toByteArray()
    }

    private class ByteArraySource(
        private val bytes: ByteArray,
    ) : BufferedSource {
        private var pos = 0

        override fun read(
            sink: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
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

    private fun hexToByteArray(hex: String): ByteArray {
        val clean = hex.filter { !it.isWhitespace() }
        val result = ByteArray(clean.length / 2)
        for (i in result.indices) {
            val idx = i * 2
            result[i] = clean.substring(idx, idx + 2).toInt(16).toByte()
        }
        return result
    }

    private fun extractGz(data: ByteArray): ByteArray {
        val decoder = GzDecoder.new(ByteArraySource(data))
        val out = mutableListOf<Byte>()
        val buf = ByteArray(128)
        while (true) {
            val n = decoder.read(buf)
            if (n <= 0) break
            for (i in 0 until n) {
                out.add(buf[i])
            }
        }
        return out.toByteArray()
    }

    private fun extractMultiGz(data: ByteArray): ByteArray {
        val decoder = MultiGzDecoder.new(ByteArraySource(data))
        val out = mutableListOf<Byte>()
        val buf = ByteArray(128)
        while (true) {
            val n = decoder.read(buf)
            if (n <= 0) break
            for (i in 0 until n) {
                out.add(buf[i])
            }
        }
        return out.toByteArray()
    }

    // multi.gz from upstream flate2 tests/multi.gz
    private val multiGzHex =
        "1f8b0800c195355700034bcb2c2a2ee102002ab34ac706000000" +
            "1f8b0800c895355700032b4e4dcecf4be102007ec00f0607000000"

    @Test
    fun testExtractSuccess() {
        val original = "Hello, flate2 gzip extraction test!".encodeToByteArray()
        val sink = ByteArraySink()
        val encoder = GzWriteEncoder.new(sink, Compression.default())
        encoder.write(original)
        encoder.finish()

        val compressed = sink.toByteArray()
        val extracted = extractGz(compressed)
        assertContentEquals(original, extracted)
    }

    @Test
    fun testExtractSuccessPartialMulti() {
        val multiGz = hexToByteArray(multiGzHex)
        val extracted = extractGz(multiGz)
        val expected = "first\n".encodeToByteArray()
        assertContentEquals(expected, extracted)
    }

    @Test
    fun testExtractSuccessMulti() {
        val multiGz = hexToByteArray(multiGzHex)
        val extracted = extractMultiGz(multiGz)
        val expected = "first\nsecond\n".encodeToByteArray()
        assertContentEquals(expected, extracted)
    }

    @Test
    fun testExtractFailure() {
        val corrupt = byteArrayOf(0x1f.toByte(), 0x8b.toByte(), 0x08.toByte(), 0x00.toByte(), 0xff.toByte(), 0xff.toByte())
        var failed = false
        try {
            val extracted = extractGz(corrupt)
            if (extracted.isEmpty()) {
                failed = true
            }
        } catch (_: Exception) {
            failed = true
        }
        assertTrue(failed, "Corrupt gzip stream must fail decompression")
    }

    @Test
    fun emptyErrorOnce() {
        val data = ByteArray(0)
        val decoder = GzDecoder.new(ByteArraySource(data))
        val buf = ByteArray(64)
        var failed = false
        try {
            val n = decoder.read(buf)
            if (n <= 0) {
                failed = true
            }
        } catch (_: Exception) {
            failed = true
        }
        assertTrue(failed, "Empty stream must fail initial read")
    }
}
