# flate2-kotlin

[![GitHub](https://img.shields.io/badge/GitHub-KotlinMania%2Fflate2--kotlin-blue?logo=github)](https://github.com/KotlinMania/flate2-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/flate2-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/flate2-kotlin)
[![CI](https://img.shields.io/github/actions/workflow/status/KotlinMania/flate2-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/flate2-kotlin/actions)
[![License: MIT](https://img.shields.io/badge/license-MIT-green)](LICENSE)

A Kotlin Multiplatform streaming compression and decompression library for
DEFLATE, zlib, and gzip formats. A faithful transliteration of
[`rust-lang/flate2-rs`](https://github.com/rust-lang/flate2-rs) to idiomatic
Kotlin — no JVM-only dependencies, no `java.util.zip`, no native FFI.

## Supported formats

- **DEFLATE** — raw DEFLATE compression and decompression
- **zlib** — DEFLATE wrapped in a zlib header/adler32 trailer
- **gzip** — DEFLATE wrapped in a gzip header/CRC-32 trailer

## Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:flate2-kotlin:0.1.1")
}
```

Available on Maven Central. Coordinates follow the
`io.github.kotlinmania:flate2-kotlin` group.

## Quick start

### Compression (write-side)

```kotlin
import io.github.kotlinmania.flate2.Compression
import io.github.kotlinmania.flate2.zlib.ZlibWriteEncoder

val output = ByteArray(256)
val encoder = ZlibWriteEncoder.new(output, Compression.default)
encoder.write("Hello, world!".toByteArray())
val compressed = encoder.finish()
// `compressed` contains the zlib-compressed data
```

### Decompression (read-side)

```kotlin
import io.github.kotlinmania.flate2.gz.GzDecoder

val source: BufferedSource = /* ... gzip data ... */
val decoder = GzDecoder.new(source)
val buffer = ByteArray(1024)
val n = decoder.read(buffer)
// `buffer` now contains decompressed bytes
```

### Gzip encoding (bufread-side)

```kotlin
import io.github.kotlinmania.flate2.gz.GzEncoder
import io.github.kotlinmania.flate2.Compression

val source: BufferedSource = /* ... uncompressed data ... */
val encoder = GzEncoder(source, Compression.default)
val compressed = ByteArray(4096)
val n = encoder.read(compressed)
```

## Supported targets

| Target            | Notes                                  |
|-------------------|----------------------------------------|
| JVM               | Full support                           |
| macOS arm64       | XCFramework + Swift Export             |
| iOS arm64         | Static framework                       |
| iOS simulator     | arm64 + x86_64 (both static)           |
| tvOS              | arm64 + simulator-arm64                |
| watchOS           | arm64 + device-arm64 + simulator-arm64 |
| Linux x64/arm64   | Full support                           |
| Windows mingw-x64 | Full support                           |
| Android           | API 24+ (KMP library)                  |
| Android Native    | arm32, arm64, x64, x86                 |
| JS                | Browser + Node.js                      |
| Wasm-JS           | Browser + Node.js                      |
| Wasm-WASI         | Node.js                                |

## API surface

All public types live under `io.github.kotlinmania.flate2`:

**Core types** — `InputSource`, `OutputSink`, `BufferedSource`, `BufReader`,
`Crc`, `CrcReader`, `CrcWriter`, `CrcWriterSink`, `Compress`, `Decompress`,
`Compression`, `CodecOps`, `DecompressOps`, `Status`, `Writer`

**Deflate** — `deflate.DeflateEncoder`, `deflate.DeflateDecoder`,
`deflate.ReadEncoder`/`ReadDecoder`, `deflate.WriteEncoder`/`WriteDecoder`

**Zlib** — `zlib.ZlibEncoder`, `zlib.ZlibDecoder`,
`zlib.ReadEncoder`/`ReadDecoder`, `zlib.WriteEncoder`/`WriteDecoder`

**Gzip** — `gz.GzEncoder`, `gz.GzDecoder`, `gz.MultiGzDecoder`,
`gz.GzHeader`, `gz.GzHeaderParser`, `gz.GzBuilder`,
`gz.GzWriteEncoder`, `gz.GzWriteDecoder`, `gz.MultiGzWriteDecoder`

## Porting status

This is an **in-progress port**. Each Kotlin source file carries a
`// port-lint: source <path>` header naming its upstream Rust counterpart
so the AST-distance tool can track provenance and drift.

### Ported modules

| Upstream Rust file  | Kotlin file              | Status        |
|---------------------|--------------------------|---------------|
| `lib.rs`            | `Lib.kt`                | Ported        |
| `crc.rs`            | `Crc.kt`                | Ported        |
| `bufreader.rs`      | `Bufreader.kt`          | Ported        |
| `mem.rs`            | `Mem.kt`                | Ported        |
| `zio.rs`            | `Zio.kt`               | Ported        |
| `deflate/mod.rs`    | `deflate/Mod.kt`        | Ported        |
| `deflate/bufread.rs`| `deflate/Bufread.kt`   | Ported        |
| `deflate/read.rs`  | `deflate/Read.kt`       | Ported        |
| `deflate/write.rs`  | `deflate/Write.kt`      | Ported        |
| `zlib/mod.rs`       | `zlib/Mod.kt`           | Ported        |
| `zlib/bufread.rs`   | `zlib/Bufread.kt`       | Ported        |
| `zlib/read.rs`      | `zlib/Read.kt`          | Ported        |
| `zlib/write.rs`     | `zlib/Write.kt`         | Ported        |
| `gz/mod.rs`         | `gz/Mod.kt` + `gz/GzHeader.kt` | Ported |
| `gz/bufread.rs`     | `gz/Bufread.kt`         | Ported        |
| `gz/read.rs`        | `gz/Read.kt`            | Ported        |
| `gz/write.rs`       | `gz/Write.kt`           | Ported        |

### Not ported (by design)

- `ffi/` — C FFI backends (miniz_oxide, zlib-rs, zlib-ng). This port uses
  a pure-Kotlin compression backend (`Mem.kt`) instead.
- Rust test harness and benchmarks — Kotlin test parity is tracked
  separately via `commonTest/`.

## Building

```bash
./gradlew build    # all targets
./gradlew test     # host-portable tests (macOS, JVM, JS, Wasm, Android unit, Swift smoke)
```

## Acknowledgments

This library is a transliteration of **[`rust-lang/flate2-rs`](https://github.com/rust-lang/flate2-rs)** by Alex Crichton, Josh Triplett, and the Rust community. All original design, algorithms, and API intent belong to the upstream authors. Bug reports about upstream behavior should go to the Rust repository.

The Kotlin port is by Sydney Renee and The Solace Project.

## License

This Kotlin port is distributed under the **MIT license**, the same license as the upstream `flate2-rs` crate. See [LICENSE](LICENSE) for the full text.

Original work copyrighted by the flate2-rs authors.
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.