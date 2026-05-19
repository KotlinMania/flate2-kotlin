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
}

/**
 * A wrapper around a readable source that calculates the CRC-32 of all
 * bytes read through it.
 */
public class CrcReader<R>(
    private val inner: R,
    private val crc: Crc = Crc(),
) {

    /** Get the [Crc] for this reader. */
    public fun crc(): Crc = crc

    /** Consume the wrapper and return the inner reader. */
    public fun intoInner(): R = inner

    /** Get the reader that is wrapped by this reader by reference. */
    public fun getRef(): R = inner

    /** Reset the CRC in this reader. */
    public fun reset() {
        crc.reset()
    }

    /**
     * Read bytes from [source] into [buffer] starting at [offset] for [length]
     * bytes, update the CRC with whatever was read, and return the count.
     */
    public fun read(
        source: R,
        buffer: ByteArray,
        offset: Int = 0,
        length: Int = buffer.size - offset,
        readFn: (R, ByteArray, Int, Int) -> Int,
    ): Int {
        val n = readFn(source, buffer, offset, length)
        if (n > 0) {
            crc.update(buffer.copyOfRange(offset, offset + n))
        }
        return n
    }

    override fun toString(): String = "CrcReader(crc=$crc)"
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

    /** Reset the CRC in this writer. */
    public fun reset() {
        crc.reset()
    }

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