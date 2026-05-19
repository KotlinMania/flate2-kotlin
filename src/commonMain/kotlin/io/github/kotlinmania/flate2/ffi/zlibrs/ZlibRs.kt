// port-lint: source ffi/zlib_rs.rs
package io.github.kotlinmania.flate2.ffi.zlibrs

import io.github.kotlinmania.flate2.*
import kotlin.test.*

// Implementation for `zlibRs` rust backend.
// 
// Every backend must provide two types:
// 
// - `Deflate` for compression, implements the `Backend` and `DeflateBackend` trait
// - `Inflate` for decompression, implements the `Backend` and `InflateBackend` trait
// 
// Additionally the backend provides a number of constants, and a `ErrorMessage` type.
// 
// ## Allocation
// 
// The (de)compression state is not boxed. The C implementations require that the zStream is
// pinned in memory (has a fixed address), because their zStream is self-referential. The most
// convenient way in rust to guarantee a stable address is to `Box` the data, but it does add an
// additional allocation.
// 
// With zlibRs the state is not self-referential and hence no boxing is needed. The `new` methods
// internally do allocate space for the (de)compression state.



public val MZ_NO_FLUSH: Int = DeflateFlush.NoFlush as Int
public val MZ_PARTIAL_FLUSH: Int = DeflateFlush.PartialFlush as Int
public val MZ_SYNC_FLUSH: Int = DeflateFlush.SyncFlush as Int
public val MZ_FULL_FLUSH: Int = DeflateFlush.FullFlush as Int
public val MZ_FINISH: Int = DeflateFlush.Finish as Int

public val MZ_DEFAULT_WINDOW_BITS: ffi.cInt = 15


public object StatusImpl {
    private fun from(value: .zlibRs.Status): Any {
        when value {
            .zlibRs.Status.Ok: io.github.kotlinmania.flate2.mem.Status.Ok,
            .zlibRs.Status.BufError: io.github.kotlinmania.flate2.mem.Status.BufError,
            .zlibRs.Status.StreamEnd: io.github.kotlinmania.flate2.mem.Status.StreamEnd,
        }
    }
}

public data class ErrorMessage(value0: String?)

public object ErrorMessageImpl {
    public fun getUnit: String? {
        this.0
    }
}

public class Inflate {
    public var inner: .zlibRs.Inflate? = null
    // NOTE: these counts do not count the dictionary.
    internal var totalIn: ULong? = null
    internal var totalOut: ULong? = null
}

public object InflateImpl {
    private fun fmt(f: fmt.Formatter): Result<Unit> {
        write(
            f,
            "zlibRs inflate internal state. totalIn: {}, totalOut: {}",
            this.totalInUnit,
            this.totalOutUnit,
        )
    }
}

public object DeflateFlushImpl {
    private fun from(value: FlushDecompress): Any {
        when value {
            FlushDecompress.null: Self.NoFlush,
            FlushDecompress.Sync: Self.SyncFlush,
            FlushDecompress.Finish: Self.Finish,
        }
    }
}

public object InflateImpl2 {
    private fun make(zlibHeader: Boolean, windowBits: UByte): Any {
        Inflate {
            inner: .zlibRs.Inflate.new(zlibHeader, windowBits),
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
        val flush = when flush {
            FlushDecompress.null: InflateFlush.NoFlush,
            FlushDecompress.Sync: InflateFlush.SyncFlush,
            FlushDecompress.Finish: InflateFlush.Finish,
        }

        val totalInStart = this.inner.totalInUnit
        val totalOutStart = this.inner.totalOutUnit

        val result = this.inner.decompress(input, output, flush)

        this.totalIn += this.inner.totalInUnit - totalInStart
        this.totalOut += this.inner.totalOutUnit - totalOutStart

        when result {
            Result.success(status): Result.success(status.intoUnit),
            Result.failure(InflateError.NeedDict { dictId }): io.github.kotlinmania.flate2.mem.decompressNeedDict(dictId),
            Result.failure(_): this.decompressErrorUnit,
        }
    }

    private fun reset(zlibHeader: Boolean): Unit {
        this.totalIn = 0
        this.totalOut = 0
        this.inner.reset(zlibHeader)
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

public object InflateImpl4 {
    private fun decompressError<T>Unit: Result<T> {
        decompressFailed(ErrorMessage(this.inner.errorMessageUnit))
    }

    public fun setDictionary(dictionary: ByteArray): Result<UInt> {
        when this.inner.setDictionary(dictionary) {
            Result.success(v): Result.success(v),
            Result.failure(_): this.decompressErrorUnit,
        }
    }
}

public class Deflate {
    public var inner: .zlibRs.Deflate? = null
    // NOTE: these counts do not count the dictionary.
    internal var totalIn: ULong? = null
    internal var totalOut: ULong? = null
}

public object DeflateImpl {
    private fun fmt(f: fmt.Formatter): Result<Unit> {
        write(
            f,
            "zlibRs deflate internal state. totalIn: {}, totalOut: {}",
            this.totalInUnit,
            this.totalOutUnit,
        )
    }
}

public object DeflateImpl2 {
    private fun make(level: Compression, zlibHeader: Boolean, windowBits: UByte): Any {
        // Check in case the integer value changes at some point.
        check(level.levelUnit <= 9)

        Deflate {
            inner: .zlibRs.Deflate.new(level.levelUnit as i32, zlibHeader, windowBits),
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
        val flush = when flush {
            FlushCompress.null: DeflateFlush.NoFlush,
            FlushCompress.Partial: DeflateFlush.PartialFlush,
            FlushCompress.Sync: DeflateFlush.SyncFlush,
            FlushCompress.Full: DeflateFlush.FullFlush,
            FlushCompress.Finish: DeflateFlush.Finish,
        }

        val totalInStart = this.inner.totalInUnit
        val totalOutStart = this.inner.totalOutUnit

        val result = this.inner.compress(input, output, flush)

        this.totalIn += this.inner.totalInUnit - totalInStart
        this.totalOut += this.inner.totalOutUnit - totalOutStart

        when result {
            Result.success(status): Result.success(status.intoUnit),
            Result.failure(_): this.compressErrorUnit,
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

public object DeflateImpl4 {
    private fun compressError<T>Unit: Result<T> {
        compressFailed(ErrorMessage(this.inner.errorMessageUnit))
    }

    public fun setDictionary(dictionary: ByteArray): Result<UInt> {
        when this.inner.setDictionary(dictionary) {
            Result.success(v): Result.success(v),
            Result.failure(_): this.compressErrorUnit,
        }
    }

    public fun setLevel(level: Compression): Result<Unit> {

        when this.inner.setLevel(level.levelUnit as i32) {
            Result.success(status): when status {
                Status.Ok: Result.success(Unit),
                Status.BufError: compressFailed(ErrorMessage("insufficient space")),
                Status.StreamEnd: {
                    unreachable!("zlib-rs is known to never return the StreamEnd status")
                }
            },
            Result.failure(_): this.compressErrorUnit,
        }
    }
}
