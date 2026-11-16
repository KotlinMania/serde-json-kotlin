#![allow(dead_code)]
extern crate alloc;
#[path="/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/bhcomp.rs"] mod bhcomp;
#[path="/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/bignum.rs"] mod bignum;
#[path="/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/digit.rs"] mod digit;
#[path="/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/exponent.rs"] mod exponent;
#[path="/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/float.rs"] mod float;
#[path="/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/math.rs"] mod math;
#[path="/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/num.rs"] mod num;
#[path="/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/rounding.rs"] mod rounding;
#[path="/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/shift.rs"] mod shift;
#[path="/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/small_powers.rs"] mod small_powers;
#[path="/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/large_powers.rs"] mod large_powers;
#[path="/Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/large_powers64.rs"] mod large_powers64;
fn main(){
std::panic::set_hook(Box::new(|_|{}));
for (b,integer,fraction,exponent) in [(1.0_f64,"1","",0),(0.00125,"","00125",0),(12.5,"12","5",0),(0.9,"9","",-1)] {
 println!("{}",bhcomp::bhcomp(b,integer.as_bytes(),fraction.as_bytes(),exponent).to_bits());
}
for c in [b'A',255] {println!("panic:{}",std::panic::catch_unwind(||bhcomp::bhcomp(1.0_f64,&[c],&[],0)).is_err());}
}
