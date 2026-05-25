// port-lint: source deflate/write.rs
package io.github.kotlinmania.flate2.deflate

import io.github.kotlinmania.flate2.Compress
import io.github.kotlinmania.flate2.CompressOps
import io.github.kotlinmania.flate2.Compression
import io.github.kotlinmania.flate2.Decompress
import io.github.kotlinmania.flate2.DecompressOps
import io.github.kotlinmania.flate2.OutputSink
import io.github.kotlinmania.flate2.Writer

/**
 * A DEFLATE encoder, or compressor.
 *
 * This structure wraps an [OutputSink] and writes compressed data to it
 * as uncompressed data is written to this encoder.
 *
 * Call [finish] to complete the compressed stream and retrieve the
 * underlying writer. Unlike the Rust original (which uses `Drop`), Kotlin
 * does not have deterministic destructors — callers must call [finish]
 * explicitly.
 */
public class DeflateWriteEncoder<W : OutputSink> private constructor(
    private val inner: Writer<W, CompressOps>,
) {
    public constructor(w: W, level: Compression) : this(
        Writer(w, CompressOps(Compress.new(level, zlibHeader = false)))
    )

    /** Acquires a reference to the underlying writer. */
    public fun getRef(): W = inner.getRef()

    /** Acquires a mutable reference to the underlying writer. */
    public fun getMut(): W = inner.getMut()

    /**
     * Resets the state of this encoder entirely, swapping out the output
     * stream for another.
     *
     * Finishes encoding the current stream, then resets the internal
     * compression state and replaces the output with [newSink].
     */
    public fun reset(newSink: W): W {
        inner.finish()
        inner.data.compress.reset()
        return inner.replace(newSink)
    }

    /**
     * Attempts to finish this output stream, writing out final chunks of data.
     *
     * After calling this, further writes may produce incorrect output.
     */
    public fun tryFinish() {
        inner.finish()
    }

    /**
     * Consumes this encoder, flushing the output stream and returning the
     * underlying writer.
     *
     * This completes the compressed stream and returns the contained writer.
     */
    public fun finish(): W {
        inner.finish()
        return inner.takeInner()
    }

    /**
     * Consumes this encoder, flushing the output stream without closing off
     * the compressed stream, and returning the underlying writer.
     *
     * The compressed stream will not be closed but only flushed.
     * To close the stream, append the two bytes `0x03` and `0x00`.
     */
    public fun flushFinish(): W {
        inner.flush()
        return inner.takeInner()
    }

    /** Returns the number of bytes written to this compressor. */
    public fun totalIn(): ULong = inner.data.totalIn()

    /** Returns the number of bytes the compressor has produced. */
    public fun totalOut(): ULong = inner.data.totalOut()

    /**
     * Writes [input] through the compressor and returns the number of
     * bytes consumed from the input.
     */
    public fun write(input: ByteArray): Int = inner.write(input)

    /**
     * Flushes the compressor, writing any buffered output to the underlying
     * sink.
     */
    public fun flush() {
        inner.flush()
    }
}

/**
 * A DEFLATE decoder, or decompressor.
 *
 * This structure wraps an [OutputSink] and writes decompressed data to it
 * as compressed data is written to this decoder.
 *
 * After decoding a single DEFLATE member, subsequent writes return zero
 * bytes consumed, allowing the caller to handle any trailing data.
 *
 * Call [finish] to complete decoding and retrieve the underlying writer.
 */
public class DeflateWriteDecoder<W : OutputSink> private constructor(
    private val inner: Writer<W, DecompressOps>,
) {
    public constructor(w: W) : this(
        Writer(w, DecompressOps(Decompress.new(zlibHeader = false)))
    )

    /** Acquires a reference to the underlying writer. */
    public fun getRef(): W = inner.getRef()

    /** Acquires a mutable reference to the underlying writer. */
    public fun getMut(): W = inner.getMut()

    /**
     * Resets the state of this decoder entirely, swapping out the output
     * stream for another.
     *
     * Finishes decoding the current stream, then resets the internal
     * decompression state and replaces the output with [newSink].
     */
    public fun reset(newSink: W): W {
        inner.finish()
        inner.data = DecompressOps(Decompress.new(zlibHeader = false))
        return inner.replace(newSink)
    }

    /**
     * Attempts to finish this output stream.
     *
     * After calling this, further writes may produce incorrect output.
     */
    public fun tryFinish() {
        inner.finish()
    }

    /**
     * Consumes this decoder, flushing the output stream and returning the
     * underlying writer.
     */
    public fun finish(): W {
        inner.finish()
        return inner.takeInner()
    }

    /** Returns the number of bytes that the decompressor has consumed. */
    public fun totalIn(): ULong = inner.data.totalIn()

    /** Returns the number of bytes that the decompressor has produced. */
    public fun totalOut(): ULong = inner.data.totalOut()

    /**
     * Writes [input] through the decompressor and returns the number of
     * bytes consumed from the input.
     */
    public fun write(input: ByteArray): Int = inner.write(input)

    /**
     * Flushes the decompressor, writing any buffered output to the
     * underlying sink.
     */
    public fun flush() {
        inner.flush()
    }
}