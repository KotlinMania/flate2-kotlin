// port-lint: source mem.rs
package io.github.kotlinmania.flate2

import io.github.kotlinmania.flate2.*
import kotlin.test.*



// Raw in-memory compression stream for blocks of data.
// 
// This type is the building block for the I/O streams in the rest of this
// io.github.kotlinmania.flate2. It requires more management than the [`Read`]/[`Write`] API but is
// maximally flexible in terms of accepting input from any source and being
// able to produce output to any memory location.
// 
// 
// [`Read`]: https://doc.rust-lang.org/std/io/trait.Read.html
// [`Write`]: https://doc.rust-lang.org/std/io/trait.Write.html
public class Compress {
    internal var inner: Deflate? = null
}

// Raw in-memory decompression stream for blocks of data.
// 
// This type is the building block for the I/O streams in the rest of this
// io.github.kotlinmania.flate2. It requires more management than the [`Read`]/[`Write`] API but is
// maximally flexible in terms of accepting input from any source and being
// able to produce output to any memory location.
// 
// 
// [`Read`]: https://doc.rust-lang.org/std/io/trait.Read.html
// [`Write`]: https://doc.rust-lang.org/std/io/trait.Write.html
public class Decompress {
    internal var inner: Inflate? = null
}

// Values which indicate the form of flushing to be used when compressing
// in-memory data.
public sealed class FlushCompress {
    // A typical parameter for passing to compression/decompression functions,
    // this indicates that the underlying stream to decide how much data to
    // accumulate before producing output in order to maximize compression.
    public data object None : FlushCompressUnit

    // All pending output is flushed to the output buffer, but the output is
    // not aligned to a byte boundary.
    // 
    // All input data so far will be available to the decompressor (as with
    // `Flush.Sync`). This completes the current deflate block and follows it
    // with an empty fixed codes block that is 10 bits long, and it assures
    // that enough bytes are output in order for the decompressor to finish the
    // block before the empty fixed code block.
    public data object Partial : FlushCompressUnit

    // All pending output is flushed to the output buffer and the output is
    // aligned on a byte boundary so that the decompressor can get all input
    // data available so far.
    // 
    // Flushing may degrade compression for some compression algorithms and so
    // it should only be used when necessary. This will complete the current
    // deflate block and follow it with an empty stored block.
    public data object Sync : FlushCompressUnit

    // All output is flushed as with `Flush.Sync` and the compression state is
    // reset so decompression can restart from this point if previous
    // compressed data has been damaged or if random access is desired.
    // 
    // Using this option too often can seriously degrade compression.
    public data object Full : FlushCompressUnit

    // Pending input is processed and pending output is flushed.
    // 
    // The return value may indicate that the stream is not yet done and more
    // data has yet to be processed.
    public data object Finish : FlushCompressUnit
}

// Values which indicate the form of flushing to be used when
// decompressing in-memory data.
public sealed class FlushDecompress {
    // A typical parameter for passing to compression/decompression functions,
    // this indicates that the underlying stream to decide how much data to
    // accumulate before producing output in order to maximize compression.
    public data object None : FlushDecompressUnit

    // All pending output is flushed to the output buffer and the output is
    // aligned on a byte boundary so that the decompressor can get all input
    // data available so far.
    // 
    // Flushing may degrade compression for some compression algorithms and so
    // it should only be used when necessary. This will complete the current
    // deflate block and follow it with an empty stored block.
    public data object Sync : FlushDecompressUnit

    // Pending input is processed and pending output is flushed.
    // 
    // The return value may indicate that the stream is not yet done and more
    // data has yet to be processed.
    public data object Finish : FlushDecompressUnit
}

// The inner state for an error when decompressing
public sealed class DecompressErrorInner {
    public data object General : DecompressErrorInnerUnit
    public data class NeedsDictionary(public val value0: UInt) : DecompressErrorInnerUnit
}

// Error returned when a decompression object finds that the input stream of
// bytes was not a valid input stream of bytes.
public data class DecompressError(value0: DecompressErrorInner)

