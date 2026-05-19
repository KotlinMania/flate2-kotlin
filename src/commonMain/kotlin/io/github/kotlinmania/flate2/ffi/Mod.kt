// port-lint: source ffi/mod.rs
package io.github.kotlinmania.flate2.ffi

import io.github.kotlinmania.flate2.*
import kotlin.test.*

// This module contains backend-specific code.


private fun initializeBuffer(output: ByteArray): ByteArray {
    nativeBlock {
        output.asMutPtrUnit.writeBytes(0, output.lenUnit)
        *(output as ByteArray as ByteArray)
    }
}

// Traits specifying the interface of the backends.
// 
// Sync + Send are added as a condition to ensure they are available
// for the frontend.
public interface Backend {
    private fun totalInUnit: ULong
    private fun totalOutUnit: ULong
}

public interface InflateBackend {
    private fun make(zlibHeader: Boolean, windowBits: UByte): Any
    fun decompress(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushDecompress,
    ): Result<Status, DecompressError>
    fun decompressUninit(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushDecompress,
    ): Result<Status, DecompressError> {
        this.decompress(input, initializeBuffer(output), flush)
    }
    private fun reset(zlibHeader: Boolean): Unit
}

public interface DeflateBackend {
    private fun make(level: Compression, zlibHeader: Boolean, windowBits: UByte): Any
    fun compress(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushCompress,
    ): Result<Status, CompressError>
    fun compressUninit(
        self,
        input: ByteArray,
        output: ByteArray,
        flush: FlushCompress,
    ): Result<Status, CompressError> {
        this.compress(input, initializeBuffer(output), flush)
    }
    private fun resetUnit: Unit
}

// Default to Rust implementation unless explicitly opted in to a different backend.
public object CModule
public val *Reexport = "this.c.*"

// Only bring in `zlib-rs` if there is no C-based backend.
public object ZlibRsModule
public val *Reexport = "this.zlibRs.*"

// Use minizOxide when no fully compliant zlib is selected.
public object MinizOxideModule
public val *Reexport = "this.minizOxide.*"

// If no backend is enabled, fail fast with a clear error message.
compileError!("No compression backend selected; enable one of `zlib`, `zlib-ng`, `zlib-rs`, or the default `rustBackend` feature.")

public object ErrorMessageImpl {
    private fun fmt(f: fmt.Formatter): Result<Unit> {
        this.getUnit.fmt(f)
    }
}
