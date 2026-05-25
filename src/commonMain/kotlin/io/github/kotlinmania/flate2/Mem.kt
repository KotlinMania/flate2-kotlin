// port-lint: source mem.rs
package io.github.kotlinmania.flate2

/**
 * Raw in-memory compression stream for blocks of data.
 *
 * This type is the building block for the I/O streams in the rest of this
 * crate. It requires more management than the [InputSource]/[OutputSink] API
 * but is maximally flexible in terms of accepting input from any source and
 * being able to produce output to any memory location.
 *
 * It is recommended to use the I/O stream adapters over this type as they are
 * easier to use.
 */
public class Compress private constructor(
    private var level: Compression,
    private var format: StreamFormat,
    private var windowBits: Int,
) {
    private var dictionary: ByteArray? = null
    private var pendingInput: ByteArray = ByteArray(0)
    private var pendingOutput: ByteArray = ByteArray(0)
    private var finished: Boolean = false
    private var totalInput: ULong = 0u
    private var totalOutput: ULong = 0u

    /**
     * Returns the total number of input bytes which have been processed by
     * this compression object.
     */
    public fun totalIn(): ULong = totalInput

    /**
     * Returns the total number of output bytes which have been produced by
     * this compression object.
     */
    public fun totalOut(): ULong = totalOutput

    /**
     * Specifies the compression dictionary to use.
     *
     * Returns the Adler-32 checksum of the dictionary.
     */
    public fun setDictionary(dictionary: ByteArray): Result<UInt> {
        this.dictionary = dictionary.copyOf()
        return Result.success(adler32(dictionary))
    }

    /**
     * Quickly resets this compressor without having to reallocate anything.
     *
     * This is equivalent to dropping this object and then creating a new one.
     */
    public fun reset() {
        pendingInput = ByteArray(0)
        pendingOutput = ByteArray(0)
        finished = false
        totalInput = 0u
        totalOutput = 0u
    }

    /**
     * Dynamically updates the compression level.
     *
     * This can be used to switch between compression levels for different
     * kinds of data, or it can be used with a call to [reset] to reuse the
     * compressor.
     */
    public fun setLevel(level: Compression): Result<Unit> {
        this.level = level
        return Result.success(Unit)
    }

    /**
     * Compresses the input data into the output, consuming only as much input
     * as needed and writing as much output as possible.
     *
     * The flush option can be any of the available [FlushCompress] parameters.
     *
     * To learn how much data was consumed or how much output was produced, use
     * [totalIn] and [totalOut] before and after this is called.
     */
    public fun compress(
        input: ByteArray,
        output: ByteArray,
        flush: FlushCompress,
    ): Result<Status> {
        if (finished && pendingOutput.isEmpty()) {
            return Result.success(Status.StreamEnd)
        }

        if (input.isNotEmpty()) {
            pendingInput = pendingInput + input
            totalInput += input.size.toULong()
        }

        if (!finished && flush == FlushCompress.Finish) {
            val compressed = try {
                encodeDeflatePayload(pendingInput, format, level, dictionary)
            } catch (error: Throwable) {
                return compressFailed(error.message)
            }
            pendingOutput = pendingOutput + compressed
            pendingInput = ByteArray(0)
            finished = true
        }

        return Result.success(copyPendingOutput(output))
    }

    /**
     * Similar to [compress] but accepts a buffer that is already allocated by
     * the caller.
     */
    public fun compressUninit(
        input: ByteArray,
        output: ByteArray,
        flush: FlushCompress,
    ): Result<Status> = compress(input, output, flush)

    /**
     * Compresses the input data into the extra space of the output list,
     * consuming only as much input as needed and writing as much output as
     * possible.
     *
     * This function has the same semantics as [compress], except that the
     * length of [output] is managed by this function. This Kotlin port appends
     * produced bytes to [output].
     */
    public fun compressVec(
        input: ByteArray,
        output: MutableList<Byte>,
        flush: FlushCompress,
    ): Result<Status> {
        val buffer = ByteArray(DEFAULT_CHUNK_SIZE)
        var nextInput = input
        var status: Status
        do {
            val before = totalOut()
            status = compress(nextInput, buffer, flush).getOrElse { return Result.failure(it) }
            val produced = (totalOut() - before).toInt()
            appendProducedBytes(buffer, output, produced)
            nextInput = ByteArray(0)
        } while (status == Status.Ok && produced == buffer.size)
        return Result.success(status)
    }

    private fun copyPendingOutput(output: ByteArray): Status {
        if (output.isEmpty()) {
            return if (pendingOutput.isEmpty() && finished) Status.StreamEnd else Status.BufError
        }
        val written = minOf(output.size, pendingOutput.size)
        if (written > 0) {
            pendingOutput.copyInto(output, endIndex = written)
            pendingOutput = pendingOutput.copyOfRange(written, pendingOutput.size)
            totalOutput += written.toULong()
        }
        return when {
            pendingOutput.isNotEmpty() -> Status.Ok
            finished -> Status.StreamEnd
            else -> Status.BufError
        }
    }

    override fun toString(): String =
        "Compress(totalIn=$totalInput, totalOut=$totalOutput, format=$format, windowBits=$windowBits)"

    public companion object {
        /**
         * Creates a new object ready for compressing data that it is given.
         *
         * The [level] argument indicates what level of compression is going to
         * be performed, and [zlibHeader] indicates whether the output data
         * should have a zlib header.
         */
        public fun new(level: Compression, zlibHeader: Boolean): Compress =
            Compress(level, if (zlibHeader) StreamFormat.Zlib else StreamFormat.Raw, DEFAULT_WINDOW_BITS)

        /**
         * Creates a new object ready for compressing data that it is given.
         *
         * The [level] argument indicates what level of compression is going to
         * be performed, and [zlibHeader] indicates whether the output data
         * should have a zlib header. The [windowBits] parameter indicates the
         * base-2 logarithm of the sliding window size and must be between 9
         * and 15.
         */
        public fun newWithWindowBits(
            level: Compression,
            zlibHeader: Boolean,
            windowBits: UByte,
        ): Compress {
            require(windowBits.toInt() in 9..15) { "windowBits must be from 9 through 15" }
            return Compress(level, if (zlibHeader) StreamFormat.Zlib else StreamFormat.Raw, windowBits.toInt())
        }

        /**
         * Creates a new object ready for compressing data that it is given.
         *
         * The [level] argument indicates what level of compression is going to
         * be performed. The produced stream uses gzip headers.
         */
        public fun newGzip(level: Compression, windowBits: UByte): Compress {
            require(windowBits.toInt() in 9..15) { "windowBits must be from 9 through 15" }
            return Compress(level, StreamFormat.Gzip, windowBits.toInt())
        }
    }
}

