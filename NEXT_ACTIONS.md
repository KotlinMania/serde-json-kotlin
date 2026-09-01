# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 29/68 (42.6%)
- **Function parity:** 278/832 matched (target 466) — 33.4%
- **Class/type parity:** 20/190 matched (target 110) — 10.5%
- **Combined symbol parity:** 298/1022 matched (target 576) — 29.2%
- **Average inline-code cosine:** 0.42 (function body across 24 matched files)
- **Average documentation cosine:** 0.63 (doc text across 24 matched files)
- **Cheat-zeroed Files:** 7
- **Critical Issues:** 20 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. serde_json.map

- **Target:** `serdejson.Map`
- **Similarity:** 0.17
- **Dependents:** 5
- **Priority Score:** 5607508.5
- **Functions:** 15/49 matched (target 25)
- **Missing functions:** `remove_entry`, `swap_remove_entry`, `shift_remove`, `shift_remove_entry`, `append`, `entry`, `iter`, `iter_mut`, `values_mut`, `into_values`, `retain`, `sort_keys`, `default`, `clone`, `clone_from`, `eq`, `hash`, `index`, `index_mut`, `fmt`, `serialize`, `deserialize`, `expecting`, `visit_unit`, `visit_map`, `from_iter`, `extend`, `into_deserializer`, `key`, `or_insert`, `or_insert_with`, `and_modify`, `into_mut`, `into_iter`
- **Types:** 0/26 matched (target 3)
- **Missing types:** `Map`, `MapImpl`, `Output`, `Visitor`, `Value`, `Deserializer`, `Entry`, `VacantEntry`, `OccupiedEntry`, `VacantEntryImpl`, `OccupiedEntryImpl`, `Item`, `IntoIter`, `Iter`, `IterImpl`, `IterMut`, `IterMutImpl`, `IntoIterImpl`, `Keys`, `KeysImpl`, `Values`, `ValuesImpl`, `ValuesMut`, `ValuesMutImpl`, `IntoValues`, `IntoValuesImpl`

### 2. serde_json.number

- **Target:** `serdejson.Number`
- **Similarity:** 0.12
- **Dependents:** 4
- **Priority Score:** 4324509.0
- **Functions:** 12/34 matched (target 28)
- **Missing functions:** `eq`, `hash`, `as_i128`, `as_u128`, `from_i128`, `from_u128`, `as_str`, `from_string_unchecked`, `fmt`, `deserialize`, `expecting`, `visit_i64`, `visit_i128`, `visit_u64`, `visit_u128`, `visit_f64`, `visit_map`, `visit_str`, `invalid_number`, `next_key_seed`, `next_value_seed`, `deserialize_any`
- **Types:** 1/11 matched (target 10)
- **Missing types:** `Number`, `NumberVisitor`, `Value`, `NumberKey`, `FieldVisitor`, `NumberFromString`, `Visitor`, `Error`, `NumberDeserializer`, `NumberFieldDeserializer`

### 3. serde_json.error

- **Target:** `serdejson.Error`
- **Similarity:** 0.33
- **Dependents:** 4
- **Priority Score:** 4122606.8
- **Functions:** 12/20 matched (target 16)
- **Missing functions:** `io_error_kind`, `from`, `fmt`, `source`, `invalid_type`, `invalid_value`, `make_error`, `starts_with_digit`
- **Types:** 2/6 matched (target 29)
- **Missing types:** `Error`, `Result`, `ErrorImpl`, `JsonUnexpected`

### 4. serde_json.iter