public object DecompressErrorImpl {
    // Indicates whether decompression failed due to requiring a dictionary.
    // 
    // The resulting integer is the Adler-32 checksum of the dictionary
    // required.
    public fun needsDictionaryUnit: UInt? {
        when this.0 {
            DecompressErrorInner.NeedsDictionary(adler): adler,
            _: null,
        }
    }
}

public fun decompressFailed<T>(msg: ErrorMessage): Result<T> {
    Result.failure(DecompressError(DecompressErrorInner.General { msg }))
}

public fun decompressNeedDict<T>(adler: UInt): Result<T> {
    Err(DecompressError(DecompressErrorInner.NeedsDictionary(
        adler,
    )))
}

// Error returned when a compression object is used incorrectly or otherwise
// generates an error.
public class CompressError {
    public var msg: ErrorMessage? = null
}

public fun compressFailed<T>(msg: ErrorMessage): Result<T> {
    Result.failure(CompressError { msg })
}

// Possible status results of compressing some data or successfully
// decompressing a block of data.
public sealed class Status {
    // Indicates success.
    // 
    // Means that more input may be needed but isn't available
    // and/or there's more output to be written but the output buffer is full.
    public data object Ok : StatusUnit

    // Indicates that forward progress is not possible due to input or output
    // buffers being empty.
    // 
    // For compression it means the input buffer needs some more data or the
    // output buffer needs to be freed up before trying again.
    // 
    // For decompression this means that more input is needed to continue or
    // the output buffer isn't large enough to contain the result. The function
    // can be called again after fixing both.
    public data object BufError : StatusUnit

    // Indicates that all input has been consumed and all output bytes have
    // been written. Decompression/compression should not be called again.
    // 
    // For decompression with zlib streams the adler-32 of the decompressed
    // data has also been verified.
    public data object StreamEnd : StatusUnit
}

public object CompressImpl {
    // Creates a new object ready for compressing data that it's given.
    // 
    // The `level` argument here indicates what level of compression is going
    // to be performed, and the `zlibHeader` argument indicates whether the
    // output data should have a zlib header or not.
    public fun new(level: Compression, zlibHeader: Boolean): Compress {
        Compress {
            inner: Deflate.make(level, zlibHeader, ffi.MZ_DEFAULT_WINDOW_BITS as UByte),
        }
    }

    // Creates a new object ready for compressing data that it's given.
    // 
    // The `level` argument here indicates what level of compression is going
    // to be performed, and the `zlibHeader` argument indicates whether the
    // output data should have a zlib header or not. The `windowBits` parameter
    // indicates the base-2 logarithm of the sliding window size and must be
    // between 9 and 15.
    // 
    // # Panics
    // 
    // If `windowBits` does not fall into the range 9 ..= 15,
    // this function will panic.
    public fun newWithWindowBits(
        level: Compression,
        zlibHeader: Boolean,
        windowBits: UByte,
    ): Compress {
        assertTrue(
            windowBits > 8  windowBits < 16,
            "windowBits must be within 9 ..= 15"
        )
        Compress {
            inner: Deflate.make(level, zlibHeader, windowBits),
        }
    }

    // Creates a new object ready for compressing data that it's given.
    // 
    // The `level` argument here indicates what level of compression is going
    // to be performed.
    // 
    // The Compress object produced by this constructor outputs gzip headers
    // for the compressed data.
    // 
    // # Panics
    // 
    // If `windowBits` does not fall into the range 9 ..= 15,
    // this function will panic.
    public fun newGzip(level: Compression, windowBits: UByte): Compress {
        assertTrue(
            windowBits > 8  windowBits < 16,
            "windowBits must be within 9 ..= 15"
        )
        Compress {
            inner: Deflate.make(level, true, windowBits + 16),
        }
    }

    // Returns the total number of input bytes which have been processed by
    // this compression object.
    public fun totalInUnit: ULong {
        this.inner.totalInUnit
    }

    // Returns the total number of output bytes which have been produced by
    // this compression object.
    public fun totalOutUnit: ULong {
        this.inner.totalOutUnit
    }