/**
 * Raw in-memory decompression stream for blocks of data.
 *
 * This type is the building block for the I/O streams in the rest of this
 * crate. It requires more management than the [InputSource]/[OutputSink] API
 * but is maximally flexible in terms of accepting input from any source and
 * being able to produce output to any memory location.
 *
 * It is recommended to use the I/O stream adapters over this type as they are
 * easier to use.
 */
public class Decompress private constructor(
    private var format: StreamFormat,
    private var windowBits: Int,
) {
    private var dictionary: ByteArray? = null
    private var pendingInput: ByteArray = ByteArray(0)
    private var pendingOutput: ByteArray = ByteArray(0)
    private var finished: Boolean = false
    private var totalInput: ULong = 0u
    private var totalOutput: ULong = 0u

    /**
     * Returns the total number of input bytes which have been processed by
     * this decompression object.
     */
    public fun totalIn(): ULong = totalInput

    /**
     * Returns the total number of output bytes which have been produced by
     * this decompression object.
     */
    public fun totalOut(): ULong = totalOutput

    /**
     * Decompresses the input data into the output, consuming only as much input
     * as needed and writing as much output as possible.
     *
     * The flush option can be any of the available [FlushDecompress]
     * parameters.
     *
     * If the first call passes [FlushDecompress.Finish] it is assumed that the
     * input and output buffers are both sized large enough to decompress the
     * entire stream in a single call.
     */
    public fun decompress(
        input: ByteArray,
        output: ByteArray,
        flush: FlushDecompress,
    ): Result<Status> {
        if (finished && pendingOutput.isEmpty()) {
            return Result.success(Status.StreamEnd)
        }

        if (input.isNotEmpty()) {
            pendingInput = pendingInput + input
            totalInput += input.size.toULong()
        }

        if (!finished && flush == FlushDecompress.Finish) {
            val decompressed = try {
                decodeDeflatePayload(pendingInput, format, dictionary)
            } catch (error: NeedsDictionaryException) {
                return decompressNeedDict(error.adler)
            } catch (error: Throwable) {
                return decompressFailed(error.message)
            }
            pendingOutput = pendingOutput + decompressed
            pendingInput = ByteArray(0)
            finished = true
        }

        return Result.success(copyPendingOutput(output))
    }

    /**
     * Similar to [decompress] but accepts a buffer that is already allocated by
     * the caller.
     */
    public fun decompressUninit(
        input: ByteArray,
        output: ByteArray,
        flush: FlushDecompress,
    ): Result<Status> = decompress(input, output, flush)

    /**
     * Decompresses the input data into the extra space in [output].
     *
     * This function has the same semantics as [decompress], except that the
     * length of [output] is managed by this function. This Kotlin port appends
     * produced bytes to [output].
     */
    public fun decompressVec(
        input: ByteArray,
        output: MutableList<Byte>,
        flush: FlushDecompress,
    ): Result<Status> {
        val buffer = ByteArray(DEFAULT_CHUNK_SIZE)
        var nextInput = input
        var status: Status
        do {
            val before = totalOut()
            status = decompress(nextInput, buffer, flush).getOrElse { return Result.failure(it) }
            val produced = (totalOut() - before).toInt()
            appendProducedBytes(buffer, output, produced)
            nextInput = ByteArray(0)
        } while (status == Status.Ok && produced == buffer.size)
        return Result.success(status)
    }

    /** Specifies the decompression dictionary to use. */
    public fun setDictionary(dictionary: ByteArray): Result<UInt> {
        this.dictionary = dictionary.copyOf()
        return Result.success(adler32(dictionary))
    }

    /**
     * Performs the equivalent of replacing this decompression state with a
     * freshly allocated copy.
     *
     * The argument provided here indicates whether the reset state will attempt
     * to decode a zlib header first.
     */
    public fun reset(zlibHeader: Boolean) {
        format = if (zlibHeader) StreamFormat.Zlib else StreamFormat.Raw
        pendingInput = ByteArray(0)
        pendingOutput = ByteArray(0)
        finished = false
        totalInput = 0u
        totalOutput = 0u
    }

    private fun copyPendingOutput(output: ByteArray): Status {
        if (output.isEmpty()) {
            return if (pendingOutput.isEmpty() && finished) Status.StreamEnd else Status.BufError
        }
        val written = minOf(output.size, pendingOutput.size)
        if (written > 0) {
            pendingOutput.copyInto(output, endIndex = written)
            pendingOutput = pendingOutput.copyOfRange(written, pendingOutput.size)
            totalOutput += written.toULong()
        }
        return when {
            pendingOutput.isNotEmpty() -> Status.Ok
            finished -> Status.StreamEnd
            else -> Status.BufError
        }
    }

    override fun toString(): String =
        "Decompress(totalIn=$totalInput, totalOut=$totalOutput, format=$format, windowBits=$windowBits)"

    public companion object {
        /**
         * Creates a new object ready for decompressing data that it is given.
         *
         * The [zlibHeader] argument indicates whether the input data is
         * expected to have a zlib header.
         */
        public fun new(zlibHeader: Boolean): Decompress =
            Decompress(if (zlibHeader) StreamFormat.Zlib else StreamFormat.Raw, DEFAULT_WINDOW_BITS)

        /**
         * Creates a new object ready for decompressing data that it is given.
         *
         * The [zlibHeader] argument indicates whether the input data is
         * expected to have a zlib header. The [windowBits] parameter indicates
         * the base-2 logarithm of the sliding window size and must be between 9
         * and 15.
         */
        public fun newWithWindowBits(zlibHeader: Boolean, windowBits: UByte): Decompress {
            require(windowBits.toInt() in 9..15) { "windowBits must be from 9 through 15" }
            return Decompress(if (zlibHeader) StreamFormat.Zlib else StreamFormat.Raw, windowBits.toInt())
        }

        /**
         * Creates a new object ready for decompressing data that it is given.
         *
         * The produced object expects gzip headers for the compressed data.
         */
        public fun newGzip(windowBits: UByte): Decompress {
            require(windowBits.toInt() in 9..15) { "windowBits must be from 9 through 15" }
            return Decompress(StreamFormat.Gzip, windowBits.toInt())
        }
    }
}

