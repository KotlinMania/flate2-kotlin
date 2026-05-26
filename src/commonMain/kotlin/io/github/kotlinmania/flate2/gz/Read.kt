// port-lint: source gz/read.rs
package io.github.kotlinmania.flate2.gz

import io.github.kotlinmania.flate2.Compression

/**
 * A gzip streaming encoder that reads uncompressed data from a readable source
 * and provides compressed data when read.
 *
 * This wraps a [GzEncoder] around a [BufReader] for convenience.
 */
public class GzReadEncoder<R : io.github.kotlinmania.flate2.InputSource> internal constructor(
    internal val inner: GzEncoder<io.github.kotlinmania.flate2.BufReader<R>>,
)

/** Create a read-side [GzReadEncoder] from a [GzBuilder] header and compression level. */
public fun <R : io.github.kotlinmania.flate2.InputSource> gzEncoder(inner: GzEncoder<io.github.kotlinmania.flate2.BufReader<R>>): GzReadEncoder<R> =
    GzReadEncoder(inner)

/**
 * A decoder for a single member of a gzip file that reads from a readable source.
 *
 * After reading a single member of the gzip data this reader will return
 * zero bytes even if there are more bytes available in the underlying source.
 */
public class GzReadDecoder<R : io.github.kotlinmania.flate2.InputSource> internal constructor(
    internal val inner: GzDecoder<io.github.kotlinmania.flate2.BufReader<R>>,
) {
    /** Returns the header associated with this stream, if it was valid. */
    public fun header(): GzHeader? = inner.header()

    /** Acquires a reference to the underlying source. */
    public fun getRef(): R = inner.getRef().getRef()

    /** Acquires a mutable reference to the underlying source. */
    public fun getMut(): R = inner.getMut().getMut()

    /** Consumes this decoder, returning the underlying source. */
    public fun intoInner(): R = inner.intoInner().intoInner()
}

/**
 * A gzip streaming decoder that decodes a gzip file with multiple members
 * from a readable source.
 */
public class MultiGzReadDecoder<R : io.github.kotlinmania.flate2.InputSource> internal constructor(
    internal val inner: MultiGzDecoder<io.github.kotlinmania.flate2.BufReader<R>>,
) {
    /** Returns the current header associated with this stream, if valid. */
    public fun header(): GzHeader? = inner.header()

    /** Acquires a reference to the underlying source. */
    public fun getRef(): R = inner.getRef().getRef()

    /** Acquires a mutable reference to the underlying source. */
    public fun getMut(): R = inner.getMut().getMut()

    /** Consumes this decoder, returning the underlying source. */
    public fun intoInner(): R = inner.intoInner().intoInner()
}