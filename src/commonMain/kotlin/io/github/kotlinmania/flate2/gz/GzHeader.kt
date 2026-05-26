// port-lint: source gz/mod.rs
package io.github.kotlinmania.flate2.gz

import io.github.kotlinmania.flate2.BufferedSource
import io.github.kotlinmania.flate2.Compression
import io.github.kotlinmania.flate2.Crc

/** FHCRC flag: the header contains a CRC-16 of the header itself. */
public const val FHCRC: Int = 1 shl 1

/** FEXTRA flag: the header contains extra fields. */
public const val FEXTRA: Int = 1 shl 2

/** FNAME flag: the header contains a filename. */
public const val FNAME: Int = 1 shl 3

/** FCOMMENT flag: the header contains a comment. */
public const val FCOMMENT: Int = 1 shl 4

/** FRESERVED bits: bits 5, 6, 7 are reserved and must be zero. */
public const val FRESERVED: Int = (1 shl 5) or (1 shl 6) or (1 shl 7)

/** The maximum length of the header filename and comment fields. */
internal const val MAX_HEADER_BUF: Int = 65535

/**
 * A structure representing the header of a gzip stream.
 *
 * The header can contain metadata about the file that was compressed,
 * if present.
 */
public data class GzHeader(
    internal val extra: ByteArray? = null,
    internal val filename: ByteArray? = null,
    internal val comment: ByteArray? = null,
    internal val operatingSystem: UByte = 255u,
    internal val mtime: UInt = 0u,
) {

    /** Returns the filename field of this gzip stream's header, if present. */
    public fun filename(): ByteArray? = filename?.copyOf()

    /** Returns the extra field of this gzip stream's header, if present. */
    public fun extra(): ByteArray? = extra?.copyOf()

    /** Returns the comment field of this gzip stream's header, if present. */
    public fun comment(): ByteArray? = comment?.copyOf()

    /**
     * Returns the operating system field of this gzip stream's header.
     *
     * There are predefined values for various operating systems.
     * 255 means that the value is unknown.
     */
    public fun operatingSystem(): UByte = operatingSystem

    /**
     * Gives the most recent modification time of the original file being compressed.
     *
     * The time is in Unix format (seconds since 00:00:00 GMT, Jan. 1, 1970).
     * A value of 0 means no time stamp is available.
     */
    public fun mtime(): UInt = mtime

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GzHeader) return false
        if (operatingSystem != other.operatingSystem) return false
        if (mtime != other.mtime) return false
        if (extra != null) {
            if (other.extra == null) return false
            if (!extra.contentEquals(other.extra)) return false
        } else if (other.extra != null) return false
        if (filename != null) {
            if (other.filename == null) return false
            if (!filename.contentEquals(other.filename)) return false
        } else if (other.filename != null) return false
        if (comment != null) {
            if (other.comment == null) return false
            if (!comment.contentEquals(other.comment)) return false
        } else if (other.comment != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = operatingSystem.hashCode()
        result = 31 * result + mtime.hashCode()
        result = 31 * result + (extra?.contentHashCode() ?: 0)
        result = 31 * result + (filename?.contentHashCode() ?: 0)
        result = 31 * result + (comment?.contentHashCode() ?: 0)
        return result
    }

    override fun toString(): String = buildString {
        append("GzHeader(mtime=")
        append(mtime)
        append(", operatingSystem=")
        append(operatingSystem)
        append(", filename=")
        append(filename?.decodeToString() ?: "null")
        append(", comment=")
        append(comment?.decodeToString() ?: "null")
        append(", extra=")
        append(if (extra != null) extra.size.toString() + " bytes" else "null")
        append(")")
    }
}

/**
 * The state machine for parsing a gzip header.
 *
 * The parser reads bytes incrementally; each call to [parse] advances
 * the state as far as possible with the bytes available in the source.
 */
