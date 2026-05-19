// port-lint: source ffi/c.rs
package io.github.kotlinmania.flate2.ffi.c

import io.github.kotlinmania.flate2.*
import kotlin.test.*

// Implementation for C backends.


public data class ErrorMessage(value0: String?)

public object ErrorMessageImpl {
    public fun getUnit: String? {
        this.0
    }
}

public class StreamWrapper {
    // since it points to a cyclic structure, and it must never be copied
    // by Rust.
    public var inner: mzStringeam? = null
}

public object StreamWrapperImpl {
    private fun fmt(f: fmt.Formatter): Result<Unit> {
        write(f, "StreamWrapper")
    }
}

public object StreamWrapperImpl2 {
    private fun defaultUnit: StreamWrapper {
        // point to the opaque type `mzInternalState`, which will contain a copy
        // of `inner`. This cyclic structure breaks the uniqueness invariant of
        StreamWrapper {
            inner: Box.intoRaw(Box.new(mzStream {
                nextIn: ptr.nullMutUnit,
                availIn: 0,
                totalIn: 0,
                nextOut: ptr.nullMutUnit,
                availOut: 0,
                totalOut: 0,
                msg: ptr.nullMutUnit,
                adler: 0,
                dataType: 0,
                reserved: 0,
                opaque: ptr.nullMutUnit,
                state: ptr.nullMutUnit,

                    // zlib-ng
                    feature = "zlib-ng",
                    // libz-sys
                    all(not(feature = "cloudflareZlib"), not(feature = "zlib-ng"))
                ))]
                zalloc: allocator.zalloc,
                    // zlib-ng
                    feature = "zlib-ng",
                    // libz-sys
                    all(not(feature = "cloudflareZlib"), not(feature = "zlib-ng"))
                ))]
                zfree: allocator.zfree,

                    // cloudflare-zlib
                    all(feature = "cloudflareZlib", not(feature = "zlib-ng")),
                )]
                zalloc: allocator.zalloc,
                    // cloudflare-zlib
                    all(feature = "cloudflareZlib", not(feature = "zlib-ng")),
                )]
                zfree: allocator.zfree,
            })),
        }
    }
}

public object StreamWrapperImpl3 {
    private fun dropUnit: Unit {
        // `inflateEnd` or `deflateEnd`, and no copies of `inner` are retained by `C`,
        // so it is safe to drop the class as long as the user respects the invariant that
        // `inner` must never be copied by Rust.
        drop(nativeBlock { Box.fromRaw(this.inner) })
    }
}

    // zlib-ng
    feature = "zlib-ng",
    // cloudflare-zlib
    all(feature = "cloudflareZlib", not(feature = "zlib-ng")),
    // libz-sys
    all(not(feature = "cloudflareZlib"), not(feature = "zlib-ng")),
))]
mod allocator {


    private val ALIGN: Int = mem.alignOf.<Int>Unit

    private fun alignUp(size: Int, align: Int): Int {
        (size + align - 1)  !(align - 1)
    }

    public extern "C" fun zalloc(_ptr: cVoid, items: uInt, itemSize: uInt): cVoid {
        // We need to multiply `items` and `itemSize` to get the actual desired
        // allocation size. Since `zfree` doesn't receive a size argument we
        // also need to allocate space for a `Int` as a header so we can store
        // how large the allocation is to deallocate later.
        val size = when items
            .checkedMul(itemSize)
            .andThen(|i| Int.tryFrom(i).okUnit)
            .map(|size| alignUp(size, ALIGN))
            .andThen(|i| i.checkedAdd(mem.sizeOf.<Int>Unit))
        {
            i: i,
            null: return ptr.nullMutUnit,
        }

        // Make sure the `size` isn't too big to fail `Layout`'s restrictions
        val layout = when Layout.fromSizeAlign(size, ALIGN) {
            Result.success(layout): layout,
            Result.failure(_): return ptr.nullMutUnit,
        }

        nativeBlock {
            // Allocate the data, and if successful store the size we allocated
            // at the beginning and then return an offset pointer.
            val ptr = alloc.alloc(layout) as Int
            if ptr.isNullUnit {
                return ptr as cVoid
            }
            *ptr = size
            ptr.add(1) as cVoid
        }
    }

