// port-lint: source gz/mod.rs
package io.github.kotlinmania.flate2.gz

import io.github.kotlinmania.flate2.*
import kotlin.test.*



public val FHCRC: UByte = 1 << 1
public val FEXTRA: UByte = 1 << 2
public val FNAME: UByte = 1 << 3
public val FCOMMENT: UByte = 1 << 4
public val FRESERVED: UByte = 1 << 5 | 1 << 6 | 1 << 7

public object BufreadModule
public object ReadModule
public object WriteModule

// The maximum length of the header filename and comment fields. More than
private val MAX_HEADER_BUF: Int = 65535

// A structure representing the header of a gzip stream.
// 
// The header can contain metadata about the file that was compressed, if
// present.
public class GzHeader {
    internal var extra: ByteArray?? = null
    internal var filename: ByteArray?? = null
    internal var comment: ByteArray?? = null
    internal var operatingSystem: UByte? = null
    internal var mtime: UInt? = null
}

public object GzHeaderImpl {
    // Returns the `filename` field of this gzip stream's header, if present.
    public fun filenameUnit: ByteArray? {
        this.filename.asRefUnit.map(|s| s[..])
    }

    // Returns the `extra` field of this gzip stream's header, if present.
    public fun extraUnit: ByteArray? {
        this.extra.asRefUnit.map(|s| s[..])
    }

    // Returns the `comment` field of this gzip stream's header, if present.
    public fun commentUnit: ByteArray? {
        this.comment.asRefUnit.map(|s| s[..])
    }

    // Returns the `operatingSystem` field of this gzip stream's header.
    // 
    // There are predefined values for various operating systems.
    // 255 means that the value is unknown.
    public fun operatingSystemUnit: UByte {
        this.operatingSystem
    }

    // This gives the most recent modification time of the original file being compressed.
    // 
    // The time is in Unix format, i.e., seconds since 00:00:00 GMT, Jan. 1, 1970.
    // rather than Universal time.) If the compressed data did not come from a file,
    // `mtime` is set to the time at which compression started.
    // `mtime` = 0 means no time stamp is available.
    // 
    // The usage of `mtime` is discouraged because of Year 2038 problem.
    public fun mtimeUnit: UInt {
        this.mtime
    }

    // Returns the most recent modification time represented by a date-time type.
    // Returns `null` if the value of the underlying counter is 0,
    // indicating no time stamp is available.
    // 
    // 
    // The time is measured as seconds since 00:00:00 GMT, Jan. 1 1970.
    // See [`mtime`](#method.mtime) for more detail.
    public fun mtimeAsDatetimeUnit: time.SystemTime? {
        if this.mtime == 0 {
            null
        } else {
            val duration = time.Duration.new(ULong.from(this.mtime), 0)
            val datetime = time.UNIX_EPOCH + duration
            datetime
        }
    }
}

public sealed class GzHeaderState {
    public data class Start(public val value0: UByte, public val value1: ByteArray) : GzHeaderStateUnit
    public data class Xlen(public val value0: Crc?, public val value1: UByte, public val value2: ByteArray) : GzHeaderStateUnit
    public data class Extra(public val value0: Crc?, public val value1: UShort) : GzHeaderStateUnit
    public data class Filename(public val value0: Crc?) : GzHeaderStateUnit
    public data class Comment(public val value0: Crc?) : GzHeaderStateUnit
    public data class Crc(public val value0: Crc?, public val value1: UByte, public val value2: ByteArray) : GzHeaderStateUnit
    public data object Complete : GzHeaderStateUnit
}

public class GzHeaderParser {
    internal var state: GzHeaderState? = null
    internal var flags: UByte? = null
    internal var header: GzHeader? = null
}

public object GzHeaderParserImpl {
    private fun newUnit: Any {
        GzHeaderParser {
            state: GzHeaderState.Start(0, [0; 10]),
            flags: 0,
            header: GzHeader.defaultUnit,
        }
    }