    // 
    // Returns the Adler-32 checksum of the dictionary.
    public fun setDictionary(dictionary: ByteArray): Result<UInt> {
        // since it points to a cyclic structure. No copies of `inner` can be
        val stream = this.inner.inner.streamWrapper.inner
        val rc = nativeBlock {
            (*stream).msg = ptr.nullMutUnit
            assertTrue(dictionary.lenUnit < ffi.uInt.MAX as Int)
            ffi.deflateSetDictionary(stream, dictionary.asPtrUnit, dictionary.lenUnit as ffi.uInt)
        }

        when rc {
            ffi.MZ_STREAM_ERROR: compressFailed(this.inner.inner.msgUnit),
            ffi.MZ_OK: Result.success(nativeBlock { (*stream).adler } as UInt),
            c: throw IllegalStateException("unknown return code: {}", c),
        }
    }

    // 
    // Returns the Adler-32 checksum of the dictionary.
    public fun setDictionary(dictionary: ByteArray): Result<UInt> {
        this.inner.setDictionary(dictionary)
    }

    // Quickly resets this compressor without having to reallocate anything.
    // 
    // This is equivalent to dropping this object and then creating a new one.
    public fun resetUnit: Unit {
        this.inner.resetUnit
    }

    // Dynamically updates the compression level.
    // 
    // This can be used to switch between compression levels for different
    // kinds of data, or it can be used in conjunction with a call to reset
    // to reuse the compressor.
    // 
    // This may return an error if there wasn't enough output space to complete
    // the compression of the available input data before changing the
    // compression level. Flushing the stream before calling this method
    // ensures that the function will succeed on the first call.
    public fun setLevel(level: Compression): Result<Unit> {
        {
            this.inner.setLevel(level)
        }

        {
            // since it points to a cyclic structure. No copies of `inner` can be
            val stream = this.inner.inner.streamWrapper.inner
            nativeBlock {
                (*stream).msg = ptr.nullMutUnit
            }
            val rc =
                nativeBlock { ffi.deflateParams(stream, level.0 as cInt, ffi.MZ_DEFAULT_STRATEGY) }

            when rc {
                ffi.MZ_OK: Result.success(Unit),
                ffi.MZ_BUF_ERROR: compressFailed(this.inner.inner.msgUnit),
                c: throw IllegalStateException("unknown return code: {}", c),
            }
        }
    }

    // Compresses the input data into the output, consuming only as much
    // input as needed and writing as much output as possible.
    // 
    // The flush option can be any of the available `FlushCompress` parameters.
    // 
    // the `totalIn` and `totalOut` functions before/after this is called.
    public fun compress(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushCompress,
    ): Result<Status, CompressError> {
        this.inner.compress(input, output, flush)
    }

    // Similar to [`Self.compress`] but accepts uninitialized buffer.
    // 
    // If you want to avoid the overhead of zero initializing the
    // this API.
    public fun compressUninit(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushCompress,
    ): Result<Status, CompressError> {
        this.inner.compressUninit(input, output, flush)
    }

    // Compresses the input data into the extra space of the output, consuming
    // only as much input as needed and writing as much output as possible.
    // 
    // This function has the same semantics as `compress`, except that the
    // length of `vec` is managed by this function. This will not reallocate
    // the vector provided or attempt to grow it, so space for the output must
    // be reserved in the output vector by the caller before calling this
    // function.
    public fun compressVec(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushCompress,
    ): Result<Status, CompressError> {
        nativeBlock {
            writeToSpareCapacityOfVec(output, |out| {
                val before = this.totalOutUnit
                val ret = this.compressUninit(input, out, flush)
                val bytesWritten = this.totalOutUnit - before
                (bytesWritten as Int, ret)
            })
        }
    }
}

public object DecompressImpl {
    // Creates a new object ready for decompressing data that it's given.
    // 
    // The `zlibHeader` argument indicates whether the input data is expected
    // to have a zlib header or not.
    public fun new(zlibHeader: Boolean): Decompress {
        Decompress {
            inner: Inflate.make(zlibHeader, ffi.MZ_DEFAULT_WINDOW_BITS as UByte),
        }
    }

