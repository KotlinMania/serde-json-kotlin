# Complete generated errors.rs adoption

Source: `/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/errors.rs`, SHA-256 `3d9f6de6245533bcb101dfd718cfed61d59dc293f6768cedae28aa13ace164f5`.

Production destination: `src/commonMain/kotlin/io/github/kotlinmania/serdejson/lexical/generated/errors/Errors.kt`. The body is byte-identical to the complete raw Native-verified output, SHA-256 `6bb0472cfffdffc4a6595c2eadfbea6c738ed39b9514673a5714b9775bfd372c`; only package/provenance headers were added. Generator commit: `3318d3b2d952a1f18b9d74a4a57b15973a35951d`.

Existing `lexical/Errors.kt` entry points now forward to generated scale, halfscale, and error-estimation methods. Algorithm.kt callers therefore execute the generated implementation without changing their existing argument order or types. The adapter supplies the explicit FloatTraits provider and a read-only scalar snapshot. It implements no error arithmetic. The actual f32/f64 providers and mask helpers remain in Num.kt/Rounding.kt and were compared against their original Rust dependencies; this does not claim those complete dependency files are translated.

The complete rebuilt production KLIB matches 8,095 canonical Rust observations through its generated API and another 8,095 through the existing adapter API. Both diff files are empty. The original adapter baseline also matched these cases and is preserved in the generator receipt; this adoption does not claim a newly discovered behavior fix. All 131 Serde JSON Native tests pass. Float values are never converted through JVM/Java APIs.

Validation is pinned to Rust aarch64-apple-darwin revision 7189d39d9ce936881b8132c6addeaf3e295cd455 and Kotlin/Native macos_arm64, with overflow/debug checks on, panic unwind and usize64. Cases cover f32/f64 underflow thresholds, unsigned overflow/wrapping decisions, extreme i32 exponents and unchanged input state. These valid f32/f64 cases have zero expected panics. The snapshot adapter has an allocation cost; no allocation/Swift ABI or other-platform runtime equivalence is claimed. Shared inputs must remain unmodified during the call.

No dependency/configuration, target, FFI or kernel changes; no remote action or publication. One newly adopted whole Rust file. Supporting record declarations, bindings, adapters and repeated comparisons add zero files.

Reproduce after compiling the Native production library: `python3 verification/generated-errors-01/check_production.py --out tmp/generated-errors-01/production-new`.