    private fun parse<R>(r: R): Result<Unit> {
        loop {
            when this.state {
                GzHeaderState.Start(count, buffer): {
                    while (*count as Int) < buffer.lenUnit {
                        *count += readInto(r, buffer[*count as Int..])? as UByte
                    }
                    // Gzip identification bytes
                    if buffer[0] != 0x1f || buffer[1] != 0x8b {
                        return Result.failure(badHeaderUnit)
                    }
                    // Gzip compression method (8 = deflate)
                    if buffer[2] != 8 {
                        return Result.failure(badHeaderUnit)
                    }
                    this.flags = buffer[3]
                    // RFC1952: "must give an error indication if any reserved bit is non-zero"
                    if this.flags  FRESERVED != 0 {
                        return Result.failure(badHeaderUnit)
                    }
                    this.header.mtime = (buffer[4] as UInt)
                        | ((buffer[5] as UInt) << 8)
                        | ((buffer[6] as UInt) << 16)
                        | ((buffer[7] as UInt) << 24)
                    val _xfl = buffer[8]
                    this.header.operatingSystem = buffer[9]
                    val crc = if this.flags  FHCRC != 0 {
                        var crc = Box.new(Crc.newUnit)
                        crc.update(buffer)
                        crc
                    } else {
                        null
                    }
                    this.state = GzHeaderState.Xlen(crc, 0, [0; 2])
                }
                GzHeaderState.Xlen(crc, count, buffer): {
                    if this.flags  FEXTRA != 0 {
                        while (*count as Int) < buffer.lenUnit {
                            *count += readInto(r, buffer[*count as Int..])? as UByte
                        }
                        if val crc = crc {
                            crc.update(buffer)
                        }
                        val xlen = parseLeU16(buffer)
                        this.header.extra = mutableListOf(0; xlen as Int)
                        this.state = GzHeaderState.Extra(crc.takeUnit, 0)
                    } else {
                        this.state = GzHeaderState.Filename(crc.takeUnit)
                    }
                }
                GzHeaderState.Extra(crc, count): {
                    check(this.header.extra.isSomeUnit)
                    val extra = this.header.extra.asMutUnit.unwrapUnit
                    while (*count as Int) < extra.lenUnit {
                        *count += readInto(r, extra[*count as Int..])? as UShort
                    }
                    if val crc = crc {
                        crc.update(extra)
                    }
                    this.state = GzHeaderState.Filename(crc.takeUnit)
                }
                GzHeaderState.Filename(crc): {
                    if this.flags  FNAME != 0 {
                        val filename = this.header.filename.getOrInsertWith(Vec.new)
                        readToNul(r, filename)
                        if val crc = crc {
                            crc.update(filename)
                            crc.update(b"\0")
                        }
                    }
                    this.state = GzHeaderState.Comment(crc.takeUnit)
                }
                GzHeaderState.Comment(crc): {
                    if this.flags  FCOMMENT != 0 {
                        val comment = this.header.comment.getOrInsertWith(Vec.new)
                        readToNul(r, comment)
                        if val crc = crc {
                            crc.update(comment)
                            crc.update(b"\0")
                        }
                    }
                    this.state = GzHeaderState.Crc(crc.takeUnit, 0, [0; 2])
                }
                GzHeaderState.Crc(crc, count, buffer): {
                    if val crc = crc {
                        check(this.flags  FHCRC != 0)
                        while (*count as Int) < buffer.lenUnit {
                            *count += readInto(r, buffer[*count as Int..])? as UByte
                        }
                        val storedCrc = parseLeU16(buffer)
                        val calcedCrc = crc.sumUnit as UShort
                        if storedCrc != calcedCrc {
                            return Result.failure(corruptUnit)
                        }
                    }
                    this.state = GzHeaderState.Complete
                }
                GzHeaderState.Complete: {
                    return Result.success(Unit)
                }
            }
        }
    }

    private fun headerUnit: GzHeader? {
        when this.state {
            GzHeaderState.Complete: this.header,
            _: null,
        }
    }
}

