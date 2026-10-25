use serde::de::{Visitor, SeqAccess};
use serde::Deserializer;
use serde_json::{Value, json};
use std::fmt;
struct Recovery;
impl<'de> Visitor<'de> for Recovery {
    type Value = (usize, usize, i64, usize, bool);
    fn expecting(&self, f: &mut fmt::Formatter) -> fmt::Result { f.write_str("sequence") }
    fn visit_seq<A: SeqAccess<'de>>(self, mut a: A) -> Result<Self::Value,A::Error> {
        let before = a.size_hint().unwrap();
        assert!(a.next_element::<i64>().is_err());
        let after = a.size_hint().unwrap();
        let next = a.next_element::<i64>()?.unwrap();
        let end = a.size_hint().unwrap();
        Ok((before,after,next,end,a.next_element::<i64>()?.is_none()))
    }
}
fn main() {
    for (text, expected) in [("[]",vec![]), ("[null]",vec![None]), ("[null,7,null,9]",vec![None,Some(7),None,Some(9)])] {
        assert_eq!(serde_json::from_str::<Vec<Option<i64>>>(text).unwrap(),expected);
        let value: Value = serde_json::from_str(text).unwrap();
        assert_eq!(serde_json::from_value::<Vec<Option<i64>>>(value.clone()).unwrap(),expected);
        assert_eq!(serde_json::from_value::<Value>(value.clone()).unwrap(),value);
        println!("array|{}|ok",text);
    }
    for text in ["[null,\"bad\",9]", "[null,]"] {
        let error = serde_json::from_str::<Vec<Option<i64>>>(text).unwrap_err();
        println!("error|{}|{}",text,error);
    }
    let state = json!(["bad",7]).deserialize_seq(Recovery).unwrap();
    assert_eq!(state,(2,1,7,0,true));
    println!("recovery|2|1|7|0|true");
}
