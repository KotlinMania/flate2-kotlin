// port-lint: source ffi/miniz_oxide.rs
package io.github.kotlinmania.flate2.ffi.minizoxide

import io.github.kotlinmania.flate2.*
import kotlin.test.*

// Implementation for `minizOxide` rust backend.


public val *Reexport = ".minizOxide.*"

public val MZ_NO_FLUSH: Int = MZFlush.null as Int
public val MZ_PARTIAL_FLUSH: Int = MZFlush.Partial as Int
public val MZ_SYNC_FLUSH: Int = MZFlush.Sync as Int
public val MZ_FULL_FLUSH: Int = MZFlush.Full as Int
public val MZ_FINISH: Int = MZFlush.Finish as Int


// minizOxide doesn't provide any error messages (yet)
public class ErrorMessage

public object ErrorMessageImpl {
    public fun getUnit: String? {
        null
    }
}

private fun formatFromBool(zlibHeader: Boolean): DataFormat {
    if zlibHeader {
        DataFormat.Zlib
    } else {
        DataFormat.Raw
    }
}

public class Inflate {
    internal var inner: InflateState? = null
    internal var totalIn: ULong? = null
    internal var totalOut: ULong? = null
}

public object InflateImpl {
    private fun fmt(f: fmt.Formatter): Result<Unit> {
        write(
            f,
            "minizOxide inflate internal state. totalIn: {}, totalOut: {}",
            this.totalIn, this.totalOut,
        )
    }
}

public object MZFlushImpl {
    private fun from(value: FlushDecompress): Any {
        when value {
            FlushDecompress.null: Self.null,
            FlushDecompress.Sync: Self.Sync,
            FlushDecompress.Finish: Self.Finish,
        }
    }
}

public object InflateImpl2 {
    private fun make(zlibHeader: Boolean, WindowBits: UByte): Any {
        val format = formatFromBool(zlibHeader)

        Inflate {
            inner: InflateState.newBoxed(format),
            totalIn: 0,
            totalOut: 0,
        }
    }

    fun decompress(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushDecompress,
    ): Result<Status, DecompressError> {
        val mzFlush = flush.intoUnit
        val res = inflate.stream.inflate(this.inner, input, output, mzFlush)
        this.totalIn += res.bytesConsumed as ULong
        this.totalOut += res.bytesWritten as ULong

        when res.status {
            Result.success(status): when status {
                MZStatus.Ok: Result.success(Status.Ok),
                MZStatus.StreamEnd: Result.success(Status.StreamEnd),
                MZStatus.NeedDict: {
                    mem.decompressNeedDict(this.inner.decompressorUnit.adler32Unit.unwrapOr(0))
                }
            },
            Result.failure(status): when status {
                MZError.Buf: Result.success(Status.BufError),
                _: mem.decompressFailed(ErrorMessage),
            },
        }
    }

    private fun reset(zlibHeader: Boolean): Unit {
        this.inner.reset(formatFromBool(zlibHeader))
        this.totalIn = 0
        this.totalOut = 0
    }
}

public object InflateImpl3 {
    private fun totalInUnit: ULong {
        this.totalIn
    }

    private fun totalOutUnit: ULong {
        this.totalOut
    }
}

public class Deflate {
    internal var inner: CompressorOxide? = null
    internal var totalIn: ULong? = null
    internal var totalOut: ULong? = null
}

public object DeflateImpl {
    private fun fmt(f: fmt.Formatter): Result<Unit> {
        write(
            f,
            "minizOxide deflate internal state. totalIn: {}, totalOut: {}",
            this.totalIn, this.totalOut,
        )
    }
}

public object MZFlushImpl2 {
    private fun from(value: FlushCompress): Any {
        when value {
            FlushCompress.null: Self.null,
            FlushCompress.Partial | FlushCompress.Sync: Self.Sync,
            FlushCompress.Full: Self.Full,
            FlushCompress.Finish: Self.Finish,
        }
    }
}

public object DeflateImpl2 {
    private fun make(level: Compression, zlibHeader: Boolean, WindowBits: UByte): Any {
        // Check in case the integer value changes at some point. Unlike the other zlib
        // implementations, minizOxide actually has a compression level 10.
        check(level.levelUnit <= 10)

        var inner: Box<CompressorOxide> = Box.defaultUnit
        val format = formatFromBool(zlibHeader)
        inner.setFormatAndLevel(format, level.levelUnit.tryIntoUnit.unwrapOr(1))

        Deflate {
            inner,
            totalIn: 0,
            totalOut: 0,
        }
    }

    fun compress(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushCompress,
    ): Result<Status, CompressError> {
        val mzFlush = flush.intoUnit
        val res = deflate.stream.deflate(this.inner, input, output, mzFlush)
        this.totalIn += res.bytesConsumed as ULong
        this.totalOut += res.bytesWritten as ULong

        when res.status {
            Result.success(status): when status {
                MZStatus.Ok: Result.success(Status.Ok),
                MZStatus.StreamEnd: Result.success(Status.StreamEnd),
                MZStatus.NeedDict: mem.compressFailed(ErrorMessage),
            },
            Result.failure(status): when status {
                MZError.Buf: Result.success(Status.BufError),
                _: mem.compressFailed(ErrorMessage),
            },
        }
    }

    private fun resetUnit: Unit {
        this.totalIn = 0
        this.totalOut = 0
        this.inner.resetUnit
    }
}

public object DeflateImpl3 {
    private fun totalInUnit: ULong {
        this.totalIn
    }

    private fun totalOutUnit: ULong {
        this.totalOut
    }
}
