# Adopted generated Digit and Exponent

Complete unchanged Rust sources in `tmp/serde_json/src/lexical/` now supply the
production code:

| Rust source | Kotlin destination under src/commonMain/kotlin/io/github/kotlinmania/serdejson/lexical/ |
|---|---|
| `digit.rs` | `generated/digit/Digit.kt` |
| `exponent.rs` | `generated/exponent/Exponent.kt` |

Each installed body is byte-identical to the Native-validated rust-kotlin output;
only a package declaration is prepended. Separate packages avoid collisions
between each complete file's generated supporting types. The earlier hand-written
`lexical/Digit.kt` and `lexical/Exponent.kt` are removed, not retained as duplicate
implementations or forwarding wrappers. Raw bodies, exact inputs, comments/name
maps and translation profiles are preserved in `digit/` and `exponent/`.

The approved catalog change adds only NumTraits 0.2.15 to the existing commonMain
bundle. Native dependencyInsight resolves exactly
`io.github.kotlinmania:num-traits-kotlin-macosarm64:0.2.15`; the consumed artifact
hash is recorded in `adoption.json`. The arithmetic is the existing U64Num checked
operations and I32Num saturating operations, not new per-port implementations.

## Caller migration and Rust adjudication

Bhcomp now calls `to_digit` with a bit-preserving Byte-to-UByte conversion and
handles the tagged Option explicitly. Rust `bhcomp.rs:47` unwraps that result, so
invalid input now throws the established panic-equivalent exception instead of
the old Kotlin fallback zero. The valid digit becomes ULong for the limb value.
Its scientific-exponent call now converts nonnegative array lengths/indexes to
the source's 64-bit usize representation. Both callers import generated functions
directly. This small manual caller migration is not counted as another generated
source file or a translation of all bhcomp.rs.

The ordinary `digit-port.diff` and `exponent-port.diff` retain comparison with
the prior hand port. Rust is the oracle. The exponent API's larger size domain
is preserved without claiming a previously observed out-of-domain caller failure.

## Validation and limits

- Normal offline `macosArm64Test` passed **129 tests**, including five new tests.
- The installed functions match the saved Rust oracle for all 256 byte inputs,
  eight checked-arithmetic cases and 16 exponent rows (32 operations).
- An unchanged-source Rust Bhcomp harness supplies four floating-point bit
  results and two invalid-digit panic cases; the migrated production callers
  match all six. Sources and commands are in `bhcomp-oracle.json`.
- Raw generated bodies are unchanged after compilation. Existing Math.kt,
  Cached.kt, adopted power tables, build scripts, settings and gradle.properties
  retain their previous hashes. Only the approved three catalog entries changed.

The adopted profile is usize64, overflow-checks on and panic unwind. Earlier
overflow-off candidate validation remains in rust-kotlin; it is not a second
production configuration. The previously verified 32/64 table selections remain
distinct. Other platforms were not run in this increment; no target was disabled.

The known `[u64::MAX,1] * 5^2048` multiplication mismatch remains unchanged in
`../generated-power-tables-01/math-comparison.diff`. It is a separate failing
reproducer and is not included among the passing tests or fixed by this adoption.

The frozen foundation batch is now **7/10 Native-verified, 5/10 adopted**.
This increment adds two adoptions of previously complete translations, not two
newly generated files. No publication or remote action was performed.