/** Values which indicate the form of flushing to use when compressing in-memory data. */
public enum class FlushCompress {
    /** A typical parameter for passing to compression and decompression functions. */
    None,

    /** All pending output is flushed, but the output is not aligned to a byte boundary. */
    Partial,

    /** All pending output is flushed and aligned on a byte boundary. */
    Sync,

    /** All output is flushed and the compression state is reset. */
    Full,

    /** Pending input is processed and pending output is flushed. */
    Finish,
}

/** Values which indicate the form of flushing to use when decompressing in-memory data. */
public enum class FlushDecompress {
    /** A typical parameter for passing to compression and decompression functions. */
    None,

    /** All pending output is flushed and aligned on a byte boundary. */
    Sync,

    /** Pending input is processed and pending output is flushed. */
    Finish,
}

/** Possible status results of compressing data or successfully decompressing a block of data. */
public enum class Status {
    /** Indicates success. */
    Ok,

    /** Indicates that forward progress is not possible until input or output buffers change. */
    BufError,

    /** Indicates that all input has been consumed and all output bytes have been written. */
    StreamEnd,
}

/** Error returned when a decompression object finds invalid input bytes. */
public class DecompressError internal constructor(
    internal val inner: DecompressErrorInner,
) : Exception(decompressDisplayMessage(inner)) {
    /**
     * Indicates whether decompression failed because a dictionary is required.
     *
     * The resulting integer is the Adler-32 checksum of the required
     * dictionary.
     */
    public fun needsDictionary(): UInt? =
        when (inner) {
            is DecompressErrorInner.NeedsDictionary -> inner.adler
            is DecompressErrorInner.General -> null
        }

    /** Retrieve the implementation's message about why the operation failed, if one exists. */
    public fun message(): String? =
        when (inner) {
            is DecompressErrorInner.General -> inner.message
            is DecompressErrorInner.NeedsDictionary -> null
        }
}

