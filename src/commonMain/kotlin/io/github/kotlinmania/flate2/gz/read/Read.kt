// port-lint: source gz/read.rs
package io.github.kotlinmania.flate2.gz.read

import io.github.kotlinmania.flate2.*
import kotlin.test.*



// A gzip streaming encoder
// 
// This structure implements a [`Read`] interface. When read from, it reads
// uncompressed data from the underlying [`Read`] and provides the compressed data.
// 
// [`Read`]: https://doc.rust-lang.org/std/io/trait.Read.html
// 
// # Examples
// 
public class GzEncoder<R> {
    internal var inner: bufread.GzEncoder<BufReader<R>>? = null
}

public fun gzEncoder<R>(inner: bufread.GzEncoder<BufReader<R>>): GzEncoder<R> {
    GzEncoder { inner }
}

public object RImpl {
    // 
    // The encoder is not configured specially for the emitted header. For
    // header configuration, see the `GzBuilder` type.
    // 
    // The data read from the stream `r` will be compressed and available
    // through the returned reader.
    public fun new(r: R, level: Compression): GzEncoder<R> {
        GzBuilder.newUnit.read(r, level)
    }
}

public object RImpl2 {
    // Acquires a reference to the underlying reader.
    public fun getRefUnit: R {
        this.inner.getRefUnit.getRefUnit
    }

    // Acquires a mutable reference to the underlying reader.
    // 
    // Note that mutation of the reader may result in surprising results if
    // this encoder is continued to be used.
    public fun getMutUnit: R {
        this.inner.getMutUnit.getMutUnit
    }

    // Returns the underlying stream, consuming this encoder
    public fun intoInnerUnit: R {
        this.inner.intoInnerUnit.intoInnerUnit
    }
}

public object RImpl3 {
    private fun read(into: ByteArray): Result<Int> {
        this.inner.read(into)
    }
}

public object RImpl4 {
    private fun write(buf: ByteArray): Result<Int> {
        this.getMutUnit.write(buf)
    }

    private fun flushUnit: Result<Unit> {
        this.getMutUnit.flushUnit
    }
}

// A decoder for a single member of a [gzip file].
// 
// This structure implements a [`Read`] interface. When read from, it reads
// compressed data from the underlying [`Read`] and provides the uncompressed data.
// 
// After reading a single member of the gzip data this reader will return
// Result.success(0) even if there are more bytes available in the underlying reader.
// `GzDecoder` may have read additional bytes past the end of the gzip data.
// If you need the following bytes, wrap the `Reader` in a `io.BufReader`
// 
// To handle gzip files that may have multiple members, see [`MultiGzDecoder`]
// or read more
// [in the introduction](../index.html#about-multi-member-gzip-files).
// 
// [gzip file]: https://www.rfc-editor.org/rfc/rfc1952#page-5
// 
// # Examples
// 
public class GzDecoder<R> {
    internal var inner: bufread.GzDecoder<BufReader<R>>? = null
}

public object RImpl5 {
    // Creates a new decoder from the given reader, immediately parsing the
    // gzip header.
    public fun new(r: R): GzDecoder<R> {
        GzDecoder {
            inner: bufread.GzDecoder.new(BufReader.new(r)),
        }
    }
}

public object RImpl6 {
    // Returns the header associated with this stream, if it was valid.
    public fun headerUnit: GzHeader? {
        this.inner.headerUnit
    }

    // Acquires a reference to the underlying reader.
    // 
    // Note that the decoder may have read past the end of the gzip data.
    public fun getRefUnit: R {
        this.inner.getRefUnit.getRefUnit
    }

    // Acquires a mutable reference to the underlying stream.
    // 
    // Note that mutation of the stream may result in surprising results if
    // this decoder continues to be used.
    // 
    // Note that the decoder may have read past the end of the gzip data.
    public fun getMutUnit: R {
        this.inner.getMutUnit.getMutUnit
    }

    // Consumes this decoder, returning the underlying reader.
    // 
    // Note that the decoder may have read past the end of the gzip data.
    // [`bufread.GzDecoder`] instead.
    public fun intoInnerUnit: R {
        this.inner.intoInnerUnit.intoInnerUnit
    }
}

public object RImpl7 {
    private fun read(into: ByteArray): Result<Int> {
        this.inner.read(into)
    }
}

