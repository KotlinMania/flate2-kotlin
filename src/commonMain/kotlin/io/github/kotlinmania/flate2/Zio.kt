// port-lint: source zio.rs
package io.github.kotlinmania.flate2

/**
 * A generic writer that wraps a [Compress] or [Decompress] codec around an
 * [OutputSink].
 *
 * Data written to this writer is compressed (or decompressed) by the codec
 * and forwarded to the underlying sink. Internally, the writer maintains a
 * buffer to accumulate codec output before flushing it to the sink.
 *
 * When this writer is no longer needed, call [finish] to flush all remaining
 * codec output. The Rust original implements `Drop` to call `finish`
 * automatically; Kotlin does not have deterministic destructors, so callers
 * must call [finish] explicitly or use `try`/`finally`.
 *
 * @param W the output sink type
 * @param D the codec type ([Compress] or [Decompress])
 */
public class Writer<W : OutputSink, D : CodecOps>(
    private var obj: W?,
    public val data: D,
) {
    private val buf: MutableList<Byte> = ArrayList(DEFAULT_BUF_SIZE)

    public companion object {
        internal const val DEFAULT_BUF_SIZE: Int = 32 * 1024

        /** Creates a new writer wrapping [sink] with the given [codec]. */
        public fun <W : OutputSink, D : CodecOps> new(sink: W, codec: D): Writer<W, D> =
            Writer(sink, codec)
    }

    /**
     * Finishes writing all remaining codec output to the underlying sink.
     *
     * Call this when all input has been written and you want to complete the
     * stream. After this returns, no more data should be written.
     */
    public fun finish() {
        do {
            dump()
            val before = data.totalOut()
            data.runVec(ByteArray(0), buf, data.flushFinish()).getOrThrow()
            if (before == data.totalOut()) break
        } while (true)
    }

    /**
     * Replaces the underlying sink with [newSink], returning the old one.
     *
     * The internal buffer is cleared. Any buffered codec output that has not
     * yet been written to the old sink is **not** flushed — call [dump]
     * before replacing if you need to flush.
     */
    public fun replace(newSink: W): W {
        buf.clear()
        val old = obj ?: throw IllegalStateException("Writer has been consumed")
        obj = newSink
        return old
    }

    /** Returns the underlying sink by reference. */
    public fun getRef(): W = obj ?: throw IllegalStateException("Writer has been consumed")

    /** Returns a mutable reference to the underlying sink. */
    public fun getMut(): W = obj ?: throw IllegalStateException("Writer has been consumed")

    /**
     * Consumes this writer and returns the underlying sink.
     *
     * After calling this method, the writer can no longer be used. Any
     * remaining codec output in the internal buffer will not be flushed —
     * call [finish] first if you need to complete the stream.
     */
    public fun takeInner(): W {
        val inner = obj ?: throw IllegalStateException("Writer has been consumed")
        obj = null
        return inner
    }

    /** Whether the underlying sink is still present (not consumed via [takeInner]). */
    public fun isPresent(): Boolean = obj != null

    /**
     * Writes [input] through the codec and returns the number of bytes
     * consumed from the input plus the resulting [Status].
     *
     * This loops internally to ensure at least some progress is made — it
     * will not return zero bytes consumed while there is still input to
     * process unless the stream has ended.
     */
    public fun writeWithStatus(input: ByteArray): Pair<Int, Status> {
        while (true) {
            dump()
            val beforeIn = data.totalIn()
            val ret = data.runVec(input, buf, data.flushNone())
            val written = (data.totalIn() - beforeIn).toInt()
            val isStreamEnd = ret.isStreamEnd

            if (input.isNotEmpty() && written == 0 && ret.isSuccess && !isStreamEnd) {
                continue
            }
            return written to ret.getOrThrow()
        }
    }

    /**
     * Writes [input] through the codec and returns the number of bytes
     * consumed from the input.
     */
    public fun write(input: ByteArray): Int = writeWithStatus(input).first

    /**
     * Flushes all pending codec output to the underlying sink.
     *
     * This flushes the codec with [CodecOps.flushSync], drains any buffered
     * output, then calls [OutputSink.flush] on the underlying sink.
     */
    public fun flush() {
        data.runVec(ByteArray(0), buf, data.flushSync()).getOrThrow()
        do {
            dump()
            val before = data.totalOut()
            data.runVec(ByteArray(0), buf, data.flushNone()).getOrThrow()
            if (before == data.totalOut()) break
        } while (true)
        obj?.flush()
    }

    /** Drains buffered codec output to the underlying sink. */
    private fun dump() {
        while (buf.isNotEmpty()) {
            val sink = obj ?: throw IllegalStateException("Writer has been consumed")
            val chunk = buf.toByteArray()
            val n = sink.write(chunk, 0, chunk.size)
            if (n == 0) {
                throw DeflateFormatException("write zero bytes to underlying sink")
            }
            if (n >= buf.size) {
                buf.clear()
            } else {
                buf.subList(0, n).clear()
            }
        }
    }
}

