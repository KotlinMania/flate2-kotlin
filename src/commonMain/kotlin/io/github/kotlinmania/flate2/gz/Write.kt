@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
// port-lint: source gz/write.rs
package io.github.kotlinmania.flate2.gz

import io.github.kotlinmania.flate2.Compress
import io.github.kotlinmania.flate2.CompressOps
import io.github.kotlinmania.flate2.Compression
import io.github.kotlinmania.flate2.Crc
import io.github.kotlinmania.flate2.CrcWriter
import io.github.kotlinmania.flate2.CrcWriterSink
import io.github.kotlinmania.flate2.Decompress
import io.github.kotlinmania.flate2.DecompressOps
import io.github.kotlinmania.flate2.OutputSink
import io.github.kotlinmania.flate2.Status
import io.github.kotlinmania.flate2.Writer
import io.github.kotlinmania.flate2.gz.GzHeaderParser.Companion.new as newParser
import kotlin.native.HiddenFromObjC

private const val CRC_BYTES_LEN: Int = 8

/**
 * A gzip streaming encoder that writes compressed data to an [OutputSink].
 *
 * Call [finish] to complete the compressed stream and retrieve the
 * underlying writer.
 */
@HiddenFromObjC
public class GzWriteEncoder<W : OutputSink> internal constructor(
    internal val inner: Writer<W, CompressOps>,
    internal val crc: Crc,
    internal var crcBytesWritten: Int,
    internal var header: ByteArray,
) {
    public companion object {
        /** Creates a new encoder which writes compressed data to [w] at the given [level]. */
        public fun <W : OutputSink> new(w: W, level: Compression): GzWriteEncoder<W> =
            GzBuilder.new().write(w, level)

        /** Creates an encoder from a [GzBuilder] header and compression level. */
        public fun <W : OutputSink> create(header: ByteArray, w: W, lvl: Compression): GzWriteEncoder<W> =
            GzWriteEncoder(
                inner = Writer.new(w, CompressOps(Compress.new(lvl, zlibHeader = false))),
                crc = Crc.new(),
                crcBytesWritten = 0,
                header = header,
            )
    }

    /** Acquires a reference to the underlying writer. */
    public fun getRef(): W = inner.getRef()

    /** Acquires a mutable reference to the underlying writer. */
    public fun getMut(): W = inner.getMut()

    /**
     * Attempt to finish this output stream, writing out final chunks of data.
     *
     * After this call, no more data should be written.
     */
    public fun tryFinish() {
        writeHeader()
        inner.finish()
        while (crcBytesWritten < CRC_BYTES_LEN) {
            val sum = crc.sum()
            val amt = crc.amount()
            val buf = byteArrayOf(
                (sum and 0xFFu).toByte(),
                ((sum shr 8) and 0xFFu).toByte(),
                ((sum shr 16) and 0xFFu).toByte(),
                ((sum shr 24) and 0xFFu).toByte(),
                (amt and 0xFFu).toByte(),
                ((amt shr 8) and 0xFFu).toByte(),
                ((amt shr 16) and 0xFFu).toByte(),
                ((amt shr 24) and 0xFFu).toByte(),
            )
            val n = inner.getMut().write(buf, crcBytesWritten, buf.size - crcBytesWritten)
            crcBytesWritten += n
        }
    }

    /**
     * Finish encoding this stream, returning the underlying writer.
     */
    public fun finish(): W {
        tryFinish()
        return inner.takeInner()
    }

    private fun writeHeader() {
        while (header.isNotEmpty()) {
            val n = inner.getMut().write(header)
            header = header.copyOfRange(minOf(n, header.size), header.size)
            if (n == 0) break
        }
    }

    /** Write [data] from [offset] for [length] bytes to this encoder. */
    public fun write(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): Int {
        check(crcBytesWritten == 0) { "write after finish" }
        writeHeader()
        val slice = if (offset == 0 && length == data.size) data else data.copyOfRange(offset, offset + length)
        val n = inner.write(slice)
        crc.update(slice.copyOfRange(0, n))
        return n
    }

    /** Flush the encoder. */
    public fun flush() {
        check(crcBytesWritten == 0) { "flush after finish" }
        writeHeader()
        inner.flush()
    }
}

/**
 * A decoder for a single member of a gzip file that writes uncompressed data
 * to an [OutputSink].
 *
 * After decoding a single member, subsequent writes will return zero,
 * allowing the caller to handle any data following the gzip member.
 */
