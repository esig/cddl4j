/**
 * Copyright (C) 2026 European Union
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the EUROPEAN UNION PUBLIC LICENCE v. 1.2 as
 * published by the European Union.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * EUROPEAN UNION PUBLIC LICENCE v. 1.2 for further details.
 *
 * You should have received a copy of the EUROPEAN UNION PUBLIC LICENCE v. 1.2.
 * along with this program. If not,
 * see https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12 .
 */
package eu.europa.ec.digit.cddl4j.cbor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Convenience facade for constructing CBOR values.
 *
 * <p>{@code CBOR} is the primary entry point for application code that needs
 * to construct CBOR values. It provides factory methods for primitive values,
 * containers, simple values, and arbitrary tags.
 *
 * <p>Returned {@link CBORValue} instances are mutable. Array and map values
 * can be modified through {@link CBORValue.CBORArrayEditor} and
 * {@link CBORValue.CBORMapEditor}.
 */
public final class CBOR {

    /**
     * Default constructor
     */
    private CBOR() {
        // Utility class.
    }

    /**
     * Decodes exactly one CBOR value from the supplied bytes.
     *
     * @param bytes the encoded CBOR data
     * @return the decoded CBOR value
     * @throws CBORDecodeException if the data is malformed, truncated, or
     *     contains trailing bytes
     * @throws NullPointerException if {@code bytes} is null
     */
    public static CBORValue decodeFromBytes(byte[] bytes) throws CBORDecodeException {
        return CBORDecoder.decode(bytes);
    }

    /**
     * Converts a supported Java value to a CBOR value.
     *
     * <p>Supported Java types are:
     * <ul>
     *   <li>{@link CBORValue}</li>
     *   <li>{@code null}</li>
     *   <li>{@link Boolean}</li>
     *   <li>{@link Byte}</li>
     *   <li>{@link Short}</li>
     *   <li>{@link Integer}</li>
     *   <li>{@link Long}</li>
     *   <li>{@link BigInteger}</li>
     *   <li>{@link Float}</li>
     *   <li>{@link Double}</li>
     *   <li>{@link String}</li>
     *   <li>{@code byte[]}</li>
     * </ul>
     *
     * @param value the Java value
     * @return the corresponding CBOR value
     * @throws IllegalArgumentException if the Java type is unsupported
     */
    public static CBORValue value(Object value) {
        if (value instanceof CBORValue) {
            return (CBORValue) value;
        }

        if (value == null) {
            return nullValue();
        }

        if (value instanceof Boolean) {
            return bool((Boolean) value);
        }

        /*
         * BigInteger must be handled separately. Converting it through
         * longValue() would silently truncate values outside the signed
         * 64-bit range.
         */
        if (value instanceof BigInteger) {
            return integer((BigInteger) value);
        }

        if (value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long) {
            return integer(((Number) value).longValue());
        }

        if (value instanceof Float || value instanceof Double) {
            return floatingPoint(((Number) value).doubleValue());
        }

        if (value instanceof String) {
            return text((String) value);
        }

        if (value instanceof byte[]) {
            return bytes((byte[]) value);
        }

        throw new IllegalArgumentException(
                "Unsupported CBOR value: " + value.getClass().getName());
    }

    /**
     * Creates a CBOR integer from a {@code long}.
     *
     * @param value the integer value
     * @return a CBOR integer
     */
    public static CBORValue integer(long value) {
        return CBORValue.scalar(CBORType.INTEGER, BigInteger.valueOf(value));
    }

    /**
     * Creates a CBOR integer from a {@link BigInteger}.
     *
     * <p>The full unsigned 64-bit CBOR integer range is supported in addition
     * to arbitrary negative integers.
     *
     * @param value the integer value
     * @return a CBOR integer
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue integer(BigInteger value) {
        Objects.requireNonNull(value, "value");
        return CBORValue.scalar(CBORType.INTEGER, value);
    }

    /**
     * Creates a CBOR floating-point value.
     *
     * <p>Floating-point values are represented as Java {@code double} values
     * in the in-memory model. The encoder chooses the preferred CBOR
     * representation when serializing the value.
     *
     * @param value the floating-point value
     * @return a CBOR floating-point value
     */
    public static CBORValue floatingPoint(double value) {
        return CBORValue.scalar(CBORType.FLOATING_POINT, value);
    }