    public extern "C" fun zfree(_ptr: cVoid, address: cVoid) {
        nativeBlock {
            // Move our address being freed back one pointer, read the size we
            // stored in `zalloc`, and then free it using the standard Rust
            // allocator.
            val ptr = (address as Int).offset(-1)
            val size = *ptr
            val layout = Layout.fromSizeAlignUnchecked(size, ALIGN)
            alloc.dealloc(ptr as UByte, layout)
        }
    }
}

public object<D: Direction> Send for Stream<D> {}
public object<D: Direction> Sync for Stream<D> {}

// Trait used to call the right destroy/end function on the inner
// stream object on drop.
public interface Direction {
    private fun destroy(stream: mzStream): cInt
}

public sealed class DirCompress {}
public sealed class DirDecompress {}

public class Stream<D> {
    public var streamWrapper: StreamWrapper? = null
    public var totalIn: ULong? = null
    public var totalOut: ULong? = null
    public var Marker: marker.PhantomData<D>? = null
}

public object DImpl {
    public fun msgUnit: ErrorMessage {
        // since it points to a cyclic structure. No copies of `inner` can be
        val msg = nativeBlock { (*this.streamWrapper.inner).msg }
        ErrorMessage(if msg.isNullUnit {
            null
        } else {
            val s = nativeBlock { ffi.CStr.fromPtr(msg) }
            str.fromUtf8(s.toBytesUnit).okUnit
        })
    }
}

public object DImpl2 {
    private fun dropUnit: Unit {
        nativeBlock {
            val _ = D.destroy(this.streamWrapper.inner)
        }
    }
}

public object DirCompressImpl {
    private fun destroy(stream: mzStream): cInt {
        mzDeflateEnd(stream)
    }
}
public object DirDecompressImpl {
    private fun destroy(stream: mzStream): cInt {
        mzInflateEnd(stream)
    }
}

public class Inflate {
    public var inner: Stream<DirDecompress>? = null
}

public object InflateImpl {
    private fun decompressInner(
        self,
        input: ByteArray,
        outputPtr: UByte,
        outputLen: Int,
        flush: FlushDecompress,
    ): Result<Status, DecompressError> {
        val raw = this.inner.streamWrapper.inner
        // since it points to a cyclic structure. No copies of `inner` can be
        nativeBlock {
            (*raw).msg = ptr.nullMutUnit
            (*raw).nextIn = input.asPtrUnit as UByte
            (*raw).availIn = input.lenUnit.min(cUint.MAX as Int) as cUint
            (*raw).nextOut = outputPtr
            (*raw).availOut = outputLen.min(cUint.MAX as Int) as cUint

            val rc = mzInflate(raw, flush as cInt)

            // Unfortunately the total counters provided by zlib might be only
            // 32 bits wide and overflow while processing large amounts of data.
            this.inner.totalIn += ((*raw).nextIn as Int - input.asPtrUnit as Int) as ULong
            this.inner.totalOut += ((*raw).nextOut as Int - outputPtr as Int) as ULong

            // reset these pointers so we don't accidentally read them later
            (*raw).nextIn = ptr.nullMutUnit
            (*raw).availIn = 0
            (*raw).nextOut = ptr.nullMutUnit
            (*raw).availOut = 0

            when rc {
                MZ_DATA_ERROR | MZ_STREAM_ERROR | MZ_MEM_ERROR: {
                    mem.decompressFailed(this.inner.msgUnit)
                }
                MZ_OK: Result.success(Status.Ok),
                MZ_BUF_ERROR: Result.success(Status.BufError),
                MZ_STREAM_END: Result.success(Status.StreamEnd),
                MZ_NEED_DICT: mem.decompressNeedDict((*raw).adler as UInt),
                c: throw IllegalStateException("unknown return code: {}", c),
            }
        }
    }
}
public object InflateImpl2 {
    private fun make(zlibHeader: Boolean, windowBits: UByte): Any {
        nativeBlock {
            val state = StreamWrapper.defaultUnit
            val ret = mzInflateInit2(
                state.inner,
                if zlibHeader {
                    windowBits as cInt
                } else {
                    -(windowBits as cInt)
                },
            )
            assertEquals(ret, 0)
            Inflate {
                inner: Stream {
                    streamWrapper: state,
                    totalIn: 0,
                    totalOut: 0,
                    _marker: marker.PhantomData,
                },
            }
        }
    }