@HiddenFromObjC
public class GzWriteDecoder<W : OutputSink> internal constructor(
    internal val inner: Writer<CrcWriterSink<W>, DecompressOps>,
    internal val crcBytes: MutableList<Byte>,
    internal val headerParser: GzHeaderParser,
) {
    public companion object {
        /** Creates a new decoder which writes uncompressed data to [w]. */
        public fun <W : OutputSink> new(w: W): GzWriteDecoder<W> {
            val crcWriter = CrcWriter.new(w)
            return GzWriteDecoder(
                inner = Writer.new(CrcWriterSink(crcWriter), DecompressOps(Decompress.new(zlibHeader = false))),
                crcBytes = mutableListOf(),
                headerParser = newParser(),
            )
        }
    }

    /** Returns the header associated with this stream. */
    public fun header(): GzHeader? = headerParser.header()

    /** Acquires a reference to the underlying writer. */
    public fun getRef(): W = inner.getRef().inner.getRef()

    /** Acquires a mutable reference to the underlying writer. */
    public fun getMut(): W = inner.getMut().inner.getMut()

    /**
     * Attempt to finish this output stream.
     */
    public fun tryFinish() {
        finishAndCheckCrc()
    }

    /**
     * Finish decoding, returning the underlying writer.
     */
    public fun finish(): W {
        finishAndCheckCrc()
        return inner.takeInner().inner.intoInner()
    }

    private fun finishAndCheckCrc() {
        inner.finish()
        if (crcBytes.size != CRC_BYTES_LEN) {
            throw IllegalStateException("corrupt gzip stream does not have a matching checksum")
        }
        val crc = ((crcBytes[0].toLong() and 0xFF) or
            ((crcBytes[1].toLong() and 0xFF) shl 8) or
            ((crcBytes[2].toLong() and 0xFF) shl 16) or
            ((crcBytes[3].toLong() and 0xFF) shl 24)).toUInt()
        val amt = ((crcBytes[4].toLong() and 0xFF) or
            ((crcBytes[5].toLong() and 0xFF) shl 8) or
            ((crcBytes[6].toLong() and 0xFF) shl 16) or
            ((crcBytes[7].toLong() and 0xFF) shl 24)).toUInt()
        if (crc != inner.getRef().inner.crc().sum()) {
            throw IllegalStateException("corrupt gzip stream does not have a matching checksum")
        }
        if (amt != inner.getRef().inner.crc().amount()) {
            throw IllegalStateException("corrupt gzip stream does not have a matching checksum")
        }
    }

    /** Write [data] from [offset] for [length] bytes to this decoder. */
    public fun write(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): Int {
        val buflen = length
        if (header() == null) {
            val slice = if (offset == 0 && length == data.size) data else data.copyOfRange(offset, offset + length)
            val bufSrc = ByteArrayBufSource(slice)
            return try {
                headerParser.parse(bufSrc)
                val consumed = length - bufSrc.remaining()
                if (consumed < length) {
                    val rest = data.copyOfRange(offset + consumed, offset + length)
                    val (n, status) = inner.writeWithStatus(rest)
                    if (status == Status.StreamEnd && n < rest.size && crcBytes.size < CRC_BYTES_LEN) {
                        val remaining = rest.size - n
                        val crcCount = minOf(remaining, CRC_BYTES_LEN - crcBytes.size)
                        for (i in 0 until crcCount) {
                            crcBytes.add(rest[n + i])
                        }
                        return consumed + n + crcCount
                    }
                    consumed + n
                } else {
                    consumed
                }
            } catch (_: Exception) {
                buflen
            }
        }

        val slice = if (offset == 0 && length == data.size) data else data.copyOfRange(offset, offset + length)
        val (n, status) = inner.writeWithStatus(slice)
        if (status == Status.StreamEnd && n < slice.size && crcBytes.size < CRC_BYTES_LEN) {
            val remaining = slice.size - n
            val crcCount = minOf(remaining, CRC_BYTES_LEN - crcBytes.size)
            for (i in 0 until crcCount) {
                crcBytes.add(slice[n + i])
            }
            return n + crcCount
        }
        return n
    }

    /** Flush the decoder. */
    public fun flush() {
        inner.flush()
    }
}

/**
 * A gzip streaming decoder that decodes a gzip file with multiple members,
 * writing uncompressed data to an [OutputSink].
 */
@HiddenFromObjC
public class MultiGzWriteDecoder<W : OutputSink> internal constructor(
    internal var inner: GzWriteDecoder<W>,
) {
    public companion object {
        /** Creates a new multi-member decoder. */
        public fun <W : OutputSink> new(w: W): MultiGzWriteDecoder<W> =
            MultiGzWriteDecoder(GzWriteDecoder.new(w))
    }

    /** Returns the header associated with the current member. */
    public fun header(): GzHeader? = inner.header()

    /** Acquires a reference to the underlying writer. */
    public fun getRef(): W = inner.getRef()

    /** Acquires a mutable reference to the underlying writer. */
    public fun getMut(): W = inner.getMut()

    /** Attempt to finish this output stream. */
    public fun tryFinish() {
        inner.tryFinish()
    }

    /** Finish decoding, returning the underlying writer. */
    public fun finish(): W = inner.finish()

    /** Write [data] to this decoder. */
    public fun write(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): Int {
        if (data.isEmpty()) return 0
        val n = inner.write(data, offset, length)
        if (n == 0) {
            inner.tryFinish()
            val w = inner.inner.takeInner().inner.intoInner()
            inner = GzWriteDecoder.new(w)
            return inner.write(data, offset, length)
        }
        return n
    }

    /** Flush the decoder. */
    public fun flush() {
        inner.flush()
    }
}

/** Create a [GzWriteEncoder] from header bytes, sink, and compression level. */
public fun <W : OutputSink> gzWriteEncoder(header: ByteArray, w: W, lvl: Compression): GzWriteEncoder<W> =
    GzWriteEncoder.create(header, w, lvl)

/** Allow [GzBuilder.write] to construct a [GzWriteEncoder]. */
public fun <W : OutputSink> GzBuilder.write(w: W, lvl: Compression): GzWriteEncoder<W> =
    GzWriteEncoder.create(intoHeader(lvl), w, lvl)

