#[cfg(test)]
mod tests {
    use serde_json::{Map, Value};
    fn array() -> Value {
        Value::Array(vec![
            Value::String("zero".into()),
            Value::String("one".into()),
        ])
    }
    #[test]
    fn negative_zero_is_rejected() {
        let value = array();
        for token in ["-0", "-00", "-000000"] {
            assert_eq!(value.pointer(&format!("/{token}")), None, "{token}");
        }
    }
    #[test]
    fn arabic_and_devanagari_digits_are_rejected() {
        let value = array();
        for token in ["٠", "١", "०", "१"] {
            assert_eq!(value.pointer(&format!("/{token}")), None, "{token}");
        }
    }
    #[test]
    fn fullwidth_digits_are_rejected() {
        let value = array();
        for token in ["０", "１"] {
            assert_eq!(value.pointer(&format!("/{token}")), None, "{token}");
        }
    }
    #[test]
    fn canonical_array_indices_work() {
        let value = array();
        assert_eq!(value.pointer("/0").and_then(Value::as_str), Some("zero"));
        assert_eq!(value.pointer("/1").and_then(Value::as_str), Some("one"));
        assert!(core::ptr::eq(value.pointer("").unwrap(), &value));
    }
    #[test]
    fn object_keys_are_not_array_indices() {
        let mut map = Map::new();
        for key in ["-0", "٠", "१", "０", "01", "+0"] {
            map.insert(key.into(), Value::String(key.into()));
        }
        let value = Value::Object(map);
        for key in ["-0", "٠", "१", "０", "01", "+0"] {
            assert_eq!(
                value.pointer(&format!("/{key}")).and_then(Value::as_str),
                Some(key)
            );
        }
    }
    #[test]
    fn pointer_escapes_and_empty_keys_still_work() {
        let mut map = Map::new();
        for (key, value) in [("a/b", "slash"), ("~1", "tilde"), ("", "empty")] {
            map.insert(key.into(), Value::String(value.into()));
        }
        let value = Value::Object(map);
        for (pointer, expected) in [("/a~1b", "slash"), ("/~01", "tilde"), ("/", "empty")] {
            assert_eq!(
                value.pointer(pointer).and_then(Value::as_str),
                Some(expected)
            );
        }
        assert_eq!(value.pointer("a/b"), None);
    }
    #[test]
    fn leading_zero_overflow_and_malformed_indices_are_rejected() {
        let value = array();
        for token in [
            "00",
            "01",
            "+0",
            "",
            "-1",
            "-",
            "2",
            "2147483648",
            "18446744073709551615",
            "18446744073709551616",
            "999999999999999999999999",
        ] {
            assert_eq!(value.pointer(&format!("/{token}")), None, "{token}");
        }
    }
}
