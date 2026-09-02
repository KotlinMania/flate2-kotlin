// port-lint: tests tests/capabilities.rs
package io.github.kotlinmania.flate2

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CapabilitiesTest {
    @Test
    fun compressNewWithWindowBitsIsPresentAndWorks() {
        val string = "hello world".encodeToByteArray()

        val encoded9 = mutableListOf<Byte>()
        val encoder9 = Compress.newWithWindowBits(Compression.default(), true, 9u)
        encoder9.compressVec(string, encoded9, FlushCompress.Finish).getOrThrow()
        assertNotEquals(0, encoded9.size)

        val decoder9 = Decompress.newWithWindowBits(true, 9u)
        val decoded9 = ByteArray(1024)
        decoder9.decompress(encoded9.toByteArray(), decoded9, FlushDecompress.Finish).getOrThrow()
        assertContentEquals(string, decoded9.copyOfRange(0, string.size))

        val encoded15 = mutableListOf<Byte>()
        val encoder15 = Compress.newWithWindowBits(Compression.default(), false, 15u)
        encoder15.compressVec(string, encoded15, FlushCompress.Finish).getOrThrow()
        assertNotEquals(0, encoded15.size)

        val decoder15 = Decompress.newWithWindowBits(false, 15u)
        val decoded15 = ByteArray(1024)
        decoder15.decompress(encoded15.toByteArray(), decoded15, FlushDecompress.Finish).getOrThrow()
        assertContentEquals(string, decoded15.copyOfRange(0, string.size))
    }

    @Test
    fun decompressNewGzipWindowBitsIsPresentAndWorks() {
        val string = "hello world".encodeToByteArray()

        for (windowBits in listOf(9u.toUByte(), 12u.toUByte(), 15u.toUByte())) {
            val encoded = mutableListOf<Byte>()
            val encoder = Compress.newGzip(Compression.default(), windowBits)
            encoder.compressVec(string, encoded, FlushCompress.Finish).getOrThrow()

            val decoder = Decompress.newGzip(windowBits)
            val decoded = ByteArray(1024)
            decoder.decompress(encoded.toByteArray(), decoded, FlushDecompress.Finish).getOrThrow()
            assertContentEquals(
                string,
                decoded.copyOfRange(0, string.size),
                "Failed with windowBits=$windowBits",
            )
        }
    }

    @Test
    fun compressNewWithWindowBitsInvalidLow() {
        assertFailsWith<IllegalArgumentException> {
            Compress.newWithWindowBits(Compression.default(), true, 8u)
        }
    }

    @Test
    fun compressNewWithWindowBitsInvalidHigh() {
        assertFailsWith<IllegalArgumentException> {
            Compress.newWithWindowBits(Compression.default(), true, 16u)
        }
    }

    @Test
    fun compressNewGzipInvalidLow() {
        assertFailsWith<IllegalArgumentException> {
            Compress.newGzip(Compression.default(), 8u)
        }
    }

    @Test
    fun compressNewGzipInvalidHigh() {
        assertFailsWith<IllegalArgumentException> {
            Compress.newGzip(Compression.default(), 16u)
        }
    }

    @Test
    fun setDictionaryWithZlibHeader() {
        val string = "hello, hello!".encodeToByteArray()
        val dictionary = "hello".encodeToByteArray()

        val encoded = mutableListOf<Byte>()
        val encoder = Compress.new(Compression.default(), true)
        val dictionaryAdler = encoder.setDictionary(dictionary).getOrThrow()

        encoder.compressVec(string, encoded, FlushCompress.Finish).getOrThrow()
        assertEquals(string.size.toULong(), encoder.totalIn())
        assertEquals(encoded.size.toULong(), encoder.totalOut())

        val decoder = Decompress.new(true)
        val decoded = ByteArray(1024)
        val decompressError =
            decoder.decompress(encoded.toByteArray(), decoded, FlushDecompress.Finish).exceptionOrNull()
                as? DecompressError
        val requiredAdler =
            decompressError?.needsDictionary()
                ?: throw AssertionError("decompression should fail requiring a dictionary")

        assertEquals(
            dictionaryAdler,
            requiredAdler,
            "the Adler-32 checksum should match the value when the dictionary was set on the compressor",
        )

        val actualAdler = decoder.setDictionary(dictionary).getOrThrow()
        assertEquals(requiredAdler, actualAdler)

        val totalIn = decoder.totalIn().toInt()
        val totalOut = decoder.totalOut().toInt()
        val decompressResult =
            decoder.decompress(
                encoded.toByteArray().copyOfRange(totalIn, encoded.size),
                decoded.copyOfRange(totalOut, decoded.size),
                FlushDecompress.Finish,
            )
        assertTrue(decompressResult.isSuccess)
        assertContentEquals(string, decoded.copyOfRange(0, decoder.totalOut().toInt()))
    }

    @Test
    fun setDictionaryRaw() {
        val string = "hello, hello!".encodeToByteArray()
        val dictionary = "hello".encodeToByteArray()

        val encoded = mutableListOf<Byte>()
        val encoder = Compress.new(Compression.default(), false)
        encoder.setDictionary(dictionary).getOrThrow()
        encoder.compressVec(string, encoded, FlushCompress.Finish).getOrThrow()

        assertEquals(string.size.toULong(), encoder.totalIn())
        assertEquals(encoded.size.toULong(), encoder.totalOut())

        val decoder = Decompress.new(false)
        decoder.setDictionary(dictionary).getOrThrow()

        val decoded = ByteArray(1024)
        val decompressResult = decoder.decompress(encoded.toByteArray(), decoded, FlushDecompress.Finish)
        assertTrue(decompressResult.isSuccess)
        assertContentEquals(string, decoded.copyOfRange(0, decoder.totalOut().toInt()))
    }

    @Test
    fun compressionLevelsAreEffective() {
        val input = "hello hello hello hello hello hello hello hello".encodeToByteArray()

        val encodedNone = mutableListOf<Byte>()
        Compress.new(Compression.none(), true)
            .compressVec(input, encodedNone, FlushCompress.Finish)
            .getOrThrow()

        val encodedBest = mutableListOf<Byte>()
        Compress.new(Compression.best(), true)
            .compressVec(input, encodedBest, FlushCompress.Finish)
            .getOrThrow()

        assertTrue(
            encodedBest.size <= encodedNone.size,
            "best compression produced larger output than no compression: best=${encodedBest.size}, none=${encodedNone.size}",
        )
    }

    @Test
    fun setLevelIsEffective() {
        val input = "hello hello hello hello hello hello hello hello".encodeToByteArray()
        val noCompression = Compression.none()
        val bestCompression = Compression.best()

        val encodedNone = mutableListOf<Byte>()
        val compressNone = Compress.new(bestCompression, true)
        compressNone.setLevel(noCompression).getOrThrow()
        compressNone.compressVec(input, encodedNone, FlushCompress.Finish).getOrThrow()

        val encodedBest = mutableListOf<Byte>()
        val compressBest = Compress.new(noCompression, true)
        compressBest.setLevel(bestCompression).getOrThrow()
        compressBest.compressVec(input, encodedBest, FlushCompress.Finish).getOrThrow()

        assertTrue(
            encodedBest.size <= encodedNone.size,
            "best compression produced larger output than no compression: best=${encodedBest.size}, none=${encodedNone.size}",
        )
    }
}
