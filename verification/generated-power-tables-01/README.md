# Directly generated lexical power tables

`small/input.rs` and `large/input.rs` are the complete Rust source inputs.
`small/generated.kt` and `large/generated.kt` are unchanged machine output.
Production `lexical/SmallPowers.kt` and `lexical/LargePowers64.kt` prepend only
the Kotlin package declaration. Source comments and selected/excluded cfg
provenance are retained. The selected production configuration is explicitly
`fast_arithmetic="64"`; this does not change target configuration.

Math.kt/Algorithm.kt now consume the generated copied read-only array views.
Their exact small integration diffs are saved here; no arithmetic implementation
was changed. All 652 source values match Rust. The 122 macOS ARM64 Native tests
pass, including three new table/caller tests. Other targets are unverified.

**Known pre-existing defect:** `[u64::MAX, 1] * 5^2048` differs from Rust in
existing Math.kt. Both the old and newly integrated port produce identical
output; `math-comparison.diff` retains the Rust mismatch. Rust's second limb is
8948726103039177410; both port versions yield 8948726103039177411.
Existing Large.iaddImpl/isub mishandle incoming versus outgoing carry. This
increment adopts correct tables; it does not claim to repair or regenerate math.rs.

Full compiler commands, original oracle, raw Native runner, baseline comparison
and the initial failed assertion are retained in rust-kotlin's
`verification/foundation-table-adoption-01/` and its corresponding `tmp/` run.
`validation.json` records exact hashes, baseline commit and validation limits.