/** Error returned when a compression object is used incorrectly or generates an error. */
public class CompressError internal constructor(
    internal val implementationMessage: String?,
) : Exception(compressDisplayMessage(implementationMessage)) {
    /** Retrieve the implementation's message about why the operation failed, if one exists. */
    public fun message(): String? = implementationMessage
}

internal sealed class DecompressErrorInner {
    data class General(val message: String?) : DecompressErrorInner()
    data class NeedsDictionary(val adler: UInt) : DecompressErrorInner()
}

internal fun <T> decompressFailed(message: String?): Result<T> =
    Result.failure(DecompressError(DecompressErrorInner.General(message)))

internal fun <T> decompressNeedDict(adler: UInt): Result<T> =
    Result.failure(DecompressError(DecompressErrorInner.NeedsDictionary(adler)))

internal fun <T> compressFailed(message: String?): Result<T> =
    Result.failure(CompressError(message))

private enum class StreamFormat {
    Raw,
    Zlib,
    Gzip,
}

private const val DEFAULT_WINDOW_BITS = 15
private const val DEFAULT_CHUNK_SIZE = 64 * 1024
private const val MAX_STORED_BLOCK_SIZE = 0xffff
private const val GZIP_FLAG_TEXT = 0x01
private const val GZIP_FLAG_HEADER_CRC = 0x02
private const val GZIP_FLAG_EXTRA = 0x04
private const val GZIP_FLAG_NAME = 0x08
private const val GZIP_FLAG_COMMENT = 0x10

private val lengthBases = intArrayOf(
    3, 4, 5, 6, 7, 8, 9, 10,
    11, 13, 15, 17, 19, 23, 27, 31,
    35, 43, 51, 59, 67, 83, 99, 115,
    131, 163, 195, 227, 258,
)

private val lengthExtraBits = intArrayOf(
    0, 0, 0, 0, 0, 0, 0, 0,
    1, 1, 1, 1, 2, 2, 2, 2,
    3, 3, 3, 3, 4, 4, 4, 4,
    5, 5, 5, 5, 0,
)

private val distanceBases = intArrayOf(
    1, 2, 3, 4, 5, 7, 9, 13,
    17, 25, 33, 49, 65, 97, 129, 193,
    257, 385, 513, 769, 1025, 1537, 2049, 3073,
    4097, 6145, 8193, 12289, 16385, 24577,
)

