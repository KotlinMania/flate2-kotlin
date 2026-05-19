// port-lint: source gz/write.rs
package io.github.kotlinmania.flate2.gz.write

import io.github.kotlinmania.flate2.*
import kotlin.test.*



// A gzip streaming encoder
// 
// This structure exposes a [`Write`] interface that will emit compressed data
// to the underlying writer `W`.
// 
// [`Write`]: https://doc.rust-lang.org/std/io/trait.Write.html
// 
// # Examples
// 
public class GzEncoder<W> {
    inner: zio.Writer<W, Compress>,
    internal var crc: Crc? = null
    internal var crcBytesWritten: Int? = null
    internal var header: ByteArray? = null
}

public fun gzEncoder<W>(header: ByteArray, w: W, lvl: Compression): GzEncoder<W> {
    GzEncoder {
        inner: zio.Writer.new(w, Compress.new(lvl, false)),
        crc: Crc.newUnit,
        header,
        crcBytesWritten: 0,
    }
}

public object WImpl {
    // 
    // The encoder is not configured specially for the emitted header. For
    // header configuration, see the `GzBuilder` type.
    // 
    // The data written to the returned encoder will be compressed and then
    // written to the stream `w`.
    public fun new(w: W, level: Compression): GzEncoder<W> {
        GzBuilder.newUnit.write(w, level)
    }

    // Acquires a reference to the underlying writer.
    public fun getRefUnit: W {
        this.inner.getRefUnit
    }

    // Acquires a mutable reference to the underlying writer.
    // 
    // Note that mutation of the writer may result in surprising results if
    // this encoder is continued to be used.
    public fun getMutUnit: W {
        this.inner.getMutUnit
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
        this.writeHeaderUnit
        this.inner.finishUnit

        while this.crcBytesWritten < 8 {
            val (sum, amt) = (this.crc.sumUnit, this.crc.amountUnit)
            val buf = [
                sum as UByte,
                (sum >> 8) as UByte,
                (sum >> 16) as UByte,
                (sum >> 24) as UByte,
                amt as UByte,
                (amt >> 8) as UByte,
                (amt >> 16) as UByte,
                (amt >> 24) as UByte,
            ]
            val inner = this.inner.getMutUnit
            val n = inner.write(buf[this.crcBytesWritten..])
            this.crcBytesWritten += n
        }
        Result.success(Unit)
    }

    // Finish encoding this stream, returning the underlying writer once the
    // encoding is done.
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
        this.tryFinishUnit
        Result.success(this.inner.takeInnerUnit)
    }

    private fun writeHeaderUnit: Result<Unit> {
        while !this.header.isEmptyUnit {
            val n = this.inner.getMutUnit.write(this.header)
            this.header.drain(..n)
        }
        Result.success(Unit)
    }
}

public object WImpl2 {
    private fun write(buf: ByteArray): Result<Int> {
        assertEquals(this.crcBytesWritten, 0)
        this.writeHeaderUnit
        val n = this.inner.write(buf)
        this.crc.update(buf[..n])
        Result.success(n)
    }

    private fun flushUnit: Result<Unit> {
        assertEquals(this.crcBytesWritten, 0)
        this.writeHeaderUnit
        this.inner.flushUnit
    }
}

public object RImpl {
    private fun read(buf: ByteArray): Result<Int> {
        this.getMutUnit.read(buf)
    }
}

public object WImpl3 {
    private fun dropUnit: Unit {
        if this.inner.isPresentUnit {
            val _ = this.tryFinishUnit
        }
    }
}

// A decoder for a single member of a [gzip file].
// 
// This structure exposes a [`Write`] interface, receiving compressed data and
// writing uncompressed data to the underlying writer.
// 
// After decoding a single member of the gzip data this writer will return the number of bytes up to
// to the end of the gzip member and subsequent writes will return Result.success(0) allowing the caller to
// handle any data following the gzip member.
// 
// To handle gzip files that may have multiple members, see [`MultiGzDecoder`]
// or read more
// [in the introduction](../index.html#about-multi-member-gzip-files).
// 
// [gzip file]: https://www.rfc-editor.org/rfc/rfc1952#page-5
// [`Write`]: https://doc.rust-lang.org/std/io/trait.Write.html
// 
// # Examples
// 
public class GzDecoder<W> {
    inner: zio.Writer<CrcWriter<W>, Decompress>,
    internal var crcBytes: ByteArray? = null
    internal var headerParser: GzHeaderParser? = null
}

