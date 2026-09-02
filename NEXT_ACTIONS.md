# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 18/21 (85.7%)
- **Function parity:** 144/325 matched (target 362) — 44.3%
- **Class/type parity:** 22/62 matched (target 78) — 35.5%
- **Combined symbol parity:** 166/387 matched (target 440) — 42.9%
- **Average inline-code cosine:** 0.53 (function body across 14 matched files)
- **Average documentation cosine:** 0.55 (doc text across 14 matched files)
- **Cheat-zeroed Files:** 4
- **Critical Issues:** 15 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. mem

- **Target:** `flate2.Mem`
- **Similarity:** 0.46
- **Dependents:** 7
- **Priority Score:** 7033305.5
- **Functions:** 22/25 matched (target 76)
- **Missing functions:** `from`, `fmt`, `write_to_spare_capacity_of_vec`
- **Types:** 8/8 matched (target 17)
- **Missing types:** _none_
- **Tests:** 3/3 matched

### 2. zio

- **Target:** `flate2.Zio`
- **Similarity:** 0.46
- **Dependents:** 5
- **Priority Score:** 5072305.5
- **Functions:** 15/19 matched (target 32)
- **Missing functions:** `none`, `sync`, `read`, `drop`
- **Types:** 1/4 matched (target 11)
- **Missing types:** `Ops`, `Error`, `Flush`

### 3. gz.write

- **Target:** `gz.Write`
- **Similarity:** 0.54
- **Dependents:** 4
- **Priority Score:** 4142404.8
- **Functions:** 10/21 matched (target 28)
- **Missing functions:** `gz_encoder`, `read`, `drop`, `decode_writer_one_chunk`, `decode_writer_partial_header`, `decode_writer_partial_header_filename`, `decode_writer_partial_header_comment`, `decode_writer_exact_header`, `decode_writer_partial_crc`, `decode_multi_writer`, `decode_extra_data`
- **Types:** 0/3 matched
- **Missing types:** `GzEncoder`, `GzDecoder`, `MultiGzDecoder`
- **Tests:** 0/8 matched

### 4. bufreader

- **Target:** `flate2.Bufreader`
- **Similarity:** 0.59
- **Dependents:** 4
- **Priority Score:** 4011104.0
- **Functions:** 9/10 matched (target 11)
- **Missing functions:** `fmt`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 5. gz.bufread

- **Target:** `gz.Bufread`
- **Similarity:** 0.51
- **Dependents:** 3
- **Priority Score:** 3041804.8
- **Functions:** 10/14 matched (target 28)
- **Missing functions:** `write`, `flush`, `multi`, `decode_extra_data`
- **Types:** 4/4 matched (target 10)
- **Missing types:** _none_
- **Tests:** 0/1 matched

### 6. gz.mod

- **Target:** `gz.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 363610.0
- **Functions:** 0/31 matched (target 0)
- **Missing functions:** `filename`, `extra`, `comment`, `operating_system`, `mtime`, `mtime_as_datetime`, `new`, `parse`, `header`, `from`, `read_into`, `read_to_nul`, `parse_le_u16`, `bad_header`, `corrupt`, `write`, `read`, `buf_read`, `into_header`, `roundtrip`, `roundtrip_zero`, `roundtrip_big`, `roundtrip_big2`, `update_crc`, `crc`, `roundtrip_header`, `fields`, `keep_reading_after_end`, `qc_reader`, `test`, `flush_after_write`
- **Types:** 0/5 matched (target 1)
- **Missing types:** `GzHeader`, `GzHeaderState`, `GzHeaderParser`, `GzBuilder`, `Rfc1952Crc`
- **Tests:** 0/12 matched

### 7. deflate.mod

- **Target:** `deflate.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 121210.0
- **Functions:** 0/12 matched (target 0)
- **Missing functions:** `roundtrip`, `drop_writes`, `total_in`, `roundtrip2`, `roundtrip3`, `reset_writer`, `reset_reader`, `reset_decoder`, `zero_length_read_with_data`, `qc_reader`, `test`, `qc_writer`
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 0/12 matched

### 8. gz.read

- **Target:** `gz.Read`
- **Similarity:** 0.23
- **Dependents:** 0
- **Priority Score:** 111507.7
- **Functions:** 4/11 matched (target 9)
- **Missing functions:** `gz_encoder`, `new`, `read`, `write`, `flush`, `set_position`, `blocked_partial_header_read`
- **Types:** 0/4 matched (target 3)
- **Missing types:** `GzEncoder`, `GzDecoder`, `MultiGzDecoder`, `BlockingCursor`
- **Tests:** 0/2 matched