private val distanceExtraBits = intArrayOf(
    0, 0, 0, 0, 1, 1, 2, 2,
    3, 3, 4, 4, 5, 5, 6, 6,
    7, 7, 8, 8, 9, 9, 10, 10,
    11, 11, 12, 12, 13, 13,
)

private fun encodeDeflatePayload(
    input: ByteArray,
    format: StreamFormat,
    level: Compression,
    dictionary: ByteArray?,
): ByteArray {
    val raw = encodeStoredDeflate(input)
    return when (format) {
        StreamFormat.Raw -> raw
        StreamFormat.Zlib -> zlibHeader(level, dictionary) + raw + adler32Bytes(input)
        StreamFormat.Gzip -> gzipHeader() + raw + crc32Bytes(input) + littleEndianUInt(input.size.toUInt())
    }
}

private fun decodeDeflatePayload(
    input: ByteArray,
    format: StreamFormat,
    dictionary: ByteArray?,
): ByteArray =
    when (format) {
        StreamFormat.Raw -> inflateRaw(input, dictionary)
        StreamFormat.Zlib -> inflateZlib(input, dictionary)
        StreamFormat.Gzip -> inflateGzip(input, dictionary)
    }

private fun encodeStoredDeflate(input: ByteArray): ByteArray {
    val out = mutableListOf<Byte>()
    var offset = 0
    do {
        val remaining = input.size - offset
        val blockSize = minOf(remaining, MAX_STORED_BLOCK_SIZE)
        val isFinal = offset + blockSize >= input.size
        out += if (isFinal) 0x01.toByte() else 0x00.toByte()
        out += (blockSize and 0xff).toByte()
        out += ((blockSize ushr 8) and 0xff).toByte()
        val nlen = blockSize xor MAX_STORED_BLOCK_SIZE
        out += (nlen and 0xff).toByte()
        out += ((nlen ushr 8) and 0xff).toByte()
        for (index in 0 until blockSize) {
            out += input[offset + index]
        }
        offset += blockSize
    } while (offset < input.size)
    return out.toByteArray()
}

private fun inflateRaw(input: ByteArray, dictionary: ByteArray?): ByteArray {
    val reader = BitReader(input)
    val output = mutableListOf<Byte>()
    var finalBlock = false
    while (!finalBlock) {
        finalBlock = reader.readBits(1) == 1
        when (val blockType = reader.readBits(2)) {
            0 -> inflateStoredBlock(reader, output)
            1 -> inflateCompressedBlock(reader, output, fixedLiteralLengthTree(), fixedDistanceTree(), dictionary)
            2 -> {
                val trees = dynamicTrees(reader)
                inflateCompressedBlock(reader, output, trees.literalLength, trees.distance, dictionary)
            }
            else -> throw DeflateFormatException("invalid deflate block type $blockType")
        }
    }
    return output.toByteArray()
}

private fun inflateStoredBlock(reader: BitReader, output: MutableList<Byte>) {
    reader.alignToByte()
    val len = reader.readByte() or (reader.readByte() shl 8)
    val nlen = reader.readByte() or (reader.readByte() shl 8)
    if ((len xor MAX_STORED_BLOCK_SIZE) != nlen) {
        throw DeflateFormatException("stored deflate block length check failed")
    }
    repeat(len) {
        output += reader.readByte().toByte()
    }
}

private fun inflateCompressedBlock(
    reader: BitReader,
    output: MutableList<Byte>,
    literalLength: HuffmanTree,
    distance: HuffmanTree,
    dictionary: ByteArray?,
) {
    while (true) {
        when (val symbol = literalLength.decode(reader)) {
            in 0..255 -> output += symbol.toByte()
            256 -> return
            in 257..285 -> {
                val lengthIndex = symbol - 257
                val length = lengthBases[lengthIndex] + reader.readBits(lengthExtraBits[lengthIndex])
                val distanceSymbol = distance.decode(reader)
                if (distanceSymbol !in distanceBases.indices) {
                    throw DeflateFormatException("invalid deflate distance symbol $distanceSymbol")
                }
                val copyDistance = distanceBases[distanceSymbol] + reader.readBits(distanceExtraBits[distanceSymbol])
                repeat(length) {
                    output += byteAtDistance(copyDistance, output, dictionary)
                }
            }
            else -> throw DeflateFormatException("invalid deflate literal symbol $symbol")
        }
    }
}

