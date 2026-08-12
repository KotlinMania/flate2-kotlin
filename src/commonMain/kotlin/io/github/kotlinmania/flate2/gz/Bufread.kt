@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
// port-lint: source gz/bufread.rs

package io.github.kotlinmania.flate2.gz

import io.github.kotlinmania.flate2.BufReader
import io.github.kotlinmania.flate2.BufferedSource
import io.github.kotlinmania.flate2.Compression
import io.github.kotlinmania.flate2.CrcReader
import io.github.kotlinmania.flate2.deflate.DeflateDecoder
import io.github.kotlinmania.flate2.deflate.DeflateEncoder
import kotlin.native.HiddenFromObjC
import io.github.kotlinmania.flate2.gz.GzHeaderParser.Companion.new as newParser

private const val CRC_BYTES_LEN: Int = 8

private fun copy(into: ByteArray, from: ByteArray, pos: IntArray): Int {
    val min = minOf(into.size, from.size - pos[0])
    from.copyInto(into, 0, pos[0], pos[0] + min)
    pos[0] += min
    return min
}

private fun finish(buf: ByteArray): Pair<UInt, UInt> {
    val crc =
        (
            buf[0].toLong() and 0xFF or
                ((buf[1].toLong() and 0xFF) shl 8) or
                ((buf[2].toLong() and 0xFF) shl 16) or
                ((buf[3].toLong() and 0xFF) shl 24)
        ).toUInt()
    val amt =
        (
            buf[4].toLong() and 0xFF or
                ((buf[5].toLong() and 0xFF) shl 8) or
                ((buf[6].toLong() and 0xFF) shl 16) or
                ((buf[7].toLong() and 0xFF) shl 24)
        ).toUInt()
    return Pair(crc, amt)
}

internal sealed class GzState {
    data class Header(
        val parser: GzHeaderParser,
    ) : GzState()

    data class Body(
        val header: GzHeader,
    ) : GzState()

    class Finished(
        val header: GzHeader,
        var pos: Int,
        val buf: ByteArray,
    ) : GzState()

    class Err(
        val error: Throwable,
    ) : GzState()

    data class End(
        val header: GzHeader?,
    ) : GzState()
}

/**
 * A gzip streaming encoder that reads uncompressed data from a buffered source
 * and provides compressed data when read.
 */
@HiddenFromObjC
public class GzEncoder<R : BufferedSource> internal constructor(
    internal val inner: DeflateEncoder<CrcReader<R>>,
    internal val headerBytes: ByteArray,
    internal var headerPos: Int = 0,
    internal var eof: Boolean = false,
) {
    public companion object {
        /** Creates a new [GzEncoder] from header bytes, source, and compression level. */
        public fun <R : BufferedSource> create(header: ByteArray, r: R, lvl: Compression): GzEncoder<R> {
            val crc = CrcReader.new(r)
            return GzEncoder(DeflateEncoder(crc, lvl), header)
        }
    }

    /** Creates a new encoder with the given [level] from source [r]. */
    public constructor(r: R, level: Compression) : this(
        DeflateEncoder(CrcReader.new(r), level),
        GzBuilder.new().intoHeader(level),
    )

    /** Acquires a reference to the underlying reader. */
    public fun getRef(): R = inner.getRef().getRef()

    /** Acquires a mutable reference to the underlying reader. */
    public fun getMut(): R = inner.getMut().getMut()

    /** Consumes this encoder, returning the underlying reader. */
    public fun intoInner(): R = inner.intoInner().intoInner()

    /**
     * Reads compressed gzip data into [dst].
     *
     * First emits the gzip header (if not already emitted), then DEFLATE-compressed
     * data from the underlying source, and finally the CRC-32 trailer.
     *
     * Returns the number of bytes written to [dst], or 0 at end of stream.
     */
    public fun read(dst: ByteArray): Int {
        var amt = 0
        var into = dst
        if (eof) {
            return readFooter(dst)
        } else if (headerPos < headerBytes.size) {
            val posArr = intArrayOf(headerPos)
            amt += copy(into, headerBytes, posArr)
            headerPos = posArr[0]
            if (amt >= into.size) return amt
            into = into.copyOfRange(amt, into.size)
        }
        val n = inner.read(into)
        return if (n == 0) {
            eof = true
            headerPos = 0
            val footer = readFooter(dst.copyOfRange(amt, dst.size))
            amt + footer
        } else {
            amt + n
        }
    }

    private fun readFooter(into: ByteArray): Int {
        if (headerPos == CRC_BYTES_LEN) return 0
        val crc = inner.getRef().crc()
        val sum = crc.sum()
        val amount = crc.amount()
        val arr =
            byteArrayOf(
                (sum and 0xFFu).toByte(),
                ((sum shr 8) and 0xFFu).toByte(),
                ((sum shr 16) and 0xFFu).toByte(),
                ((sum shr 24) and 0xFFu).toByte(),
                (amount and 0xFFu).toByte(),
                ((amount shr 8) and 0xFFu).toByte(),
                ((amount shr 16) and 0xFFu).toByte(),
                ((amount shr 24) and 0xFFu).toByte(),
            )
        val posArr = intArrayOf(headerPos)
        val result = copy(into, arr, posArr)
        headerPos = posArr[0]
        return result
    }
}