public object RImpl8 {
    private fun write(buf: ByteArray): Result<Int> {
        this.getMutUnit.write(buf)
    }

    private fun flushUnit: Result<Unit> {
        this.getMutUnit.flushUnit
    }
}

// A gzip streaming decoder that decodes a [gzip file] that may have multiple members.
// 
// This structure implements a [`Read`] interface. When read from, it reads
// compressed data from the underlying [`Read`] and provides the uncompressed
// data.
// 
// A gzip file consists of a series of *members* concatenated one after another.
// MultiGzDecoder decodes all members of a file and returns Result.success(0) once the
// underlying reader does.
// 
// To handle members separately, see [GzDecoder] or read more
// [in the introduction](../index.html#about-multi-member-gzip-files).
// 
// [gzip file]: https://www.rfc-editor.org/rfc/rfc1952#page-5
// 
// # Examples
// 
public class MultiGzDecoder<R> {
    internal var inner: bufread.MultiGzDecoder<BufReader<R>>? = null
}

public object RImpl9 {
    // Creates a new decoder from the given reader, immediately parsing the
    // (first) gzip header. If the gzip stream contains multiple members all will
    // be decoded.
    public fun new(r: R): MultiGzDecoder<R> {
        MultiGzDecoder {
            inner: bufread.MultiGzDecoder.new(BufReader.new(r)),
        }
    }
}

public object RImpl10 {
    // Returns the current header associated with this stream, if it's valid.
    public fun headerUnit: GzHeader? {
        this.inner.headerUnit
    }

    // Acquires a reference to the underlying reader.
    public fun getRefUnit: R {
        this.inner.getRefUnit.getRefUnit
    }

    // Acquires a mutable reference to the underlying stream.
    // 
    // Note that mutation of the stream may result in surprising results if
    // this decoder is continued to be used.
    public fun getMutUnit: R {
        this.inner.getMutUnit.getMutUnit
    }

    // Consumes this decoder, returning the underlying reader.
    public fun intoInnerUnit: R {
        this.inner.intoInnerUnit.intoInnerUnit
    }
}

public object RImpl11 {
    private fun read(into: ByteArray): Result<Int> {
        this.inner.read(into)
    }
}

public object RImpl12 {
    private fun write(buf: ByteArray): Result<Int> {
        this.getMutUnit.write(buf)
    }

    private fun flushUnit: Result<Unit> {
        this.getMutUnit.flushUnit
    }
}

object Tests {


    // a cursor turning EOF into blocking errors
    public class BlockingCursor {
        public var cursor: Cursor<ByteArray>? = null
    }

    public object BlockingCursorImpl {
        public fun newUnit: BlockingCursor {
            BlockingCursor {
                cursor: Cursor.new(mutableListOf<Any>Unit),
            }
        }

        public fun setPosition(pos: ULong): Unit {
            this.cursor.setPosition(pos)
        }
    }

    public object BlockingCursorImpl2 {
        private fun write(buf: ByteArray): Result<Int> {
            this.cursor.write(buf)
        }
        private fun flushUnit: Result<Unit> {
            this.cursor.flushUnit
        }
    }

    public object BlockingCursorImpl3 {
        private fun read(buf: ByteArray): Result<Int> {
            val r = this.cursor.read(buf)
            when r {
                Result.failure(ref err): {
                    if err.kindUnit == ErrorKind.UnexpectedEof {
                        return Result.failure(ErrorKind.WouldBlock.intoUnit)
                    }
                }
                Result.success(0): {
                    // regular EOF turned into blocking error
                    return Result.failure(ErrorKind.WouldBlock.intoUnit)
                }
                Result.success(_n): {}
            }
            r
        }
    }

    @Test
    private fun blockedPartialHeaderReadUnit: Unit {
        // this is a reader which receives data afterwards
        var r = BlockingCursor.newUnit
        val data = mutableListOf(1, 2, 3)

        when r.writeAll(data) {
            Result.success(Unit): {}
            _: {
                throw IllegalStateException("Unexpected result for writeAll")
            }
        }
        r.setPosition(0)

        // this is unused except for the buffering
        var decoder = GzDecoder.new(r)
        var out = Vec.withCapacity(7)
        when decoder.read(out) {
            Result.failure(e): {
                assertEquals(e.kindUnit, ErrorKind.WouldBlock)
            }
            _: {
                throw IllegalStateException("Unexpected result for decoder.read")
            }
        }
    }
}