### 9. zlib.mod

- **Target:** `zlib.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 101010.0
- **Functions:** 0/10 matched (target 0)
- **Missing functions:** `roundtrip`, `drop_writes`, `total_in`, `roundtrip2`, `roundtrip3`, `reset_decoder`, `bad_input`, `qc_reader`, `test`, `qc_writer`
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 0/10 matched

### 10. zlib.read

- **Target:** `zlib.Read`
- **Similarity:** 0.48
- **Dependents:** 0
- **Priority Score:** 91605.2
- **Functions:** 7/14 matched
- **Missing functions:** `new`, `new_with_compress`, `write`, `flush`, `new_with_buf`, `new_with_decompress`, `new_with_decompress_and_buf`
- **Types:** 0/2 matched
- **Missing types:** `ZlibEncoder`, `ZlibDecoder`

### 11. zlib.write

- **Target:** `zlib.Write`
- **Similarity:** 0.63
- **Dependents:** 0
- **Priority Score:** 71703.7
- **Functions:** 10/15 matched (target 19)
- **Missing functions:** `new`, `new_with_compress`, `read`, `new_with_decompress`, `decode_extra_data`
- **Types:** 0/2 matched
- **Missing types:** `ZlibEncoder`, `ZlibDecoder`
- **Tests:** 0/1 matched

### 12. ffi.mod

- **Target:** `gz.GzHeader [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 70710.0
- **Functions:** 0/4 matched (target 21)
- **Missing functions:** `initialize_buffer`, `decompress_uninit`, `compress_uninit`, `fmt`
- **Types:** 0/3 matched (target 11)
- **Missing types:** `Backend`, `InflateBackend`, `DeflateBackend`
- **Provenance warning:** port-lint provenance header matched only by basename: `gz/mod.rs` vs expected `ffi/mod.rs`
- **Proposed provenance header:** `// port-lint: source ffi/mod.rs` (current: `// port-lint: source gz/mod.rs`)
- **Lint issues:** 1

### 13. zlib.bufread

- **Target:** `zlib.Bufread`
- **Similarity:** 0.48
- **Dependents:** 0
- **Priority Score:** 61705.2
- **Functions:** 9/15 matched (target 18)
- **Missing functions:** `new`, `new_with_compress`, `write`, `flush`, `new_with_decompress`, `decode_extra_data`
- **Types:** 2/2 matched
- **Missing types:** _none_
- **Tests:** 0/1 matched

### 14. deflate.read

- **Target:** `deflate.Read`
- **Similarity:** 0.55
- **Dependents:** 0
- **Priority Score:** 61304.5
- **Functions:** 7/11 matched (target 14)
- **Missing functions:** `new`, `write`, `flush`, `new_with_buf`
- **Types:** 0/2 matched
- **Missing types:** `DeflateEncoder`, `DeflateDecoder`

### 15. deflate.write

- **Target:** `deflate.Write`
- **Similarity:** 0.68
- **Dependents:** 0
- **Priority Score:** 51503.2
- **Functions:** 10/13 matched (target 19)
- **Missing functions:** `new`, `read`, `decode_extra_data`
- **Types:** 0/2 matched
- **Missing types:** `DeflateEncoder`, `DeflateDecoder`
- **Tests:** 0/1 matched

### 16. deflate.bufread

- **Target:** `deflate.Bufread`
- **Similarity:** 0.52
- **Dependents:** 0
- **Priority Score:** 41604.8
- **Functions:** 10/14 matched (target 20)
- **Missing functions:** `new`, `write`, `flush`, `decode_extra_data`
- **Types:** 2/2 matched
- **Missing types:** _none_
- **Tests:** 0/1 matched

### 17. lib

- **Target:** `flate2.Lib`
- **Similarity:** 0.57
- **Dependents:** 0
- **Priority Score:** 20904.3
- **Functions:** 6/8 matched (target 16)
- **Missing functions:** `_assert_send_sync`, `random_bytes`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 0/1 matched

### 18. crc

- **Target:** `flate2.Crc`
- **Similarity:** 0.77
- **Dependents:** 0
- **Priority Score:** 1802.3
- **Functions:** 15/15 matched (target 37)
- **Missing functions:** _none_
- **Types:** 3/3 matched (target 5)
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