/**
 * A decoder for a single member of a gzip file.
 *
 * After reading a single member of the gzip data this reader will return
 * zero bytes even if there are more bytes available in the underlying reader.
 * Use [intoInner] after a zero-byte read to recover the underlying reader.
 */
@HiddenFromObjC
public class GzDecoder<R : BufferedSource> internal constructor(
    internal var state: GzState,
    internal val reader: CrcReader<BufReader<DeflateDecoder<R>>>,
    internal var multi: Boolean,
) {
    public companion object {
        /** Creates a new [GzDecoder] from a buffered source. */
        public fun <R : BufferedSource> new(r: R): GzDecoder<R> {
            val headerParser = newParser()
            val state =
                try {
                    val bufData = r.fillBuffer()
                    if (bufData.isNotEmpty()) {
                        val src = ByteArrayBufSource(bufData)
                        headerParser.parse(src)
                        val consumed = bufData.size - src.remaining()
                        r.consume(consumed)
                    }
                    if (headerParser.header() != null) {
                        GzState.Body(headerParser.header()!!)
                    } else {
                        GzState.Header(headerParser)
                    }
                } catch (_: Exception) {
                    GzState.Header(headerParser)
                }
            val decoder = DeflateDecoder<R>(r)
            val bufDecoder: BufReader<DeflateDecoder<R>> = BufReader.new(decoder)
            val crcReader = CrcReader.new(bufDecoder)
            return GzDecoder(state, crcReader, false)
        }
    }

    /** Returns the header associated with this stream, if it was valid. */
    public fun header(): GzHeader? =
        when (state) {
            is GzState.Body -> (state as GzState.Body).header
            is GzState.Finished -> (state as GzState.Finished).header
            is GzState.End -> (state as GzState.End).header
            else -> null
        }

    /** Acquires a reference to the underlying reader. */
    public fun getRef(): R = reader.getRef().getRef().getRef()

    /** Acquires a mutable reference to the underlying stream. */
    public fun getMut(): R = reader.getMut().getMut().getMut()

    /** Consumes this decoder, returning the underlying reader. */
    public fun intoInner(): R = reader.intoInner().intoInner().intoInner()

    /**
     * Reads decompressed gzip data into [dst].
     *
     * Returns the number of bytes written to [dst], or 0 when the gzip
     * member is fully consumed.
     */
    public fun read(dst: ByteArray): Int {
        while (true) {
            when (state) {
                is GzState.Header -> {
                    val parser = (state as GzState.Header).parser
                    val inner = getMut()
                    val bufData = inner.fillBuffer()
                    val src = ByteArrayBufSource(bufData)
                    parser.parse(src)
                    val consumed = bufData.size - src.remaining()
                    inner.consume(consumed)
                    state = GzState.Body(parser.header()!!)
                }
                is GzState.Body -> {
                    if (dst.isEmpty()) return 0
                    val n = reader.read(dst)
                    if (n == 0) {
                        state = GzState.Finished((state as GzState.Body).header, 0, ByteArray(CRC_BYTES_LEN))
                    } else {
                        return n
                    }
                }
                is GzState.Finished -> {
                    val s = state as GzState.Finished
                    if (s.pos < CRC_BYTES_LEN) {
                        val inner = getMut()
                        val remaining = CRC_BYTES_LEN - s.pos
                        val bufData = inner.fillBuffer()
                        val avail = minOf(remaining, bufData.size)
                        bufData.copyInto(s.buf, s.pos, 0, avail)
                        s.pos += avail
                        inner.consume(avail)
                    }
                    if (s.pos >= CRC_BYTES_LEN) {
                        val (crc, amt) = finish(s.buf)
                        if (crc != reader.crc().sum() || amt != reader.crc().amount()) {
                            state = GzState.End(s.header)
                            throw IllegalStateException("corrupt gzip stream: CRC mismatch")
                        } else if (multi) {
                            val inner = getMut()
                            val avail = inner.fillBuffer()
                            if (avail.isEmpty()) {
                                state = GzState.End(s.header)
                            } else {
                                reader.reset()
                                reader.getMut().getMut().resetData()
                                state = GzState.Header(newParser())
                            }
                        } else {
                            state = GzState.End(s.header)
                        }
                    }
                }
                is GzState.Err -> {
                    val err = (state as GzState.Err).error
                    state = GzState.End(null)
                    throw err
                }
                is GzState.End -> return 0
            }
        }
    }
}