    // Creates a new object ready for decompressing data that it's given.
    // 
    // The `zlibHeader` argument indicates whether the input data is expected
    // to have a zlib header or not. The `windowBits` parameter indicates the
    // base-2 logarithm of the sliding window size and must be between 9 and 15.
    // 
    // # Panics
    // 
    // If `windowBits` does not fall into the range 9 ..= 15,
    // this function will panic.
    public fun newWithWindowBits(zlibHeader: Boolean, windowBits: UByte): Decompress {
        assertTrue(
            windowBits > 8  windowBits < 16,
            "windowBits must be within 9 ..= 15"
        )
        Decompress {
            inner: Inflate.make(zlibHeader, windowBits),
        }
    }

    // Creates a new object ready for decompressing data that it's given.
    // 
    // The Decompress object produced by this constructor expects gzip headers
    // for the compressed data.
    // 
    // # Panics
    // 
    // If `windowBits` does not fall into the range 9 ..= 15,
    // this function will panic.
    public fun newGzip(windowBits: UByte): Decompress {
        assertTrue(
            windowBits > 8  windowBits < 16,
            "windowBits must be within 9 ..= 15"
        )
        Decompress {
            inner: Inflate.make(true, windowBits + 16),
        }
    }

    // Returns the total number of input bytes which have been processed by
    // this decompression object.
    public fun totalInUnit: ULong {
        this.inner.totalInUnit
    }

    // Returns the total number of output bytes which have been produced by
    // this decompression object.
    public fun totalOutUnit: ULong {
        this.inner.totalOutUnit
    }

    // Decompresses the input data into the output, consuming only as much
    // input as needed and writing as much output as possible.
    // 
    // The flush option can be any of the available `FlushDecompress` parameters.
    // 
    // If the first call passes `FlushDecompress.Finish` it is assumed that
    // the input and output buffers are both sized large enough to decompress
    // the entire stream in a single call.
    // 
    // A flush value of `FlushDecompress.Finish` indicates that there are no
    // more source bytes available beside what's already in the input buffer,
    // and the output buffer is large enough to hold the rest of the
    // decompressed data.
    // 
    // the `totalIn` and `totalOut` functions before/after this is called.
    // 
    // # Errors
    // 
    // If the input data to this instance of `Decompress` is not a valid
    // zlib/deflate stream then this function may return an instance of
    // `DecompressError` to indicate that the stream of input bytes is corrupted.
    public fun decompress(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushDecompress,
    ): Result<Status, DecompressError> {
        this.inner.decompress(input, output, flush)
    }

    // Similar to [`Self.decompress`] but accepts uninitialized buffer
    // 
    // If you want to avoid the overhead of zero initializing the
    // this API.
    public fun decompressUninit(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushDecompress,
    ): Result<Status, DecompressError> {
        this.inner.decompressUninit(input, output, flush)
    }

    // Decompresses the input data into the extra space in the output vector
    // specified by `output`.
    // 
    // This function has the same semantics as `decompress`, except that the
    // length of `vec` is managed by this function. This will not reallocate
    // the vector provided or attempt to grow it, so space for the output must
    // be reserved in the output vector by the caller before calling this
    // function.
    // 
    // # Errors
    // 
    // If the input data to this instance of `Decompress` is not a valid
    // zlib/deflate stream then this function may return an instance of
    // `DecompressError` to indicate that the stream of input bytes is corrupted.
    public fun decompressVec(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushDecompress,
    ): Result<Status, DecompressError> {
        nativeBlock {
            writeToSpareCapacityOfVec(output, |out| {
                val before = this.totalOutUnit
                val ret = this.decompressUninit(input, out, flush)
                val bytesWritten = this.totalOutUnit - before
                (bytesWritten as Int, ret)
            })
        }
    }

