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

/**
 * Identifies the major type of a CBOR encoded data item.
 *
 * <p>A CBOR initial byte contains a three-bit major type in its most
 * significant bits and five bits of additional information. This enum
 * represents the former.
 *
 * <p>This is a property of the CBOR encoding and should not be confused with
 * {@link CBORType}, which describes the semantic type of a decoded value.
 */
public enum CBORMajorType {

    /** Major type 0: unsigned integer. */
    UNSIGNED_INTEGER(0),

    /** Major type 1: negative integer. */
    NEGATIVE_INTEGER(1),

    /** Major type 2: byte string. */
    BYTE_STRING(2),

    /** Major type 3: text string. */
    TEXT_STRING(3),

    /** Major type 4: array. */
    ARRAY(4),

    /** Major type 5: map. */
    MAP(5),

    /** Major type 6: semantic tag. */
    TAG(6),

    /** Major type 7: floating point, simple value, boolean, null, undefined, or break. */
    SPECIAL(7);

    /** CBOR major type number. */
    private final int value;

    /**
     * Creates a CBOR major type.
     *
     * @param value the CBOR major type number
     */
    CBORMajorType(int value) {
        this.value = value;
    }

    /**
     * Returns the CBOR major type encoded in an initial byte.
     *
     * @param initialByte the CBOR initial byte
     * @return the corresponding major type
     * @throws IllegalArgumentException if the initial byte is outside the unsigned byte range
     */
    public static CBORMajorType fromInitialByte(int initialByte) {
        if (initialByte < 0 || initialByte > 0xff) {
            throw new IllegalArgumentException("Initial byte must be between 0 and 255");
        }

        return fromValue(initialByte >>> 5);
    }

    /**
     * Returns the CBOR major type identified by its numeric value.
     *
     * @param value the CBOR major type number
     * @return the corresponding major type
     * @throws IllegalArgumentException if the value is not a valid CBOR major type
     */
    public static CBORMajorType fromValue(int value) {
        for (CBORMajorType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid CBOR major type: " + value);
    }

    /**
     * Returns the numeric CBOR major type value.
     *
     * @return the major type number
     */
    public int getValue() {
        return value;
    }

}
