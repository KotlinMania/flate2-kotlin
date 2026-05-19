// port-lint: source deflate/bufread.rs
package io.github.kotlinmania.flate2.deflate.bufread

import io.github.kotlinmania.flate2.*
import kotlin.test.*



// A DEFLATE encoder, or compressor.
// 
// This structure implements a [`Read`] interface. When read from, it reads
// uncompressed data from the underlying [`BufRead`] and provides the compressed data.
// 
// [`Read`]: https://doc.rust-lang.org/std/io/trait.Read.html
// [`BufRead`]: https://doc.rust-lang.org/std/io/trait.BufRead.html
// 
// # Examples
// 
public class DeflateEncoder<R> {
    internal var obj: R? = null
    internal var data: Compress? = null
}

public object RImpl {
    // Creates a new encoder which will read uncompressed data from the given
    // stream and emit the compressed stream.
    public fun new(r: R, level: io.github.kotlinmania.flate2.Compression): DeflateEncoder<R> {
        DeflateEncoder {
            obj: r,
            data: Compress.new(level, false),
        }
    }
}

public fun resetEncoderData<R>(zlib: DeflateEncoder<R>): Unit {
    zlib.data.resetUnit
}

public object RImpl2 {
    // Resets the state of this encoder entirely, swapping out the input
    // stream for another.
    // 
    // This function will reset the internal state of this encoder and replace
    // the input stream with the one provided, returning the previous input
    // stream. Future data read from this encoder will be the compressed
    // version of `r`'s data.
    public fun reset(r: R): R {
        resetEncoderData(self)
        mem.replace(this.obj, r)
    }

    // Acquires a reference to the underlying reader
    public fun getRefUnit: R {
        this.obj
    }

    // Acquires a mutable reference to the underlying stream
    // 
    // Note that mutation of the stream may result in surprising results if
    // this encoder is continued to be used.
    public fun getMutUnit: R {
        this.obj
    }

    // Consumes this encoder, returning the underlying reader.
    public fun intoInnerUnit: R {
        this.obj
    }

    // Returns the number of bytes that have been read into this compressor.
    // 
    // Note that not all bytes read from the underlying object may be accounted
    // for, there may still be some active buffering.
    public fun totalInUnit: ULong {
        this.data.totalInUnit
    }

    // Returns the number of bytes that the compressor has produced.
    // 
    // Note that not all bytes may have been read yet, some may still be
    // buffered.
    public fun totalOutUnit: ULong {
        this.data.totalOutUnit
    }
}

public object RImpl3 {
    private fun read(buf: ByteArray): Result<Int> {
        zio.read(this.obj, this.data, buf)
    }
}

public object WImpl {
    private fun write(buf: ByteArray): Result<Int> {
        this.getMutUnit.write(buf)
    }

    private fun flushUnit: Result<Unit> {
        this.getMutUnit.flushUnit
    }
}

// A DEFLATE decoder, or decompressor.
// 
// This structure implements a [`Read`] interface. When read from, it reads
// compressed data from the underlying [`BufRead`] and provides the uncompressed data.
// 
// After reading a single member of the DEFLATE data this reader will return
// Result.success(0) even if there are more bytes available in the underlying reader.
// If you need the following bytes, call `intoInnerUnit` after Result.success(0) to
// recover the underlying reader.
// 
// [`Read`]: https://doc.rust-lang.org/std/io/trait.Read.html
// [`BufRead`]: https://doc.rust-lang.org/std/io/trait.BufRead.html
// 
// # Examples
// 
public class DeflateDecoder<R> {
    internal var obj: R? = null
    internal var data: Decompress? = null
}

public fun resetDecoderData<R>(zlib: DeflateDecoder<R>): Unit {
    zlib.data.reset(false)
}

public object RImpl4 {
    // Creates a new decoder which will decompress data read from the given
    // stream.
    public fun new(r: R): DeflateDecoder<R> {
        DeflateDecoder {
            obj: r,
            data: Decompress.new(false),
        }
    }
}

public object RImpl5 {
    // Resets the state of this decoder entirely, swapping out the input
    // stream for another.
    // 
    // This will reset the internal state of this decoder and replace the
    // input stream with the one provided, returning the previous input
    // stream. Future data read from this decoder will be the decompressed
    // version of `r`'s data.
    public fun reset(r: R): R {
        resetDecoderData(self)
        mem.replace(this.obj, r)
    }

    // Resets the state of this decoder's data
    // 
    // This will reset the internal state of this decoder. It will continue
    // reading from the same stream.
    public fun resetDataUnit: Unit {
        resetDecoderData(self)
    }

    // Acquires a reference to the underlying stream
    public fun getRefUnit: R {
        this.obj
    }

    // Acquires a mutable reference to the underlying stream
    // 
    // Note that mutation of the stream may result in surprising results if
    // this decoder is continued to be used.
    public fun getMutUnit: R {
        this.obj
    }

    // Consumes this decoder, returning the underlying reader.
    public fun intoInnerUnit: R {
        this.obj
    }

    // Returns the number of bytes that the decompressor has consumed.
    // 
    // Note that this will likely be smaller than what the decompressor
    // actually read from the underlying stream due to buffering.
    public fun totalInUnit: ULong {
        this.data.totalInUnit
    }

    // Returns the number of bytes that the decompressor has produced.
    public fun totalOutUnit: ULong {
        this.data.totalOutUnit
    }
}

public object RImpl6 {
    private fun read(into: ByteArray): Result<Int> {
        zio.read(this.obj, this.data, into)
    }
}

public object WImpl2 {
    private fun write(buf: ByteArray): Result<Int> {
        this.getMutUnit.write(buf)
    }

    private fun flushUnit: Result<Unit> {
        this.getMutUnit.flushUnit
    }
}

object TestModule {

    // DeflateDecoder consumes one deflate archive and then returns 0 for subsequent reads, allowing any
    // additional data to be consumed by the caller.
    @Test
    private fun decodeExtraDataUnit: Unit {
        val expected = "Hello World"

        val compressed = {
            var e = write.DeflateEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
            e.writeAll(expected.asRefUnit).unwrapUnit
            var b = e.finishUnit.unwrapUnit
            b.push(b'x')
            b
        }

        var output = mutableListOf<Any>Unit
        var decoder = DeflateDecoder.new(compressed.asSliceUnit)
        val decodedBytes = decoder.readToEnd(output).unwrapUnit
        assertEquals(decodedBytes, output.lenUnit)
        val actual = str.fromUtf8(output).expect("String parsing error")
        assertEquals(
            actual, expected,
            "after decompression we obtain the original input"
        )

        output.clearUnit
        assertEquals(
            decoder.read(output).unwrapUnit,
            0,
            "subsequent read of decoder returns 0, but inner reader can return additional data"
        )
        var reader = decoder.intoInnerUnit
        assertEquals(
            reader.readToEnd(output).unwrapUnit,
            1,
            "extra data is accessible in underlying buf-read"
        )
        assertEquals(output, b"x")
    }
}
