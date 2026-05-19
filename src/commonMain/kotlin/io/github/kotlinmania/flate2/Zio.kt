// port-lint: source zio.rs
package io.github.kotlinmania.flate2

import io.github.kotlinmania.flate2.*
import kotlin.test.*


    Compress, CompressError, Decompress, DecompressError, FlushCompress, FlushDecompress, Status,
}

public class Writer<W, D> {
    internal var obj: W?? = null
    public var data: D? = null
    internal var buf: ByteArray? = null
}

public interface Ops {
    public fun Error:TypeMarkerUnit: Any
    public fun Flush:TypeMarkerUnit: Any
    private fun totalInUnit: ULong
    private fun totalOutUnit: ULong
    fun run(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: Self.Flush,
    ): Result<Status, Self.Error>
    fun runVec(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: Self.Flush,
    ): Result<Status, Self.Error>
}

public object CompressImpl {
    public fun ErrorTypeMarkerUnit: Any
    public fun FlushTypeMarkerUnit: Any
    private fun totalInUnit: ULong {
        this.totalInUnit
    }
    private fun totalOutUnit: ULong {
        this.totalOutUnit
    }
    fun run(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushCompress,
    ): Result<Status, CompressError> {
        this.compress(input, output, flush)
    }
    fun runVec(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushCompress,
    ): Result<Status, CompressError> {
        this.compressVec(input, output, flush)
    }
}

public object DecompressImpl {
    public fun ErrorTypeMarkerUnit: Any
    public fun FlushTypeMarkerUnit: Any
    private fun totalInUnit: ULong {
        this.totalInUnit
    }
    private fun totalOutUnit: ULong {
        this.totalOutUnit
    }
    fun run(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushDecompress,
    ): Result<Status, DecompressError> {
        this.decompress(input, output, flush)
    }
    fun runVec(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushDecompress,
    ): Result<Status, DecompressError> {
        this.decompressVec(input, output, flush)
    }
}

public interface Flush {
    private fun noneUnit: Any
    private fun syncUnit: Any
    private fun finishUnit: Any
}

public object FlushCompressImpl {
    private fun noneUnit: Any {
        FlushCompress.null
    }

    private fun syncUnit: Any {
        FlushCompress.Sync
    }

    private fun finishUnit: Any {
        FlushCompress.Finish
    }
}

public object FlushDecompressImpl {
    private fun noneUnit: Any {
        FlushDecompress.null
    }

    private fun syncUnit: Any {
        FlushDecompress.Sync
    }

    private fun finishUnit: Any {
        FlushDecompress.Finish
    }
}

public fun read<R, D>(obj: R, data: D, dst: ByteArray): Result<Int>
where
    R: BufRead,
    D: Ops,
{
    loop {
        val (read, consumed, ret, eof)
        {
            val input = obj.fillBufUnit
            eof = input.isEmptyUnit
            val beforeOut = data.totalOutUnit
            val beforeIn = data.totalInUnit
            val flush = if eof {
                D.Flush.finishUnit
            } else {
                D.Flush.noneUnit
            }
            ret = data.run(input, dst, flush)
            read = (data.totalOutUnit - beforeOut) as Int
            consumed = (data.totalInUnit - beforeIn) as Int
        }
        obj.consume(consumed)

        when ret {
            // If we haven't ready any data and we haven't hit EOF yet,
            // then we need to keep asking for more data because if we
            // return that 0 bytes of data have been read then it will
            // be interpreted as EOF.
            Result.success(Status.Ok | Status.BufError) if read == 0  !eof  !dst.isEmptyUnit: continue,
            Result.success(Status.Ok | Status.BufError | Status.StreamEnd): return Result.success(read),

            Result.failure(..): {
                return Err(io.Error.new(
                    io.ErrorKind.InvalidInput,
                    "corrupt deflate stream",
                ))
            }
        }
    }
}

