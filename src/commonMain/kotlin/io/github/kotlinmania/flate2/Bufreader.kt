// port-lint: source bufreader.rs
package io.github.kotlinmania.flate2

import kotlin.math.min

/**
 * A buffered reader that wraps a readable source, reading ahead into an
 * internal buffer to satisfy small reads efficiently.
 *
 * This class mirrors the upstream `BufReader<R>` which wraps a `Read`
 * implementor. The upstream uses `std::io::BufReader` — this port provides
 * the same buffering contract for [InputSource] types.
 */
public class BufReader<R : InputSource> internal constructor(
    private var inner: R,
    private val buf: ByteArray,
    private var pos: Int = 0,
    private var cap: Int = 0,
) {

    public constructor(inner: R) : this(inner, ByteArray(DEFAULT_BUF_SIZE))

    override fun toString(): String =
        "BufReader(reader=$inner, buffer=${cap - pos}/${buf.size})"

    /** Get the underlying source by reference. */
    public fun getRef(): R = inner

    /** Get a mutable reference to the underlying source. */
    public fun getMut(): R = inner

    /** Consume the wrapper and return the underlying source. */
    public fun intoInner(): R = inner

    /**
     * Replace the underlying source with [newInner], returning the old one.
     * Resets the buffer position and capacity to zero.
     */
    public fun reset(newInner: R): R {
        pos = 0
        cap = 0
        val old = inner
        inner = newInner
        return old
    }

    /**
     * Read bytes into [sink] starting at [offset] for up to [length] bytes.
     *
     * If the internal buffer is empty and the requested read is larger than
     * the buffer, bypasses buffering and reads directly from the source.
     */
    public fun read(sink: ByteArray, offset: Int = 0, length: Int = sink.size - offset): Int {
        if (pos == cap && length >= buf.size) {
            return inner.read(sink, offset, length)
        }
        val available = fillBuffer()
        val toCopy = minOf(available.size, length)
        available.copyInto(sink, offset, 0, toCopy)
        consume(toCopy)
        return toCopy
    }

    /**
     * Return a copy of the currently buffered data.
     *
     * If the buffer is exhausted, reads more data from the underlying source
     * into the internal buffer first.
     */
    public fun fillBuffer(): ByteArray {
        if (pos == cap) {
            cap = inner.read(buf, 0, buf.size)
            pos = 0
            if (cap <= 0) return ByteArray(0)
        }
        return buf.copyOfRange(pos, cap)
    }

    /** Mark [amount] bytes from the buffer as consumed. */
    public fun consume(amount: Int) {
        pos = min(pos + amount, cap)
    }

    public companion object {
        internal const val DEFAULT_BUF_SIZE: Int = 32 * 1024
    }
}