private fun byteAtDistance(
    distance: Int,
    output: MutableList<Byte>,
    dictionary: ByteArray?,
): Byte {
    if (distance <= 0) {
        throw DeflateFormatException("deflate distance must be positive")
    }
    val dict = dictionary ?: ByteArray(0)
    val absoluteIndex = dict.size + output.size - distance
    if (absoluteIndex < 0) {
        throw DeflateFormatException("deflate distance exceeds available history")
    }
    return if (absoluteIndex < dict.size) {
        dict[absoluteIndex]
    } else {
        output[absoluteIndex - dict.size]
    }
}

private fun dynamicTrees(reader: BitReader): DynamicTrees {
    val literalLengthCount = reader.readBits(5) + 257
    val distanceCount = reader.readBits(5) + 1
    val codeLengthCount = reader.readBits(4) + 4
    val codeLengthOrder = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)
    val codeLengths = IntArray(19)
    for (index in 0 until codeLengthCount) {
        codeLengths[codeLengthOrder[index]] = reader.readBits(3)
    }
    val codeLengthTree = HuffmanTree(codeLengths)
    val lengths = mutableListOf<Int>()
    val totalCount = literalLengthCount + distanceCount
    while (lengths.size < totalCount) {
        when (val symbol = codeLengthTree.decode(reader)) {
            in 0..15 -> lengths += symbol
            16 -> {
                if (lengths.isEmpty()) {
                    throw DeflateFormatException("repeat code appeared before any code length")
                }
                val repeatCount = reader.readBits(2) + 3
                repeat(repeatCount) { lengths += lengths.last() }
            }
            17 -> {
                val repeatCount = reader.readBits(3) + 3
                repeat(repeatCount) { lengths += 0 }
            }
            18 -> {
                val repeatCount = reader.readBits(7) + 11
                repeat(repeatCount) { lengths += 0 }
            }
            else -> throw DeflateFormatException("invalid code length symbol $symbol")
        }
        if (lengths.size > totalCount) {
            throw DeflateFormatException("dynamic code lengths overran their declared size")
        }
    }
    val literalLengths = lengths.take(literalLengthCount).toIntArray()
    val distanceLengths = lengths.drop(literalLengthCount).toIntArray()
    return DynamicTrees(HuffmanTree(literalLengths), HuffmanTree(distanceLengths))
}

private data class DynamicTrees(
    val literalLength: HuffmanTree,
    val distance: HuffmanTree,
)

private fun fixedLiteralLengthTree(): HuffmanTree {
    val lengths = IntArray(288)
    for (symbol in 0..143) lengths[symbol] = 8
    for (symbol in 144..255) lengths[symbol] = 9
    for (symbol in 256..279) lengths[symbol] = 7
    for (symbol in 280..287) lengths[symbol] = 8
    return HuffmanTree(lengths)
}

private fun fixedDistanceTree(): HuffmanTree =
    HuffmanTree(IntArray(32) { 5 })

private class HuffmanTree(lengths: IntArray) {
    private val table: Map<Int, Int>
    private val maxBits: Int

    init {
        maxBits = lengths.maxOrNull() ?: 0
        if (maxBits == 0) {
            table = emptyMap()
        } else {
            val counts = IntArray(maxBits + 1)
            for (length in lengths) {
                if (length < 0) {
                    throw DeflateFormatException("negative huffman code length")
                }
                if (length > 0) counts[length]++
            }
            val nextCodes = IntArray(maxBits + 1)
            var code = 0
            for (bits in 1..maxBits) {
                code = (code + counts[bits - 1]) shl 1
                nextCodes[bits] = code
            }
            val mutable = mutableMapOf<Int, Int>()
            for (symbol in lengths.indices) {
                val length = lengths[symbol]
                if (length != 0) {
                    val assignedCode = nextCodes[length]
                    nextCodes[length] = assignedCode + 1
                    mutable[(length shl 16) or assignedCode] = symbol
                }
            }
            table = mutable
        }
    }

    fun decode(reader: BitReader): Int {
        var code = 0
        for (bits in 1..maxBits) {
            code = (code shl 1) or reader.readBits(1)
            val symbol = table[(bits shl 16) or code]
            if (symbol != null) return symbol
        }
        throw DeflateFormatException("invalid huffman code")
    }
}

