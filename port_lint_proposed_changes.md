# port-lint Proposed Changes

**Generated:** 2026-09-02
**Source:** tmp/flate2/src
**Target:** src/commonMain/kotlin

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `src/commonMain/kotlin/io/github/kotlinmania/flate2/gz/GzHeader.kt` | `// port-lint: source gz/mod.rs` | `// port-lint: source ffi/mod.rs` | `ffi/mod.rs` | `port-lint provenance header matched only by basename: 'gz/mod.rs' vs expected 'ffi/mod.rs'` |
