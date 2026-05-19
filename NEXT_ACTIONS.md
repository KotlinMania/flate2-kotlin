# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 3/21 (14.3%)
- **Function parity:** 22/399 matched (target 41) — 5.5%
- **Class/type parity:** 5/54 matched (target 6) — 9.3%
- **Combined symbol parity:** 27/453 matched (target 47) — 6.0%
- **Average inline-code cosine:** 0.52 (function body across 3 matched files)
- **Average documentation cosine:** 0.49 (doc text across 3 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 3 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. bufreader

- **Target:** `flate2.Bufreader`
- **Similarity:** 0.45
- **Dependents:** 4
- **Priority Score:** 4041105.5
- **Functions:** 6/10 matched (target 8)
- **Missing functions:** `fmt`, `new`, `with_buf`, `fill_buf`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 2. crc

- **Target:** `flate2.Crc`
- **Similarity:** 0.54
- **Dependents:** 0
- **Priority Score:** 51804.6
- **Functions:** 10/15 matched (target 24)
- **Missing functions:** `new`, `get_mut`, `fill_buf`, `consume`, `flush`
- **Types:** 3/3 matched (target 4)
- **Missing types:** _none_

### 3. lib

- **Target:** `flate2.Lib`
- **Similarity:** 0.57
- **Dependents:** 0
- **Priority Score:** 20904.3
- **Functions:** 6/8 matched (target 9)
- **Missing functions:** `_assert_send_sync`, `random_bytes`
- **Types:** 1/1 matched
- **Missing types:** _none_
- **Tests:** 0/1 matched

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
## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `deflate.mod` | `deflate.Mod` | 0 | `deflate/mod.rs` | `deflate/Mod.kt` |
| `ffi.mod` | `ffi.Mod` | 0 | `ffi/mod.rs` | `ffi/Mod.kt` |
| `gz.mod` | `gz.Mod` | 0 | `gz/mod.rs` | `gz/Mod.kt` |
| `zlib.mod` | `zlib.Mod` | 0 | `zlib/mod.rs` | `zlib/Mod.kt` |