private class BitReader(private val input: ByteArray) {
    private var byteIndex: Int = 0
    private var bitBuffer: Int = 0
    private var bitCount: Int = 0

    fun readBits(count: Int): Int {
        require(count >= 0) { "bit count must not be negative" }
        while (bitCount < count) {
            if (byteIndex >= input.size) {
                throw DeflateFormatException("unexpected end of deflate stream")
            }
            bitBuffer = bitBuffer or ((input[byteIndex].toInt() and 0xff) shl bitCount)
            byteIndex += 1
            bitCount += 8
        }
        val value = bitBuffer and ((1 shl count) - 1)
        bitBuffer = bitBuffer ushr count
        bitCount -= count
        return value
    }

    fun readByte(): Int = readBits(8)

    fun alignToByte() {
        val drop = bitCount % 8
        if (drop != 0) {
            readBits(drop)
        }
    }
}

private fun inflateZlib(input: ByteArray, dictionary: ByteArray?): ByteArray {
    if (input.size < 6) {
        throw DeflateFormatException("zlib stream is too short")
    }
    val cmf = input[0].toInt() and 0xff
    val flg = input[1].toInt() and 0xff
    if ((cmf and 0x0f) != 8) {
        throw DeflateFormatException("zlib stream does not use deflate compression")
    }
    if (((cmf shl 8) + flg) % 31 != 0) {
        throw DeflateFormatException("zlib header check failed")
    }
    var offset = 2
    if ((flg and 0x20) != 0) {
        if (input.size < 10) {
            throw DeflateFormatException("zlib stream is missing dictionary checksum")
        }
        val adler = readBigEndianUInt(input, offset)
        offset += 4
        if (dictionary == null) {
            throw NeedsDictionaryException(adler)
        }
    }
    val compressedEnd = input.size - 4
    if (compressedEnd < offset) {
        throw DeflateFormatException("zlib stream is missing checksum")
    }
    val output = inflateRaw(input.copyOfRange(offset, compressedEnd), dictionary)
    val expected = readBigEndianUInt(input, compressedEnd)
    val actual = adler32(output)
    if (expected != actual) {
        throw DeflateFormatException("zlib checksum mismatch")
    }
    return output
}

private fun inflateGzip(input: ByteArray, dictionary: ByteArray?): ByteArray {
    if (input.size < 18) {
        throw DeflateFormatException("gzip stream is too short")
    }
    if ((input[0].toInt() and 0xff) != 0x1f || (input[1].toInt() and 0xff) != 0x8b) {
        throw DeflateFormatException("gzip header magic is invalid")
    }
    if ((input[2].toInt() and 0xff) != 8) {
        throw DeflateFormatException("gzip stream does not use deflate compression")
    }
    val flags = input[3].toInt() and 0xff
    if ((flags and GZIP_FLAG_TEXT) != 0) {
        throw DeflateFormatException("gzip text flag is not supported")
    }
    var offset = 10
    if ((flags and GZIP_FLAG_EXTRA) != 0) {
        ensureAvailable(input, offset, 2, "gzip extra field length")
        val extraLength = (input[offset].toInt() and 0xff) or ((input[offset + 1].toInt() and 0xff) shl 8)
        offset += 2
        ensureAvailable(input, offset, extraLength, "gzip extra field")
        offset += extraLength
    }
    if ((flags and GZIP_FLAG_NAME) != 0) {
        offset = skipZeroTerminated(input, offset, "gzip file name")
    }
    if ((flags and GZIP_FLAG_COMMENT) != 0) {
        offset = skipZeroTerminated(input, offset, "gzip comment")
    }
    if ((flags and GZIP_FLAG_HEADER_CRC) != 0) {
        ensureAvailable(input, offset, 2, "gzip header checksum")
        offset += 2
    }
    val compressedEnd = input.size - 8
    if (compressedEnd < offset) {
        throw DeflateFormatException("gzip stream is missing trailer")
    }
    val output = inflateRaw(input.copyOfRange(offset, compressedEnd), dictionary)
    val expectedCrc = readLittleEndianUInt(input, compressedEnd)
    val expectedSize = readLittleEndianUInt(input, compressedEnd + 4)
    val actualCrc = crc32(output)
    if (expectedCrc != actualCrc) {
        throw DeflateFormatException("gzip checksum mismatch")
    }
    if (expectedSize != output.size.toUInt()) {
        throw DeflateFormatException("gzip uncompressed size mismatch")
    }
    return output
}