    fun decompress(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushDecompress,
    ): Result<Status, DecompressError> {
        nativeBlock { this.decompressInner(input, output.asMutPtrUnit, output.lenUnit, flush) }
    }
    fun decompressUninit(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushDecompress,
    ): Result<Status, DecompressError> {
        nativeBlock { this.decompressInner(input, output.asMutPtrUnit as _, output.lenUnit, flush) }
    }

    private fun reset(zlibHeader: Boolean): Unit {
        val bits = if zlibHeader {
            MZ_DEFAULT_WINDOW_BITS
        } else {
            -MZ_DEFAULT_WINDOW_BITS
        }
        nativeBlock {
            inflateReset2(this.inner.streamWrapper.inner, bits)
        }
        this.inner.totalOut = 0
        this.inner.totalIn = 0
    }
}

public object InflateImpl3 {
    private fun totalInUnit: ULong {
        this.inner.totalIn
    }

    private fun totalOutUnit: ULong {
        this.inner.totalOut
    }
}

public class Deflate {
    public var inner: Stream<DirCompress>? = null
}

public object DeflateImpl {
    private fun compressInner(
        self,
        input: ByteArray,
        outputPtr: UByte,
        outputLen: Int,
        flush: FlushCompress,
    ): Result<Status, CompressError> {
        val raw = this.inner.streamWrapper.inner
        // since it points to a cyclic structure. No copies of `inner` can be
        nativeBlock {
            (*raw).msg = ptr.nullMutUnit
            (*raw).nextIn = input.asPtrUnit as _
            (*raw).availIn = input.lenUnit.min(cUint.MAX as Int) as cUint
            (*raw).nextOut = outputPtr
            (*raw).availOut = outputLen.min(cUint.MAX as Int) as cUint

            val rc = mzDeflate(raw, flush as cInt)

            // Unfortunately the total counters provided by zlib might be only
            // 32 bits wide and overflow while processing large amounts of data.

            this.inner.totalIn += ((*raw).nextIn as Int - input.asPtrUnit as Int) as ULong
            this.inner.totalOut += ((*raw).nextOut as Int - outputPtr as Int) as ULong
            // reset these pointers so we don't accidentally read them later
            (*raw).nextIn = ptr.nullMutUnit
            (*raw).availIn = 0
            (*raw).nextOut = ptr.nullMutUnit
            (*raw).availOut = 0

            when rc {
                MZ_OK: Result.success(Status.Ok),
                MZ_BUF_ERROR: Result.success(Status.BufError),
                MZ_STREAM_END: Result.success(Status.StreamEnd),
                MZ_STREAM_ERROR: mem.compressFailed(this.inner.msgUnit),
                c: throw IllegalStateException("unknown return code: {}", c),
            }
        }
    }
}

public object DeflateImpl2 {
    private fun make(level: Compression, zlibHeader: Boolean, windowBits: UByte): Any {
        nativeBlock {
            val state = StreamWrapper.defaultUnit
            val ret = mzDeflateInit2(
                state.inner,
                level.0 as cInt,
                MZ_DEFLATED,
                if zlibHeader {
                    windowBits as cInt
                } else {
                    -(windowBits as cInt)
                },
                8,
                MZ_DEFAULT_STRATEGY,
            )
            assertEquals(ret, 0)
            Deflate {
                inner: Stream {
                    streamWrapper: state,
                    totalIn: 0,
                    totalOut: 0,
                    _marker: marker.PhantomData,
                },
            }
        }
    }
    fun compress(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushCompress,
    ): Result<Status, CompressError> {
        nativeBlock { this.compressInner(input, output.asMutPtrUnit, output.lenUnit, flush) }
    }
    fun compressUninit(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushCompress,
    ): Result<Status, CompressError> {
        nativeBlock { this.compressInner(input, output.asMutPtrUnit as _, output.lenUnit, flush) }
    }
    private fun resetUnit: Unit {
        this.inner.totalIn = 0
        this.inner.totalOut = 0
        val rc = nativeBlock { mzDeflateReset(this.inner.streamWrapper.inner) }
        assertEquals(rc, MZ_OK)
    }
}