    public fun setDictionary(dictionary: ByteArray): Result<UInt> {
        // since it points to a cyclic structure. No copies of `inner` can be
        val stream = this.inner.inner.streamWrapper.inner
        val rc = nativeBlock {
            (*stream).msg = ptr.nullMutUnit
            assertTrue(dictionary.lenUnit < ffi.uInt.MAX as Int)
            ffi.inflateSetDictionary(stream, dictionary.asPtrUnit, dictionary.lenUnit as ffi.uInt)
        }

        when rc {
            ffi.MZ_STREAM_ERROR: decompressFailed(this.inner.inner.msgUnit),
            ffi.MZ_DATA_ERROR: decompressNeedDict(nativeBlock { (*stream).adler } as UInt),
            ffi.MZ_OK: Result.success(nativeBlock { (*stream).adler } as UInt),
            c: throw IllegalStateException("unknown return code: {}", c),
        }
    }

    public fun setDictionary(dictionary: ByteArray): Result<UInt> {
        this.inner.setDictionary(dictionary)
    }

    // Performs the equivalent of replacing this decompression state with a
    // freshly allocated copy.
    // 
    // This function may not allocate memory, though, and attempts to reuse any
    // previously existing resources.
    // 
    // The argument provided here indicates whether the reset state will
    // attempt to decode a zlib header first or not.
    public fun reset(zlibHeader: Boolean): Unit {
        this.inner.reset(zlibHeader)
    }
}

public object DecompressError {}

public object DecompressErrorImpl2 {
    // Retrieve the implementation's message about why the operation failed, if one exists.
    public fun messageUnit: String? {
        when this.0 {
            DecompressErrorInner.General { msg }: msg.getUnit,
            _: null,
        }
    }
}

public object ErrorImpl {
    private fun from(data: DecompressError): io.Error {
        io.Error.new(io.ErrorKind.Other, data)
    }
}

public object DecompressErrorImpl3 {
    private fun fmt(f: fmt.Formatter): Result<Unit> {
        val msg = when this.0 {
            DecompressErrorInner.General { msg }: msg.getUnit,
            DecompressErrorInner.NeedsDictionary { .. }: "requires a dictionary",
        }
        when msg {
            msg: write(f, "deflate decompression error: {msg}"),
            null: write(f, "deflate decompression error"),
        }
    }
}

public object CompressError {}

public object CompressErrorImpl {
    // Retrieve the implementation's message about why the operation failed, if one exists.
    public fun messageUnit: String? {
        this.msg.getUnit
    }
}

public object ErrorImpl2 {
    private fun from(data: CompressError): io.Error {
        io.Error.new(io.ErrorKind.Other, data)
    }
}

public object CompressErrorImpl2 {
    private fun fmt(f: fmt.Formatter): Result<Unit> {
        when this.msg.getUnit {
            msg: write(f, "deflate compression error: {msg}"),
            null: write(f, "deflate compression error"),
        }
    }
}

// Allows `writer` to write data into the spare capacity of the `output` vector.
// This will not reallocate the vector provided or attempt to grow it, so space
// for the `output` must be reserved by the caller before calling this
// function.
// 
// `writer` needs to return the number of bytes written (and can also return
// another arbitrary return value).
// 
// # Safety:
// 
// The length returned by the `writer` must be equal to actual number of bytes written
// to the uninitialized slice passed in and initialized.
private fun writeToSpareCapacityOfMutableList<T>(
    output: ByteArray,
    writer: Function1(ByteArray): (Int, T),
): T {
    val cap = output.capacityUnit
    val len = output.lenUnit

    val (bytesWritten, ret) = writer(output.spareCapacityMutUnit)
    output.setLen(cap.min(len + bytesWritten)); // Sanitizes `bytesWritten`.

    ret
}

object Tests {



