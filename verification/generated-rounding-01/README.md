# Complete generated rounding.rs adoption

Source: `tmp/serde_json/src/lexical/rounding.rs`, SHA-256 `4762af3612880b17468ff2a1bb800c8e99f08330ae439699dd53d8dc5463ad13`. Generator source commit: `18f124d` in rust-kotlin.

Destination: `src/common/commonMain/kotlin/io/github/kotlinmania/serdejson/lexical/generated/rounding/Rounding.kt`. Its body is byte-identical to the complete Rust/Native verified raw output, SHA-256 `57c34cb20cf760fc5761ad7ea49ebe29ab244a18840e2ea5600764403a91e388`; only package/provenance headers were added.

Existing `lexical/Rounding.kt` entry points now call the generated functions. The scalar record adapter copies state back on ordinary and exceptional exits. Before each callback, it exposes the current generated state through the caller's original ExtendedFloat object, and copies callback changes back even when the callback throws. Normalization delegates to the existing Float.kt operation. No rounding arithmetic is implemented in the adapter.

All **131 Native tests pass**. The rebuilt actual production KLIB matches **3,852 canonical Rust observations through the generated API** and **3,852 through the existing adapter API**, including 1,073 panic cases. A third pass uses the exact same frozen inputs and records callback-entry mantissa/exponent values, while checking that Kotlin callbacks receive the original object; all **3,852 rows match**. The callback tracing leaves all original observation columns unchanged. Three empty diffs, commands, KLIB/dependency hashes and complete outputs are retained in `production/`. Repeated comparisons add no file credit.

Proof uses checked overflow/debug assertions, panic unwind and usize64 on Rust aarch64-apple-darwin revision 7189d39d9ce936881b8132c6addeaf3e295cd455 and Kotlin/Native macos_arm64. Actual f32/f64, scalar constant, shift and normalization providers are used. Custom callback capture Drop, allocation timing, escaping loans, Swift ABI and other target runtime equivalence are not proved. Existing checked Rust behavior, including panics, is intentional; the old handwritten adapter is not the oracle.

Exactly one newly adopted whole file. No target, FFI, dependency, kernel or remote changes. The pre-existing multiplication issue remains outside this change.

Reproduce after compiling production: `./verification/generated-rounding-01/check_production.sh --out build/generated-rounding-01/production-new`.