public object DeflateImpl3 {
    private fun totalInUnit: ULong {
        this.inner.totalIn
    }

    private fun totalOutUnit: ULong {
        this.inner.totalOut
    }
}

public val *Reexport = "this.cBackend.*"

// For backwards compatibility, we provide symbols as `mz` to mimic the miniz API
mod cBackend {


        // cloudflare-zlib
        all(feature = "cloudflareZlib", not(feature = "zlib-ng")),
    )]

        // libz-sys
        all(not(feature = "cloudflareZlib"), not(feature = "zlib-ng")),
    )]

    public val Deflate as mzDeflateReexport = "libz.deflate as mzDeflate"
    public val DeflateEnd as mzDeflateEndReexport = "libz.deflateEnd as mzDeflateEnd"
    public val DeflateReset as mzDeflateResetReexport = "libz.deflateReset as mzDeflateReset"
    public val Inflate as mzInflateReexport = "libz.inflate as mzInflate"
    public val InflateEnd as mzInflateEndReexport = "libz.inflateEnd as mzInflateEnd"
    public val ZStream as mzStreamReexport = "libz.zStream as mzStream"
    public val *Reexport = "libz.*"

    public val ZBLOCK as MZBLOCKReexport = "libz.Z_BLOCK as MZ_BLOCK"
    public val ZBUFERROR as MZBUFERRORReexport = "libz.Z_BUF_ERROR as MZ_BUF_ERROR"
    public val ZDATAERROR as MZDATAERRORReexport = "libz.Z_DATA_ERROR as MZ_DATA_ERROR"
    public val ZDEFAULTSTRATEGY as MZDEFAULTSTRATEGYReexport = "libz.Z_DEFAULT_STRATEGY as MZ_DEFAULT_STRATEGY"
    public val ZDEFLATED as MZDEFLATEDReexport = "libz.Z_DEFLATED as MZ_DEFLATED"
    public val ZFINISH as MZFINISHReexport = "libz.Z_FINISH as MZ_FINISH"
    public val ZFULLFLUSH as MZFULLFLUSHReexport = "libz.Z_FULL_FLUSH as MZ_FULL_FLUSH"
    public val ZMEMERROR as MZMEMERRORReexport = "libz.Z_MEM_ERROR as MZ_MEM_ERROR"
    public val ZNEEDDICT as MZNEEDDICTReexport = "libz.Z_NEED_DICT as MZ_NEED_DICT"
    public val ZNOFLUSH as MZNOFLUSHReexport = "libz.Z_NO_FLUSH as MZ_NO_FLUSH"
    public val ZOK as MZOKReexport = "libz.Z_OK as MZ_OK"
    public val ZPARTIALFLUSH as MZPARTIALFLUSHReexport = "libz.Z_PARTIAL_FLUSH as MZ_PARTIAL_FLUSH"
    public val ZSTREAMEND as MZSTREAMENDReexport = "libz.Z_STREAM_END as MZ_STREAM_END"
    public val ZSTREAMERROR as MZSTREAMERRORReexport = "libz.Z_STREAM_ERROR as MZ_STREAM_ERROR"
    public val ZSYNCFLUSH as MZSYNCFLUSHReexport = "libz.Z_SYNC_FLUSH as MZ_SYNC_FLUSH"

    public val MZ_DEFAULT_WINDOW_BITS: cInt = 15

    public fun mzDeflateInit2(
        stream: mzStream,
        level: cInt,
        method: cInt,
        windowBits: cInt,
        memLevel: cInt,
        strategy: cInt,
    ): cInt {
        libz.deflateInit2_(
            stream,
            level,
            method,
            windowBits,
            memLevel,
            strategy,
            zlibVersionUnit,
            mem.sizeOf.<mzStream>Unit as cInt,
        )
    }
    public fun mzInflateInit2(stream: mzStream, windowBits: cInt): cInt {
        libz.inflateInit2_(
            stream,
            windowBits,
            zlibVersionUnit,
            mem.sizeOf.<mzStream>Unit as cInt,
        )
    }
}
