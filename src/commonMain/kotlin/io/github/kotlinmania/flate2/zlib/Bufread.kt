@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
// port-lint: source zlib/bufread.rs
package io.github.kotlinmania.flate2.zlib

import io.github.kotlinmania.flate2.BufferedSource
import io.github.kotlinmania.flate2.BufReader
import io.github.kotlinmania.flate2.Compress
import io.github.kotlinmania.flate2.Compression
import io.github.kotlinmania.flate2.Decompress
import io.github.kotlinmania.flate2.readThroughCodec
import kotlin.native.HiddenFromObjC

/** Resets the internal compression state of [encoder]. */
public fun <R : BufferedSource> resetEncoderData(encoder: ZlibEncoder<R>) {
    encoder.resetData()
}

/** Resets the internal decompression state of [decoder]. */
public fun <R : BufferedSource> resetDecoderData(decoder: ZlibDecoder<R>) {
    decoder.resetData()
}

/**
 * A zlib encoder, or compressor.
 *
 * When read from, this reads uncompressed data from the underlying
 * [BufferedSource] and provides the zlib-compressed data.
 *
 * @param R the type of the underlying read source
 */
@HiddenFromObjC
public class ZlibEncoder<R : BufferedSource>(
    private var obj: R,
    private val data: Compress,
) {
    public constructor(r: R, level: Compression) : this(r, Compress.new(level, zlibHeader = true))

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
     * Reads compressed data into [dst], reading uncompressed data from the
     * underlying source as needed.
     */
    public fun read(dst: ByteArray): Int =
        readThroughCodec(obj, io.github.kotlinmania.flate2.CompressOps(data), dst)
}

/**
 * A zlib decoder, or decompressor.
 *
 * When read from, this reads zlib-compressed data from the underlying
 * [BufferedSource] and provides the uncompressed data.
 *
 * After reading a single zlib member, a subsequent read returns 0 bytes.
 * Use [intoInner] to recover the remaining data.
 *
 * @param R the type of the underlying read source
 */
@HiddenFromObjC
public class ZlibDecoder<R : BufferedSource>(
    private var obj: R,
    private val data: Decompress,
) {
    public constructor(r: R) : this(r, Decompress.new(zlibHeader = true))

    /** Resets the internal decompression state and replaces the underlying source. */
    public fun reset(r: R): R {
        data.reset(zlibHeader = true)
        val old = obj
        obj = r
        return old
    }

    /** Resets the internal decompression state, keeping the same source. */
    public fun resetData() {
        data.reset(zlibHeader = true)
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
     * Reads decompressed data into [dst], reading compressed data from the
     * underlying source as needed.
     */
    public fun read(dst: ByteArray): Int =
        readThroughCodec(obj, io.github.kotlinmania.flate2.DecompressOps(data), dst)
}