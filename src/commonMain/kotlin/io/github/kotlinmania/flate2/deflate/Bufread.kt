@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
// port-lint: source deflate/bufread.rs

package io.github.kotlinmania.flate2.deflate

import io.github.kotlinmania.flate2.BufferedSource
import io.github.kotlinmania.flate2.Compress
import io.github.kotlinmania.flate2.Compression
import io.github.kotlinmania.flate2.Decompress
import io.github.kotlinmania.flate2.InputSource
import io.github.kotlinmania.flate2.readThroughCodec
import kotlin.native.HiddenFromObjC

/** Resets the internal compression state of [encoder]. */
public fun <R : BufferedSource> resetEncoderData(encoder: DeflateEncoder<R>) {
    encoder.resetData()
}

/** Resets the internal decompression state of [decoder]. */
public fun <R : BufferedSource> resetDecoderData(decoder: DeflateDecoder<R>) {
    decoder.resetData()
}

/**
 * A DEFLATE encoder, or compressor.
 *
 * When read from, this reads uncompressed data from the underlying
 * [BufferedSource] and provides the compressed data.
 *
 * @param R the type of the underlying read source
 */
@HiddenFromObjC
public class DeflateEncoder<R : BufferedSource>(
    private var obj: R,
    private val data: Compress,
) : InputSource {
    public constructor(r: R, level: Compression) : this(r, Compress.new(level, zlibHeader = false))

    /** Resets the internal compression state, keeping the same source. */
    public fun resetData() {
        data.reset()
    }

    /** Resets the internal compression state and replaces the underlying source. */
    public fun reset(r: R): R {
        data.reset()
        val old = obj
        obj = r
        return old
    }

    /** Acquires a reference to the underlying source. */
    public fun getRef(): R = obj

    /** Acquires a mutable reference to the underlying source. */
    public fun getMut(): R = obj

    /** Consumes this encoder, returning the underlying source. */
    public fun intoInner(): R = obj

    /** Returns the total number of bytes read into this compressor. */
    public fun totalIn(): ULong = data.totalIn()

    /** Returns the total number of bytes the compressor has produced. */
    public fun totalOut(): ULong = data.totalOut()

    /**
     * Reads compressed data into [sink], reading uncompressed data from the
     * underlying source as needed.
     *
     * Returns the number of bytes written to [sink].
     */
    override fun read(sink: ByteArray, offset: Int, length: Int): Int {
        val buf = ByteArray(length)
        val n =
            readThroughCodec(
                obj,
                io.github.kotlinmania.flate2
                    .CompressOps(data),
                buf,
            )
        if (n > 0) {
            buf.copyInto(sink, offset, 0, n)
        }
        return n
    }

    /**
     * Reads compressed data into [dst].
     *
     * Returns the number of bytes written.
     */
    public fun read(dst: ByteArray): Int =
        readThroughCodec(
            obj,
            io.github.kotlinmania.flate2
                .CompressOps(data),
            dst,
        )
}

/**
 * A DEFLATE decoder, or decompressor.
 *
 * When read from, this reads compressed data from the underlying
 * [BufferedSource] and provides the uncompressed data.
 *
 * After reading a single DEFLATE member, a subsequent read returns 0 bytes
 * even if more data is available in the underlying source. Use
 * [intoInner] to recover the remaining data.
 *
 * @param R the type of the underlying read source
 */
@HiddenFromObjC
public class DeflateDecoder<R : BufferedSource>(
    private var obj: R,
    private val data: Decompress,
) : InputSource {
    public constructor(r: R) : this(r, Decompress.new(zlibHeader = false))

    /** Resets the internal decompression state and replaces the underlying source. */
    public fun reset(r: R): R {
        data.reset(zlibHeader = false)
        val old = obj
        obj = r
        return old
    }

    /** Resets the internal decompression state, keeping the same source. */
    public fun resetData() {
        data.reset(zlibHeader = false)
    }

    /** Acquires a reference to the underlying source. */
    public fun getRef(): R = obj

    /** Acquires a mutable reference to the underlying source. */
    public fun getMut(): R = obj

    /** Consumes this decoder, returning the underlying source. */
    public fun intoInner(): R = obj

    /** Returns the total number of bytes the decompressor has consumed. */
    public fun totalIn(): ULong = data.totalIn()

    /** Returns the total number of bytes the decompressor has produced. */
    public fun totalOut(): ULong = data.totalOut()

    /**
     * Reads decompressed data into [sink], reading compressed data from the
     * underlying source as needed.
     *
     * Returns the number of bytes written to [sink].
     */
    override fun read(sink: ByteArray, offset: Int, length: Int): Int {
        val buf = ByteArray(length)
        val n =
            readThroughCodec(
                obj,
                io.github.kotlinmania.flate2
                    .DecompressOps(data),
                buf,
            )
        if (n > 0) {
            buf.copyInto(sink, offset, 0, n)
        }
        return n
    }

    /**
     * Reads decompressed data into [dst].
     *
     * Returns the number of bytes written.
     */
    public fun read(dst: ByteArray): Int =
        readThroughCodec(
            obj,
            io.github.kotlinmania.flate2
                .DecompressOps(data),
            dst,
        )
}
