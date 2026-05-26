// port-lint: source crc.rs
package io.github.kotlinmania.flate2

/**
 * Simple CRC-32 checksum bindings backed by the standard reflected polynomial.
 *
 * The checksum algorithm uses the ISO 3309 / ITU-T V.42 polynomial
 * (0xEDB88320 reflected), identical to what the upstream `Crc32Fast`
 * Rust crate computes.
 */

/**
 * A CRC-32 accumulator that tracks both the running checksum and the number
 * of bytes processed.
 *
 * Use [update] to feed data, [sum] to read the current checksum, and [reset]
 * to start over.
 */
public class Crc private constructor(
    private var amt: UInt,
    private var hasher: Crc32Hasher,
) {

    /** Create a new CRC with initial state zero. */
    public constructor() : this(0u, Crc32Hasher())

    /** Returns the current CRC-32 checksum. */
    public fun sum(): UInt = hasher.finalize()

    /**
     * The number of bytes that have been used to calculate the CRC.
     * This value is only accurate if the amount is lower than 2^32.
     */
    public fun amount(): UInt = amt

    /** Update the CRC with the bytes in [data]. */
    public fun update(data: ByteArray) {
        amt += data.size.toUInt()
        hasher.update(data)
    }

    /** Reset the CRC to its initial state. */
    public fun reset() {
        amt = 0u
        hasher.reset()
    }

    /** Combine the CRC with the CRC for the subsequent block of bytes. */
    public fun combine(additional: Crc) {
        amt += additional.amt
        hasher.combine(additional.hasher)
    }

    override fun toString(): String = "Crc(sum=${sum()}, amount=$amt)"

    public companion object {
        /** Create a new CRC accumulator. */
        public fun new(): Crc = Crc()
    }
}

/**
 * A wrapper around a readable source that calculates the CRC-32 of all
 * bytes read through it.
 *
 * When [R] is a [BufferedSource], this class also implements [BufferedSource],
 * delegating buffer operations to the inner source while tracking CRC on
 * consumed bytes.
 */
public class CrcReader<R>(
    private val inner: R,
    private val crc: Crc = Crc(),
) : BufferedSource where R : BufferedSource {

    /** Get the [Crc] for this reader. */
    public fun crc(): Crc = crc

    /** Consume the wrapper and return the inner reader. */
    public fun intoInner(): R = inner

    /** Get the reader that is wrapped by this reader by reference. */
    public fun getRef(): R = inner

    /** Get the reader that is wrapped by this reader by mutable reference. */
    public fun getMut(): R = inner

    /** Reset the CRC in this reader. */
    public fun reset() {
        crc.reset()
    }

    /**
     * Read bytes from the wrapped source into [sink], update the CRC with
     * whatever was read, and return the count.
     */
    override fun read(sink: ByteArray, offset: Int, length: Int): Int {
        val n = inner.read(sink, offset, length)
        if (n > 0) {
            crc.update(sink.copyOfRange(offset, offset + n))
        }
        return n
    }

    /**
     * Return the currently buffered bytes from the wrapped source.
     * The returned array is a copy of the inner buffer.
     */
    override fun fillBuffer(): ByteArray = inner.fillBuffer()

    /**
     * Consume [amount] bytes from the inner buffer and include them in the CRC.
     */
    override fun consume(amount: Int) {
        require(amount >= 0) { "amount must be non-negative" }
        val data = inner.fillBuffer()
        require(amount <= data.size) { "amount exceeds buffered byte count" }
        if (amount > 0) {
            crc.update(data.copyOfRange(0, amount))
        }
        inner.consume(amount)
    }

    /** Return the currently buffered bytes (alias for [fillBuffer]). */
    public fun fillBuf(): ByteArray = fillBuffer()

    override fun toString(): String = "CrcReader(crc=$crc)"

    public companion object {
        /** Create a new CRC reader wrapping a buffered source. */
        public fun <R : BufferedSource> new(reader: R): CrcReader<R> = CrcReader(reader)
    }
}

/**
 * A wrapper around a writable sink that calculates the CRC-32 of all
 * bytes written through it.
 */