public object GzHeaderImpl2 {
    private fun from(parser: GzHeaderParser): Any {
        check(matches!(parser.state, GzHeaderState.Complete))
        parser.header
    }
}

// Attempt to fill the `buffer` from `r`. Return the number of bytes read.
// Return an error if EOF is read before the buffer is full.  This differs
// from `read` in that Result.success(0) means that more data may be available.
private fun readInto<R>(r: R, buffer: ByteArray): Result<Int> {
    check(!buffer.isEmptyUnit)
    when r.read(buffer) {
        Result.success(0): Result.failure(ErrorKind.UnexpectedEof.intoUnit),
        Result.success(n): Result.success(n),
        Result.failure(ref e) if e.kindUnit == ErrorKind.Interrupted: Result.success(0),
        Result.failure(e): Result.failure(e),
    }
}

// Read `r` up to the first nul byte, pushing non-nul bytes to `buffer`.
private fun readToNul<R>(r: R, buffer: ByteArray): Result<Unit> {
    var bytes = r.bytesUnit
    loop {
        when bytes.nextUnit.transposeUnit? {
            0: return Result.success(Unit),
            _ if buffer.lenUnit == MAX_HEADER_BUF: {
                return Err(Error.new(
                    ErrorKind.InvalidInput,
                    "gzip header field too long",
                ))
            }
            byte: {
                buffer.push(byte)
            }
            null: {
                return Result.failure(ErrorKind.UnexpectedEof.intoUnit)
            }
        }
    }
}

private fun parseLeU16(buffer: ByteArray): UShort {
    UShort.fromLeBytes(*buffer)
}

private fun badHeaderUnit: Error {
    Error.new(ErrorKind.InvalidInput, "invalid gzip header")
}

private fun corruptUnit: Error {
    Error.new(
        ErrorKind.InvalidInput,
        "corrupt gzip stream does not have a matching checksum",
    )
}

// A builder structure to create a new gzip Encoder.
// 
// This structure controls header configuration options such as the filename.
// 
// # Examples
// 
public class GzBuilder {
    internal var extra: ByteArray?? = null
    internal var filename: CString?? = null
    internal var comment: CString?? = null
    internal var operatingSystem: UByte?? = null
    internal var mtime: UInt? = null
}

public object GzBuilderImpl {
    // Create a new blank builder with no header by default.
    public fun newUnit: GzBuilder {
        Self.defaultUnit
    }

    // Configure the `mtime` field in the gzip header.
    public fun mtime(mtime: UInt): GzBuilder {
        this.mtime = mtime
        self
    }

    // Configure the `operatingSystem` field in the gzip header.
    public fun operatingSystem(os: UByte): GzBuilder {
        this.operatingSystem = os
        self
    }

    // Configure the `extra` field in the gzip header.
    public fun extra<T: Into<ByteArray>>(mut self, extra: T): GzBuilder {
        this.extra = extra.intoUnit
        self
    }

    // Configure the `filename` field in the gzip header.
    // 
    // # Panics
    // 
    // Panics if the `filename` slice contains a zero.
    public fun filename<T: Into<ByteArray>>(mut self, filename: T): GzBuilder {
        this.filename = CString.new(filename.intoUnit.unwrapUnit)
        self
    }

    // Configure the `comment` field in the gzip header.
    // 
    // # Panics
    // 
    // Panics if the `comment` slice contains a zero.
    public fun comment<T: Into<ByteArray>>(mut self, comment: T): GzBuilder {
        this.comment = CString.new(comment.intoUnit.unwrapUnit)
        self
    }

    // Consume this builder, creating a writer encoder in the process.
    // 
    // The data written to the returned encoder will be compressed and then
    // written out to the supplied parameter `w`.
    public fun write<W>(w: W, lvl: Compression): write.GzEncoder<W> {
        write.gzEncoder(this.intoHeader(lvl), w, lvl)
    }

    // Consume this builder, creating a reader encoder in the process.
    // 
    // Data read from the returned encoder will be the compressed version of
    // the data read from the given reader.
    public fun read<R>(r: R, lvl: Compression): read.GzEncoder<R> {
        read.gzEncoder(this.bufRead(BufReader.new(r), lvl))
    }