/**
 * A gzip streaming decoder that decodes a gzip file that may have multiple members.
 */
@HiddenFromObjC
public class MultiGzDecoder<R : BufferedSource>(
    internal val inner: GzDecoder<R>,
) {
    public companion object {
        /** Creates a new [MultiGzDecoder] from a buffered source. */
        public fun <R : BufferedSource> new(r: R): MultiGzDecoder<R> {
            val decoder = GzDecoder.new(r)
            decoder.multi = true
            return MultiGzDecoder(decoder)
        }
    }

    /** Returns the current header associated with this stream, if valid. */
    public fun header(): GzHeader? = inner.header()

    /** Acquires a reference to the underlying reader. */
    public fun getRef(): R = inner.getRef()

    /** Acquires a mutable reference to the underlying stream. */
    public fun getMut(): R = inner.getMut()

    /** Consumes this decoder, returning the underlying reader. */
    public fun intoInner(): R = inner.intoInner()

    /** Reads decompressed data into [dst]. */
    public fun read(dst: ByteArray): Int = inner.read(dst)
}

/** Create a [GzEncoder] from header bytes, source, and compression level. */
public fun <R : BufferedSource> gzEncoder(header: ByteArray, r: R, lvl: Compression): GzEncoder<R> =
    GzEncoder.create(header, r, lvl)

/** Create a [GzDecoder] from a buffered source. */
public fun <R : BufferedSource> gzDecoder(r: R): GzDecoder<R> =
    GzDecoder.new(r)

/** Create a [MultiGzDecoder] from a buffered source. */
public fun <R : BufferedSource> multiGzDecoder(r: R): MultiGzDecoder<R> =
    MultiGzDecoder.new(r)

/** Allow [GzBuilder.bufRead] to construct a [GzEncoder]. */
public fun <R : BufferedSource> GzBuilder.bufRead(r: R, lvl: Compression): GzEncoder<R> =
    gzEncoder(intoHeader(lvl), r, lvl)

/**
 * A minimal [BufferedSource] backed by a byte array, used for feeding
 * header bytes to [GzHeaderParser].
 */
internal class ByteArrayBufSource(
    private val data: ByteArray,
) : BufferedSource {
    private var pos: Int = 0

    override fun fillBuffer(): ByteArray {
        if (pos >= data.size) return ByteArray(0)
        return data.copyOfRange(pos, data.size)
    }

    override fun consume(amount: Int) {
        pos += amount
    }

    override fun read(sink: ByteArray, offset: Int, length: Int): Int {
        val available = data.size - pos
        if (available <= 0) return -1
        val toRead = minOf(length, available)
        data.copyInto(sink, destinationOffset = offset, startIndex = pos, endIndex = pos + toRead)
        pos += toRead
        return toRead
    }

    fun remaining(): Int = data.size - pos
}