    /**
     * Creates a CBOR boolean.
     *
     * @param value the boolean value
     * @return the canonical CBOR boolean value
     */
    public static CBORValue bool(boolean value) {
        return value ? CBORValue.TRUE : CBORValue.FALSE;
    }

    /**
     * Returns the CBOR {@code true} value.
     *
     * @return the CBOR true value
     */
    public static CBORValue trueValue() {
        return CBORValue.TRUE;
    }

    /**
     * Returns the CBOR {@code false} value.
     *
     * @return the CBOR false value
     */
    public static CBORValue falseValue() {
        return CBORValue.FALSE;
    }

    /**
     * Returns the CBOR {@code null} value.
     *
     * @return the CBOR null value
     */
    public static CBORValue nullValue() {
        return CBORValue.NULL;
    }

    /**
     * Returns the CBOR {@code undefined} value.
     *
     * @return the CBOR undefined value
     */
    public static CBORValue undefined() {
        return CBORValue.UNDEFINED;
    }

    /**
     * Creates a CBOR simple value.
     *
     * <p>The value must be in the CBOR simple-value range {@code 0..255}.
     * Values 20 through 23 should normally be created using the dedicated
     * boolean, null, and undefined methods.
     *
     * @param value the simple value number
     * @return a CBOR simple value
     * @throws IllegalArgumentException if the value is outside 0..255
     */
    public static CBORValue simpleValue(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException(
                    "CBOR simple value must be between 0 and 255");
        }