public sealed class GzHeaderState {
    /** Initial state: accumulating the fixed 10-byte gzip header. */
    public data class Start(
        val count: Int = 0,
        val buffer: ByteArray = ByteArray(10),
    ) : GzHeaderState()

    /** Reading the 2-byte XLEN field (only if FEXTRA is set). */
    public data class Xlen(
        val crc: Crc? = null,
        val count: Int = 0,
        val buffer: ByteArray = ByteArray(2),
    ) : GzHeaderState()

    /** Reading the extra field bytes. */
    public data class Extra(
        val crc: Crc? = null,
        val count: Int = 0,
    ) : GzHeaderState()

    /** Reading the null-terminated filename. */
    public data class Filename(
        val crc: Crc? = null,
    ) : GzHeaderState()

    /** Reading the null-terminated comment. */
    public data class Comment(
        val crc: Crc? = null,
    ) : GzHeaderState()

    /** Reading the 2-byte header CRC (only if FHCRC is set). */
    public data class Crc16(
        val crc: Crc? = null,
        val count: Int = 0,
        val buffer: ByteArray = ByteArray(2),
    ) : GzHeaderState()

    /** Header fully parsed. */
    public data object Complete : GzHeaderState()
}

/**
 * An incremental parser for gzip headers.
 *
 * Create a new parser with [GzHeaderParser.new], then call [parse]
 * repeatedly with a [BufferedSource] until it returns without error.
 * After successful parsing, [header] returns the parsed [GzHeader].
 */
