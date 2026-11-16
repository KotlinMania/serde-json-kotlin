#[cfg(test)]
mod tests {
    use serde_json::{Map, Value};
    const A: &str = "\u{10000}";
    const B: &str = "\u{e000}";
    const MAX: &str = "\u{10ffff}";
    const BMP_MAX: &str = "\u{ffff}";
    fn map(keys: &[&str]) -> Map<String, Value> {
        keys.iter()
            .map(|k| ((*k).into(), Value::String((*k).into())))
            .collect()
    }
    fn check(keys: &[&str], expected: &[&str]) {
        let mut value = map(keys);
        value.sort_keys();
        assert_eq!(
            value.keys().map(String::as_str).collect::<Vec<_>>(),
            expected
        );
        for key in keys {
            assert_eq!(value.get(*key).and_then(Value::as_str), Some(*key));
        }
    }
    #[test]
    fn ascii_and_prefix_order() {
        check(&["b", "a", "aa", ""], &["", "a", "aa", "b"]);
    }
    #[test]
    fn bmp_precedes_supplementary() {
        check(&[A, B], &[B, A]);
    }
    #[test]
    fn maximum_scalar_follows_bmp() {
        check(&[MAX, BMP_MAX], &[BMP_MAX, MAX]);
    }
    #[test]
    fn no_unicode_normalization() {
        check(&["é", "e\u{301}", "e"], &["e", "e\u{301}", "é"]);
    }
    #[test]
    fn supplementary_prefix_order() {
        check(
            &["\u{10000}a", A, "\u{e000}a", B],
            &[B, "\u{e000}a", A, "\u{10000}a"],
        );
    }
    #[test]
    fn recursive_objects_inside_arrays() {
        let mut inner = map(&[A, B]);
        inner.insert(
            "array".into(),
            Value::Array(vec![Value::Object(map(&[MAX, BMP_MAX]))]),
        );
        let mut value = Value::Object(inner);
        value.sort_all_objects();
        assert_eq!(
            value
                .as_object()
                .unwrap()
                .keys()
                .map(String::as_str)
                .collect::<Vec<_>>(),
            vec!["array", B, A]
        );
        assert_eq!(
            value["array"][0]
                .as_object()
                .unwrap()
                .keys()
                .map(String::as_str)
                .collect::<Vec<_>>(),
            vec![BMP_MAX, MAX]
        );
    }
    #[test]
    fn values_and_repeat_sort_are_preserved() {
        let mut value = map(&[A, B]);
        value.insert(A.into(), Value::String("replaced".into()));
        value.sort_keys();
        value.sort_keys();
        assert_eq!(
            value.keys().map(String::as_str).collect::<Vec<_>>(),
            vec![B, A]
        );
        assert_eq!(value.get(A).and_then(Value::as_str), Some("replaced"));
        assert_eq!(value.get(B).and_then(Value::as_str), Some(B));
    }
}