- **Target:** `serdejson.Iter`
- **Similarity:** 0.68
- **Dependents:** 2
- **Priority Score:** 2020703.2
- **Functions:** 4/5 matched
- **Missing functions:** `new`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Item`

### 5. serde_json.de

- **Target:** `serdejson.De [STUB]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1709610.0
- **Functions:** 26/84 matched (target 52)
- **Missing functions:** `new`, `from_reader`, `from_slice`, `from_str`, `visit`, `invalid_type`, `end`, `into_iter`, `disable_recursion_limit`, `peek_or_null`, `eat_char`, `next_char`, `next_char_or_null`, `error`, `peek_error`, `parse_whitespace`, `peek_invalid_type`, `deserialize_number`, `do_deserialize_f32`, `do_deserialize_i128`, `do_deserialize_u128`, `scan_integer128`, `fix_position`, `parse_ident`, `parse_integer`, `parse_decimal`, `parse_exponent`, `f64_from_parts`, `parse_long_integer`, `parse_long_decimal`, `parse_long_exponent`, `parse_decimal_overflow`, `parse_exponent_overflow`, `f64_long_from_parts`, `parse_any_signed_number`, `parse_any_number`, `scan_or_eof`, `scan_integer`, `scan_number`, `scan_decimal`, `scan_exponent`, `parse_object_colon`, `end_seq`, `end_map`, `ignore_value`, `ignore_integer`, `ignore_decimal`, `ignore_exponent`, `deserialize_raw_value`, `has_next_element`, `has_next_key`, `variant_seed`, `unit_variant`, `newtype_variant_seed`, `tuple_variant`, `struct_variant`, `peek_end_of_value`, `from_trait`
- **Types:** 0/12 matched (target 8)
- **Missing types:** `Deserializer`, `ParserNumber`, `Err`, `Error`, `SeqAccess`, `MapAccess`, `VariantAccess`, `Variant`, `UnitVariantAccess`, `MapKey`, `StreamDeserializer`, `Item`

### 6. value.index