        return CBORValue.scalar(CBORType.SIMPLE_VALUE, value);
    }

    /**
     * Creates a CBOR text string.
     *
     * @param value the text
     * @return a CBOR text string
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue text(String value) {
        Objects.requireNonNull(value, "value");
        return CBORValue.scalar(CBORType.TEXT_STRING, value);
    }

    /**
     * Creates a CBOR byte string.
     *
     * <p>The supplied byte array is defensively copied.
     *
     * @param value the bytes
     * @return a CBOR byte string
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue bytes(byte[] value) {
        Objects.requireNonNull(value, "value");
        return CBORValue.scalar(CBORType.BYTE_STRING, value.clone());
    }

    /**
     * Creates an empty CBOR map.
     *
     * <p>The returned editor operates on a new {@link CBORValue} map.
     * Call {@link CBORValue.CBORMapEditor#value()} when the editing session
     * is complete and the resulting CBOR value is required.
     *
     * @return an editor for a new empty CBOR map
     */
    public static CBORValue.CBORMapEditor map() {
        return CBORValue.map(new ArrayList<>()).editMap();
    }

    /**
     * Creates a CBOR map from the supplied Java map.
     *
     * <p>The iteration order of the supplied map is preserved. Each key and
     * value is converted to a {@link CBORValue} using {@link #value(Object)}.
     *
     * @param map the source map
     * @return an editor for the new CBOR map
     * @throws NullPointerException if {@code map} is null
     * @throws IllegalArgumentException if duplicate CBOR keys result from
     *         the conversion
     */
    public static CBORValue.CBORMapEditor map(Map<?, ?> map) {
        Objects.requireNonNull(map, "map");

        CBORValue.CBORMapEditor editor = map();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            editor.put(entry.getKey(), entry.getValue());
        }

        return editor;
    }

    /**
     * Creates an empty CBOR array.
     *
     * <p>The returned editor operates on a new {@link CBORValue} array.
     * Call {@link CBORValue.CBORArrayEditor#value()} when the editing session
     * is complete and the resulting CBOR value is required.
     *
     * @return an editor for a new empty CBOR array
     */
    public static CBORValue.CBORArrayEditor array() {
        return CBORValue.array(new ArrayList<>()).editArray();
    }

    /**
     * Creates an empty CBOR array with the specified initial capacity.
     *
     * <p>The initial capacity is only a performance hint and does not affect
     * the resulting CBOR representation.
     *
     * @param initialCapacity the initial capacity of the underlying array
     * @return an editor for a new empty CBOR array
     * @throws IllegalArgumentException if {@code initialCapacity} is negative
     */
    public static CBORValue.CBORArrayEditor array(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial capacity must not be negative");
        }

        return CBORValue.array(new ArrayList<>(initialCapacity)).editArray();
    }

    /**
     * Creates a CBOR array from the supplied collection.
     *
     * <p>Each element is converted to a {@link CBORValue} using
     * {@link #value(Object)}. The collection size is used as the initial
     * capacity of the underlying array.
     *
     * @param values the source collection
     * @return an editor for the new CBOR array
     * @throws NullPointerException if {@code values} or an element is null
     */
    public static CBORValue.CBORArrayEditor array(Collection<?> values) {
        Objects.requireNonNull(values, "values");

        CBORValue.CBORArrayEditor editor = array(values.size());

        for (Object value : values) {
            editor.add(value);
        }

        return editor;
    }

    /**
     * Creates a CBOR array from the supplied Java array.
     *
     * @param values the source array
     * @param <T> the array element type
     * @return an editor for the new CBOR array
     * @throws NullPointerException if {@code values} or an element is null
     */
    public static <T> CBORValue.CBORArrayEditor array(T[] values) {
        Objects.requireNonNull(values, "values");

        CBORValue.CBORArrayEditor editor = array(values.length);

        for (T value : values) {
            editor.add(value);
        }

        return editor;
    }

    /**
     * Creates an arbitrary CBOR semantic tag.
     *
     * @param tag the unsigned 64-bit tag number
     * @param value the tagged value
     * @return the tagged CBOR value
     * @throws NullPointerException if {@code tag} or {@code value} is null
     * @throws IllegalArgumentException if {@code tag} is outside uint64 range
     */
    public static CBORValue tag(BigInteger tag, CBORValue value) {
        return CBORValue.tag(tag, value);
    }

    /**
     * Creates an arbitrary CBOR semantic tag.
     *
     * <p>This overload supports tag numbers from 0 through
     * {@link Long#MAX_VALUE}. Use the {@link BigInteger} overload for the full
     * unsigned 64-bit tag range.
     *
     * @param tag the non-negative tag number
     * @param value the tagged value
     * @return the tagged CBOR value
     * @throws IllegalArgumentException if {@code tag} is negative
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue tag(long tag, CBORValue value) {
        if (tag < 0) {
            throw new IllegalArgumentException("CBOR tag must be non-negative");
        }

        return tag(BigInteger.valueOf(tag), value);
    }

    /**
     * Creates a CBOR sequence containing the supplied data items.
     *
     * <p>A CBOR sequence consists of zero or more independently encoded CBOR
     * data items. The items are kept in the supplied order and are encoded
     * consecutively without an enclosing CBOR array or other container.
     *
     * <p>{@link CBORValue} models a single CBOR data item and has no sequence
     * type, so this method returns a byte-string value containing the sequence
     * encoding. This API shape is intentional and avoids inventing a non-CBOR
     * wrapper data type.
     *
     * <p>This is distinct from {@link #array()}, which creates a single CBOR
     * array data item.
     *
     * @param values the CBOR data items to include in the sequence
     * @return a byte-string {@link CBORValue} containing the sequence encoding
     * @throws NullPointerException if {@code values} or one of its elements is null
     */
    public static CBORValue encodeSequence(CBORValue... values) {
        Objects.requireNonNull(values, "values");

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (CBORValue value : values) {
                output.write(value.encodeToBytes());
            }
            return bytes(output.toByteArray());

        } catch (IOException e) {
            throw new IllegalStateException("Unable to encode CBOR sequence", e);
        }
    }

}