public class GzHeaderParser private constructor(
    private var state: GzHeaderState,
    private var flags: Int,
    private var header: GzHeader,
) {

    public companion object {
        /** Create a new header parser in its initial state. */
        public fun new(): GzHeaderParser = GzHeaderParser(
            state = GzHeaderState.Start(),
            flags = 0,
            header = GzHeader(),
        )
    }

    /**
     * Parse bytes from [source] to advance the header state machine.
     *
     * Returns normally when parsing is complete or when more bytes are
     * needed (the source returns zero bytes). Throws on invalid data.
     */
    public fun parse(source: BufferedSource) {
        var currentState = state
        parsing@ while (true) {
            when (val s = currentState) {
                is GzHeaderState.Start -> {
                    val buf = s.buffer.copyOf()
                    var count = s.count
                    while (count < buf.size) {
                        val read = readInto(source, buf, count)
                        if (read == 0) break
                        count += read
                    }
                    if (count < buf.size) {
                        currentState = GzHeaderState.Start(count, buf)
                        break@parsing
                    }
                    check(buf[0] == GZIP_ID1 && buf[1] == GZIP_ID2) { "invalid gzip header" }
                    check(buf[2].toInt() == 8) { "invalid gzip header" }
                    flags = buf[3].toInt() and 0xFF
                    check(flags and FRESERVED == 0) { "invalid gzip header" }
                    val mtimeVal = ((buf[4].toLong() and 0xFF)) or
                        ((buf[5].toLong() and 0xFF) shl 8) or
                        ((buf[6].toLong() and 0xFF) shl 16) or
                        ((buf[7].toLong() and 0xFF) shl 24)
                    val os = buf[9].toUByte()
                    val crc = if (flags and FHCRC != 0) {
                        Crc.new().also { it.update(buf) }
                    } else null
                    header = GzHeader(
                        extra = header.extra,
                        filename = header.filename,
                        comment = header.comment,
                        operatingSystem = os,
                        mtime = mtimeVal.toUInt(),
                    )
                    currentState = GzHeaderState.Xlen(crc = crc)
                }
                is GzHeaderState.Xlen -> {
                    if (flags and FEXTRA != 0) {
                        val buf = s.buffer.copyOf()
                        var count = s.count
                        while (count < buf.size) {
                            val read = readInto(source, buf, count)
                            if (read == 0) break
                            count += read
                        }
                        if (count < buf.size) {
                            currentState = GzHeaderState.Xlen(s.crc, count, buf)
                            break@parsing
                        }
                        s.crc?.update(buf)
                        val xlen = parseLeU16(buf).toInt()
                        header = GzHeader(
                            extra = ByteArray(xlen),
                            filename = header.filename,
                            comment = header.comment,
                            operatingSystem = header.operatingSystem,
                            mtime = header.mtime,
                        )
                        currentState = GzHeaderState.Extra(crc = s.crc, count = 0)
                    } else {
                        currentState = GzHeaderState.Filename(crc = s.crc)
                    }
                }
                is GzHeaderState.Extra -> {
                    val extra = header.extra!!
                    var count = s.count
                    while (count < extra.size) {
                        val read = readInto(source, extra, count)
                        if (read == 0) break
                        count += read
                    }
                    if (count < extra.size) {
                        currentState = GzHeaderState.Extra(crc = s.crc, count)
                        break@parsing
                    }
                    s.crc?.update(extra)
                    currentState = GzHeaderState.Filename(crc = s.crc)
                }
                is GzHeaderState.Filename -> {
                    if (flags and FNAME != 0) {
                        val filenameBuf = mutableListOf<Byte>()
                        readToNul(source, filenameBuf)
                        header = GzHeader(
                            extra = header.extra,
                            filename = filenameBuf.toByteArray(),
                            comment = header.comment,
                            operatingSystem = header.operatingSystem,
                            mtime = header.mtime,
                        )
                        s.crc?.update(header.filename!!)
                        s.crc?.update(byteArrayOf(0))
                    }
                    currentState = GzHeaderState.Comment(crc = s.crc)
                }
                is GzHeaderState.Comment -> {
                    if (flags and FCOMMENT != 0) {
                        val commentBuf = mutableListOf<Byte>()
                        readToNul(source, commentBuf)
                        header = GzHeader(
                            extra = header.extra,
                            filename = header.filename,
                            comment = commentBuf.toByteArray(),
                            operatingSystem = header.operatingSystem,
                            mtime = header.mtime,
                        )
                        s.crc?.update(header.comment!!)
                        s.crc?.update(byteArrayOf(0))
                    }
                    currentState = GzHeaderState.Crc16(crc = s.crc)
                }
                is GzHeaderState.Crc16 -> {
                    if (s.crc != null) {
                        val buf = s.buffer.copyOf()
                        var count = s.count
                        while (count < buf.size) {
                            val read = readInto(source, buf, count)
                            if (read == 0) break
                            count += read
                        }
                        if (count < buf.size) {
                            currentState = GzHeaderState.Crc16(s.crc, count, buf)
                            break@parsing
                        }
                        val storedCrc = parseLeU16(buf).toInt()
                        val calculatedCrc = s.crc.sum().toInt() and 0xFFFF
                        check(storedCrc == calculatedCrc) {
                            "corrupt gzip stream does not have a matching checksum"
                        }
                    }
                    currentState = GzHeaderState.Complete
                }
                is GzHeaderState.Complete -> break@parsing
            }
        }
        state = currentState
    }

    /** Returns the parsed header, or null if parsing is not yet complete. */
    public fun header(): GzHeader? = when (state) {
        is GzHeaderState.Complete -> header
        else -> null
    }
}

/**
 * A builder for constructing a gzip encoder with custom header fields.
 *
 * Use [GzBuilder.new] to create a default builder, then configure
 * header fields and call [write] to create a writer-side encoder or
 * [bufRead] to create a read-side encoder.
 */