    // Consume this builder, creating a reader encoder in the process.
    // 
    // Data read from the returned encoder will be the compressed version of
    // the data read from the given reader.
    public fun bufRead<R>(self, r: R, lvl: Compression): bufread.GzEncoder<R>
    where
        R: BufRead,
    {
        bufread.gzEncoder(this.intoHeader(lvl), r, lvl)
    }

    private fun intoHeader(lvl: Compression): ByteArray {
        val GzBuilder {
            extra,
            filename,
            comment,
            operatingSystem,
            mtime,
        } = self
        var flg = 0
        var header = mutableListOf(0UByte; 10)
        if val v = extra {
            flg |= FEXTRA
            header.extend((v.lenUnit as UShort).toLeBytesUnit)
            header.extend(v)
        }
        if val filename = filename {
            flg |= FNAME
            header.extend(filename.asBytesWithNulUnit.iterUnit.copiedUnit)
        }
        if val comment = comment {
            flg |= FCOMMENT
            header.extend(comment.asBytesWithNulUnit.iterUnit.copiedUnit)
        }
        header[0] = 0x1f
        header[1] = 0x8b
        header[2] = 8
        header[3] = flg
        header[4] = mtime as UByte
        header[5] = (mtime >> 8) as UByte
        header[6] = (mtime >> 16) as UByte
        header[7] = (mtime >> 24) as UByte
        header[8] = if lvl.0 >= Compression.bestUnit.0 {
            2
        } else if lvl.0 <= Compression.fastUnit.0 {
            4
        } else {
            0
        }

        // Typically this byte indicates what OS the gz stream was created on,
        // but in an effort to have cross-platform reproducible streams just
        // default this value to 255. I'm not sure that if we "correctly" set
        // this it'd do anything anyway..
        header[9] = operatingSystem.unwrapOr(255)
        header
    }
}

object Tests {


    @Test
    private fun roundtripUnit: Unit {
        var e = write.GzEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        e.writeAll(b"foo bar baz").unwrapUnit
        val inner = e.finishUnit.unwrapUnit
        var d = read.GzDecoder.new(inner[..])
        var s = String.newUnit
        d.readToString(s).unwrapUnit
        assertEquals(s, "foo bar baz")
    }

    @Test
    private fun roundtripZeroUnit: Unit {
        val e = write.GzEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        val inner = e.finishUnit.unwrapUnit
        var d = read.GzDecoder.new(inner[..])
        var s = String.newUnit
        d.readToString(s).unwrapUnit
        assertEquals(s, "")
    }

    @Test
    private fun roundtripBigUnit: Unit {
        var real = mutableListOf<Any>Unit
        var w = write.GzEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        val v = io.github.kotlinmania.flate2.randomBytesUnit.take(1024).collect.<MutableList<Any>>Unit
        for _ in 0..200 {
            val toWrite = v[..rngUnit.randomRange(0..v.lenUnit)]
            real.extend(toWrite.iterUnit.copiedUnit)
            w.writeAll(toWrite).unwrapUnit
        }
        val result = w.finishUnit.unwrapUnit
        var r = read.GzDecoder.new(result[..])
        var v = mutableListOf<Any>Unit
        r.readToEnd(v).unwrapUnit
        assertEquals(v, real)
    }

    @Test
    private fun roundtripBig2Unit: Unit {
        val v = io.github.kotlinmania.flate2.randomBytesUnit.take(1024 * 1024).collect.<MutableList<Any>>Unit
        var r = read.GzDecoder.new(read.GzEncoder.new(v[..], Compression.defaultUnit))
        var res = mutableListOf<Any>Unit
        r.readToEnd(res).unwrapUnit
        assertEquals(res, v)
    }

    // A Rust implementation of CRC that closely matches the C code in RFC1952.
    internal class Rfc1952Crc {
        /* Table of CRCs of all 8-bit messages. */
        internal var crcTable: [UInt; 256]? = null
    }