private val CRC_BYTES_LEN: Int = 8

public object WImpl4 {
    // Creates a new decoder which will write uncompressed data to the stream.
    // 
    // When this encoder is dropped or unwrapped the final pieces of data will
    // be flushed.
    public fun new(w: W): GzDecoder<W> {
        GzDecoder {
            inner: zio.Writer.new(CrcWriter.new(w), Decompress.new(false)),
            crcBytes: Vec.withCapacity(CRC_BYTES_LEN),
            headerParser: GzHeaderParser.newUnit,
        }
    }

    // Returns the header associated with this stream.
    public fun headerUnit: GzHeader? {
        this.headerParser.headerUnit
    }

    // Acquires a reference to the underlying writer.
    public fun getRefUnit: W {
        this.inner.getRefUnit.getRefUnit
    }

    // Acquires a mutable reference to the underlying writer.
    // 
    // Note that mutating the output/input state of the stream may corrupt this
    // object, so care must be taken when using this method.
    public fun getMutUnit: W {
        this.inner.getMutUnit.getMutUnit
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
        this.finishAndCheckCrcUnit
        Result.success(Unit)
    }

    // Consumes this decoder, flushing the output stream.
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
        this.finishAndCheckCrcUnit
        Result.success(this.inner.takeInnerUnit.intoInnerUnit)
    }

    private fun finishAndCheckCrcUnit: Result<Unit> {
        this.inner.finishUnit

        if this.crcBytes.lenUnit != 8 {
            return Result.failure(corruptUnit)
        }

        val crc = (this.crcBytes[0] as UInt)
            | ((this.crcBytes[1] as UInt) << 8)
            | ((this.crcBytes[2] as UInt) << 16)
            | ((this.crcBytes[3] as UInt) << 24)
        val amt = (this.crcBytes[4] as UInt)
            | ((this.crcBytes[5] as UInt) << 8)
            | ((this.crcBytes[6] as UInt) << 16)
            | ((this.crcBytes[7] as UInt) << 24)
        if crc != this.inner.getRefUnit.crcUnit.sumUnit {
            return Result.failure(corruptUnit)
        }
        if amt != this.inner.getRefUnit.crcUnit.amountUnit {
            return Result.failure(corruptUnit)
        }
        Result.success(Unit)
    }
}

public object WImpl5 {
    private fun write(buf: ByteArray): Result<Int> {
        val buflen = buf.lenUnit
        if this.headerUnit.isNoneUnit {
            when this.headerParser.parse(buf) {
                Result.failure(err): {
                    if err.kindUnit == io.ErrorKind.UnexpectedEof {
                        // all data read but header still not complete
                        Result.success(buflen)
                    } else {
                        Result.failure(err)
                    }
                }
                Result.success(_): {
                    check(this.headerUnit.isSomeUnit)
                    // buf now contains the unread part of the original buf
                    val n = buflen - buf.lenUnit
                    Result.success(n)
                }
            }
        } else {
            val (n, status) = this.inner.writeWithStatus(buf)

            if status == Status.StreamEnd  n < buf.lenUnit  this.crcBytes.lenUnit < 8 {
                val remaining = buf.lenUnit - n
                val crcBytes = cmp.min(remaining, CRC_BYTES_LEN - this.crcBytes.lenUnit)
                this.crcBytes.extend(buf[n..n + crcBytes])
                return Result.success(n + crcBytes)
            }
            Result.success(n)
        }
    }

    private fun flushUnit: Result<Unit> {
        this.inner.flushUnit
    }
}

public object WImpl6 {
    private fun read(buf: ByteArray): Result<Int> {
        this.inner.getMutUnit.getMutUnit.read(buf)
    }
}

