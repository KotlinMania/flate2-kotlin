// port-lint: source lib.rs
package io.github.kotlinmania.flate2

/**
 * A DEFLATE-based stream compression/decompression library.
 *
 * This library provides support for compression and decompression of
 * DEFLATE-based streams:
 *
 *  * the DEFLATE format itself
 *  * the zlib format
 *  * gzip
 *
 * These three formats are all closely related and largely only differ in their
 * headers/footers. This library has three types in each submodule for dealing
 * with these three formats.
 *
 * ## Implementation
 *
 * In addition to supporting three formats, this library supports several different
 * backends, controlled through feature flags:
 *
 *  * the default backend — currently a pure-Kotlin port of the MinizOxide
 *    algorithm, which is itself a port of miniz.c to Rust. This backend
 *    requires no native code and uses only safe Kotlin.
 *
 *    Note: the default backend may at some point be switched to a ZlibRs
 *    based implementation; use the MinizOxide backend explicitly if
 *    that is not desired.
 *
 *  * ZlibRs — a Rust rewrite of zlib, offered as the fastest backend
 *    at the cost of some native code.
 *
 * Several backends implemented in C are also available.
 * These are useful in case you are already using a specific C implementation
 * and need the result of compression to be bit-identical.
 * See the library's README for details on the available C backends.
 *
 * The ZlibRs backend typically outperforms all the C implementations.
 *
 * ## Feature Flags
 *
 * Activate the document-features cargo feature to see feature docs in the
 * upstream Rust documentation.
 *
 * ## Ambiguous feature selection
 *
 * As Cargo features are additive, while backends are not, there is an order in which backends
 * become active if multiple are selected:
 *
 *  * ZlibNg
 *  * ZlibRs
 *  * CloudflareZlib
 *  * MinizOxide
 *
 * ## Organization
 *
 * This library consists of three main modules: [bufread], [read], and [write]. Each module
 * implements DEFLATE, zlib, and gzip for buffered-read input types, read input
 * types, and write output types respectively.
 *
 * Use the [bufread] implementations if you can provide a buffered-read type for the input.
 *
 * The [read] implementations conveniently wrap a read type in a buffered-read implementation.
 * However, the [read] implementations may
 * [read past the end of the input data](https://github.com/rust-lang/flate2-rs/issues/338),
 * making the read type useless for subsequent reads of the input. If you need to re-use the
 * read type, wrap it in a buffered reader, use the [bufread] implementations,
 * and perform subsequent reads on the buffered reader.
 *
 * The [write] implementations are most useful when there is no way to create a buffered-read
 * type, notably when reading async iterators (streams).
 *
 * Note that types which operate over a specific interface often implement the mirroring interface as well.
 * For example a `bufread.DeflateDecoder<T>` *also* exposes write capabilities if `T` is a
 * writable sink. That is, the "dual interface" is forwarded directly to the underlying object if
 * available.
 *
 * ## About multi-member Gzip files
 *
 * While most `gzip` files one encounters will have a single *member* that can be read
 * with the `GzDecoder`, there may be some files which have multiple members.
 *
 * A `GzDecoder` will only read the first member of gzip data, which may unexpectedly
 * provide partial results when a multi-member gzip file is encountered. `GzDecoder` is appropriate
 * for data that is designed to be read as single members from a multi-member file. `bufread.GzDecoder`
 * and `write.GzDecoder` also allow non-gzip data following gzip data to be handled.
 *
 * The `MultiGzDecoder` on the other hand will decode all members of a `gzip` file
 * into one consecutive stream of bytes, which hides the underlying *members* entirely.
 * If a file contains non-gzip data after the gzip data, MultiGzDecoder will
 * emit an error after decoding the gzip data. This behavior matches the `gzip`,
 * `gunzip`, and `zcat` command line tools.
 */

/**
 * When compressing data, the compression level can be specified by a value in
 * this class.
 */
public class Compression private constructor(private val level: UInt) {

    /** Returns an integer representing the compression level, typically on a
     *  scale of 0-9. See [Companion.new] for details about compression levels. */
    public fun level(): UInt = level

    override fun equals(other: Any?): Boolean = other is Compression && other.level == level

    override fun hashCode(): Int = level.hashCode()

    override fun toString(): String = "Compression($level)"

    public companion object {
        /** Creates a new description of the compression level with an explicitly
         *  specified integer.
         *
         *  The integer here is typically on a scale of 0-9 where 0 means "no
         *  compression" and 9 means "take as long as you'd like". */
        public fun new(level: UInt): Compression = Compression(level)

        /** No compression is to be performed, this may actually inflate data
         *  slightly when encoding. */
        public fun none(): Compression = Compression(0u)

        /** Optimize for the best speed of encoding. */
        public fun fast(): Compression = Compression(1u)

        /** Optimize for the size of data being encoded. */
        public fun best(): Compression = Compression(9u)

        /** The default compression level (6). */
        public fun default(): Compression = Compression(6u)
    }
}
