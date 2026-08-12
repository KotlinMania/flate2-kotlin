@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
// port-lint: source gz/read.rs

package io.github.kotlinmania.flate2.gz

import io.github.kotlinmania.flate2.BufReader
import io.github.kotlinmania.flate2.InputSource
import kotlin.native.HiddenFromObjC

/**
 * A gzip streaming encoder that reads uncompressed data from a readable source
 * and provides compressed data when read.
 *
 * This wraps a [GzEncoder] around a [BufReader] for convenience.
 */
@HiddenFromObjC
public class GzReadEncoder<R : InputSource> internal constructor(
    internal val inner: GzEncoder<BufReader<R>>,
)

/** Create a read-side [GzReadEncoder] wrapping [inner]. */
public fun <R : InputSource> gzReadEncoder(inner: GzEncoder<BufReader<R>>): GzReadEncoder<R> =
    GzReadEncoder(inner)

/**
 * A decoder for a single member of a gzip file that reads from a readable source.
 *
 * After reading a single member of the gzip data this reader will return
 * zero bytes even if there are more bytes available in the underlying source.
 */
@HiddenFromObjC
public class GzReadDecoder<R : InputSource> internal constructor(
    internal val inner: GzDecoder<BufReader<R>>,
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
@HiddenFromObjC
public class MultiGzReadDecoder<R : InputSource> internal constructor(
    internal val inner: MultiGzDecoder<BufReader<R>>,
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
