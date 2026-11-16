# Adopted generated lexical/shift.rs

The complete raw-generated checked profile is installed in `src/common/commonMain/kotlin/io/github/kotlinmania/serdejson/lexical/generated/shift/Shift.kt`; only a package line is prepended. `lexical/Shift.kt` forwards shr, overflowingShr and shl. It copies state back in finally because Rust can write mantissa before an exponent overflow. No arithmetic is reimplemented by the adapter.

131 Native tests pass, including partial-write and invalid-count regression tests. A friend consumer of the complete production Gradle KLIB matches all 2,106 checked-profile Rust observations, including 1,254 panic cases and retained state. All actual dependency paths/hashes and exact compiler arguments are in production-validation.json. The two earlier dependency-inspection attempts failed because their task lacked an explicit Native compile prerequisite; they did not invalidate the completed Native tests.

Generator commit and immutable hashes are in adoption.json. Full Rust source, compiler proof, raw outputs for both profiles, failures and reproducible scripts are in rust-kotlin/verification/foundation-batch-04/shift-01. Compiler proof uses existing original modules; supporting ExtendedFloat declarations are not counted as another whole translated file. This is one new adopted file in the frozen ten-file batch.

The installed profile is Rust aarch64-apple-darwin, usize64, overflow checks/debug assertions enabled, panic unwind; validated on Kotlin/Native macos_arm64. Other targets remain configured but were not runtime-tested here. Existing public APIs, targets, FFI, dependencies and workflows are unchanged. The pre-existing multiplication failure [u64::MAX,1]*5^2048 is unchanged. No remote actions or publication.
