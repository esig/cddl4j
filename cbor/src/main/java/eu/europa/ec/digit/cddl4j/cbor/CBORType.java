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

/**
 * Identifies the semantic type of a CBOR value.
 *
 * <p>This enum describes the in-memory CBOR data model rather than the CBOR
 * wire encoding. In particular, a CBOR integer may be encoded using either
 * major type 0 or major type 1, while major type 7 may represent several
 * {@code CBORType} values.
 *
 * <p>Use {@link CBORMajorType} when reasoning about the encoded CBOR
 * representation.
 */
public enum CBORType {

    /** CBOR unsigned or negative integer. */
    INTEGER,

    /** CBOR floating-point number. */
    FLOATING_POINT,

    /** CBOR byte string. */
    BYTE_STRING,

    /** CBOR text string. */
    TEXT_STRING,

    /** CBOR array. */
    ARRAY,

    /** CBOR map. */
    MAP,

    /** CBOR boolean. */
    BOOLEAN,

    /** CBOR null. */
    NULL,

    /** CBOR simple value other than the predefined simple values. */
    SIMPLE_VALUE,

    /** CBOR semantic tag. */
    TAG,

    /** CBOR undefined. */
    UNDEFINED

}