private fun zlibHeader(level: Compression, dictionary: ByteArray?): ByteArray {
    val cmf = 0x78
    val levelHint = when (level.level()) {
        0u, 1u -> 0x00
        2u, 3u, 4u, 5u -> 0x40
        6u, 7u -> 0x80
        else -> 0xc0
    }
    var flg = levelHint or if (dictionary == null) 0 else 0x20
    while (((cmf shl 8) + flg) % 31 != 0) {
        flg += 1
    }
    val prefix = byteArrayOf(cmf.toByte(), flg.toByte())
    return if (dictionary == null) prefix else prefix + adler32Bytes(dictionary)
}

private fun gzipHeader(): ByteArray =
    byteArrayOf(
        0x1f.toByte(),
        0x8b.toByte(),
        0x08.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0xff.toByte(),
    )

private fun appendProducedBytes(buffer: ByteArray, output: MutableList<Byte>, byteCount: Int) {
    for (index in 0 until byteCount) {
        output += buffer[index]
    }
}

private fun adler32(input: ByteArray): UInt {
    var a = 1u
    var b = 0u
    for (byte in input) {
        a = (a + byte.toUByte().toUInt()) % 65521u
        b = (b + a) % 65521u
    }
    return (b shl 16) or a
}

private fun adler32Bytes(input: ByteArray): ByteArray =
    bigEndianUInt(adler32(input))

private fun crc32(input: ByteArray): UInt {
    val crc = Crc()
    crc.update(input)
    return crc.sum()
}

private fun crc32Bytes(input: ByteArray): ByteArray =
    littleEndianUInt(crc32(input))

private fun bigEndianUInt(value: UInt): ByteArray =
    byteArrayOf(
        ((value shr 24) and 0xffu).toByte(),
        ((value shr 16) and 0xffu).toByte(),
        ((value shr 8) and 0xffu).toByte(),
        (value and 0xffu).toByte(),
    )

private fun littleEndianUInt(value: UInt): ByteArray =
    byteArrayOf(
        (value and 0xffu).toByte(),
        ((value shr 8) and 0xffu).toByte(),
        ((value shr 16) and 0xffu).toByte(),
        ((value shr 24) and 0xffu).toByte(),
    )

private fun readBigEndianUInt(input: ByteArray, offset: Int): UInt {
    ensureAvailable(input, offset, 4, "big-endian integer")
    return ((input[offset].toUIntByte() shl 24) or
        (input[offset + 1].toUIntByte() shl 16) or
        (input[offset + 2].toUIntByte() shl 8) or
        input[offset + 3].toUIntByte())
}

private fun readLittleEndianUInt(input: ByteArray, offset: Int): UInt {
    ensureAvailable(input, offset, 4, "little-endian integer")
    return (input[offset].toUIntByte() or
        (input[offset + 1].toUIntByte() shl 8) or
        (input[offset + 2].toUIntByte() shl 16) or
        (input[offset + 3].toUIntByte() shl 24))
}

private fun Byte.toUIntByte(): UInt =
    (toInt() and 0xff).toUInt()

private fun ensureAvailable(input: ByteArray, offset: Int, count: Int, description: String) {
    if (offset < 0 || count < 0 || offset + count > input.size) {
        throw DeflateFormatException("unexpected end while reading $description")
    }
}

private fun skipZeroTerminated(input: ByteArray, start: Int, description: String): Int {
    var offset = start
    while (offset < input.size) {
        if (input[offset] == 0.toByte()) return offset + 1
        offset += 1
    }
    throw DeflateFormatException("unterminated $description")
}

private fun decompressDisplayMessage(inner: DecompressErrorInner): String =
    when (inner) {
        is DecompressErrorInner.General ->
            inner.message?.let { "deflate decompression error: $it" } ?: "deflate decompression error"
        is DecompressErrorInner.NeedsDictionary ->
            "deflate decompression error: requires a dictionary"
    }

private fun compressDisplayMessage(message: String?): String =
    message?.let { "deflate compression error: $it" } ?: "deflate compression error"

internal class DeflateFormatException(message: String) : Exception(message)

internal class NeedsDictionaryException(val adler: UInt) : Exception("requires a dictionary")
