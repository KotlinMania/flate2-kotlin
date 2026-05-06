# flate2-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Fflate2--kotlin-blue.svg)](https://github.com/KotlinMania/flate2-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/flate2-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/flate2-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/flate2-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/flate2-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`rust-lang/flate2-rs`](https://github.com/rust-lang/flate2-rs).

**Original Project:** This port is based on [`rust-lang/flate2-rs`](https://github.com/rust-lang/flate2-rs). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `rust-lang/flate2-rs`

> The text below is reproduced and lightly edited from [`https://github.com/rust-lang/flate2-rs`](https://github.com/rust-lang/flate2-rs). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

## flate2

[![Crates.io](https://img.shields.io/crates/v/flate2.svg?maxAge=2592000)](https://crates.io/crates/flate2)
[![Documentation](https://docs.rs/flate2/badge.svg)](https://docs.rs/flate2)

A streaming compression/decompression library DEFLATE-based streams in Rust.

This crate by default uses the `miniz_oxide` crate, a port of `miniz.c` to pure
Rust. This crate also supports other [backends](#backends), such as the widely
available zlib library or the high-performance zlib-ng library.

Supported formats:

* deflate
* zlib
* gzip

```toml
# Cargo.toml
[dependencies]
flate2 = "1.0"
```

## MSRV (Minimum Supported Rust Version) Policy

This crate supports the current and previous stable versions of the Rust compiler.
For example, if the current stable is 1.80, this crate supports 1.80 and 1.79.

Other compiler versions may work, but failures may not be treated as a `flate2` bug.

The `Cargo.toml` file specifies a `rust-version` for which builds of the current version
passed at some point. This value is indicative only, and may change at any time.

The `rust-version` is a best-effort measured value and is different to the MSRV. The
`rust-version` can be incremented by a PR in order to pass tests, as long as the MSRV
continues to hold. When the `rust-version` increases, the next release should be a minor
version, to allow any affected users to pin to a previous minor version.

## Compression

```rust
use std::io::prelude::*;
use flate2::Compression;
use flate2::write::ZlibEncoder;

fn main() {
    let mut e = ZlibEncoder::new(Vec::new(), Compression::default());
    e.write_all(b"foo");
    e.write_all(b"bar");
    let compressed_bytes = e.finish();
}
```

## Decompression

```rust,no_run
use std::io::prelude::*;
use flate2::read::GzDecoder;

fn main() {
    let mut d = GzDecoder::new("...".as_bytes());
    let mut s = String::new();
    d.read_to_string(&mut s).unwrap();
    println!("{}", s);
}
```

## Backends

The default `miniz_oxide` backend has the advantage of only using safe Rust.

If you want maximum performance while still benefiting from a Rust
implementation at the cost of some `unsafe`, you can use `zlib-rs`:

```toml
[dependencies]
flate2 = { version = "1.0.17", features = ["zlib-rs"], default-features = false }
```

### C backends

While zlib-rs is [the fastest overall](https://trifectatech.org/blog/zlib-rs-is-faster-than-c/),
the zlib-ng C library can be slightly faster in certain cases:

```toml
[dependencies]
flate2 = { version = "1.0.17", features = ["zlib-ng"], default-features = false }
```

Note that the `"zlib-ng"` feature works even if some other part of your crate
graph depends on zlib.

However, if you're already using another C or Rust library that depends on
zlib, and you want to avoid including both zlib and zlib-ng, you can use that
for Rust code as well:

```toml
[dependencies]
flate2 = { version = "1.0.17", features = ["zlib"], default-features = false }
```

Or, if you have C or Rust code that depends on zlib and you want to use zlib-ng
via libz-sys in zlib-compat mode, use:

```toml
[dependencies]
flate2 = { version = "1.0.17", features = ["zlib-ng-compat"], default-features = false }
```

Note that when using the `"zlib-ng-compat"` feature, if any crate in your
dependency graph explicitly requests stock zlib, or uses libz-sys directly
without `default-features = false`, you'll get stock zlib rather than zlib-ng.
See [the libz-sys
README](https://github.com/rust-lang/libz-sys/blob/main/README.md) for details.
To avoid that, use the `"zlib-ng"` feature instead.

# License

This project is licensed under either of

 * Apache License, Version 2.0, ([LICENSE-APACHE](https://github.com/rust-lang/flate2-rs/blob/HEAD/LICENSE-APACHE) or
   https://www.apache.org/licenses/LICENSE-2.0)
 * MIT license ([LICENSE-MIT](https://github.com/rust-lang/flate2-rs/blob/HEAD/LICENSE-MIT) or
   https://opensource.org/licenses/MIT)

at your option.

### Contribution

Unless you explicitly state otherwise, any contribution intentionally submitted
for inclusion in this project by you, as defined in the Apache-2.0 license,
shall be dual licensed as above, without any additional terms or conditions.

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:flate2-kotlin:0.1.0-SNAPSHOT")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same MIT license as the upstream [`rust-lang/flate2-rs`](https://github.com/rust-lang/flate2-rs). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the flate2-rs authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`rust-lang/flate2-rs`](https://github.com/rust-lang/flate2-rs) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