public class CrcWriter<W>(
    private val inner: W,
    private val crc: Crc = Crc(),
) {

    /** Get the [Crc] for this writer. */
    public fun crc(): Crc = crc

    /** Consume the wrapper and return the inner writer. */
    public fun intoInner(): W = inner

    /** Get the writer that is wrapped by this writer by reference. */
    public fun getRef(): W = inner

    /** Get the writer that is wrapped by this writer by mutable reference. */
    public fun getMut(): W = inner

    /** Reset the CRC in this writer. */
    public fun reset() {
        crc.reset()
    }

    /**
     * Write bytes to the wrapped writer, update the CRC with whatever was
     * written, and return the count.
     */
    public fun write(
        buffer: ByteArray,
        offset: Int = 0,
        length: Int = buffer.size - offset,
        writeFn: (W, ByteArray, Int, Int) -> Int,
    ): Int = write(inner, buffer, offset, length, writeFn)

    /**
     * Write bytes to [sink] from [buffer] starting at [offset] for [length]
     * bytes, update the CRC with whatever was written, and return the count.
     */
    public fun write(
        sink: W,
        buffer: ByteArray,
        offset: Int = 0,
        length: Int = buffer.size - offset,
        writeFn: (W, ByteArray, Int, Int) -> Int,
    ): Int {
        val n = writeFn(sink, buffer, offset, length)
        if (n > 0) {
            crc.update(buffer.copyOfRange(offset, offset + n))
        }
        return n
    }

    override fun toString(): String = "CrcWriter(crc=$crc)"

    public companion object {
        /** Create a new CRC writer. */
        public fun <W> new(writer: W): CrcWriter<W> = CrcWriter(writer)
    }
}

/**
 * [OutputSink] implementation for [CrcWriter] that delegates to the inner
 * sink while updating the CRC.
 */
public class CrcWriterSink<W : OutputSink>(
    public val inner: CrcWriter<W>,
) : OutputSink {

    override fun write(source: ByteArray, offset: Int, length: Int): Int {
        val n = inner.getMut().write(source, offset, length)
        if (n > 0) {
            inner.crc().update(source.copyOfRange(offset, offset + n))
        }
        return n
    }

    override fun flush() {
        inner.getMut().flush()
    }
}

/** Write bytes to the wrapped sink and include them in the CRC. */
public fun <W> CrcWriter<W>.write(
    buffer: ByteArray,
    offset: Int = 0,
    length: Int = buffer.size - offset,
): Int where W : OutputSink =
    write(buffer, offset, length) { sink, source, at, count ->
        sink.write(source, at, count)
    }

/** Flush the wrapped sink. */
public fun <W> CrcWriter<W>.flush() where W : OutputSink {
    getMut().flush()
}

/**
 * CRC-32 hasher using the standard reflected polynomial (ISO 3309).
 *
 * This is the same algorithm implemented by the upstream `Crc32Fast`
 * Rust crate. It uses a precomputed 256-entry lookup table for
 * byte-at-a-time updates.
 */
internal class Crc32Hasher {
    private var state: Int = INIT_STATE

    fun update(data: ByteArray) {
        for (b in data) {
            state = TABLE[(state xor b.toInt()) and 0xFF] xor (state ushr 8)
        }
    }

    fun finalize(): UInt = state.toUInt() xor FINAL_XOR

    fun reset() {
        state = INIT_STATE
    }

    fun combine(other: Crc32Hasher) {
        state = combineCrc32(state xor FINAL_XOR.toInt(), other.state xor FINAL_XOR.toInt())
    }

    companion object {
        private const val INIT_STATE: Int = -1
        private const val FINAL_XOR: UInt = 0xFFFFFFFFu
        private const val POLY: Int = -0x12477CE0

        private val TABLE: IntArray = IntArray(256) { i ->
            var crc = i
            repeat(8) {
                crc = if (crc and 1 != 0) (crc ushr 1) xor POLY else crc ushr 1
            }
            crc
        }

        private fun gf2MatrixTimes(mat: Int, vec: Int): Int {
            var result = 0
            var m = mat
            var v = vec
            while (v != 0) {
                if (v and 1 != 0) result = result xor m
                v = v ushr 1
                m = m shl 1
            }
            return result
        }

        private fun combineCrc32(crc1: Int, crc2: Int): Int {
            val even = IntArray(32)
            val odd = IntArray(32)
            odd[0] = POLY
            even[0] = gf2MatrixTimes(POLY, POLY)
            var i = 1
            while (i < 32) {
                odd[i] = gf2MatrixTimes(even[i - 1], odd[i - 1])
                even[i] = gf2MatrixTimes(even[i - 1], even[i - 1])
                i += 1
            }
            var result = crc1
            i = 0
            while (i < 32) {
                if ((crc2 ushr i) and 1 != 0) {
                    result = result xor even[i]
                }
                i += 1
            }
            return result
        }
    }
}
