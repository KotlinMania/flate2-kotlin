@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
// port-lint: source zlib/read.rs
package io.github.kotlinmania.flate2.zlib

import io.github.kotlinmania.flate2.BufReader
import io.github.kotlinmania.flate2.BufferedSource
import io.github.kotlinmania.flate2.Compression
import kotlin.native.HiddenFromObjC

/**
 * A zlib encoder, or compressor.
 *
 * This structure reads uncompressed data from the underlying [BufferedSource]
 * and provides the zlib-compressed data when read from.
 *
 * @param R the type of the underlying read source
 */
@HiddenFromObjC
public class ZlibReadEncoder<R : BufferedSource>(
    private val inner: ZlibEncoder<BufReader<R>>,
) {
    public constructor(r: R, level: Compression) : this(
        ZlibEncoder(BufReader(r), level)
    )

    /** Resets the state of this encoder entirely, swapping out the input stream. */
    public fun reset(r: R): R {
        resetEncoderData(inner)
        return inner.getMut().reset(r)
    }

    /** Acquires a reference to the underlying reader. */
    public fun getRef(): R = inner.getRef().getRef()

    /** Acquires a mutable reference to the underlying stream. */
    public fun getMut(): BufReader<R> = inner.getMut()

    /** Consumes this encoder, returning the underlying reader. */
    public fun intoInner(): R = inner.intoInner().intoInner()

    /** Returns the number of bytes that have been read into this compressor. */
    public fun totalIn(): ULong = inner.totalIn()

    /** Returns the number of bytes that the compressor has produced. */
    public fun totalOut(): ULong = inner.totalOut()

    /**
     * Reads compressed data into [dst], reading uncompressed data from the
     * underlying source as needed.
     */
    public fun read(dst: ByteArray): Int = inner.read(dst)
}

/**
 * A zlib decoder, or decompressor.
 *
 * This structure reads zlib-compressed data from the underlying
 * [BufferedSource] and provides the uncompressed data when read from.
 *
 * After reading a single zlib member, a subsequent read returns 0 bytes.
 * Use [intoInner] to recover the remaining data.
 *
 * @param R the type of the underlying read source
 */
@HiddenFromObjC
public class ZlibReadDecoder<R : BufferedSource>(
    private val inner: ZlibDecoder<BufReader<R>>,
) {
    public constructor(r: R) : this(ZlibDecoder(BufReader(r)))

    public constructor(r: R, buffer: ByteArray) : this(ZlibDecoder(BufReader(buffer, r)))

    /** Resets the state of this decoder entirely, swapping out the input stream. */
    public fun reset(r: R): R {
        resetDecoderData(inner)
        return inner.getMut().reset(r)
    }

    /** Acquires a reference to the underlying stream. */
    public fun getRef(): R = inner.getRef().getRef()

    /** Acquires a mutable reference to the underlying stream. */
    public fun getMut(): BufReader<R> = inner.getMut()

    /** Consumes this decoder, returning the underlying reader. */
    public fun intoInner(): R = inner.intoInner().intoInner()

    /** Returns the number of bytes that the decompressor has consumed. */
    public fun totalIn(): ULong = inner.totalIn()

    /** Returns the number of bytes that the decompressor has produced. */
    public fun totalOut(): ULong = inner.totalOut()

    /**
     * Reads decompressed data into [dst], reading compressed data from the
     * underlying source as needed.
     */
    public fun read(dst: ByteArray): Int = inner.read(dst)
}