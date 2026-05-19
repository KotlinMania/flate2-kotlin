// port-lint: source deflate/write.rs
package io.github.kotlinmania.flate2.deflate.write

import io.github.kotlinmania.flate2.*
import kotlin.test.*



// A DEFLATE encoder, or compressor.
// 
// This structure implements a [`Write`] interface and takes a stream of
// uncompressed data, writing the compressed data to the wrapped writer.
// 
// [`Write`]: https://doc.rust-lang.org/std/io/trait.Write.html
// 
// # Examples
// 
public class DeflateEncoder<W> {
    inner: zio.Writer<W, Compress>,
}

public object WImpl {
    // Creates a new encoder which will write compressed data to the stream
    // given at the given compression level.
    // 
    // When this encoder is dropped or unwrapped the final pieces of data will
    // be flushed.
    public fun new(w: W, level: io.github.kotlinmania.flate2.Compression): DeflateEncoder<W> {
        DeflateEncoder {
            inner: zio.Writer.new(w, Compress.new(level, false)),
        }
    }

    // Acquires a reference to the underlying writer.
    public fun getRefUnit: W {
        this.inner.getRefUnit
    }

    // Acquires a mutable reference to the underlying writer.
    // 
    // Note that mutating the output/input state of the stream may corrupt this
    // object, so care must be taken when using this method.
    public fun getMutUnit: W {
        this.inner.getMutUnit
    }

    // Resets the state of this encoder entirely, swapping out the output
    // stream for another.
    // 
    // This function will finish encoding the current stream into the current
    // output stream before swapping out the two output streams. If the stream
    // cannot be finished an error is returned.
    // 
    // After the current stream has been finished, this will reset the internal
    // state of this encoder and replace the output stream with the one
    // provided, returning the previous output stream. Future data written to
    // this encoder will be the compressed into the stream `w` provided.
    // 
    // # Errors
    // 
    // This function will perform I/O to complete this stream, and any I/O
    // errors which occur will be returned from this function.
    public fun reset(w: W): Result<W> {
        this.inner.finishUnit
        this.inner.data.resetUnit
        Result.success(this.inner.replace(w))
    }

    // Attempt to finish this output stream, writing out final chunks of data.
    // 
    // Note that this function can only be used once data has finished being
    // written to the output stream. After this function is called then further
    // calls to `write` may result in a panic.
    // 
    // # Panics
    // 
    // Attempts to write data to this stream may result in a panic after this
    // function is called.
    // 
    // # Errors
    // 
    // This function will perform I/O to complete this stream, and any I/O
    // errors which occur will be returned from this function.
    public fun tryFinishUnit: Result<Unit> {
        this.inner.finishUnit
    }

    // Consumes this encoder, flushing the output stream.
    // 
    // This will flush the underlying data stream, close off the compressed
    // stream and, if successful, return the contained writer.
    // 
    // Note that this function may not be suitable to call in a situation where
    // the underlying stream is an asynchronous I/O stream. To finish a stream
    // the `tryFinish` (or `shutdown`) method should be used instead. To
    // re-acquire ownership of a stream it is safe to call this method after
    // `tryFinish` or `shutdown` has returned `Ok`.
    // 
    // # Errors
    // 
    // This function will perform I/O to complete this stream, and any I/O
    // errors which occur will be returned from this function.
    public fun finishUnit: Result<W> {
        this.inner.finishUnit
        Result.success(this.inner.takeInnerUnit)
    }

    // Consumes this encoder, flushing the output stream.
    // 
    // This will flush the underlying data stream and then return the contained
    // writer if the flush succeeded.
    // The compressed stream will not closed but only flushed. This
    // means that obtained byte array can by extended by another deflated
    // stream. To close the stream add the two bytes 0x3 and 0x0.
    // 
    // # Errors
    // 
    // This function will perform I/O to complete this stream, and any I/O
    // errors which occur will be returned from this function.
    public fun flushFinishUnit: Result<W> {
        this.inner.flushUnit
        Result.success(this.inner.takeInnerUnit)
    }

    // Returns the number of bytes that have been written to this compressor.
    // 
    // Note that not all bytes written to this object may be accounted for,
    // there may still be some active buffering.
    public fun totalInUnit: ULong {
        this.inner.data.totalInUnit
    }

    // Returns the number of bytes that the compressor has produced.
    // 
    // Note that not all bytes may have been written yet, some may still be
    // buffered.
    public fun totalOutUnit: ULong {
        this.inner.data.totalOutUnit
    }
}

public object WImpl2 {
    private fun write(buf: ByteArray): Result<Int> {
        this.inner.write(buf)
    }

    private fun flushUnit: Result<Unit> {
        this.inner.flushUnit
    }
}

public object WImpl3 {
    private fun read(buf: ByteArray): Result<Int> {
        this.inner.getMutUnit.read(buf)
    }
}