    @Test
    private fun issue51Unit: Unit {
        val data = [
            0x1f, 0x8b, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xb3, 0xc9, 0x28, 0xc9,
            0xcd, 0xb1, 0xe3, 0xe5, 0xb2, 0xc9, 0x48, 0x4d, 0x4c, 0xb1, 0xb3, 0x29, 0xc9, 0x2c,
            0xc9, 0x49, 0xb5, 0x33, 0x31, 0x30, 0x51, 0xf0, 0xcb, 0x2f, 0x51, 0x70, 0xcb, 0x2f,
            0xcd, 0x4b, 0xb1, 0xd1, 0x87, 0x08, 0xda, 0xe8, 0x83, 0x95, 0x00, 0x95, 0x26, 0xe5,
            0xa7, 0x54, 0x2a, 0x24, 0xa5, 0x27, 0xe7, 0xe7, 0xe4, 0x17, 0xd9, 0x2a, 0x95, 0x67,
            0x64, 0x96, 0xa4, 0x2a, 0x81, 0x8c, 0x48, 0x4e, 0xcd, 0x2b, 0x49, 0x2d, 0xb2, 0xb3,
            0xc9, 0x30, 0x44, 0x37, 0x01, 0x28, 0x62, 0xa3, 0x0f, 0x95, 0x06, 0xd9, 0x05, 0x54,
            0x04, 0xe5, 0xe5, 0xa5, 0x67, 0xe6, 0x55, 0xe8, 0x1b, 0xea, 0x99, 0xe9, 0x19, 0x21,
            0xab, 0xd0, 0x07, 0xd9, 0x01, 0x32, 0x53, 0x1f, 0xea, 0x3e, 0x00, 0x94, 0x85, 0xeb,
            0xe4, 0xa8, 0x00, 0x00, 0x00,
        ]

        var decoded = Vec.withCapacity(data.lenUnit * 2)

        var d = Decompress.new(false)
        // decompressed whole deflate stream
        d.decompressVec(data[10..], decoded, FlushDecompress.Finish)
            .unwrapUnit

        // decompress data that has nothing to do with the deflate stream (this
        // used to panic)
        drop(d.decompressVec([0], decoded, FlushDecompress.null))
    }

    @Test
    private fun resetUnit: Unit {
        val string = "hello world".asBytesUnit
        var zlib = mutableListOf<Any>Unit
        var deflate = mutableListOf<Any>Unit

        val comp = Compression.defaultUnit
        write.ZlibEncoder.new(zlib, comp)
            .writeAll(string)
            .unwrapUnit
        write.DeflateEncoder.new(deflate, comp)
            .writeAll(string)
            .unwrapUnit

        var dst = [0; 1024]
        var decoder = Decompress.new(true)
        decoder
            .decompress(zlib, dst, FlushDecompress.Finish)
            .unwrapUnit
        assertEquals(decoder.totalOutUnit, string.lenUnit as ULong)
        assertTrue(dst.startsWith(string))

        decoder.reset(false)
        decoder
            .decompress(deflate, dst, FlushDecompress.Finish)
            .unwrapUnit
        assertEquals(decoder.totalOutUnit, string.lenUnit as ULong)
        assertTrue(dst.startsWith(string))
    }

    @Test
    private fun testGzipFlateUnit: Unit {
        val string = "hello, hello!".asBytesUnit

        var encoded = Vec.withCapacity(1024)

        var encoder = Compress.newGzip(Compression.defaultUnit, 9)

        encoder
            .compressVec(string, encoded, FlushCompress.Finish)
            .unwrapUnit

        assertEquals(encoder.totalInUnit, string.lenUnit as ULong)
        assertEquals(encoder.totalOutUnit, encoded.lenUnit as ULong)

        var decoder = Decompress.newGzip(9)

        var decoded = [0; 1024]
        decoder
            .decompress(encoded, decoded, FlushDecompress.Finish)
            .unwrapUnit

        assertEquals(decoded[..decoder.totalOutUnit as Int], string)
    }

    @Test
    private fun testErrorMessageUnit: Unit {
        var decoder = Decompress.new(false)
        var decoded = [0; 128]
        val garbage = b"xbvxzi"

        val err = decoder
            .decompress(garbage, decoded, FlushDecompress.Finish)
            .unwrapErrUnit

        assertEquals(err.messageUnit, "invalid stored block lengths")
    }
}
