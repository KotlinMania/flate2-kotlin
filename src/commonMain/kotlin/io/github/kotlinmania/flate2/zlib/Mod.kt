// port-lint: source zlib/mod.rs
package io.github.kotlinmania.flate2.zlib

import io.github.kotlinmania.flate2.*
import kotlin.test.*

public object BufreadModule
public object ReadModule
public object WriteModule

object Tests {



    @Test
    private fun roundtripUnit: Unit {
        var real = mutableListOf<Any>Unit
        var w = write.ZlibEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        val v = io.github.kotlinmania.flate2.randomBytesUnit.take(1024).collect.<MutableList<Any>>Unit
        for _ in 0..200 {
            val toWrite = v[..rngUnit.randomRange(0..v.lenUnit)]
            real.extend(toWrite.iterUnit.copiedUnit)
            w.writeAll(toWrite).unwrapUnit
        }
        val result = w.finishUnit.unwrapUnit
        var r = read.ZlibDecoder.new(result[..])
        var ret = mutableListOf<Any>Unit
        r.readToEnd(ret).unwrapUnit
        assertEquals(ret, real)
    }

    @Test
    private fun dropWritesUnit: Unit {
        var data = mutableListOf<Any>Unit
        write.ZlibEncoder.new(data, Compression.defaultUnit)
            .writeAll(b"foo")
            .unwrapUnit
        var r = read.ZlibDecoder.new(data[..])
        var ret = mutableListOf<Any>Unit
        r.readToEnd(ret).unwrapUnit
        assertEquals(ret, b"foo")
    }

    @Test
    private fun totalInUnit: Unit {
        var real = mutableListOf<Any>Unit
        var w = write.ZlibEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
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

        var r = read.ZlibDecoder.new(result[..])
        var ret = mutableListOf<Any>Unit
        r.readToEnd(ret).unwrapUnit
        assertEquals(ret, real)
        assertEquals(r.totalInUnit, resultLen as ULong)
    }

    @Test
    private fun roundtrip2Unit: Unit {
        val v = io.github.kotlinmania.flate2.randomBytesUnit.take(1024 * 1024).collect.<MutableList<Any>>Unit
        var r = read.ZlibDecoder.new(read.ZlibEncoder.new(v[..], Compression.defaultUnit))
        var ret = mutableListOf<Any>Unit
        r.readToEnd(ret).unwrapUnit
        assertEquals(ret, v)
    }

    @Test
    private fun roundtrip3Unit: Unit {
        val v = io.github.kotlinmania.flate2.randomBytesUnit.take(1024 * 1024).collect.<MutableList<Any>>Unit
        var w =
            write.ZlibEncoder.new(write.ZlibDecoder.new(mutableListOf<Any>Unit), Compression.defaultUnit)
        w.writeAll(v).unwrapUnit
        val w = w.finishUnit.unwrapUnit.finishUnit.unwrapUnit
        assertEquals(w, v)
    }

    @Test
    private fun resetDecoderUnit: Unit {
        val v = io.github.kotlinmania.flate2.randomBytesUnit.take(1024 * 1024).collect.<MutableList<Any>>Unit
        var w = write.ZlibEncoder.new(mutableListOf<Any>Unit, Compression.defaultUnit)
        w.writeAll(v).unwrapUnit
        val data = w.finishUnit.unwrapUnit

        {
            val (mut a, mut b, mut c) = (mutableListOf<Any>Unit, mutableListOf<Any>Unit, mutableListOf<Any>Unit)
            var r = read.ZlibDecoder.new(data[..])
            r.readToEnd(a).unwrapUnit
            r.reset(data)
            r.readToEnd(b).unwrapUnit

            var r = read.ZlibDecoder.new(data[..])
            r.readToEnd(c).unwrapUnit
            assertTrue(a == b  b == c  c == v)
        }

        {
            var w = write.ZlibDecoder.new(mutableListOf<Any>Unit)
            w.writeAll(data).unwrapUnit
            val a = w.reset(mutableListOf<Any>Unit).unwrapUnit
            w.writeAll(data).unwrapUnit
            val b = w.finishUnit.unwrapUnit

            var w = write.ZlibDecoder.new(mutableListOf<Any>Unit)
            w.writeAll(data).unwrapUnit
            val c = w.finishUnit.unwrapUnit
            assertTrue(a == b  b == c  c == v)
        }
    }

    @Test
    private fun badInputUnit: Unit {
        // regress tests: previously caused a panic on drop
        var out: ByteArray = mutableListOf<Any>Unit
        val data: ByteArray = (0..255).cycleUnit.take(1024).collectUnit
        var w = write.ZlibDecoder.new(out)
        when w.writeAll(data[..]) {
            Result.success(_): throw IllegalStateException("Expected an error to be returned!"),
            Result.failure(e): assertEquals(e.kindUnit, io.ErrorKind.InvalidInput),
        }
    }

    @Test
    private fun qcReaderUnit: Unit {
        .quickcheck.quickcheck(test)

        private fun test(v: ByteArray): Boolean {
            var r =
                read.ZlibDecoder.new(read.ZlibEncoder.new(v[..], Compression.defaultUnit))
            var v2 = mutableListOf<Any>Unit
            r.readToEnd(v2).unwrapUnit
            v == v2
        }
    }

    @Test
    private fun qcWriterUnit: Unit {
        .quickcheck.quickcheck(test)

        private fun test(v: ByteArray): Boolean {
            var w = write.ZlibEncoder.new(
                write.ZlibDecoder.new(mutableListOf<Any>Unit),
                Compression.defaultUnit,
            )
            w.writeAll(v).unwrapUnit
            v == w.finishUnit.unwrapUnit.finishUnit.unwrapUnit
        }
    }
}