// A DEFLATE decoder, or decompressor.
// 
// This structure implements a [`Write`] and will emit a stream of decompressed
// data when fed a stream of compressed data.
// 
// After decoding a single member of the DEFLATE data this writer will return the number of bytes up to
// to the end of the DEFLATE member and subsequent writes will return Result.success(0) allowing the caller to
// handle any data following the DEFLATE member.
// 
// [`Write`]: https://doc.rust-lang.org/std/io/trait.Read.html
// 
// # Examples
// 
public class DeflateDecoder<W> {
    inner: zio.Writer<W, Decompress>,
}

public object WImpl4 {
    // Creates a new decoder which will write uncompressed data to the stream.
    // 
    // When this encoder is dropped or unwrapped the final pieces of data will
    // be flushed.
    public fun new(w: W): DeflateDecoder<W> {
        DeflateDecoder {
            inner: zio.Writer.new(w, Decompress.new(false)),
        }
    }

    // Acquires a reference to the underlying writer.
    public fun getRefUnit: W {
        this.inner.getRefUnit
    }

    // Acquires a mutable reference to the underlying writer.
    // 
    // Note that mutating the output/input state of the stream may corrupt this
    // object, so care must be taken when using this method.
    public fun getMutUnit: W {
        this.inner.getMutUnit
    }

    // Resets the state of this decoder entirely, swapping out the output
    // stream for another.
    // 
    // This function will finish encoding the current stream into the current
    // output stream before swapping out the two output streams.
    // 
    // This will then reset the internal state of this decoder and replace the
    // output stream with the one provided, returning the previous output
    // stream. Future data written to this decoder will be decompressed into
    // the output stream `w`.
    // 
    // # Errors
    // 
    // This function will perform I/O to finish the stream, and if that I/O
    // returns an error then that will be returned from this function.
    public fun reset(w: W): Result<W> {
        this.inner.finishUnit
        this.inner.data = Decompress.new(false)
        Result.success(this.inner.replace(w))
    }

    // Attempt to finish this output stream, writing out final chunks of data.
    // 
    // Note that this function can only be used once data has finished being
    // written to the output stream. After this function is called then further
    // calls to `write` may result in a panic.
    // 
    // # Panics
    // 
    // Attempts to write data to this stream may result in a panic after this
    // function is called.
    // 
    // # Errors
    // 
    // This function will perform I/O to finish the stream, returning any
    // errors which happen.
    public fun tryFinishUnit: Result<Unit> {
        this.inner.finishUnit
    }

    // Consumes this encoder, flushing the output stream.
    // 
    // This will flush the underlying data stream and then return the contained
    // writer if the flush succeeded.
    // 
    // Note that this function may not be suitable to call in a situation where
    // the underlying stream is an asynchronous I/O stream. To finish a stream
    // the `tryFinish` (or `shutdown`) method should be used instead. To
    // re-acquire ownership of a stream it is safe to call this method after
    // `tryFinish` or `shutdown` has returned `Ok`.
    // 
    // # Errors
    // 
    // This function will perform I/O to complete this stream, and any I/O
    // errors which occur will be returned from this function.
    public fun finishUnit: Result<W> {
        this.inner.finishUnit
        Result.success(this.inner.takeInnerUnit)
    }

    // Returns the number of bytes that the decompressor has consumed for
    // decompression.
    // 
    // Note that this will likely be smaller than the number of bytes
    // successfully written to this stream due to internal buffering.
    public fun totalInUnit: ULong {
        this.inner.data.totalInUnit
    }

    // Returns the number of bytes that the decompressor has written to its
    // output stream.
    public fun totalOutUnit: ULong {
        this.inner.data.totalOutUnit
    }
}

public object WImpl5 {
    private fun write(buf: ByteArray): Result<Int> {
        this.inner.write(buf)
    }

    private fun flushUnit: Result<Unit> {
        this.inner.flushUnit
    }
}

public object WImpl6 {
    private fun read(buf: ByteArray): Result<Int> {
        this.inner.getMutUnit.read(buf)
    }
}

object Tests {

    const STR: str = "Hello World Hello World Hello World Hello World Hello World \
        Hello World Hello World Hello World Hello World Hello World \
        Hello World Hello World Hello World Hello World Hello World \
        Hello World Hello World Hello World Hello World Hello World \
        Hello World Hello World Hello World Hello World Hello World"

    // DeflateDecoder consumes one zlib archive and then returns 0 for subsequent writes, allowing any
    // additional data to be consumed by the caller.
    @Test
    private fun decodeExtraDataUnit: Unit {
        val compressed = {
            var e = DeflateEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
            e.writeAll(STR.asRefUnit).unwrapUnit
            var b = e.finishUnit.unwrapUnit
            b.push(b'x')
            b
        }

        var writer = mutableListOf<Any>Unit
        var decoder = DeflateDecoder.new(writer)
        var consumedBytes = 0
        loop {
            val n = decoder.write(compressed[consumedBytes..]).unwrapUnit
            if n == 0 {
                break
            }
            consumedBytes += n
        }
        writer = decoder.finishUnit.unwrapUnit
        val actual = String.fromUtf8(writer).expect("String parsing error")
        assertEquals(actual, STR)
        assertEquals(compressed[consumedBytes..], b"x")
    }
}
