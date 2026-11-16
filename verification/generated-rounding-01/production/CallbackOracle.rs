#![allow(dead_code, unused_imports)]
mod lexical {
    #[path = "/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/float.rs"] pub mod float;
    #[path = "/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/num.rs"] pub mod num;
    #[path = "/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/rounding.rs"] pub mod rounding;
    #[path = "/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/shift.rs"] pub mod shift;
}
use lexical::{float::ExtendedFloat, num::Float, rounding::*};
use std::panic::{catch_unwind, AssertUnwindSafe};

fn generic<F: Float>(width: u32) {
    let exponents = [i32::MIN, i32::MIN + 64, F::DENORMAL_EXPONENT - 65,
        F::DENORMAL_EXPONENT - 64, F::DENORMAL_EXPONENT - F::DEFAULT_SHIFT - 1,
        F::DENORMAL_EXPONENT - F::DEFAULT_SHIFT, F::DENORMAL_EXPONENT,
        -1, 0, F::MAX_EXPONENT - 1, F::MAX_EXPONENT, F::MAX_EXPONENT + 1,
        i32::MAX - 64, i32::MAX];
    for mant in [0, 1, 3, 0x7fff_ffff_ffff_ffff, 0x8000_0000_0000_0000, u64::MAX] {
        for exp in exponents {
            for op in 0..3 {
                for algorithm in 0..4 {
                    let mut fp = ExtendedFloat { mant, exp };
                    let mut calls = 0;
                    let mut observed = None;
                    let mut callback_state = None;
                    let result = catch_unwind(AssertUnwindSafe(|| {
                        let callback = |value: &mut ExtendedFloat, shift| {
                            calls += 1;
                            observed = Some(shift);
                            callback_state = Some((value.mant, value.exp));
                            match algorithm {
                                0 => round_nearest_tie_even(value, shift),
                                1 => round_downward(value, shift),
                                2 => {},
                                _ => { value.mant = 17; panic!("callback"); }
                            }
                        };
                        match op {
                            0 => round_to_float::<F, _>(&mut fp, callback),
                            1 => round_to_native::<F, _>(&mut fp, callback),
                            _ => avoid_overflow::<F>(&mut fp),
                        }
                    }));
                    println!("generic|{width}|{op}|{algorithm}|{mant}|{exp}|{}|{}|{}|{calls}|{:?}|{:?}",
                        if result.is_ok() { "O" } else { "P" }, fp.mant, fp.exp, observed, callback_state);
                }
            }
        }
    }
}

fn main() {
    std::panic::set_hook(Box::new(|_| {}));
    for n in [0, 1, 2, 23, 24, 31, 32, 52, 53, 63, 64, 65, u64::MAX] {
        for op in 0..3 {
            let result = catch_unwind(|| match op { 0 => nth_bit(n), 1 => lower_n_mask(n), _ => lower_n_halfway(n) });
            println!("mask|{op}|{n}|{}", result.map(|v| format!("O|{v}")).unwrap_or_else(|_| "P".into()));
        }
        for bit in [0, 1, 23, 32, 52, 63, 64, 65, u64::MAX] {
            let result = catch_unwind(|| internal_n_mask(bit, n));
            println!("internal|{bit}|{n}|{}", result.map(|v| format!("O|{v}")).unwrap_or_else(|_| "P".into()));
        }
    }
    for mant in [0, 1, 2, 3, 0x8000_0000_0000_0000, u64::MAX] {
        for exp in [i32::MIN, -64, -1, 0, 1, 64, i32::MAX] {
            for shift in [i32::MIN, -1, 0, 1, 2, 23, 32, 52, 63, 64, 65, i32::MAX] {
                for op in 0..3 {
                    let mut fp = ExtendedFloat { mant, exp };
                    let mut flags = (false, false);
                    let result = catch_unwind(AssertUnwindSafe(|| match op {
                        0 => flags = round_nearest(&mut fp, shift),
                        1 => round_nearest_tie_even(&mut fp, shift),
                        _ => round_downward(&mut fp, shift),
                    }));
                    println!("shift|{op}|{mant}|{exp}|{shift}|{}|{}|{}|{}|{}",
                        if result.is_ok() { "O" } else { "P" }, fp.mant, fp.exp, flags.0, flags.1);
                }
            }
            for above in [false, true] { for halfway in [false, true] {
                let mut fp = ExtendedFloat { mant, exp };
                let result = catch_unwind(AssertUnwindSafe(|| tie_even(&mut fp, above, halfway)));
                println!("tie|{mant}|{exp}|{above}|{halfway}|{}|{}|{}",
                    if result.is_ok() { "O" } else { "P" }, fp.mant, fp.exp);
            }}
        }
    }
    generic::<f32>(32);
    generic::<f64>(64);
}
