# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 21/21 (100.0%)
- **Function parity:** 106/306 matched (target 191) — 34.6%
- **Class/type parity:** 51/62 matched (target 186) — 82.3%
- **Combined symbol parity:** 157/368 matched (target 377) — 42.7%
- **Average inline-code cosine:** 0.33 (function body across 17 matched files)
- **Average documentation cosine:** 0.09 (doc text across 17 matched files)
- **Cheat-zeroed Files:** 5
- **Critical Issues:** 20 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. mem

- **Target:** `flate2.Mem`
- **Similarity:** 0.35
- **Dependents:** 7
- **Priority Score:** 7123306.5
- **Functions:** 13/25 matched (target 16)
- **Missing functions:** `decompress_failed`, `decompress_need_dict`, `compress_failed`, `total_in`, `total_out`, `reset`, `message`, `fmt`, `write_to_spare_capacity_of_vec`, `issue51`, `test_gzip_flate`, `test_error_message`
- **Types:** 8/8 matched (target 21)
- **Missing types:** _none_
- **Tests:** 0/3 matched

### 2. zio

- **Target:** `flate2.Zio`
- **Similarity:** 0.15
- **Dependents:** 5
- **Priority Score:** 5162308.5
- **Functions:** 4/19 matched (target 7)
- **Missing functions:** `total_in`, `total_out`, `run`, `none`, `sync`, `finish`, `read`, `get_ref`, `get_mut`, `take_inner`, `is_present`, `write_with_status`, `dump`, `flush`, `drop`
- **Types:** 3/4 matched (target 5)
- **Missing types:** `Error`

### 3. gz.write

- **Target:** `write.Write`
- **Similarity:** 0.15
- **Dependents:** 4
- **Priority Score:** 4182408.5
- **Functions:** 3/21 matched (target 8)
- **Missing functions:** `gz_encoder`, `get_ref`, `get_mut`, `try_finish`, `finish`, `write_header`, `flush`, `drop`, `header`, `finish_and_check_crc`, `decode_writer_one_chunk`, `decode_writer_partial_header`, `decode_writer_partial_header_filename`, `decode_writer_partial_header_comment`, `decode_writer_exact_header`, `decode_writer_partial_crc`, `decode_multi_writer`, `decode_extra_data`
- **Types:** 3/3 matched (target 10)
- **Missing types:** _none_
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

- **Target:** `bufread.Bufread`
- **Similarity:** 0.35
- **Dependents:** 3
- **Priority Score:** 3081806.5
- **Functions:** 6/14 matched (target 12)
- **Missing functions:** `gz_encoder`, `get_ref`, `get_mut`, `into_inner`, `flush`, `multi`, `header`, `decode_extra_data`
- **Types:** 4/4 matched (target 19)
- **Missing types:** _none_
- **Tests:** 0/1 matched

### 6. gz.mod

