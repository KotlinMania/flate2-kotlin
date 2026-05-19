// port-lint: source zlib/read.rs
package io.github.kotlinmania.flate2.zlib.read

import io.github.kotlinmania.flate2.*
import kotlin.test.*



// A ZLIB encoder, or compressor.
// 
// This structure implements a [`Read`] interface. When read from, it reads
// uncompressed data from the underlying [`Read`] and provides the compressed data.
// 
// [`Read`]: https://doc.rust-lang.org/std/io/trait.Read.html
// 
// # Examples
// 
public class ZlibEncoder<R> {
    internal var inner: bufread.ZlibEncoder<BufReader<R>>? = null
}

public object RImpl {
    // Creates a new encoder which will read uncompressed data from the given
    // stream and emit the compressed stream.
    public fun new(r: R, level: io.github.kotlinmania.flate2.Compression): ZlibEncoder<R> {
        ZlibEncoder {
            inner: bufread.ZlibEncoder.new(BufReader.new(r), level),
        }
    }

    // Creates a new encoder with the given `compression` settings which will
    // read uncompressed data from the given stream `r` and emit the compressed stream.
    public fun newWithCompress(r: R, compression: io.github.kotlinmania.flate2.Compress): ZlibEncoder<R> {
        ZlibEncoder {
            inner: bufread.ZlibEncoder.newWithCompress(BufReader.new(r), compression),
        }
    }
}

public object RImpl2 {
    // Resets the state of this encoder entirely, swapping out the input
    // stream for another.
    // 
    // This function will reset the internal state of this encoder and replace
    // the input stream with the one provided, returning the previous input
    // stream. Future data read from this encoder will be the compressed
    // version of `r`'s data.
    // 
    // Note that there may be currently buffered data when this function is
    // called, and in that case the buffered data is discarded.
    public fun reset(r: R): R {
        super.bufread.resetEncoderData(this.inner)
        this.inner.getMutUnit.reset(r)
    }

    // Acquires a reference to the underlying stream
    public fun getRefUnit: R {
        this.inner.getRefUnit.getRefUnit
    }

    // Acquires a mutable reference to the underlying stream
    // 
    // Note that mutation of the stream may result in surprising results if
    // this encoder is continued to be used.
    public fun getMutUnit: R {
        this.inner.getMutUnit.getMutUnit
    }

    // Consumes this encoder, returning the underlying reader.
    // 
    // Note that there may be buffered bytes which are not re-acquired as part
    // of this transition. It's recommended to only call this function after
    // EOF has been reached.
    public fun intoInnerUnit: R {
        this.inner.intoInnerUnit.intoInnerUnit
    }

    // Returns the number of bytes that have been read into this compressor.
    // 
    // Note that not all bytes read from the underlying object may be accounted
    // for, there may still be some active buffering.
    public fun totalInUnit: ULong {
        this.inner.totalInUnit
    }

    // Returns the number of bytes that the compressor has produced.
    // 
    // Note that not all bytes may have been read yet, some may still be
    // buffered.
    public fun totalOutUnit: ULong {
        this.inner.totalOutUnit
    }
}

public object RImpl3 {
    private fun read(buf: ByteArray): Result<Int> {
        this.inner.read(buf)
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

// A ZLIB decoder, or decompressor.
// 
// This structure implements a [`Read`] interface. When read from, it reads
// compressed data from the underlying [`Read`] and provides the uncompressed data.
// 
// After reading a single member of the ZLIB data this reader will return
// Result.success(0) even if there are more bytes available in the underlying reader.
// `ZlibDecoder` may have read additional bytes past the end of the ZLIB data.
// If you need the following bytes, wrap the `Reader` in a `io.BufReader`
// 
// [`Read`]: https://doc.rust-lang.org/std/io/trait.Read.html
// 
// # Examples
// 
public class ZlibDecoder<R> {
    internal var inner: bufread.ZlibDecoder<BufReader<R>>? = null
}

public object RImpl4 {
    // Creates a new decoder which will decompress data read from the given
    // stream.
    public fun new(r: R): ZlibDecoder<R> {
        ZlibDecoder.newWithBuf(r, mutableListOf(0; 32 * 1024))
    }

    // Creates a new decoder which will decompress data read from the given
    // stream `r`, using `buf` as backing to speed up reading.
    // 
    // Note that the specified buffer will only be used up to its current
    // length. The buffer's capacity will also not grow over time.
    public fun newWithBuf(r: R, buf: ByteArray): ZlibDecoder<R> {
        ZlibDecoder {
            inner: bufread.ZlibDecoder.new(BufReader.withBuf(buf, r)),
        }
    }

    // Creates a new decoder which will decompress data read from the given
    // stream `r`, along with `decompression` settings.
    public fun newWithDecompress(r: R, decompression: Decompress): ZlibDecoder<R> {
        ZlibDecoder.newWithDecompressAndBuf(r, mutableListOf(0; 32 * 1024), decompression)
    }

    // Creates a new decoder which will decompress data read from the given
    // stream `r`, using `buf` as backing to speed up reading,
    // along with `decompression` settings to configure decoder.
    // 
    // Note that the specified buffer will only be used up to its current
    // length. The buffer's capacity will also not grow over time.
    public fun newWithDecompressAndBuf(
        r: R,
        buf: ByteArray,
        decompression: Decompress,
    ): ZlibDecoder<R> {
        ZlibDecoder {
            inner: bufread.ZlibDecoder.newWithDecompress(
                BufReader.withBuf(buf, r),
                decompression,
            ),
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
    // 
    // Note that there may be currently buffered data when this function is
    // called, and in that case the buffered data is discarded.
    public fun reset(r: R): R {
        super.bufread.resetDecoderData(this.inner)
        this.inner.getMutUnit.reset(r)
    }

    // Acquires a reference to the underlying stream
    public fun getRefUnit: R {
        this.inner.getRefUnit.getRefUnit
    }

    // Acquires a mutable reference to the underlying stream
    // 
    // Note that mutation of the stream may result in surprising results if
    // this decoder is continued to be used.
    public fun getMutUnit: R {
        this.inner.getMutUnit.getMutUnit
    }

    // Consumes this decoder, returning the underlying reader.
    // 
    // Note that there may be buffered bytes which are not re-acquired as part
    // of this transition. It's recommended to only call this function after
    // EOF has been reached.
    public fun intoInnerUnit: R {
        this.inner.intoInnerUnit.intoInnerUnit
    }

    // Returns the number of bytes that the decompressor has consumed.
    // 
    // Note that this will likely be smaller than what the decompressor
    // actually read from the underlying stream due to buffering.
    public fun totalInUnit: ULong {
        this.inner.totalInUnit
    }

    // Returns the number of bytes that the decompressor has produced.
    public fun totalOutUnit: ULong {
        this.inner.totalOutUnit
    }
}

public object RImpl6 {
    private fun read(into: ByteArray): Result<Int> {
        this.inner.read(into)
    }
}

public object RImpl7 {
    private fun write(buf: ByteArray): Result<Int> {
        this.getMutUnit.write(buf)
    }

    private fun flushUnit: Result<Unit> {
        this.getMutUnit.flushUnit
    }
}