public data class GzBuilder(
    private val extra: ByteArray? = null,
    private val filename: ByteArray? = null,
    private val comment: ByteArray? = null,
    private val operatingSystem: UByte? = null,
    private val mtime: UInt = 0u,
) {

    public companion object {
        /** Create a new blank builder with no header by default. */
        public fun new(): GzBuilder = GzBuilder()
    }

    /** Configure the mtime field in the gzip header. */
    public fun mtime(mtime: UInt): GzBuilder = copy(mtime = mtime)

    /** Configure the [os] field in the gzip header. */
    public fun operatingSystem(os: UByte): GzBuilder = copy(operatingSystem = os)

    /** Configure the extra field in the gzip header. */
    public fun extra(extra: ByteArray): GzBuilder = copy(extra = extra)

    /** Configure the filename field in the gzip header. The byte array must not contain a zero. */
    public fun filename(filename: ByteArray): GzBuilder {
        require(0 !in filename) { "filename must not contain zero bytes" }
        return copy(filename = filename)
    }

    /** Configure the comment field in the gzip header. The byte array must not contain a zero. */
    public fun comment(comment: ByteArray): GzBuilder {
        require(0 !in comment) { "comment must not contain zero bytes" }
        return copy(comment = comment)
    }

    /**
     * Build the gzip header bytes from this builder and compression level.
     * Used internally by the encoder constructors.
     */
    internal fun intoHeader(level: Compression): ByteArray {
        var flg = 0
        val header = MutableList(10) { 0.toByte() }
        extra?.let {
            flg = flg or FEXTRA
            header.add((it.size and 0xFFFF).toByte())
            header.add(((it.size shr 8) and 0xFF).toByte())
            header.addAll(it.toList())
        }
        filename?.let {
            flg = flg or FNAME
            header.addAll(it.toList())
            header.add(0)
        }
        comment?.let {
            flg = flg or FCOMMENT
            header.addAll(it.toList())
            header.add(0)
        }
        header[0] = GZIP_ID1
        header[1] = GZIP_ID2
        header[2] = 8
        header[3] = flg.toByte()
        header[4] = (mtime and 0xFFu).toByte()
        header[5] = ((mtime shr 8) and 0xFFu).toByte()
        header[6] = ((mtime shr 16) and 0xFFu).toByte()
        header[7] = ((mtime shr 24) and 0xFFu).toByte()
        header[8] = if (level.level() >= Compression.best().level()) {
            2.toByte()
        } else if (level.level() <= Compression.fast().level()) {
            4.toByte()
        } else {
            0.toByte()
        }
        header[9] = (operatingSystem ?: 255u).toByte()
        return header.toByteArray()
    }
}

private const val GZIP_ID1: Byte = 0x1F.toByte()
private const val GZIP_ID2: Byte = 0x8B.toByte()

/**
 * Read bytes from [source] into [buffer] starting at [offset].
 * Returns 0 if no bytes are currently available (would block),
 * throws on unexpected EOF.
 */
internal fun readInto(source: BufferedSource, buffer: ByteArray, offset: Int): Int {
    require(offset < buffer.size) { "buffer overflow" }
    val available = source.fillBuffer()
    if (available.isEmpty()) return 0
    val toRead = minOf(available.size, buffer.size - offset)
    available.copyInto(buffer, destinationOffset = offset, startIndex = 0, endIndex = toRead)
    source.consume(toRead)
    return toRead
}

/**
 * Read from [source] until a null byte is encountered, collecting non-null
 * bytes into [buffer]. Throws if the field exceeds [MAX_HEADER_BUF] bytes
 * or if EOF is reached before the null terminator.
 */
internal fun readToNul(source: BufferedSource, buffer: MutableList<Byte>) {
    while (true) {
        val available = source.fillBuffer()
        if (available.isEmpty()) throw IllegalStateException("unexpected end of gzip header")
        for (b in available) {
            source.consume(1)
            if (b == 0.toByte()) return
            check(buffer.size < MAX_HEADER_BUF) { "gzip header field too long" }
            buffer.add(b)
        }
    }
}

/** Parse a little-endian unsigned 16-bit integer from two bytes. */
internal fun parseLeU16(buf: ByteArray): UShort {
    return ((buf[0].toLong() and 0xFF) or ((buf[1].toLong() and 0xFF) shl 8)).toUShort()
}