- **Target:** `gz.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 333610.0
- **Functions:** 1/31 matched (target 1)
- **Missing functions:** `filename`, `extra`, `comment`, `operating_system`, `mtime`, `mtime_as_datetime`, `new`, `parse`, `header`, `read_into`, `read_to_nul`, `parse_le_u16`, `bad_header`, `corrupt`, `write`, `read`, `buf_read`, `into_header`, `roundtrip`, `roundtrip_zero`, `roundtrip_big`, `roundtrip_big2`, `update_crc`, `crc`, `roundtrip_header`, `fields`, `keep_reading_after_end`, `qc_reader`, `test`, `flush_after_write`
- **Types:** 2/5 matched (target 12)
- **Missing types:** `GzHeaderState`, `GzBuilder`, `Rfc1952Crc`
- **Tests:** 0/12 matched

### 7. ffi.c

- **Target:** `c.C`
- **Similarity:** 0.12
- **Dependents:** 0
- **Priority Score:** 222908.8
- **Functions:** 4/21 matched (target 5)
- **Missing functions:** `get`, `default`, `drop`, `align_up`, `zalloc`, `zfree`, `msg`, `destroy`, `decompress_inner`, `decompress`, `decompress_uninit`, `reset`, `total_in`, `total_out`, `compress_inner`, `compress`, `compress_uninit`
- **Types:** 3/8 matched (target 7)
- **Missing types:** `Direction`, `DirCompress`, `DirDecompress`, `Stream`, `Inflate`

### 8. ffi.zlib_rs

- **Target:** `zlibrs.ZlibRs [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 121610.0
- **Functions:** 2/13 matched (target 4)
- **Missing functions:** `from`, `get`, `decompress`, `reset`, `total_in`, `total_out`, `decompress_error`, `set_dictionary`, `compress`, `compress_error`, `set_level`
- **Types:** 2/3 matched (target 4)
- **Missing types:** `Deflate`

### 9. deflate.mod

- **Target:** `deflate.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 121210.0
- **Functions:** 0/12 matched (target 2)
- **Missing functions:** `roundtrip`, `drop_writes`, `total_in`, `roundtrip2`, `roundtrip3`, `reset_writer`, `reset_reader`, `reset_decoder`, `zero_length_read_with_data`, `qc_reader`, `test`, `qc_writer`
- **Types:** 0/0 matched (target 4)
- **Missing types:** _none_
- **Tests:** 0/12 matched

### 10. deflate.bufread

- **Target:** `commonMain.kotlin.io.github.kotlinmania.flate2.deflate.bufread.Bufread`
- **Similarity:** 0.28
- **Dependents:** 0
- **Priority Score:** 101607.2
- **Functions:** 4/14 matched (target 8)
- **Missing functions:** `reset_encoder_data`, `get_ref`, `get_mut`, `into_inner`, `total_in`, `total_out`, `flush`, `reset_decoder_data`, `reset_data`, `decode_extra_data`
- **Types:** 2/2 matched (target 11)
- **Missing types:** _none_
- **Tests:** 0/1 matched

### 11. zlib.mod

- **Target:** `zlib.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 101010.0
- **Functions:** 0/10 matched (target 2)
- **Missing functions:** `roundtrip`, `drop_writes`, `total_in`, `roundtrip2`, `roundtrip3`, `reset_decoder`, `bad_input`, `qc_reader`, `test`, `qc_writer`
- **Types:** 0/0 matched (target 4)
- **Missing types:** _none_
- **Tests:** 0/10 matched

### 12. zlib.write

- **Target:** `commonMain.kotlin.io.github.kotlinmania.flate2.zlib.write.Write`
- **Similarity:** 0.33
- **Dependents:** 0
- **Priority Score:** 91706.7
- **Functions:** 6/15 matched (target 10)
- **Missing functions:** `get_ref`, `get_mut`, `try_finish`, `finish`, `flush_finish`, `total_in`, `total_out`, `flush`, `decode_extra_data`
- **Types:** 2/2 matched (target 9)
- **Missing types:** _none_
- **Tests:** 0/1 matched

### 13. zlib.bufread

- **Target:** `commonMain.kotlin.io.github.kotlinmania.flate2.zlib.bufread.Bufread`
- **Similarity:** 0.35
- **Dependents:** 0
- **Priority Score:** 91706.5
- **Functions:** 6/15 matched (target 10)
- **Missing functions:** `reset_encoder_data`, `get_ref`, `get_mut`, `into_inner`, `total_in`, `total_out`, `flush`, `reset_decoder_data`, `decode_extra_data`
- **Types:** 2/2 matched (target 11)
- **Missing types:** _none_
- **Tests:** 0/1 matched

### 14. deflate.write

- **Target:** `commonMain.kotlin.io.github.kotlinmania.flate2.deflate.write.Write`
- **Similarity:** 0.28
- **Dependents:** 0
- **Priority Score:** 91507.2
- **Functions:** 4/13 matched (target 8)
- **Missing functions:** `get_ref`, `get_mut`, `try_finish`, `finish`, `flush_finish`, `total_in`, `total_out`, `flush`, `decode_extra_data`
- **Types:** 2/2 matched (target 9)
- **Missing types:** _none_
- **Tests:** 0/1 matched

### 15. gz.read

- **Target:** `read.Read`
- **Similarity:** 0.31
- **Dependents:** 0
- **Priority Score:** 81506.9
- **Functions:** 3/11 matched
- **Missing functions:** `gz_encoder`, `get_ref`, `get_mut`, `into_inner`, `flush`, `header`, `set_position`, `blocked_partial_header_read`
- **Types:** 4/4 matched (target 19)
- **Missing types:** _none_
- **Tests:** 0/2 matched

### 16. ffi.miniz_oxide

- **Target:** `minizoxide.MinizOxide`
- **Similarity:** 0.24
- **Dependents:** 0
- **Priority Score:** 71307.6
- **Functions:** 3/10 matched (target 5)
- **Missing functions:** `get`, `from`, `decompress`, `reset`, `total_in`, `total_out`, `compress`
- **Types:** 3/3 matched (target 9)
- **Missing types:** _none_
- **Lint issues:** 2

### 17. zlib.read

- **Target:** `commonMain.kotlin.io.github.kotlinmania.flate2.zlib.read.Read`
- **Similarity:** 0.42
- **Dependents:** 0
- **Priority Score:** 61605.8
- **Functions:** 8/14 matched (target 12)
- **Missing functions:** `get_ref`, `get_mut`, `into_inner`, `total_in`, `total_out`, `flush`
- **Types:** 2/2 matched (target 10)
- **Missing types:** _none_

### 18. deflate.read

- **Target:** `commonMain.kotlin.io.github.kotlinmania.flate2.deflate.read.Read`
- **Similarity:** 0.36
- **Dependents:** 0
- **Priority Score:** 61306.4
- **Functions:** 5/11 matched (target 9)
- **Missing functions:** `get_ref`, `get_mut`, `into_inner`, `total_in`, `total_out`, `flush`
- **Types:** 2/2 matched (target 10)
- **Missing types:** _none_

### 19. lib

- **Target:** `flate2.Lib`
- **Similarity:** 0.57
- **Dependents:** 0
- **Priority Score:** 20904.3
- **Functions:** 6/8 matched (target 9)
- **Missing functions:** `_assert_send_sync`, `random_bytes`
- **Types:** 1/1 matched
- **Missing types:** _none_
- **Tests:** 0/1 matched

### 20. ffi.mod

- **Target:** `ffi.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10710.0
- **Functions:** 4/4 matched
- **Missing functions:** _none_
- **Types:** 2/3 matched (target 6)
- **Missing types:** `InflateBackend`

### 21. crc

- **Target:** `flate2.Crc`
- **Similarity:** 0.75
- **Dependents:** 0
- **Priority Score:** 1802.5
- **Functions:** 15/15 matched (target 37)
- **Missing functions:** _none_
- **Types:** 3/3 matched (target 4)
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Next Commands

```bash
# Initialize task queue for systematic porting
cd tools/ast_distance
./ast_distance --init-tasks ../../tmp/flate2/src rust ../../src/commonMain/kotlin/io/github/kotlinmania/flate2 kotlin tasks.json ../../AGENTS.md

# Get next high-priority task
./ast_distance --assign tasks.json <agent-id>
```