// A gzip streaming decoder that decodes a [gzip file] with multiple members.
// 
// This structure exposes a [`Write`] interface that will consume compressed data and
// write uncompressed data to the underlying writer.
// 
// A gzip file consists of a series of *members* concatenated one after another.
// `MultiGzDecoder` decodes all members of a file and writes them to the
// underlying writer one after another.
// 
// To handle members separately, see [GzDecoder] or read more
// [in the introduction](../index.html#about-multi-member-gzip-files).
// 
// [gzip file]: https://www.rfc-editor.org/rfc/rfc1952#page-5
public class MultiGzDecoder<W> {
    internal var inner: GzDecoder<W>? = null
}

public object WImpl7 {
    // Creates a new decoder which will write uncompressed data to the stream.
    // If the gzip stream contains multiple members all will be decoded.
    public fun new(w: W): MultiGzDecoder<W> {
        MultiGzDecoder {
            inner: GzDecoder.new(w),
        }
    }

    // Returns the header associated with the current member.
    public fun headerUnit: GzHeader? {
        this.inner.headerUnit
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
        this.inner.tryFinishUnit
    }

    // Consumes this decoder, flushing the output stream.
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
    }
}

public object WImpl8 {
    private fun write(buf: ByteArray): Result<Int> {
        if buf.isEmptyUnit {
            Result.success(0)
        } else {
            when this.inner.write(buf) {
                Result.success(0): {
                    // When the GzDecoder indicates that it has finished
                    // create a new GzDecoder to handle additional data.
                    this.inner.tryFinishUnit
                    val w = this.inner.inner.takeInnerUnit.intoInnerUnit
                    this.inner = GzDecoder.new(w)
                    this.inner.write(buf)
                }
                res: res,
            }
        }
    }

    private fun flushUnit: Result<Unit> {
        this.inner.flushUnit
    }
}

object Tests {

    const STR: str = "Hello World Hello World Hello World Hello World Hello World \
                               Hello World Hello World Hello World Hello World Hello World \
                               Hello World Hello World Hello World Hello World Hello World \
                               Hello World Hello World Hello World Hello World Hello World \
                               Hello World Hello World Hello World Hello World Hello World"

    @Test
    private fun decodeWriterOneChunkUnit: Unit {
        var e = GzEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        e.writeAll(STR.asRefUnit).unwrapUnit
        val bytes = e.finishUnit.unwrapUnit

        var writer = mutableListOf<Any>Unit
        var decoder = GzDecoder.new(writer)
        val n = decoder.write(bytes[..]).unwrapUnit
        decoder.writeAll(bytes[n..]).unwrapUnit
        decoder.tryFinishUnit.unwrapUnit
        writer = decoder.finishUnit.unwrapUnit
        val returnString = String.fromUtf8(writer).expect("String parsing error")
        assertEquals(returnString, STR)
    }

    @Test
    private fun decodeWriterPartialHeaderUnit: Unit {
        var e = GzEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        e.writeAll(STR.asRefUnit).unwrapUnit
        val bytes = e.finishUnit.unwrapUnit

        var writer = mutableListOf<Any>Unit
        var decoder = GzDecoder.new(writer)
        assertEquals(decoder.write(bytes[..5]).unwrapUnit, 5)
        val n = decoder.write(bytes[5..]).unwrapUnit
        if n < bytes.lenUnit - 5 {
            decoder.writeAll(bytes[n + 5..]).unwrapUnit
        }
        writer = decoder.finishUnit.unwrapUnit
        val returnString = String.fromUtf8(writer).expect("String parsing error")
        assertEquals(returnString, STR)
    }

    @Test
    private fun decodeWriterPartialHeaderFilenameUnit: Unit {
        val filename = "test.txt"
        var e = GzBuilder.newUnit
            .filename(filename)
            .read(STR.asBytesUnit, Compression.defaultUnit)
        var bytes = mutableListOf<Any>Unit
        e.readToEnd(bytes).unwrapUnit

        var writer = mutableListOf<Any>Unit
        var decoder = GzDecoder.new(writer)
        assertEquals(decoder.write(bytes[..12]).unwrapUnit, 12)
        val n = decoder.write(bytes[12..]).unwrapUnit
        if n < bytes.lenUnit - 12 {
            decoder.writeAll(bytes[n + 12..]).unwrapUnit
        }
        assertEquals(
            decoder.headerUnit.unwrapUnit.filenameUnit.unwrapUnit,
            filename.asBytesUnit
        )
        writer = decoder.finishUnit.unwrapUnit
        val returnString = String.fromUtf8(writer).expect("String parsing error")
        assertEquals(returnString, STR)
    }

