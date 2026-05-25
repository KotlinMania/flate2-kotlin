// Minimal blocking I/O interfaces for flate2-kotlin, mirroring the upstream
// read, write, and buffered-read traits in a multiplatform-safe way.
package io.github.kotlinmania.flate2

/**
 * A readable source of bytes, mirroring the upstream `Read` trait.
 *
 * Reads up to [sink].size - [offset] bytes into [sink] starting at
 * [offset] and returns the number of bytes read, or -1 if the end of
 * the source has been reached.
 */
public interface InputSource {
    public fun read(sink: ByteArray, offset: Int = 0, length: Int = sink.size - offset): Int
}

/**
 * A writable sink of bytes, mirroring the upstream `Write` trait.
 *
 * Writes bytes from [source] starting at [offset] for [length] bytes,
 * and returns the number of bytes written.
 */
public interface OutputSink {
    public fun write(source: ByteArray, offset: Int = 0, length: Int = source.size - offset): Int
    public fun flush()
}

/**
 * A readable source that supports buffered access, mirroring the
 * upstream `BufRead` trait.
 *
 * [fillBuffer] returns a copy of the currently buffered data.
 * [consume] advances the buffer position by [amount] bytes.
 */
public interface BufferedSource : InputSource {
    public fun fillBuffer(): ByteArray
    public fun consume(amount: Int)
}
