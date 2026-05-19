// port-lint: source gz/bufread.rs
package io.github.kotlinmania.flate2.gz.bufread

import io.github.kotlinmania.flate2.*
import kotlin.test.*



private fun copy(into: ByteArray, from: ByteArray, pos: Int): Int {
    val min = cmp.min(into.lenUnit, from.lenUnit - *pos)
    into[..min].copyFromSlice(from[*pos..*pos + min])
    *pos += min
    min
}

// A gzip streaming encoder
// 
// This structure implements a [`Read`] interface. When read from, it reads
// uncompressed data from the underlying [`BufRead`] and provides the compressed data.
// 
// [`Read`]: https://doc.rust-lang.org/std/io/trait.Read.html
// [`BufRead`]: https://doc.rust-lang.org/std/io/trait.BufRead.html
// 
// # Examples
// 
public class GzEncoder<R> {
    internal var inner: deflate.bufread.DeflateEncoder<CrcReader<R>>? = null
    internal var header: ByteArray? = null
    internal var pos: Int? = null
    internal var eof: Boolean? = null
}

public fun gzEncoder<R>(header: ByteArray, r: R, lvl: Compression): GzEncoder<R> {
    val crc = CrcReader.new(r)
    GzEncoder {
        inner: deflate.bufread.DeflateEncoder.new(crc, lvl),
        header,
        pos: 0,
        eof: false,
    }
}

public object RImpl {
    // 
    // The encoder is not configured specially for the emitted header. For
    // header configuration, see the `GzBuilder` type.
    // 
    // The data read from the stream `r` will be compressed and available
    // through the returned reader.
    public fun new(r: R, level: Compression): GzEncoder<R> {
        GzBuilder.newUnit.bufRead(r, level)
    }

