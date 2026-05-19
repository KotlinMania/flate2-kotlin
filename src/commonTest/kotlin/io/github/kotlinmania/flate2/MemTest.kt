// port-lint: source mem.rs
package io.github.kotlinmania.flate2

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MemTest {
    @Test
    fun issue51() {
        val data = bytes(
            0x1f, 0x8b, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xb3, 0xc9, 0x28, 0xc9,
            0xcd, 0xb1, 0xe3, 0xe5, 0xb2, 0xc9, 0x48, 0x4d, 0x4c, 0xb1, 0xb3, 0x29, 0xc9, 0x2c,
            0xc9, 0x49, 0xb5, 0x33, 0x31, 0x30, 0x51, 0xf0, 0xcb, 0x2f, 0x51, 0x70, 0xcb, 0x2f,
            0xcd, 0x4b, 0xb1, 0xd1, 0x87, 0x08, 0xda, 0xe8, 0x83, 0x95, 0x00, 0x95, 0x26, 0xe5,
            0xa7, 0x54, 0x2a, 0x24, 0xa5, 0x27, 0xe7, 0xe7, 0xe4, 0x17, 0xd9, 0x2a, 0x95, 0x67,
            0x64, 0x96, 0xa4, 0x2a, 0x81, 0x8c, 0x48, 0x4e, 0xcd, 0x2b, 0x49, 0x2d, 0xb2, 0xb3,
            0xc9, 0x30, 0x44, 0x37, 0x01, 0x28, 0x62, 0xa3, 0x0f, 0x95, 0x06, 0xd9, 0x05, 0x54,
            0x04, 0xe5, 0xe5, 0xa5, 0x67, 0xe6, 0x55, 0xe8, 0x1b, 0xea, 0x99, 0xe9, 0x19, 0x21,
            0xab, 0xd0, 0x07, 0xd9, 0x01, 0x32, 0x53, 0x1f, 0xea, 0x3e, 0x00, 0x94, 0x85, 0xeb,
            0xe4, 0xa8, 0x00, 0x00, 0x00,
        )

        val decoded = mutableListOf<Byte>()
        val decoder = Decompress.new(false)
        decoder.decompressVec(data.copyOfRange(10, data.size), decoded, FlushDecompress.Finish).getOrThrow()

        val second = decoder.decompressVec(byteArrayOf(0), decoded, FlushDecompress.None)

        assertTrue(second.isSuccess)
    }

    @Test
    fun reset() {
        val string = "hello world".encodeToByteArray()
        val zlib = compressBytes(Compress.new(Compression.default(), zlibHeader = true), string)
        val deflate = compressBytes(Compress.new(Compression.default(), zlibHeader = false), string)

        val output = ByteArray(1024)
        val decoder = Decompress.new(true)
        decoder.decompress(zlib, output, FlushDecompress.Finish).getOrThrow()
        assertEquals(string.size.toULong(), decoder.totalOut())
        assertContentEquals(string, output.copyOfRange(0, string.size))

        decoder.reset(false)
        decoder.decompress(deflate, output, FlushDecompress.Finish).getOrThrow()
        assertEquals(string.size.toULong(), decoder.totalOut())
        assertContentEquals(string, output.copyOfRange(0, string.size))
    }

    @Test
    fun testGzipFlate() {
        val string = "hello, hello!".encodeToByteArray()
        val encoder = Compress.newGzip(Compression.default(), 9u.toUByte())
        val encoded = mutableListOf<Byte>()

        encoder.compressVec(string, encoded, FlushCompress.Finish).getOrThrow()

        assertEquals(string.size.toULong(), encoder.totalIn())
        assertEquals(encoded.size.toULong(), encoder.totalOut())

        val decoder = Decompress.newGzip(9u.toUByte())
        val decoded = ByteArray(1024)
        decoder.decompress(encoded.toByteArray(), decoded, FlushDecompress.Finish).getOrThrow()

        assertContentEquals(string, decoded.copyOfRange(0, decoder.totalOut().toInt()))
    }

    @Test
    fun rawCompressionEmitsAStoredDeflateBlock() {
        val encoded = compressBytes(Compress.new(Compression.none(), zlibHeader = false), "abc".encodeToByteArray())

        assertContentEquals(bytes(0x01, 0x03, 0x00, 0xfc, 0xff, 0x61, 0x62, 0x63), encoded)
    }

    @Test
    fun testErrorMessage() {
        val decoder = Decompress.new(false)
        val decoded = ByteArray(128)
        val garbage = "xbvxzi".encodeToByteArray()

        val err = decoder.decompress(garbage, decoded, FlushDecompress.Finish).exceptionOrNull()

        assertTrue(err is DecompressError)
        assertNotNull(err.message())
    }

    @Test
    fun setDictionaryReturnsAdler32() {
        val dictionary = "common".encodeToByteArray()
        val compressor = Compress.new(Compression.default(), zlibHeader = true)
        val decompressor = Decompress.new(zlibHeader = true)

        assertEquals(0x08CA028Au, compressor.setDictionary(dictionary).getOrThrow())
        assertEquals(0x08CA028Au, decompressor.setDictionary(dictionary).getOrThrow())
    }

    private fun compressBytes(compressor: Compress, input: ByteArray): ByteArray {
        val output = mutableListOf<Byte>()
        compressor.compressVec(input, output, FlushCompress.Finish).getOrThrow()
        return output.toByteArray()
    }

    private fun bytes(vararg values: Int): ByteArray =
        ByteArray(values.size) { index -> values[index].toByte() }
}
