// port-lint: source deflate/mod.rs
package io.github.kotlinmania.flate2.deflate

import io.github.kotlinmania.flate2.*
import kotlin.test.*

public object BufreadModule
public object ReadModule
public object WriteModule

object Tests {



    @Test
    private fun roundtripUnit: Unit {
        var real = mutableListOf<Any>Unit
        var w = write.DeflateEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        val v = io.github.kotlinmania.flate2.randomBytesUnit.take(1024).collect.<MutableList<Any>>Unit
        for _ in 0..200 {
            val toWrite = v[..rngUnit.randomRange(0..v.lenUnit)]
            real.extend(toWrite.iterUnit.copiedUnit)
            w.writeAll(toWrite).unwrapUnit
        }
        val result = w.finishUnit.unwrapUnit
        var r = read.DeflateDecoder.new(result[..])
        var ret = mutableListOf<Any>Unit
        r.readToEnd(ret).unwrapUnit
        assertEquals(ret, real)
    }

    @Test
    private fun dropWritesUnit: Unit {
        var data = mutableListOf<Any>Unit
        write.DeflateEncoder.new(data, Compression.defaultUnit)
            .writeAll(b"foo")
            .unwrapUnit
        var r = read.DeflateDecoder.new(data[..])
        var ret = mutableListOf<Any>Unit
        r.readToEnd(ret).unwrapUnit
        assertEquals(ret, b"foo")
    }

    @Test
    private fun totalInUnit: Unit {
        var real = mutableListOf<Any>Unit
        var w = write.DeflateEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        val v = io.github.kotlinmania.flate2.randomBytesUnit.take(1024).collect.<MutableList<Any>>Unit
        for _ in 0..200 {
            val toWrite = v[..rngUnit.randomRange(0..v.lenUnit)]
            real.extend(toWrite.iterUnit.copiedUnit)
            w.writeAll(toWrite).unwrapUnit
        }
        var result = w.finishUnit.unwrapUnit

        val resultLen = result.lenUnit

        for _ in 0..200 {
            result.extend(v.iterUnit.copiedUnit)
        }

        var r = read.DeflateDecoder.new(result[..])
        var ret = mutableListOf<Any>Unit
        r.readToEnd(ret).unwrapUnit
        assertEquals(ret, real)
        assertEquals(r.totalInUnit, resultLen as ULong)
    }

    @Test
    private fun roundtrip2Unit: Unit {
        val v = io.github.kotlinmania.flate2.randomBytesUnit.take(1024 * 1024).collect.<MutableList<Any>>Unit
        var r =
            read.DeflateDecoder.new(read.DeflateEncoder.new(v[..], Compression.defaultUnit))
        var ret = mutableListOf<Any>Unit
        r.readToEnd(ret).unwrapUnit
        assertEquals(ret, v)
    }

    @Test
    private fun roundtrip3Unit: Unit {
        val v = io.github.kotlinmania.flate2.randomBytesUnit.take(1024 * 1024).collect.<MutableList<Any>>Unit
        var w = write.DeflateEncoder.new(
            write.DeflateDecoder.new(mutableListOf<Any>Unit),
            Compression.defaultUnit,
        )
        w.writeAll(v).unwrapUnit
        val w = w.finishUnit.unwrapUnit.finishUnit.unwrapUnit
        assertEquals(w, v)
    }

    @Test
    private fun resetWriterUnit: Unit {
        val v = io.github.kotlinmania.flate2.randomBytesUnit.take(1024 * 1024).collect.<MutableList<Any>>Unit
        var w = write.DeflateEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        w.writeAll(v).unwrapUnit
        val a = w.reset(mutableListOf<Any>Unit).unwrapUnit
        w.writeAll(v).unwrapUnit
        val b = w.finishUnit.unwrapUnit

        var w = write.DeflateEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        w.writeAll(v).unwrapUnit
        val c = w.finishUnit.unwrapUnit
        assertTrue(a == b  b == c)
    }

    @Test
    private fun resetReaderUnit: Unit {
        val v = io.github.kotlinmania.flate2.randomBytesUnit.take(1024 * 1024).collect.<MutableList<Any>>Unit
        val (mut a, mut b, mut c) = (mutableListOf<Any>Unit, mutableListOf<Any>Unit, mutableListOf<Any>Unit)
        var r = read.DeflateEncoder.new(v[..], Compression.defaultUnit)
        r.readToEnd(a).unwrapUnit
        r.reset(v[..])
        r.readToEnd(b).unwrapUnit

        var r = read.DeflateEncoder.new(v[..], Compression.defaultUnit)
        r.readToEnd(c).unwrapUnit
        assertTrue(a == b  b == c)
    }

    @Test
    private fun resetDecoderUnit: Unit {
        val v = io.github.kotlinmania.flate2.randomBytesUnit.take(1024 * 1024).collect.<MutableList<Any>>Unit
        var w = write.DeflateEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        w.writeAll(v).unwrapUnit
        val data = w.finishUnit.unwrapUnit

        {
            val (mut a, mut b, mut c) = (mutableListOf<Any>Unit, mutableListOf<Any>Unit, mutableListOf<Any>Unit)
            var r = read.DeflateDecoder.new(data[..])
            r.readToEnd(a).unwrapUnit
            r.reset(data)
            r.readToEnd(b).unwrapUnit

            var r = read.DeflateDecoder.new(data[..])
            r.readToEnd(c).unwrapUnit
            assertTrue(a == b  b == c  c == v)
        }

        {
            var w = write.DeflateDecoder.new(mutableListOf<Any>Unit)
            w.writeAll(data).unwrapUnit
            val a = w.reset(mutableListOf<Any>Unit).unwrapUnit
            w.writeAll(data).unwrapUnit
            val b = w.finishUnit.unwrapUnit

            var w = write.DeflateDecoder.new(mutableListOf<Any>Unit)
            w.writeAll(data).unwrapUnit
            val c = w.finishUnit.unwrapUnit
            assertTrue(a == b  b == c  c == v)
        }
    }

    @Test
    private fun zeroLengthReadWithDataUnit: Unit {
        val m = mutableListOf(3UByte; 128 * 1024 + 1)
        var c = read.DeflateEncoder.new(m[..], Compression.defaultUnit)

        var result = mutableListOf<Any>Unit
        c.readToEnd(result).unwrapUnit

        var d = read.DeflateDecoder.new(result[..])
        var data = mutableListOf<Any>Unit
        assertEquals(d.read(data).unwrapUnit, 0)
    }

    @Test
    private fun qcReaderUnit: Unit {
        .quickcheck.quickcheck(test)

        private fun test(v: ByteArray): Boolean {
            var r = read.DeflateDecoder.new(read.DeflateEncoder.new(
                v[..],
                Compression.defaultUnit,
            ))
            var v2 = mutableListOf<Any>Unit
            r.readToEnd(v2).unwrapUnit
            v == v2
        }
    }

    @Test
    private fun qcWriterUnit: Unit {
        .quickcheck.quickcheck(test)

        private fun test(v: ByteArray): Boolean {
            var w = write.DeflateEncoder.new(
                write.DeflateDecoder.new(mutableListOf<Any>Unit),
                Compression.defaultUnit,
            )
            w.writeAll(v).unwrapUnit
            v == w.finishUnit.unwrapUnit.finishUnit.unwrapUnit
        }
    }
}