/**
 * Abstraction over the codec operations that a [Writer] can perform.
 *
 * Both [Compress] and [Decompress] are wrapped via [CompressOps] and
 * [DecompressOps] so that [Writer] can operate generically over either
 * codec type.
 */
public interface CodecOps {
    /** Total bytes input so far. */
    public fun totalIn(): ULong

    /** Total bytes output so far. */
    public fun totalOut(): ULong

    /** Run the codec with the given input, output, and flush mode. */
    public fun run(input: ByteArray, output: ByteArray, flush: FlushKind): CodecResult

    /** Run the codec, appending output to [output]. */
    public fun runVec(input: ByteArray, output: MutableList<Byte>, flush: FlushKind): CodecResult

    /** The "none" flush mode for this codec. */
    public fun flushNone(): FlushKind

    /** The "sync" flush mode for this codec. */
    public fun flushSync(): FlushKind

    /** The "finish" flush mode for this codec. */
    public fun flushFinish(): FlushKind
}

/**
 * The flush mode for codec operations.
 *
 * This unifies [FlushCompress] and [FlushDecompress] into a single sealed
 * hierarchy that [CodecOps] can work with generically.
 */
public sealed class FlushKind {
    /** No special flushing. */
    public object None : FlushKind()
    /** Sync flush — produce output up to a byte boundary. */
    public object Sync : FlushKind()
    /** Finish the stream. */
    public object Finish : FlushKind()
}

/**
 * The result of a codec operation.
 *
 * Carries a [Status] on success and an optional error message on failure,
 * matching the upstream pattern where `Compress` returns
 * `Result<Status, CompressError>` and `Decompress` returns
 * `Result<Status, DecompressError>`.
 */
public sealed class CodecResult {
    /** Codec operation succeeded with the given [status]. */
    public data class Success(val status: Status) : CodecResult()

    /** Codec operation failed with an optional error [message]. */
    public data class Failure(val message: String?) : CodecResult()

    public val isSuccess: Boolean get() = this is Success
    public val isFailure: Boolean get() = this is Failure
    public val isStreamEnd: Boolean get() = this is Success && status == Status.StreamEnd

    public fun getOrThrow(): Status = when (this) {
        is Success -> status
        is Failure -> throw DeflateFormatException(message ?: "deflate error")
    }
}

/** Convert a [FlushCompress] value to a [FlushKind]. */
public fun FlushCompress.toFlushKind(): FlushKind = when (this) {
    FlushCompress.None -> FlushKind.None
    FlushCompress.Partial -> FlushKind.None
    FlushCompress.Sync -> FlushKind.Sync
    FlushCompress.Full -> FlushKind.Sync
    FlushCompress.Finish -> FlushKind.Finish
}

/** Convert a [FlushKind] value to a [FlushCompress]. */
public fun FlushKind.toFlushCompress(): FlushCompress = when (this) {
    FlushKind.None -> FlushCompress.None
    FlushKind.Sync -> FlushCompress.Sync
    FlushKind.Finish -> FlushCompress.Finish
}