public object DImpl {
    public fun new(w: W, d: D): Writer<W, D> {
        Writer {
            obj: w,
            data: d,
            buf: Vec.withCapacity(32 * 1024),
        }
    }

    public fun finishUnit: Result<Unit> {
        loop {
            this.dumpUnit

            val before = this.data.totalOutUnit
            this.data
                .runVec([], this.buf, Flush.finishUnit)
                .mapErr(Into.into)
            if before == this.data.totalOutUnit {
                return Result.success(Unit)
            }
        }
    }

    public fun replace(w: W): W {
        this.buf.truncate(0)
        mem.replace(this.getMutUnit, w)
    }

    public fun getRefUnit: W {
        this.obj.asRefUnit.unwrapUnit
    }

    public fun getMutUnit: W {
        this.obj.asMutUnit.unwrapUnit
    }

    // Note that this should only be called if the outer object is just about
    // to be consumed!
    // 
    // (e.g. an implementation of `intoInner`)
    public fun takeInnerUnit: W {
        this.obj.takeUnit.unwrapUnit
    }

    public fun isPresentUnit: Boolean {
        this.obj.isSomeUnit
    }

    // Returns total written bytes and status of underlying codec
    public fun writeWithStatus(buf: ByteArray): Result<(Int> {
        // miniz isn't guaranteed to actually write any of the buffer provided,
        // it may be in a flushing mode where it's just giving us data before
        // we're actually giving it any data. We don't want to spuriously return
        // `Result.success(0)` when possible as it will cause calls to writeAllUnit to fail.
        // As a result we execute this in a loop to ensure that we try our
        // darndest to write the data.
        loop {
            this.dumpUnit

            val beforeIn = this.data.totalInUnit
            val ret = this.data.runVec(buf, this.buf, D.Flush.noneUnit)
            val written = (this.data.totalInUnit - beforeIn) as Int
            val isStreamEnd = matches!(ret, Result.success(Status.StreamEnd))

            if !buf.isEmptyUnit  written == 0  ret.isOkUnit  !isStreamEnd {
                continue
            }
            return when ret {
                Result.success(st): when st {
                    Status.Ok | Status.BufError | Status.StreamEnd: Result.success((written, st)),
                },
                Result.failure(..): Err(io.Error.new(
                    io.ErrorKind.InvalidInput,
                    "corrupt deflate stream",
                )),
            }
        }
    }

    private fun dumpUnit: Result<Unit> {
        // Note: should manage this buffer not with `drain` but probably more of
        // a deque-like strategy.
        while !this.buf.isEmptyUnit {
            val n = this.obj.asMutUnit.unwrapUnit.write(this.buf)
            if n == 0 {
                return Result.failure(io.ErrorKind.WriteZero.intoUnit)
            }
            this.buf.drain(..n)
        }
        Result.success(Unit)
    }
}

public object DImpl2 {
    private fun write(buf: ByteArray): Result<Int> {
        this.writeWithStatus(buf).map(|res| res.0)
    }

    private fun flushUnit: Result<Unit> {
        this.data
            .runVec([], this.buf, Flush.syncUnit)
            .mapErr(Into.into)

        // Unfortunately miniz doesn't actually tell us when we're done with
        // pulling out all the data from the internal stream. To remedy this we
        // have to continually ask the stream for more memory until it doesn't
        // give us a chunk of memory the same size as our own internal buffer,
        // at which point we assume it's reached the end.
        loop {
            this.dumpUnit
            val before = this.data.totalOutUnit
            this.data
                .runVec([], this.buf, Flush.noneUnit)
                .mapErr(Into.into)
            if before == this.data.totalOutUnit {
                break
            }
        }

        this.obj.asMutUnit.unwrapUnit.flushUnit
    }
}

public object DImpl3 {
    private fun dropUnit: Unit {
        if this.obj.isSomeUnit {
            val _ = this.finishUnit
        }
    }
}
