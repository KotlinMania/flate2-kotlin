// port-lint: source gz/bufread.rs
package io.github.kotlinmania.flate2.gz

import io.github.kotlinmania.flate2.Compression

/**
 * A gzip streaming encoder that reads uncompressed data from a buffered source
 * and provides compressed data when read.
 *
 * This wraps a DEFLATE encoder with CRC-32 bookkeeping and gzip header/footer.
 *
 * Construction requires a pre-built [DeflateEncoder] wrapping a [CrcReader];
 * use the [gzEncoder] factory function or [GzBuilder.bufRead] to create one.
 */
public class GzEncoder<R> internal constructor(
    internal val inner: Any,
    internal val header: ByteArray,
) {
    /** Acquires a reference to the underlying reader. */
    public fun getRef(): R = throw UnsupportedOperationException("Use gzEncoder() factory to construct")

    /** Acquires a mutable reference to the underlying reader. */
    public fun getMut(): R = throw UnsupportedOperationException("Use gzEncoder() factory to construct")

    /** Consumes this encoder, returning the underlying stream. */
    public fun intoInner(): R = throw UnsupportedOperationException("Use gzEncoder() factory to construct")
}

/**
 * A decoder for a single member of a gzip file.
 *
 * After reading a single member of the gzip data this reader will return
 * zero bytes even if there are more bytes available in the underlying reader.
 * Use [intoInner] after a zero-byte read to recover the underlying reader.
 */
public class GzDecoder<R> internal constructor(
    internal var state: GzState,
    internal val reader: Any,
    internal var multi: Boolean,
) {
    /** Returns the header associated with this stream, if it was valid. */
    public fun header(): GzHeader? = when (state) {
        is GzState.Body -> (state as GzState.Body).header
        is GzState.Finished -> (state as GzState.Finished).header
        is GzState.End -> (state as GzState.End).header
        else -> null
    }

    /** Acquires a reference to the underlying reader. */
    public fun getRef(): R = throw UnsupportedOperationException("Use gzDecoder() factory to construct")

    /** Acquires a mutable reference to the underlying stream. */
    public fun getMut(): R = throw UnsupportedOperationException("Use gzDecoder() factory to construct")

    /** Consumes this decoder, returning the underlying reader. */
    public fun intoInner(): R = throw UnsupportedOperationException("Use gzDecoder() factory to construct")
}

internal sealed class GzState {
    data class Header(val parser: GzHeaderParser) : GzState()
    data class Body(val header: GzHeader) : GzState()
    data class Finished(val header: GzHeader, val pos: Int, val buf: ByteArray) : GzState()
    data class Err(val error: Throwable) : GzState()
    data class End(val header: GzHeader?) : GzState()
}

/**
 * A gzip streaming decoder that decodes a gzip file that may have multiple members.
 */
public class MultiGzDecoder<R>(internal val inner: GzDecoder<R>) {
    /** Returns the current header associated with this stream, if valid. */
    public fun header(): GzHeader? = inner.header()

    /** Acquires a reference to the underlying reader. */
    public fun getRef(): R = inner.getRef()

    /** Acquires a mutable reference to the underlying stream. */
    public fun getMut(): R = inner.getMut()

    /** Consumes this decoder, returning the underlying reader. */
    public fun intoInner(): R = inner.intoInner()
}

/** Create a [GzEncoder] from header bytes, source, and compression level.
 *
 * Prerequisite: [DeflateEncoder] and [CrcReader] must implement [BufferedSource]
 * for this to work with full type safety. Currently a placeholder.
 */
public fun <R> gzEncoder(header: ByteArray, r: R, lvl: Compression): GzEncoder<R> {
    throw UnsupportedOperationException("gzEncoder requires BufferedSource conformance on DeflateEncoder/CrcReader — not yet wired")
}

/** Create a [GzDecoder] from a buffered source. */
public fun <R> gzDecoder(r: R): GzDecoder<R> {
    throw UnsupportedOperationException("gzDecoder requires BufferedSource conformance on DeflateDecoder/CrcReader — not yet wired")
}

/** Create a [MultiGzDecoder] from a buffered source. */
public fun <R> multiGzDecoder(r: R): MultiGzDecoder<R> {
    throw UnsupportedOperationException("multiGzDecoder requires BufferedSource conformance — not yet wired")
}

/** Allow [GzBuilder.bufRead] to construct a [GzEncoder]. */
public fun <R> GzBuilder.bufRead(r: R, lvl: Compression): GzEncoder<R> =
    gzEncoder(intoHeader(lvl), r, lvl)