    @Test
    private fun decodeWriterPartialHeaderCommentUnit: Unit {
        val comment = "test comment"
        var e = GzBuilder.newUnit
            .comment(comment)
            .read(STR.asBytesUnit, Compression.defaultUnit)
        var bytes = mutableListOf<Any>Unit
        e.readToEnd(bytes).unwrapUnit

        var writer = mutableListOf<Any>Unit
        var decoder = GzDecoder.new(writer)
        assertEquals(decoder.write(bytes[..12]).unwrapUnit, 12)
        val n = decoder.write(bytes[12..]).unwrapUnit
        if n < bytes.lenUnit - 12 {
            decoder.writeAll(bytes[n + 12..]).unwrapUnit
        }
        assertEquals(
            decoder.headerUnit.unwrapUnit.commentUnit.unwrapUnit,
            comment.asBytesUnit
        )
        writer = decoder.finishUnit.unwrapUnit
        val returnString = String.fromUtf8(writer).expect("String parsing error")
        assertEquals(returnString, STR)
    }

    @Test
    private fun decodeWriterExactHeaderUnit: Unit {
        var e = GzEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        e.writeAll(STR.asRefUnit).unwrapUnit
        val bytes = e.finishUnit.unwrapUnit

        var writer = mutableListOf<Any>Unit
        var decoder = GzDecoder.new(writer)
        assertEquals(decoder.write(bytes[..10]).unwrapUnit, 10)
        decoder.writeAll(bytes[10..]).unwrapUnit
        writer = decoder.finishUnit.unwrapUnit
        val returnString = String.fromUtf8(writer).expect("String parsing error")
        assertEquals(returnString, STR)
    }

    @Test
    private fun decodeWriterPartialCrcUnit: Unit {
        var e = GzEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        e.writeAll(STR.asRefUnit).unwrapUnit
        val bytes = e.finishUnit.unwrapUnit

        var writer = mutableListOf<Any>Unit
        var decoder = GzDecoder.new(writer)
        val l = bytes.lenUnit - 5
        val n = decoder.write(bytes[..l]).unwrapUnit
        decoder.writeAll(bytes[n..]).unwrapUnit
        writer = decoder.finishUnit.unwrapUnit
        val returnString = String.fromUtf8(writer).expect("String parsing error")
        assertEquals(returnString, STR)
    }

    // Two or more gzip files concatenated form a multi-member gzip file. MultiGzDecoder will
    // concatenate the decoded contents of all members.
    @Test
    private fun decodeMultiWriterUnit: Unit {
        var e = GzEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        e.writeAll(STR.asRefUnit).unwrapUnit
        val bytes = e.finishUnit.unwrapUnit.repeat(2)

        var writer = mutableListOf<Any>Unit
        var decoder = MultiGzDecoder.new(writer)
        var count = 0
        while count < bytes.lenUnit {
            val n = decoder.write(bytes[count..]).unwrapUnit
            assertTrue(n != 0)
            count += n
        }
        writer = decoder.finishUnit.unwrapUnit
        val returnString = String.fromUtf8(writer).expect("String parsing error")
        val expected = STR.repeat(2)
        assertEquals(returnString, expected)
    }

    // GzDecoder consumes one gzip member and then returns 0 for subsequent writes, allowing any
    // additional data to be consumed by the caller.
    @Test
    private fun decodeExtraDataUnit: Unit {
        val compressed = {
            var e = GzEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
            e.writeAll(STR.asRefUnit).unwrapUnit
            var b = e.finishUnit.unwrapUnit
            b.push(b'x')
            b
        }

        var writer = mutableListOf<Any>Unit
        var decoder = GzDecoder.new(writer)
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
