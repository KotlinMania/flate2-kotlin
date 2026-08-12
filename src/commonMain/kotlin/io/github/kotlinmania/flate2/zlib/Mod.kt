// port-lint: source zlib/mod.rs
// Module ledger for the zlib package.
// Upstream zlib/mod.rs re-exports bufread, read, and write submodules.
// In Kotlin these are package-private files in the same package;
// this file tracks the provenance only.

package io.github.kotlinmania.flate2.zlib

internal object ZlibMod