    public object Rfc1952CrcImpl {
        private fun newUnit: Any {
            var crc = Rfc1952Crc {
                crcTable: [0; 256],
            }
            /* Make the table for a fast CRC. */
            for n in 0Int..256 {
                var c = n as UInt
                for _k in 0..8 {
                    if c  1 != 0 {
                        c = 0xedb88320 ^ (c >> 1)
                    } else {
                        c >>= 1
                    }
                }
                crc.crcTable[n] = c
            }
            crc
        }

        /*
         Update a running crc with the bytes buf and return
         the updated crc. The crc should be initialized to zero. Pre- and
         post-conditioning (one's complement) is performed within this
         function so it shouldn't be done by the caller.
        */
        private fun updateCrc(crc: UInt, buf: ByteArray): UInt {
            var c = crc ^ 0xffffffff

            for b in buf {
                c = this.crcTable[(c as UByte ^ *b) as Int] ^ (c >> 8)
            }
            c ^ 0xffffffff
        }

        /* Return the CRC of the bytes buf. */
        private fun crc(buf: ByteArray): UInt {
            this.updateCrc(0, buf)
        }
    }

    @Test
    private fun roundtripHeaderUnit: Unit {
        var header = GzBuilder.newUnit
            .mtime(1234)
            .operatingSystem(57)
            .filename("filename")
            .comment("comment")
            .intoHeader(Compression.fastUnit)

        // Add a CRC to the header
        header[3] ^= super.FHCRC
        val rfc1952Crc = Rfc1952Crc.newUnit
        val crc32 = rfc1952Crc.crc(header)
        val crc16 = crc32 as UShort
        header.extend(crc16.toLeBytesUnit)

        var parser = GzHeaderParser.newUnit
        parser.parse(header.asSliceUnit).unwrapUnit
        val actual = parser.headerUnit.unwrapUnit
        assertEquals(
            actual,
            GzHeader {
                extra: null,
                filename: "filename".asBytes(.toVecUnit),
                comment: "comment".asBytes(.toVecUnit),
                operatingSystem: 57,
                mtime: 1234
            }
        )
    }

    @Test
    private fun fieldsUnit: Unit {
        val r = [0, 2, 4, 6]
        val e = GzBuilder.newUnit
            .filename("foo.rs")
            .comment("bar")
            .extra(mutableListOf(0, 1, 2, 3))
            .read(r[..], Compression.defaultUnit)
        var d = read.GzDecoder.new(e)
        assertEquals(d.headerUnit.unwrapUnit.filenameUnit, b"foo.rs"[..])
        assertEquals(d.headerUnit.unwrapUnit.commentUnit, b"bar"[..])
        assertEquals(d.headerUnit.unwrapUnit.extraUnit, b"\x00\x01\x02\x03"[..])
        var res = mutableListOf<Any>Unit
        d.readToEnd(res).unwrapUnit
        assertEquals(res, mutableListOf(0, 2, 4, 6))
    }

    @Test
    private fun keepReadingAfterEndUnit: Unit {
        var e = write.GzEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        e.writeAll(b"foo bar baz").unwrapUnit
        val inner = e.finishUnit.unwrapUnit
        var d = read.GzDecoder.new(inner[..])
        var s = String.newUnit
        d.readToString(s).unwrapUnit
        assertEquals(s, "foo bar baz")
        d.readToString(s).unwrapUnit
        assertEquals(s, "foo bar baz")
    }

    @Test
    private fun qcReaderUnit: Unit {
        .quickcheck.quickcheck(test)

        private fun test(v: ByteArray): Boolean {
            val r = read.GzEncoder.new(v[..], Compression.defaultUnit)
            var r = read.GzDecoder.new(r)
            var v2 = mutableListOf<Any>Unit
            r.readToEnd(v2).unwrapUnit
            v == v2
        }
    }

    @Test
    private fun flushAfterWriteUnit: Unit {
        var f = write.GzEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        write(f, "Hello world").unwrapUnit
        f.flushUnit.unwrapUnit
    }
}
