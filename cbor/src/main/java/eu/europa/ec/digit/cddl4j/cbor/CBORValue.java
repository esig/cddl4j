/**
 * Copyright (C) 2026 European Union
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the EUROPEAN UNION PUBLIC LICENCE v. 1.2 as
 * published by the European Union.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * EUROPEAN UNION PUBLIC LICENCE v. 1.2 for further details.
 * <p>
 * You should have received a copy of the EUROPEAN UNION PUBLIC LICENCE v. 1.2.
 * along with this program. If not,
 * see https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12 .
 */
package eu.europa.ec.digit.cddl4j.cbor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Mutable representation of a CBOR data item.
 *
 * <p>A {@code CBORValue} represents both scalar and container CBOR values.
 * Container values can be modified through the dedicated editing methods
 * {@link #editArray()} and {@link #editMap()}.
 *
 * <p>The editors are lightweight views over the enclosing {@code CBORValue}.
 * They do not represent a separate CBOR model and do not copy the value.
 * Modification is performed directly on the {@code CBORValue}.
 *
 * <p>For example:
 *
 * <pre>
 * CBORValue value = CBOR.map()
 *         .editMap()
 *         .put("name", "Alice")
 *         .put("age", 42)
 *         .put("active", true)
 *         .value();
 * </pre>
 *
 * <p>Nested values can be edited directly:
 *
 * <pre>
 * value.get("address")
 *         .editMap()
 *         .put("city", "Brussels")
 *         .put("country", "BE");
 * </pre>
 *
 * <p>CBOR maps preserve insertion order. They do not use a Java
 * {@link java.util.Map} internally because CBOR permits arbitrary CBOR
 * values as map keys.
 */
public final class CBORValue {

    /** Type of the CBOR value. */
    private final CBORType type;

    /** The underlying mutable value. */
    private Object value;

    /**
     * Creates a CBOR value.
     *
     * @param type the CBOR type
     * @param value the value
     */
    private CBORValue(CBORType type, Object value) {
        this.type = Objects.requireNonNull(type, "type");
        this.value = copyValue(type, value);
    }

    /**
     * The canonical CBOR {@code true} value.
     */
    public static final CBORValue TRUE = new CBORValue(CBORType.BOOLEAN, Boolean.TRUE);

    /**
     * The canonical CBOR {@code false} value.
     */
    public static final CBORValue FALSE = new CBORValue(CBORType.BOOLEAN, Boolean.FALSE);

    /**
     * The canonical CBOR {@code null} value.
     */
    public static final CBORValue NULL = new CBORValue(CBORType.NULL, null);

    /**
     * The canonical CBOR {@code undefined} value.
     */
    public static final CBORValue UNDEFINED = new CBORValue(CBORType.UNDEFINED, null);

    /**
     * Returns the CBOR type represented by this value.
     *
     * @return the CBOR type
     */
    public CBORType getType() {
        return type;
    }

    /**
     * Tests whether this value is a CBOR null.
     *
     * @return {@code true} if this value is null
     */
    public boolean isNull() {
        return type == CBORType.NULL;
    }

    /**
     * Tests whether this value is CBOR undefined.
     *
     * @return {@code true} if this value is undefined
     */
    public boolean isUndefined() {
        return type == CBORType.UNDEFINED;
    }

    /**
     * Tests whether this value is a CBOR boolean.
     *
     * @return {@code true} if this value is a boolean
     */
    public boolean isBoolean() {
        return type == CBORType.BOOLEAN;
    }

    /**
     * Tests whether this value is a CBOR integer.
     *
     * @return {@code true} if this value is an integer
     */
    public boolean isInteger() {
        return type == CBORType.INTEGER;
    }

    /**
     * Tests whether this value is a negative CBOR integer.
     *
     * <p>A negative CBOR integer is represented using CBOR major type 1.
     * This method returns {@code false} for values of any other CBOR type.
     *
     * @return {@code true} if this value is an integer less than zero
     */
    public boolean isNegativeInteger() {
        return isInteger() && asInteger().signum() < 0;
    }

    /**
     * Tests whether this value is an unsigned CBOR integer.
     *
     * <p>An unsigned CBOR integer is a non-negative integer represented using
     * CBOR major type 0. This method returns {@code false} for values of any
     * other CBOR type, including tagged bignums such as CBOR tag 2
     * ({@code biguint}).
     *
     * @return {@code true} if this value is a non-negative integer
     */
    public boolean isUnsignedInteger() {
        return isInteger() && asInteger().signum() >= 0;
    }

    /**
     * Tests whether this value is a CBOR floating-point value.
     *
     * @return {@code true} if this value is floating point
     */
    public boolean isFloatingPoint() {
        return type == CBORType.FLOATING_POINT;
    }

    /**
     * Tests whether this value is a CBOR byte string.
     *
     * @return {@code true} if this value is a byte string
     */
    public boolean isByteString() {
        return type == CBORType.BYTE_STRING;
    }

    /**
     * Tests whether this value is a CBOR text string.
     *
     * @return {@code true} if this value is a text string
     */
    public boolean isTextString() {
        return type == CBORType.TEXT_STRING;
    }

    /**
     * Tests whether this value is a CBOR array.
     *
     * @return {@code true} if this value is an array
     */
    public boolean isArray() {
        return type == CBORType.ARRAY;
    }

    /**
     * Tests whether this value is a CBOR map.
     *
     * @return {@code true} if this value is a map
     */
    public boolean isMap() {
        return type == CBORType.MAP;
    }

    /**
     * Tests whether this value is a CBOR tag.
     *
     * @return {@code true} if this value is a tag
     */
    public boolean isTag() {
        return type == CBORType.TAG;
    }

    /**
     * Tests whether this value is a CBOR simple value.
     *
     * @return {@code true} if this value is a simple value
     */
    public boolean isSimpleValue() {
        return type == CBORType.SIMPLE_VALUE;
    }

    /**
     * Returns the tag number.
     *
     * @return the unsigned 64-bit tag number
     * @throws IllegalStateException if this value is not a tag
     */
    public BigInteger getTag() {
        requireType(CBORType.TAG);
        return ((TagValue) value).tag();
    }

    /**
     * Returns the value contained by this tag.
     *
     * <p>The returned object is the actual nested value. Modifying it also
     * modifies the tagged value.
     *
     * @return the tagged value
     * @throws IllegalStateException if this value is not a tag
     */
    public CBORValue getTaggedValue() {
        requireType(CBORType.TAG);
        return ((TagValue) value).value();
    }

    /**
     * Returns the number of elements in an array or map.
     *
     * @return the number of elements
     * @throws IllegalStateException if this value is not an array or map
     */
    public int size() {
        if (isArray()) {
            return arrayElements().size();
        }

        if (isMap()) {
            return mapEntries().size();
        }

        throw new IllegalStateException("Not a container");
    }

    /**
     * Returns an element from an array.
     *
     * <p>The returned object is the actual nested value.
     *
     * @param index the zero-based index
     * @return the array element
     * @throws IllegalStateException if this value is not an array
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public CBORValue get(int index) {
        requireType(CBORType.ARRAY);
        return arrayElements().get(index);
    }

    /**
     * Returns the value associated with a map key.
     *
     * <p>The supplied key is converted to a {@link CBORValue} using
     * {@link CBOR#value(Object)} before the lookup. Consequently, both ordinary
     * Java values and explicitly constructed {@link CBORValue} instances can be
     * used as keys.
     *
     * <p>The returned object is the actual nested value and is not copied.
     *
     * @param key the Java or CBOR value representing the map key
     * @return the associated value, or {@code null} if the key is absent
     * @throws IllegalArgumentException if the key type is not supported by
     *     {@link CBOR#value(Object)}
     * @throws IllegalStateException if this value is not a CBOR map
     */
    public CBORValue get(Object key) {
        requireType(CBORType.MAP);
        CBORValue cborKey = CBOR.value(key);

        for (CBORMapEntry entry : mapEntries()) {
            if (entry.key().equals(cborKey)) {
                return entry.value();
            }
        }

        return null;
    }

    /**
     * Returns the value associated with the supplied map key as a string.
     *
     * <p>The key is converted to a {@link CBORValue} using {@link CBOR#value(Object)}.
     * No type conversion is performed on the associated value. The value is returned
     * only when the key is present and the associated value is a CBOR text string.
     *
     * @param key the Java or CBOR value representing the map key
     * @return the string value, or {@code null} if the key is absent or the associated
     *     value is not a CBOR text string
     * @throws IllegalArgumentException if the key type is not supported by
     *     {@link CBOR#value(Object)}
     * @throws IllegalStateException if this value is not a CBOR map
     */
    public String getAsStringOrNull(Object key) {
        CBORValue value = get(key);
        return value == null ? null : value.asStringOrNull();
    }

    /**
     * Returns the value associated with the supplied map key as an integer.
     *
     * <p>The key is converted to a {@link CBORValue} using {@link CBOR#value(Object)}.
     * No type conversion is performed on the associated value. The value is returned
     * only when the key is present and the associated value is a CBOR integer.
     *
     * @param key the Java or CBOR value representing the map key
     * @return the integer value, or {@code null} if the key is absent or the associated
     *     value is not a CBOR integer
     * @throws IllegalArgumentException if the key type is not supported by
     *     {@link CBOR#value(Object)}
     * @throws IllegalStateException if this value is not a CBOR map
     */
    public BigInteger getAsIntegerOrNull(Object key) {
        CBORValue value = get(key);
        return value == null ? null : value.asIntegerOrNull();
    }

    /**
     * Returns the value associated with the supplied map key as a floating-point value.
     *
     * <p>The key is converted to a {@link CBORValue} using {@link CBOR#value(Object)}.
     * No type conversion is performed on the associated value. The value is returned
     * only when the key is present and the associated value is a CBOR floating-point value.
     *
     * @param key the Java or CBOR value representing the map key
     * @return the floating-point value, or {@code null} if the key is absent or the
     *     associated value is not a CBOR floating-point value
     * @throws IllegalArgumentException if the key type is not supported by
     *     {@link CBOR#value(Object)}
     * @throws IllegalStateException if this value is not a CBOR map
     */
    public Double getAsDoubleOrNull(Object key) {
        CBORValue value = get(key);
        return value == null ? null : value.asDoubleOrNull();
    }

    /**
     * Returns the value associated with the supplied map key as a boolean.
     *
     * <p>The key is converted to a {@link CBORValue} using {@link CBOR#value(Object)}.
     * No type conversion is performed on the associated value. The value is returned
     * only when the key is present and the associated value is a CBOR boolean.
     *
     * @param key the Java or CBOR value representing the map key
     * @return the boolean value, or {@code null} if the key is absent or the associated
     *     value is not a CBOR boolean
     * @throws IllegalArgumentException if the key type is not supported by
     *     {@link CBOR#value(Object)}
     * @throws IllegalStateException if this value is not a CBOR map
     */
    public Boolean getAsBooleanOrNull(Object key) {
        CBORValue value = get(key);
        return value == null ? null : value.asBooleanOrNull();
    }

    /**
     * Returns the value associated with the supplied map key as a byte string.
     *
     * <p>The key is converted to a {@link CBORValue} using {@link CBOR#value(Object)}.
     * No type conversion is performed on the associated value. The value is returned
     * only when the key is present and the associated value is a CBOR byte string.
     *
     * <p>The returned byte array is a defensive copy.
     *
     * @param key the Java or CBOR value representing the map key
     * @return the byte string, or {@code null} if the key is absent or the associated
     *     value is not a CBOR byte string
     * @throws IllegalArgumentException if the key type is not supported by
     *     {@link CBOR#value(Object)}
     * @throws IllegalStateException if this value is not a CBOR map
     */
    public byte[] getAsBytesOrNull(Object key) {
        CBORValue value = get(key);
        return value == null ? null : value.asBytesOrNull();
    }

    /**
     * Returns the value associated with the supplied map key as a CBOR array.
     *
     * <p>The key is converted to a {@link CBORValue} using {@link CBOR#value(Object)}.
     * No type conversion is performed on the associated value. The value is returned
     * only when the key is present and the associated value is a CBOR array.
     *
     * @param key the Java or CBOR value representing the map key
     * @return the CBOR array value, or {@code null} if the key is absent or the
     *     associated value is not a CBOR array
     * @throws IllegalArgumentException if the key type is not supported by
     *     {@link CBOR#value(Object)}
     * @throws IllegalStateException if this value is not a CBOR map
     */
    public CBORValue getAsArrayOrNull(Object key) {
        CBORValue value = get(key);
        return value == null ? null : value.asArrayOrNull();
    }

    /**
     * Returns the value associated with the supplied map key as a CBOR map.
     *
     * <p>The key is converted to a {@link CBORValue} using {@link CBOR#value(Object)}.
     * No type conversion is performed on the associated value. The value is returned
     * only when the key is present and the associated value is a CBOR map.
     *
     * @param key the Java or CBOR value representing the map key
     * @return the CBOR map value, or {@code null} if the key is absent or the associated
     *     value is not a CBOR map
     * @throws IllegalArgumentException if the key type is not supported by
     *     {@link CBOR#value(Object)}
     * @throws IllegalStateException if this value is not a CBOR map
     */
    public CBORValue getAsMapOrNull(Object key) {
        CBORValue value = get(key);
        return value == null ? null : value.asMapOrNull();
    }

    /**
     * Returns the array element at the supplied index as a string.
     *
     * <p>No type conversion is performed. The value is returned only when the
     * element exists and is a CBOR text string.
     *
     * @param index the array index
     * @return the string value, or {@code null} if the index is outside the array
     *     or the element is not a CBOR text string
     * @throws IllegalStateException if this value is not a CBOR array
     */
    public String getAsStringOrNull(int index) {
        CBORValue value = get(index);
        return value == null ? null : value.asStringOrNull();
    }

    /**
     * Returns the array element at the supplied index as an integer.
     *
     * <p>No type conversion is performed. The value is returned only when the
     * element exists and is a CBOR integer.
     *
     * @param index the array index
     * @return the integer value, or {@code null} if the index is outside the array
     *     or the element is not a CBOR integer
     * @throws IllegalStateException if this value is not a CBOR array
     */
    public BigInteger getAsIntegerOrNull(int index) {
        CBORValue value = get(index);
        return value == null ? null : value.asIntegerOrNull();
    }

    /**
     * Returns the array element at the supplied index as a floating-point value.
     *
     * <p>No type conversion is performed. The value is returned only when the
     * element exists and is a CBOR floating-point value.
     *
     * @param index the array index
     * @return the floating-point value, or {@code null} if the index is outside
     *     the array or the element is not a CBOR floating-point value
     * @throws IllegalStateException if this value is not a CBOR array
     */
    public Double getAsDoubleOrNull(int index) {
        CBORValue value = get(index);
        return value == null ? null : value.asDoubleOrNull();
    }

    /**
     * Returns the array element at the supplied index as a boolean.
     *
     * <p>No type conversion is performed. The value is returned only when the
     * element exists and is a CBOR boolean.
     *
     * @param index the array index
     * @return the boolean value, or {@code null} if the index is outside the array
     *     or the element is not a CBOR boolean
     * @throws IllegalStateException if this value is not a CBOR array
     */
    public Boolean getAsBooleanOrNull(int index) {
        CBORValue value = get(index);
        return value == null ? null : value.asBooleanOrNull();
    }

    /**
     * Returns the array element at the supplied index as a byte string.
     *
     * <p>No type conversion is performed. The value is returned only when the
     * element exists and is a CBOR byte string.
     *
     * <p>The returned byte array is a defensive copy.
     *
     * @param index the array index
     * @return the byte string, or {@code null} if the index is outside the array
     *     or the element is not a CBOR byte string
     * @throws IllegalStateException if this value is not a CBOR array
     */
    public byte[] getAsBytesOrNull(int index) {
        CBORValue value = get(index);
        return value == null ? null : value.asBytesOrNull();
    }

    /**
     * Returns the array element at the supplied index as a CBOR array.
     *
     * <p>No type conversion is performed. The value is returned only when the
     * element exists and is a CBOR array.
     *
     * @param index the array index
     * @return the CBOR array value, or {@code null} if the index is outside the
     *     array or the element is not a CBOR array
     * @throws IllegalStateException if this value is not a CBOR array
     */
    public CBORValue getAsArrayOrNull(int index) {
        CBORValue value = get(index);
        return value == null ? null : value.asArrayOrNull();
    }

    /**
     * Returns the array element at the supplied index as a CBOR map.
     *
     * <p>No type conversion is performed. The value is returned only when the
     * element exists and is a CBOR map.
     *
     * @param index the array index
     * @return the CBOR map value, or {@code null} if the index is outside the array
     *     or the element is not a CBOR map
     * @throws IllegalStateException if this value is not a CBOR array
     */
    public CBORValue getAsMapOrNull(int index) {
        CBORValue value = get(index);
        return value == null ? null : value.asMapOrNull();
    }

    /**
     * Returns the elements of a CBOR array in order.
     *
     * <p>The returned list is an immutable snapshot.
     *
     * @return the array elements
     * @throws IllegalStateException if this value is not an array
     */
    public List<CBORValue> getElements() {
        requireType(CBORType.ARRAY);

        return Collections.unmodifiableList(
                new ArrayList<>(arrayElements()));
    }

    /**
     * Tests whether a map contains a key.
     *
     * @param key the map key
     * @return {@code true} if the key exists
     */
    public boolean containsKey(CBORValue key) {
        Objects.requireNonNull(key, "key");
        requireType(CBORType.MAP);

        for (CBORMapEntry entry : mapEntries()) {
            if (entry.key().equals(key)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether a map contains a Java or CBOR key.
     *
     * @param key the map key
     * @return {@code true} if the key exists
     */
    public boolean containsKey(Object key) {
        return containsKey(
                CBOR.value(Objects.requireNonNull(key, "key")));
    }

    /**
     * Returns the keys of a CBOR map in insertion order.
     *
     * <p>The returned set is an immutable snapshot.
     *
     * @return the map keys
     */
    public List<CBORValue> getKeys() {
        requireType(CBORType.MAP);

        List<CBORValue> keys = new ArrayList<>();

        for (CBORMapEntry entry : mapEntries()) {
            keys.add(entry.key());
        }

        return Collections.unmodifiableList(keys);
    }

    /**
     * Returns the entries of a CBOR map in insertion order.
     *
     * <p>The returned list is an immutable snapshot.
     *
     * @return the map entries
     */
    public List<CBORMapEntry> getEntries() {
        requireType(CBORType.MAP);

        return Collections.unmodifiableList(
                new ArrayList<>(mapEntries()));
    }

    /**
     * Starts an editing session for this array.
     *
     * <p>The editor operates directly on this value. Its modification
     * methods return the same editor, allowing multiple operations to be
     * chained. Call {@link CBORArrayEditor#value()} when the editing session
     * is complete.
     *
     * @return an editor for this array
     * @throws IllegalStateException if this value is not an array
     */
    public CBORArrayEditor editArray() {
        requireType(CBORType.ARRAY);
        return new CBORArrayEditor();
    }

    /**
     * Starts an editing session for this map.
     *
     * <p>The editor operates directly on this value. Its modification
     * methods return the same editor, allowing multiple operations to be
     * chained. Call {@link CBORMapEditor#value()} when the editing session
     * is complete.
     *
     * @return an editor for this map
     * @throws IllegalStateException if this value is not a map
     */
    public CBORMapEditor editMap() {
        requireType(CBORType.MAP);
        return new CBORMapEditor();
    }

    /**
     * Returns the text string value.
     *
     * @return the text string
     */
    public String asString() {
        requireType(CBORType.TEXT_STRING);
        return (String) value;
    }

    /**
     * Returns a defensive copy of the byte string.
     *
     * @return the byte string
     */
    public byte[] asBytes() {
        requireType(CBORType.BYTE_STRING);
        return ((byte[]) value).clone();
    }

    /**
     * Returns the boolean value.
     *
     * @return the boolean value
     */
    public boolean asBoolean() {
        requireType(CBORType.BOOLEAN);
        return (Boolean) value;
    }

    /**
     * Returns the floating-point value as a Java {@code double}.
     *
     * @return the value as a double
     */
    public double asDouble() {
        requireType(CBORType.FLOATING_POINT);
        return ((Number) value).doubleValue();
    }

    /**
     * Returns the integer as a {@link BigInteger}.
     *
     * @return the integer value
     */
    public BigInteger asInteger() {
        requireType(CBORType.INTEGER);
        return (BigInteger) value;
    }

    /**
     * Returns the CBOR simple value.
     *
     * @return the simple value number
     */
    public int asSimpleValue() {
        requireType(CBORType.SIMPLE_VALUE);
        return (Integer) value;
    }

    /**
     * Returns this value as a string, or {@code null} when it is not a CBOR text string.
     *
     * @return the string value, or {@code null} if this value is not a CBOR text string
     */
    private String asStringOrNull() {
        return type == CBORType.TEXT_STRING ? (String) value : null;
    }

    /**
     * Returns this value as an integer, or {@code null} when it is not a CBOR integer.
     *
     * @return the integer value, or {@code null} if this value is not a CBOR integer
     */
    private BigInteger asIntegerOrNull() {
        return type == CBORType.INTEGER ? (BigInteger) value : null;
    }

    /**
     * Returns this value as a floating-point value, or {@code null} when it is not
     * a CBOR floating-point value.
     *
     * @return the floating-point value, or {@code null} if this value is not a CBOR
     *     floating-point value
     */
    private Double asDoubleOrNull() {
        return type == CBORType.FLOATING_POINT ? (Double) value : null;
    }

    /**
     * Returns this value as a boolean, or {@code null} when it is not a CBOR boolean.
     *
     * @return the boolean value, or {@code null} if this value is not a CBOR boolean
     */
    private Boolean asBooleanOrNull() {
        return type == CBORType.BOOLEAN ? (Boolean) value : null;
    }

    /**
     * Returns this value as a byte string, or {@code null} when it is not a CBOR byte string.
     *
     * <p>The returned byte array is a defensive copy.
     *
     * @return the byte string, or {@code null} if this value is not a CBOR byte string
     */
    private byte[] asBytesOrNull() {
        return type == CBORType.BYTE_STRING ? ((byte[]) value).clone() : null;
    }

    /**
     * Returns this value as a CBOR array, or {@code null} when it is not a CBOR array.
     *
     * @return this value if it is a CBOR array, otherwise {@code null}
     */
    private CBORValue asArrayOrNull() {
        return type == CBORType.ARRAY ? this : null;
    }

    /**
     * Returns this value as a CBOR map, or {@code null} when it is not a CBOR map.
     *
     * @return this value if it is a CBOR map, otherwise {@code null}
     */
    private CBORValue asMapOrNull() {
        return type == CBORType.MAP ? this : null;
    }

    /**
     * Encodes this CBOR value into deterministic CBOR bytes.
     *
     * @return the encoded CBOR bytes
     */
    public byte[] encodeToBytes() {
        return CBOREncoder.encode(this);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof CBORValue)) {
            return false;
        }

        CBORValue that = (CBORValue) other;

        if (type != that.type) {
            return false;
        }

        if (type == CBORType.MAP) {
            return mapEquals(that);
        }

        return Objects.deepEquals(value, that.value);
    }

    @Override
    public int hashCode() {
        if (type == CBORType.MAP) {
            return 31 * type.hashCode() + mapHashCode();
        }

        return 31 * type.hashCode() + deepHash(value);
    }

    private boolean mapEquals(CBORValue other) {
        List<CBORMapEntry> left = mapEntries();
        List<CBORMapEntry> right = other.mapEntries();

        if (left.size() != right.size()) {
            return false;
        }

        for (CBORMapEntry entry : left) {
            if (!other.containsKey(entry.key())) {
                return false;
            }

            CBORValue otherValue = other.get(entry.key());

            if (!entry.value().equals(otherValue)) {
                return false;
            }
        }

        return true;
    }

    private int mapHashCode() {
        int result = 0;

        for (CBORMapEntry entry : mapEntries()) {
            result += 31 * entry.key().hashCode()
                    + entry.value().hashCode();
        }

        return result;
    }

    @Override
    public String toString() {
        return CBOREncoder.describe(this);
    }

    static CBORValue scalar(CBORType type, Object value) {
        return new CBORValue(type, value);
    }

    static CBORValue tag(BigInteger tag, CBORValue value) {
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(value, "value");

        if (tag.signum() < 0 || tag.bitLength() > 64) {
            throw new IllegalArgumentException(
                    "CBOR tag must be an unsigned 64-bit integer");
        }

        return new CBORValue(
                CBORType.TAG,
                new TagValue(tag, value));
    }

    static CBORValue array(List<CBORValue> values) {
        Objects.requireNonNull(values, "values");

        return new CBORValue(
                CBORType.ARRAY,
                new ArrayList<>(values));
    }

    static CBORValue map(List<CBORMapEntry> entries) {
        Objects.requireNonNull(entries, "entries");

        return new CBORValue(
                CBORType.MAP,
                new ArrayList<>(entries));
    }

    static Object raw(CBORValue value) {
        return value.value;
    }

    @SuppressWarnings("unchecked")
    private static Object copyValue(CBORType type, Object value) {
        switch (type) {
            case NULL:
            case UNDEFINED:
                return null;

            case BYTE_STRING:
                return ((byte[]) Objects.requireNonNull(value, "value"))
                        .clone();

            case ARRAY:
                List<CBORValue> array =
                        (List<CBORValue>) Objects.requireNonNull(
                                value, "value");

                return new ArrayList<>(array);

            case MAP:
                List<CBORMapEntry> entries =
                        (List<CBORMapEntry>) Objects.requireNonNull(
                                value, "value");

                return new ArrayList<>(entries);

            case TAG:
                return Objects.requireNonNull(value, "value");

            default:
                return Objects.requireNonNull(value, "value");
        }
    }

    @SuppressWarnings("unchecked")
    private List<CBORValue> arrayElements() {
        requireType(CBORType.ARRAY);
        return (List<CBORValue>) value;
    }

    @SuppressWarnings("unchecked")
    private List<CBORMapEntry> mapEntries() {
        requireType(CBORType.MAP);
        return (List<CBORMapEntry>) value;
    }

    private void requireType(CBORType expected) {
        if (type != expected) {
            throw new IllegalStateException("Expected " + expected + " but was " + type);
        }
    }

    private static int deepHash(Object value) {
        if (value instanceof byte[]) {
            return Arrays.hashCode((byte[]) value);
        }

        return Objects.hashCode(value);
    }

    private static CBORValue normalizeKey(Object key) {
        Objects.requireNonNull(key, "key");

        return key instanceof CBORValue ? (CBORValue) key : CBOR.value(key);
    }

    private static CBORValue normalizeValue(Object value) {
        return value instanceof CBORValue ? (CBORValue) value : CBOR.value(value);
    }

    /**
     * Editor for a CBOR array.
     *
     * <p>This class is deliberately nested inside {@link CBORValue}. It is
     * not another CBOR representation; it is merely a temporary mutation
     * facade over an existing array value.
     */
    public final class CBORArrayEditor {

        CBORArrayEditor() {
            // Intentionally private.
        }

        /**
         * Adds a value to the end of the array.
         *
         * @param element the value to add
         * @return this editor
         */
        public CBORArrayEditor add(Object element) {
            arrayElements().add(normalizeValue(element));
            return this;
        }

        /**
         * Replaces an array element.
         *
         * @param index the zero-based index
         * @param element the replacement value
         * @return this editor
         */
        public CBORArrayEditor set(int index, Object element) {
            arrayElements().set(index, normalizeValue(element));
            return this;
        }

        /**
         * Removes an array element by index.
         *
         * @param index the zero-based index
         * @return this editor
         */
        public CBORArrayEditor remove(int index) {
            arrayElements().remove(index);
            return this;
        }

        /**
         * Removes the first element equal to the supplied value.
         *
         * @param element the value to remove
         * @return this editor
         */
        public CBORArrayEditor remove(Object element) {
            CBORValue normalized = normalizeValue(element);

            List<CBORValue> elements = arrayElements();

            for (int i = 0; i < elements.size(); i++) {
                if (elements.get(i).equals(normalized)) {
                    elements.remove(i);
                    break;
                }
            }

            return this;
        }

        /**
         * Removes all elements from the array.
         *
         * @return this editor
         */
        public CBORArrayEditor clear() {
            arrayElements().clear();
            return this;
        }

        /**
         * Returns the {@link CBORValue} being edited.
         *
         * <p>No copy is made. Subsequent modifications through this editor
         * continue to modify the returned value.
         *
         * @return the enclosing CBOR value
         */
        public CBORValue value() {
            return CBORValue.this;
        }

    }

    /**
     * Editor for a CBOR map.
     *
     * <p>This class is deliberately nested inside {@link CBORValue}. It is
     * not another CBOR representation; it is merely a temporary mutation
     * facade over an existing map value.
     */
    public final class CBORMapEditor {

        CBORMapEditor() {
            // Intentionally private.
        }

        /**
         * Adds a new key/value pair to the map.
         *
         * <p>Map keys must be unique. Use {@link #set(Object, Object)} to
         * replace an existing value.
         *
         * @param key the map key
         * @param element the value
         * @return this editor
         * @throws IllegalArgumentException if the key already exists
         */
        public CBORMapEditor put(Object key, Object element) {
            CBORValue normalizedKey = normalizeKey(key);

            if (containsKey(normalizedKey)) {
                throw new IllegalArgumentException("Duplicate CBOR map key: " + normalizedKey);
            }
            mapEntries().add(new CBORMapEntry(normalizedKey, normalizeValue(element)));
            return this;
        }

        /**
         * Replaces the value associated with an existing key.
         *
         * @param key the map key
         * @param element the replacement value
         * @return this editor
         * @throws IllegalArgumentException if the key does not exist
         */
        public CBORMapEditor set(Object key, Object element) {
            CBORValue normalizedKey = normalizeKey(key);
            List<CBORMapEntry> entries = mapEntries();

            for (int i = 0; i < entries.size(); i++) {
                CBORMapEntry entry = entries.get(i);

                if (entry.key().equals(normalizedKey)) {
                    entries.set(i, new CBORMapEntry(entry.key(), normalizeValue(element)));
                    return this;
                }
            }

            throw new IllegalArgumentException("No value associated with key: " + normalizedKey);
        }

        /**
         * Removes the value associated with a key.
         *
         * <p>If the key does not exist, this method has no effect.
         *
         * @param key the map key
         * @return this editor
         */
        public CBORMapEditor remove(Object key) {
            CBORValue normalizedKey = normalizeKey(key);
            List<CBORMapEntry> entries = mapEntries();

            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).key().equals(normalizedKey)) {
                    entries.remove(i);
                    break;
                }
            }

            return this;
        }

        /**
         * Removes all entries from the map.
         *
         * @return this editor
         */
        public CBORMapEditor clear() {
            mapEntries().clear();
            return this;
        }

        /**
         * Returns the {@link CBORValue} being edited.
         *
         * <p>No copy is made. Subsequent modifications through this editor
         * continue to modify the returned value.
         *
         * @return the enclosing CBOR value
         */
        public CBORValue value() {
            return CBORValue.this;
        }

    }

    /**
     * Represents a tag value.
     */
    private static final class TagValue {

        /** Tag number. */
        private final BigInteger tag;

        /** Value contained by the tag. */
        private final CBORValue value;

        /**
         * Default constructor
         *
         * @param tag {@link BigInteger} tag
         * @param value {@link CBORValue} value
         */
        private TagValue(BigInteger tag, CBORValue value) {
            this.tag = Objects.requireNonNull(tag, "tag");
            this.value = Objects.requireNonNull(value, "value");
        }

        private BigInteger tag() {
            return tag;
        }

        private CBORValue value() {
            return value;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof TagValue)) {
                return false;
            }

            TagValue that = (TagValue) other;

            return tag.equals(that.tag)
                    && value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return 31 * tag.hashCode() + value.hashCode();
        }

    }

}