- **Target:** `serdejson.ValueIndex [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1081010.0
- **Functions:** 2/6 matched (target 7)
- **Missing functions:** `index_into_mut`, `fmt`, `index`, `index_mut`
- **Types:** 0/4 matched (target 3)
- **Missing types:** `Index`, `Sealed`, `Type`, `Output`

### 7. lexical.cached_float80

- **Target:** `lexical.CachedFloat80`
- **Similarity:** 0.97
- **Dependents:** 1
- **Priority Score:** 1000100.2
- **Functions:** 1/1 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 8. serde_json.ser

- **Target:** `serdejson.Ser`
- **Similarity:** 0.05
- **Dependents:** 0
- **Priority Score:** 820709.5
- **Functions:** 26/87 matched (target 33)
- **Missing functions:** `new`, `pretty`, `with_formatter`, `into_inner`, `serialize_i128`, `serialize_u128`, `serialize_newtype_struct`, `serialize_newtype_variant`, `serialize_some`, `collect_str`, `write_str`, `serialize_element`, `serialize_field`, `serialize_key`, `serialize_value`, `invalid_number`, `invalid_raw_value`, `key_must_be_a_string`, `float_key_must_be_finite`, `write_null`, `write_bool`, `write_i8`, `write_i16`, `write_i32`, `write_i64`, `write_i128`, `write_u8`, `write_u16`, `write_u32`, `write_u64`, `write_u128`, `write_f32`, `write_f64`, `write_number_str`, `begin_string`, `end_string`, `write_string_fragment`, `write_char_escape`, `write_byte_array`, `begin_array`, `end_array`, `begin_array_value`, `end_array_value`, `begin_object`, `end_object`, `begin_object_key`, `end_object_key`, `begin_object_value`, `end_object_value`, `write_raw_fragment`, `with_indent`, `default`, `format_escaped_str`, `format_escaped_str_contents`, `to_writer`, `to_writer_pretty`, `to_vec`, `to_vec_pretty`, `to_string`, `to_string_pretty`, `indent`
- **Types:** 0/20 matched (target 4)
- **Missing types:** `Serializer`, `Ok`, `Error`, `SerializeSeq`, `SerializeTuple`, `SerializeTupleStruct`, `SerializeTupleVariant`, `SerializeMap`, `SerializeStruct`, `SerializeStructVariant`, `Adapter`, `State`, `Compound`, `MapKeySerializer`, `NumberStrEmitter`, `RawValueStrEmitter`, `CharEscape`, `Formatter`, `CompactFormatter`, `PrettyFormatter`

### 9. serde_json.raw

- **Target:** `serdejson.Raw`
- **Similarity:** 0.01
- **Dependents:** 0
- **Priority Score:** 646609.9
- **Functions:** 1/52 matched (target 6)
- **Missing functions:** `from_borrowed`, `from_owned`, `into_owned`, `clone`, `to_owned`, `default`, `fmt`, `from_string`, `get`, `from`, `to_raw_value`, `deserialize`, `expecting`, `visit_map`, `visit_str`, `visit_borrowed_str`, `visit_string`, `deserialize_any`, `next_key_seed`, `next_value_seed`, `into_deserializer`, `deserialize_bool`, `deserialize_i8`, `deserialize_i16`, `deserialize_i32`, `deserialize_i64`, `deserialize_i128`, `deserialize_u8`, `deserialize_u16`, `deserialize_u32`, `deserialize_u64`, `deserialize_u128`, `deserialize_f32`, `deserialize_f64`, `deserialize_char`, `deserialize_str`, `deserialize_string`, `deserialize_bytes`, `deserialize_byte_buf`, `deserialize_option`, `deserialize_unit`, `deserialize_unit_struct`, `deserialize_newtype_struct`, `deserialize_seq`, `deserialize_tuple`, `deserialize_tuple_struct`, `deserialize_map`, `deserialize_struct`, `deserialize_enum`, `deserialize_identifier`, `deserialize_ignored_any`
- **Types:** 1/14 matched (target 2)
- **Missing types:** `Owned`, `ReferenceVisitor`, `Value`, `BoxedVisitor`, `RawKey`, `FieldVisitor`, `ReferenceFromString`, `BoxedFromString`, `RawKeyDeserializer`, `Error`, `OwnedRawDeserializer`, `BorrowedRawDeserializer`, `Deserializer`

### 10. value.de

- **Target:** `serdejson.ValueDe [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 356910.0
- **Functions:** 34/50 matched (target 48)
- **Missing functions:** `visit_bool`, `visit_i64`, `visit_i128`, `visit_u64`, `visit_u128`, `visit_f64`, `visit_str`, `visit_string`, `visit_none`, `visit_some`, `visit_seq`, `from_str`, `into_deserializer`, `new`, `visit_array_ref`, `invalid_type`
- **Types:** 0/19 matched (target 6)
- **Missing types:** `ValueVisitor`, `Value`, `Err`, `Error`, `EnumDeserializer`, `Variant`, `Deserializer`, `VariantDeserializer`, `SeqDeserializer`, `MapDeserializer`, `EnumRefDeserializer`, `VariantRefDeserializer`, `SeqRefDeserializer`, `MapRefDeserializer`, `MapKeyDeserializer`, `KeyClassifier`, `KeyClass`, `BorrowedCowStrDeserializer`, `UnitOnly`

### 11. serde_json.read

- **Target:** `serdejson.Read`
- **Similarity:** 0.08
- **Dependents:** 0
- **Priority Score:** 324109.2
- **Functions:** 6/31 matched (target 12)
- **Missing functions:** `deref`, `new`, `parse_str_bytes`, `peek_position`, `parse_str_raw`, `ignore_str`, `decode_hex_escape`, `begin_raw_buffering`, `end_raw_buffering`, `set_failed`, `position_of_index`, `skip_to_escape`, `skip_to_escape_slow`, `is_escape`, `next_or_eof`, `peek_or_eof`, `error`, `as_str`, `parse_escape`, `parse_unicode_escape`, `push_wtf8_codepoint`, `ignore_escape`, `decode_hex_val_slow`, `build_hex_table`, `decode_four_hex_digits`
- **Types:** 3/10 matched (target 3)
- **Missing types:** `Read`, `Reference`, `Target`, `IoRead`, `Sealed`, `Chunk`, `Fused`

### 12. value.ser

- **Target:** `serdejson.ValueSer`
- **Similarity:** 0.11
- **Dependents:** 0
- **Priority Score:** 295508.9
- **Functions:** 26/41 matched (target 29)
- **Missing functions:** `serialize`, `serialize_i128`, `serialize_u128`, `serialize_newtype_struct`, `serialize_newtype_variant`, `serialize_some`, `collect_str`, `serialize_element`, `serialize_field`, `serialize_key`, `serialize_value`, `key_must_be_a_string`, `float_key_must_be_finite`, `invalid_number`, `invalid_raw_value`
- **Types:** 0/14 matched (target 5)
- **Missing types:** `Serializer`, `Ok`, `Error`, `SerializeSeq`, `SerializeTuple`, `SerializeTupleStruct`, `SerializeTupleVariant`, `SerializeMap`, `SerializeStruct`, `SerializeStructVariant`, `SerializeVec`, `MapKeySerializer`, `NumberValueEmitter`, `RawValueEmitter`

### 13. value.mod

- **Target:** `serdejson.Value [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 93610.0
- **Functions:** 26/34 matched (target 32)
- **Missing functions:** `fmt`, `write`, `flush`, `io_error`, `get_mut`, `as_object_mut`, `as_array_mut`, `pointer_mut`
- **Types:** 1/2 matched (target 7)
- **Missing types:** `WriterFormatter`

### 14. lexical.math

- **Target:** `lexical.Math [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 84210.0
- **Functions:** 32/38 matched (target 52)
- **Missing functions:** `as_limb`, `as_wide`, `split_u64`, `hi64_1`, `hi64_2`, `hi64_3`
- **Types:** 2/4 matched (target 6)
- **Missing types:** `Hi64`, `Math`

### 15. lexical.num

- **Target:** `lexical.Num`
- **Similarity:** 0.54
- **Dependents:** 0
- **Priority Score:** 32004.6
- **Functions:** 13/13 matched (target 19)
- **Missing functions:** _none_
- **Types:** 4/7 matched (target 8)
- **Missing types:** `AsCast`, `Float`, `Unsigned`

### 16. lexical.bignum

- **Target:** `lexical.Bignum`
- **Similarity:** 0.18
- **Dependents:** 0
- **Priority Score:** 20408.2
- **Functions:** 1/3 matched (target 4)
- **Missing functions:** `data`, `data_mut`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 17. lexical.rounding

- **Target:** `lexical.Rounding`
- **Similarity:** 0.75
- **Dependents:** 0
- **Priority Score:** 11302.5
- **Functions:** 12/13 matched (target 12)
- **Missing functions:** `downard`
- **Types:** 0/0 matched
- **Missing types:** _none_

### 18. lexical.cached

- **Target:** `lexical.Cached`
- **Similarity:** 0.78
- **Dependents:** 0
- **Priority Score:** 10902.2
- **Functions:** 5/6 matched (target 5)
- **Missing functions:** `len`
- **Types:** 3/3 matched (target 4)
- **Missing types:** _none_

### 19. value.partial_eq

- **Target:** `serdejson.ValuePartialEq`
- **Similarity:** 0.48
- **Dependents:** 0
- **Priority Score:** 10705.2
- **Functions:** 6/7 matched
- **Missing functions:** `eq`
- **Types:** 0/0 matched
- **Missing types:** _none_

### 20. lexical.bhcomp

- **Target:** `lexical.Bhcomp`
- **Similarity:** 0.66
- **Dependents:** 0
- **Priority Score:** 10703.4
- **Functions:** 6/7 matched
- **Missing functions:** `round_nearest_tie_even`
- **Types:** 0/0 matched
- **Missing types:** _none_

### 21. value.from

- **Target:** `serdejson.ValueFrom`
- **Similarity:** 0.52
- **Dependents:** 0
- **Priority Score:** 10204.8
- **Functions:** 1/2 matched (target 19)
- **Missing functions:** `from_iter`
- **Types:** 0/0 matched
- **Missing types:** _none_

### 22. lexical.errors

- **Target:** `lexical.Errors`
- **Similarity:** 0.91
- **Dependents:** 0
- **Priority Score:** 500.9
- **Functions:** 4/4 matched (target 5)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_

### 23. lexical.algorithm

- **Target:** `lexical.Algorithm`
- **Similarity:** 0.81
- **Dependents:** 0
- **Priority Score:** 401.9
- **Functions:** 4/4 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 24. lexical.shift

- **Target:** `lexical.Shift`
- **Similarity:** 0.86
- **Dependents:** 0
- **Priority Score:** 301.4
- **Functions:** 3/3 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 25. io.mod

- **Target:** `serdejson.Io [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 15)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 6)
- **Missing types:** _none_

### 26. lexical.mod

- **Target:** `lexical.Lexical [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 20)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 27. lexical.large_powers64

- **Target:** `lexical.LargePowers64`
- **Similarity:** 1.00
- **Dependents:** 0
- **Priority Score:** 0.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Matched

| Source | Target | Path |
|--------|--------|------|
| `serde_json.macros` | `serdejson.Macros` | `serde_json/src/macros` |
| `serde_json.lib` | `serdejson.Lib` | `serde_json/src/lib` |