/** Convert a [FlushDecompress] value to a [FlushKind]. */
public fun FlushDecompress.toFlushKind(): FlushKind = when (this) {
    FlushDecompress.None -> FlushKind.None
    FlushDecompress.Sync -> FlushKind.Sync
    FlushDecompress.Finish -> FlushKind.Finish
}

/** Convert a [FlushKind] value to a [FlushDecompress]. */
public fun FlushKind.toFlushDecompress(): FlushDecompress = when (this) {
    FlushKind.None -> FlushDecompress.None
    FlushKind.Sync -> FlushDecompress.Sync
    FlushKind.Finish -> FlushDecompress.Finish
}

/** Convert a Kotlin [Result]<[Status]> to a [CodecResult]. */
public fun Result<Status>.toCodecResult(): CodecResult = fold(
    { CodecResult.Success(it) },
    { CodecResult.Failure(it.message) },
)

/** [Compress] adapter implementing [CodecOps]. */
public class CompressOps(private val compress: Compress) : CodecOps {
    override fun totalIn(): ULong = compress.totalIn()
    override fun totalOut(): ULong = compress.totalOut()
    override fun run(input: ByteArray, output: ByteArray, flush: FlushKind): CodecResult =
        compress.compress(input, output, flush.toFlushCompress()).toCodecResult()

    override fun runVec(input: ByteArray, output: MutableList<Byte>, flush: FlushKind): CodecResult =
        compress.compressVec(input, output, flush.toFlushCompress()).toCodecResult()

    override fun flushNone(): FlushKind = FlushKind.None
    override fun flushSync(): FlushKind = FlushKind.Sync
    override fun flushFinish(): FlushKind = FlushKind.Finish
}

/** [Decompress] adapter implementing [CodecOps]. */
public class DecompressOps(private val decompress: Decompress) : CodecOps {
    override fun totalIn(): ULong = decompress.totalIn()
    override fun totalOut(): ULong = decompress.totalOut()
    override fun run(input: ByteArray, output: ByteArray, flush: FlushKind): CodecResult =
        decompress.decompress(input, output, flush.toFlushDecompress()).toCodecResult()

    override fun runVec(input: ByteArray, output: MutableList<Byte>, flush: FlushKind): CodecResult =
        decompress.decompressVec(input, output, flush.toFlushDecompress()).toCodecResult()

    override fun flushNone(): FlushKind = FlushKind.None
    override fun flushSync(): FlushKind = FlushKind.Sync
    override fun flushFinish(): FlushKind = FlushKind.Finish
}

/**
 * Reads from a [BufferedSource] through the given codec into [dst].
 *
 * This mirrors the upstream `read` function in `zio.rs`. It reads data from
 * the source, feeds it through the codec, and writes output into [dst].
 * Returns the number of bytes written to [dst]. When the source reaches
 * end-of-stream, the codec is flushed with a "finish" signal.
 *
 * If no data is produced and the source has not reached EOF, the function
 * loops to request more data — returning zero would otherwise be
 * interpreted as EOF.
 *
 * @param source the buffered source to read from
 * @param codec the codec (wrapped in [CodecOps]) to process the data
 * @param dst the output buffer to write into
 * @return the number of bytes written to [dst]
 * @throws DeflateFormatException if the codec encounters corrupt input
 */
public fun readThroughCodec(
    source: BufferedSource,
    codec: CodecOps,
    dst: ByteArray,
): Int {
    while (true) {
        val input = source.fillBuffer()
        val eof = input.isEmpty()
        val beforeOut = codec.totalOut()
        val beforeIn = codec.totalIn()
        val flush = if (eof) codec.flushFinish() else codec.flushNone()
        val ret = codec.run(input, dst, flush)
        val read = (codec.totalOut() - beforeOut).toInt()
        val consumed = (codec.totalIn() - beforeIn).toInt()
        source.consume(consumed)

        when (ret) {
            is CodecResult.Success -> {
                if ((ret.status == Status.Ok || ret.status == Status.BufError) &&
                    read == 0 && !eof && dst.isNotEmpty()
                ) {
                    continue
                }
                return read
            }
            is CodecResult.Failure -> {
                throw DeflateFormatException(ret.message ?: "corrupt deflate stream")
            }
        }
    }
}