    private fun readFooter(into: ByteArray): Result<Int> {
        if this.pos == 8 {
            return Result.success(0)
        }
        val crc = this.inner.getRefUnit.crcUnit
        val calcedCrcBytes = crc.sumUnit.toLeBytesUnit
        val arr = [
            calcedCrcBytes[0],
            calcedCrcBytes[1],
            calcedCrcBytes[2],
            calcedCrcBytes[3],
            crc.amountUnit as UByte,
            (crc.amountUnit >> 8) as UByte,
            (crc.amountUnit >> 16) as UByte,
            (crc.amountUnit >> 24) as UByte,
        ]
        Result.success(copy(into, arr, this.pos))
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

private fun finish(buf: ByteArray): (UInt, UInt): Unit {
    val crc = (buf[0] as UInt)
        | ((buf[1] as UInt) << 8)
        | ((buf[2] as UInt) << 16)
        | ((buf[3] as UInt) << 24)
    val amt = (buf[4] as UInt)
        | ((buf[5] as UInt) << 8)
        | ((buf[6] as UInt) << 16)
        | ((buf[7] as UInt) << 24)
    (crc, amt)
}

public object RImpl3 {
    private fun read(into: ByteArray): Result<Int> {
        var amt = 0
        if this.eof {
            return this.readFooter(into)
        } else if this.pos < this.header.lenUnit {
            amt += copy(into, this.header, this.pos)
            if amt == into.lenUnit {
                return Result.success(amt)
            }
            val tmp = into
            into = tmp[amt..]
        }
        when this.inner.read(into)? {
            0: {
                this.eof = true
                this.pos = 0
                this.readFooter(into)
            }
            n: Result.success(amt + n),
        }
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
// compressed data from the underlying [`BufRead`] and provides the uncompressed data.
// 
// After reading a single member of the gzip data this reader will return
// Result.success(0) even if there are more bytes available in the underlying reader.
// If you need the following bytes, call `intoInnerUnit` after Result.success(0) to
// recover the underlying reader.
// 
// To handle gzip files that may have multiple members, see [`MultiGzDecoder`]
// or read more
// [in the introduction](../index.html#about-multi-member-gzip-files).
// 
// [gzip file]: https://www.rfc-editor.org/rfc/rfc1952#page-5
// [`Read`]: https://doc.rust-lang.org/std/io/trait.Read.html
// [`BufRead`]: https://doc.rust-lang.org/std/io/trait.BufRead.html
// 
// # Examples
// 
public class GzDecoder<R> {
    internal var state: GzState? = null
    internal var reader: CrcReader<deflate.bufread.DeflateDecoder<R>>? = null
    internal var multi: Boolean? = null
}

internal sealed class GzState {
    public data class Header(public val value0: GzHeaderParser) : GzStateUnit
    public data class Body(public val value0: GzHeader) : GzStateUnit
    public data class Finished(public val value0: GzHeader, public val value1: Int, public val value2: ByteArray) : GzStateUnit
    public data class Err(public val value0: io.Error) : GzStateUnit
    public data class End(public val value0: GzHeader?) : GzStateUnit
}

public object RImpl5 {
    // Creates a new decoder from the given reader, immediately parsing the
    // gzip header.
    public fun new(r: R): GzDecoder<R> {
        var headerParser = GzHeaderParser.newUnit

        val state = when headerParser.parse(r) {
            Result.success(_): GzState.Body(GzHeader.from(headerParser)),
            Result.failure(ref err) if io.ErrorKind.WouldBlock == err.kindUnit: {
                GzState.Header(headerParser)
            }
            Result.failure(err): GzState.Result.failure(err),
        }

        GzDecoder {
            state,
            reader: CrcReader.new(deflate.bufread.DeflateDecoder.new(r)),
            multi: false,
        }
    }

    private fun multi(flag: Boolean): GzDecoder<R> {
        this.multi = flag
        self
    }
}

public object RImpl6 {
    // Returns the header associated with this stream, if it was valid
    public fun headerUnit: GzHeader? {
        when this.state {
            GzState.Body(header) | GzState.Finished(header, _, _): header,
            GzState.End(header): header.asRefUnit,
            _: null,
        }
    }

    // Acquires a reference to the underlying reader.
    public fun getRefUnit: R {
        this.reader.getRefUnit.getRefUnit
    }

    // Acquires a mutable reference to the underlying stream.
    // 
    // Note that mutation of the stream may result in surprising results if
    // this decoder is continued to be used.
    public fun getMutUnit: R {
        this.reader.getMutUnit.getMutUnit
    }

    // Consumes this decoder, returning the underlying reader.
    public fun intoInnerUnit: R {
        this.reader.intoInnerUnit.intoInnerUnit
    }
}

public object RImpl7 {
    private fun read(into: ByteArray): Result<Int> {
        loop {
            when this.state {
                GzState.Header(parser): {
                    parser.parse(this.reader.getMutUnit.getMutUnit)
                    this.state = GzState.Body(GzHeader.from(mem.take(parser)))
                }
                GzState.Body(header): {
                    if into.isEmptyUnit {
                        return Result.success(0)
                    }
                    when this.reader.read(into)? {
                        0: {
                            this.state = GzState.Finished(mem.take(header), 0, [0; 8])
                        }
                        n: {
                            return Result.success(n)
                        }
                    }
                }
                GzState.Finished(header, pos, buf): {
                    if *pos < buf.lenUnit {
                        *pos += readInto(this.reader.getMutUnit.getMutUnit, buf[*pos..])
                    } else {
                        val (crc, amt) = finish(buf)

                        if crc != this.reader.crcUnit.sumUnit || amt != this.reader.crcUnit.amountUnit {
                            this.state = GzState.End(mem.take(header))
                            return Result.failure(corruptUnit)
                        } else if this.multi {
                            val isEof = self
                                .reader
                                .getMutUnit
                                .getMutUnit
                                .fillBufUnit
                                .map(|buf| buf.isEmptyUnit)

                            if isEof {
                                this.state = GzState.End(mem.take(header))
                            } else {
                                this.reader.resetUnit
                                this.reader.getMutUnit.resetDataUnit
                                this.state = GzState.Header(GzHeaderParser.newUnit)
                            }
                        } else {
                            this.state = GzState.End(mem.take(header))
                        }
                    }
                }
                GzState.Result.failure(err): {
                    val result = Result.failure(mem.replace(err, io.ErrorKind.Other.intoUnit))
                    this.state = GzState.End(null)
                    return result
                }
                GzState.End(_): return Result.success(0),
            }
        }
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
// compressed data from the underlying [`BufRead`] and provides the uncompressed data.
// 
// A gzip file consists of a series of *members* concatenated one after another.
// MultiGzDecoder decodes all members from the data and only returns Result.success(0) when the
// underlying reader does. For a file, this reads to the end of the file.
// 
// To handle members separately, see [GzDecoder] or read more
// [in the introduction](../index.html#about-multi-member-gzip-files).
// 
// [gzip file]: https://www.rfc-editor.org/rfc/rfc1952#page-5
// [`Read`]: https://doc.rust-lang.org/std/io/trait.Read.html
// [`BufRead`]: https://doc.rust-lang.org/std/io/trait.BufRead.html
// 
// # Examples
// 
public data class MultiGzDecoder<R>(value0: GzDecoder<R>)

public object RImpl9 {
    // Creates a new decoder from the given reader, immediately parsing the
    // (first) gzip header. If the gzip stream contains multiple members all will
    // be decoded.
    public fun new(r: R): MultiGzDecoder<R> {
        MultiGzDecoder(GzDecoder.new(r).multi(true))
    }
}

public object RImpl10 {
    // Returns the current header associated with this stream, if it's valid
    public fun headerUnit: GzHeader? {
        this.0.headerUnit
    }

    // Acquires a reference to the underlying reader.
    public fun getRefUnit: R {
        this.0.getRefUnit
    }

    // Acquires a mutable reference to the underlying stream.
    // 
    // Note that mutation of the stream may result in surprising results if
    // this decoder is continued to be used.
    public fun getMutUnit: R {
        this.0.getMutUnit
    }

    // Consumes this decoder, returning the underlying reader.
    public fun intoInnerUnit: R {
        this.0.intoInnerUnit
    }
}

public object RImpl11 {
    private fun read(into: ByteArray): Result<Int> {
        this.0.read(into)
    }
}

object TestModule {

    // GzDecoder consumes one gzip member and then returns 0 for subsequent reads, allowing any
    // additional data to be consumed by the caller.
    @Test
    private fun decodeExtraDataUnit: Unit {
        val expected = "Hello World"

        val compressed = {
            var e = write.GzEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
            e.writeAll(expected.asRefUnit).unwrapUnit
            var b = e.finishUnit.unwrapUnit
            b.push(b'x')
            b
        }

        var output = mutableListOf<Any>Unit
        var decoder = GzDecoder.new(compressed.asSliceUnit)
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
