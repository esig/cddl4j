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
package eu.europa.ec.digit.cddl4j.validator.schema.type;

import eu.europa.ec.digit.cddl4j.cbor.CBORMajorType;
import eu.europa.ec.digit.cddl4j.cbor.CBORType;
import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.cbor.CBOREncoder;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

import java.util.Objects;

/**
 * Represents a CDDL primitive type from the CDDL prelude or a raw CBOR major
 * type.
 *
 * <p>The supported CDDL prelude types are {@code uint}, {@code int},
 * {@code nint}, {@code float}, {@code float16}, {@code float32},
 * {@code float64}, {@code bool}, {@code tstr}, {@code bstr}, {@code nil},
 * {@code undefined}, and {@code any}. The aliases {@code text}, {@code bytes},
 * and {@code null} are also recognized by {@link #forName(String)}.
 *
 * <p>A primitive type does not implicitly unwrap tagged values. Tagged CBOR
 * values must be validated using a type that explicitly describes the tag.
 */
public final class PrimitiveType extends CddlType {

    /** The CDDL {@code uint} type for non-negative integers. */
    public static final PrimitiveType UINT = new PrimitiveType(Kind.UINT);

    /** The CDDL {@code int} type for all integer values. */
    public static final PrimitiveType INT = new PrimitiveType(Kind.INT);

    /** The CDDL {@code nint} type for negative integers. */
    public static final PrimitiveType NINT = new PrimitiveType(Kind.NINT);

    /** The CDDL {@code float} type for floating-point values of any precision. */
    public static final PrimitiveType FLOAT = new PrimitiveType(Kind.FLOAT);

    /** The CDDL {@code float16} type for 16-bit floating-point values. */
    public static final PrimitiveType FLOAT16 = new PrimitiveType(Kind.FLOAT16);

    /** The CDDL {@code float32} type for 32-bit floating-point values. */
    public static final PrimitiveType FLOAT32 = new PrimitiveType(Kind.FLOAT32);

    /** The CDDL {@code float64} type for 64-bit floating-point values. */
    public static final PrimitiveType FLOAT64 = new PrimitiveType(Kind.FLOAT64);

    /** The CDDL {@code bool} type for boolean values. */
    public static final PrimitiveType BOOL = new PrimitiveType(Kind.BOOL);

    /** The CDDL {@code tstr} type for text strings. */
    public static final PrimitiveType TSTR = new PrimitiveType(Kind.TSTR);

    /** The CDDL {@code bstr} type for byte strings. */
    public static final PrimitiveType BSTR = new PrimitiveType(Kind.BSTR);

    /** The CDDL {@code nil} type for CBOR null values. */
    public static final PrimitiveType NIL = new PrimitiveType(Kind.NIL);

    /** The CDDL {@code undefined} type for CBOR undefined values. */
    public static final PrimitiveType UNDEFINED = new PrimitiveType(Kind.UNDEFINED);

    /** The CDDL {@code any} type accepting any CBOR data item. */
    public static final PrimitiveType ANY = new PrimitiveType(Kind.ANY);

    /**
     * Identifies the kind of primitive CDDL type.
     */
    public enum Kind {

        /** An unsigned integer ({@code uint}). */
        UINT,

        /** An integer of either sign ({@code int}). */
        INT,

        /** A negative integer ({@code nint}). */
        NINT,

        /** A floating-point value of any precision ({@code float}). */
        FLOAT,

        /** A 16-bit floating-point value ({@code float16}). */
        FLOAT16,

        /** A 32-bit floating-point value ({@code float32}). */
        FLOAT32,

        /** A 64-bit floating-point value ({@code float64}). */
        FLOAT64,

        /** A boolean value ({@code bool}). */
        BOOL,

        /** A text string ({@code tstr}). */
        TSTR,

        /** A byte string ({@code bstr}). */
        BSTR,

        /** A CBOR null value ({@code nil}). */
        NIL,

        /** A CBOR undefined value ({@code undefined}). */
        UNDEFINED,

        /** Any CBOR data item ({@code any}). */
        ANY,

        /** A raw CBOR major type ({@code #0} through {@code #7}). */
        MAJOR_TYPE
    }

    /** The primitive kind represented by this type. */
    private final Kind kind;

    /** The CBOR major type when {@link #kind} is {@link Kind#MAJOR_TYPE}. */
    private final CBORMajorType majorType;

    /**
     * Creates a primitive type of the specified kind.
     *
     * @param kind the primitive type kind
     */
    private PrimitiveType(Kind kind) {
        this.kind = kind;
        this.majorType = null;
    }

    /**
     * Creates a primitive type representing a raw CBOR major type.
     *
     * @param majorType the CBOR major type
     */
    private PrimitiveType(CBORMajorType majorType) {
        this.kind = Kind.MAJOR_TYPE;
        this.majorType = majorType;
    }

    /**
     * Creates a primitive type representing a raw CBOR major type.
     *
     * <p>The major type is represented in CDDL using the syntax {@code #n},
     * where {@code n} is between {@code 0} and {@code 7}.
     *
     * @param major the CBOR major type number
     * @return a primitive type representing the specified CBOR major type
     * @throws IllegalArgumentException if {@code major} is outside the range
     *     {@code 0..7}
     */
    public static PrimitiveType majorType(int major) {
        return majorType(CBORMajorType.fromValue(major));
    }

    /**
     * Creates a primitive type representing a raw CBOR major type.
     *
     * @param majorType the CBOR major type
     * @return a primitive type representing the specified CBOR major type
     * @throws NullPointerException if {@code majorType} is null
     */
    public static PrimitiveType majorType(CBORMajorType majorType) {
        if (majorType == null) {
            throw new NullPointerException("majorType");
        }

        return new PrimitiveType(majorType);
    }

    /**
     * Looks up a primitive type by its CDDL prelude name.
     *
     * <p>The canonical names {@code tstr}, {@code bstr}, and {@code nil} are
     * recognized together with the aliases {@code text}, {@code bytes}, and
     * {@code null}. The name {@code undefined} is also supported.
     *
     * @param name the CDDL primitive type name
     * @return the corresponding primitive type, or {@code null} if the name
     *     does not identify a supported primitive type
     */
    public static PrimitiveType forName(String name) {
        switch (name) {
            case "uint":
                return UINT;
            case "int":
                return INT;
            case "nint":
                return NINT;
            case "float":
                return FLOAT;
            case "float16":
                return FLOAT16;
            case "float32":
                return FLOAT32;
            case "float64":
                return FLOAT64;
            case "bool":
                return BOOL;
            case "tstr":
            case "text":
                return TSTR;
            case "bstr":
            case "bytes":
                return BSTR;
            case "nil":
            case "null":
                return NIL;
            case "undefined":
                return UNDEFINED;
            case "any":
                return ANY;
            default:
                return null;
        }
    }

    /**
     * Gets the kind of the primitive type.
     *
     * @return the primitive kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the CBOR major type represented by this primitive type.
     *
     * @return the CBOR major type, or {@code null} when this is not a raw
     *     major-type primitive
     */
    public CBORMajorType getMajorType() {
        return majorType;
    }

    /**
     * Validates a CBOR value against this primitive type.
     *
     * <p>The {@code any} type accepts every CBOR data item, including tagged
     * values. A raw major type explicitly accepts the corresponding encoded
     * major type. Other primitive types reject tagged values because tags are
     * not implicitly removed during validation.
     *
     * @param value the CBOR value to validate
     * @param ctx the validation context
     * @return a successful validation result if the value conforms to this
     *     primitive type; otherwise, a failed validation result
     */
    @Override
    public ValidationResult validate(CBORValue value, ValidationContext ctx) {
        if (value == null) {
            return fail(ctx, "Expected " + this + " but got null CBOR object");
        }

        if (kind == Kind.ANY) {
            return ValidationResult.success();
        }

        if (kind == Kind.MAJOR_TYPE) {
            return validateMajorType(value, ctx);
        }

        if (value.isTag()) {
            return fail(ctx, "Expected " + this + " but got tagged value " + cborTypeName(value));
        }

        switch (kind) {
            case UINT:
                return validateUnsignedInteger(value, ctx);
            case NINT:
                return validateNegativeInteger(value, ctx);
            case INT:
                return value.getType() == CBORType.INTEGER
                        ? ValidationResult.success()
                        : fail(ctx, "Expected int but got " + cborTypeName(value));
            case FLOAT:
                return validateFloatingPoint(value, ctx);
            case FLOAT16:
                return validateFloatingPointPrecision(value, ctx, 0xf9, 0xf9, "float16");
            case FLOAT32:
                return validateFloatingPointPrecision(value, ctx, 0xf9, 0xfa, "float32");
            case FLOAT64:
                return validateFloatingPoint(value, ctx);
            case BOOL:
                return value.getType() == CBORType.BOOLEAN
                        ? ValidationResult.success()
                        : fail(ctx, "Expected boolean but got " + cborTypeName(value));
            case TSTR:
                return value.getType() == CBORType.TEXT_STRING
                        ? ValidationResult.success()
                        : fail(ctx, "Expected tstr (text string) but got " + cborTypeName(value));
            case BSTR:
                return value.getType() == CBORType.BYTE_STRING
                        ? ValidationResult.success()
                        : fail(ctx, "Expected bstr (byte string) but got " + cborTypeName(value));
            case NIL:
                return value.isNull()
                        ? ValidationResult.success()
                        : fail(ctx, "Expected nil/null but got " + cborTypeName(value));
            case UNDEFINED:
                return value.isUndefined()
                        ? ValidationResult.success()
                        : fail(ctx, "Expected undefined but got " + cborTypeName(value));
            case ANY:
            case MAJOR_TYPE:
                throw new IllegalStateException("Unexpected primitive kind: " + kind);
            default:
                throw new IllegalStateException("Unhandled primitive kind: " + kind);
        }
    }

    private ValidationResult validateFloatingPoint(CBORValue value, ValidationContext ctx) {
        return value.getType() == CBORType.FLOATING_POINT
                ? ValidationResult.success()
                : fail(ctx, "Expected floating point but got " + cborTypeName(value));
    }

    private ValidationResult validateFloatingPointPrecision(CBORValue value, ValidationContext ctx,
                                                             int minimumEncoding, int maximumEncoding, String typeName) {
        if (value.getType() != CBORType.FLOATING_POINT) {
            return fail(ctx, "Expected " + typeName + " but got " + cborTypeName(value));
        }

        int initialByte = CBOREncoder.encode(value)[0] & 0xff;
        return initialByte >= minimumEncoding && initialByte <= maximumEncoding
                ? ValidationResult.success()
                : fail(ctx, "Expected " + typeName + " but got a value not representable at that precision");
    }

    /**
     * Validates a CBOR value as an unsigned integer.
     *
     * @param value the CBOR value to validate
     * @param ctx the validation context
     * @return a successful validation result if the value is non-negative
     *     integer; otherwise, a failed validation result
     */
    private ValidationResult validateUnsignedInteger(CBORValue value, ValidationContext ctx) {
        if (value.getType() != CBORType.INTEGER) {
            return fail(ctx, "Expected uint (major type 0) but got " + cborTypeName(value));
        }

        return value.isUnsignedInteger()
                ? ValidationResult.success()
                : fail(ctx, "Expected uint (non-negative) but got negative integer");
    }

    /**
     * Validates a CBOR value as a negative integer.
     *
     * @param value the CBOR value to validate
     * @param ctx the validation context
     * @return a successful validation result if the value is negative
     *     integer; otherwise, a failed validation result
     */
    private ValidationResult validateNegativeInteger(CBORValue value, ValidationContext ctx) {
        if (value.getType() != CBORType.INTEGER) {
            return fail(ctx, "Expected nint (major type 1) but got " + cborTypeName(value));
        }

        return value.isNegativeInteger()
                ? ValidationResult.success()
                : fail(ctx, "Expected nint (negative integer) but got non-negative value");
    }

    /**
     * Validates a CBOR value against a raw CBOR major type.
     *
     * @param value the CBOR value to validate
     * @param ctx the validation context
     * @return a successful validation result if the value conforms to the
     *     configured major type; otherwise, a failed validation result
     */
    private ValidationResult validateMajorType(CBORValue value, ValidationContext ctx) {
        Objects.requireNonNull(majorType);
        switch (majorType) {
            case UNSIGNED_INTEGER:
                return value.isUnsignedInteger()
                        ? ValidationResult.success()
                        : fail(ctx, "Expected major type 0 (uint) but got " + cborTypeName(value));

            case NEGATIVE_INTEGER:
                return value.isNegativeInteger()
                        ? ValidationResult.success()
                        : fail(ctx, "Expected major type 1 (nint) but got " + cborTypeName(value));

            case BYTE_STRING:
                return value.getType() == CBORType.BYTE_STRING
                        ? ValidationResult.success()
                        : fail(ctx, "Expected major type 2 (bstr) but got " + cborTypeName(value));

            case TEXT_STRING:
                return value.getType() == CBORType.TEXT_STRING
                        ? ValidationResult.success()
                        : fail(ctx, "Expected major type 3 (tstr) but got " + cborTypeName(value));

            case ARRAY:
                return value.getType() == CBORType.ARRAY
                        ? ValidationResult.success()
                        : fail(ctx, "Expected major type 4 (array) but got " + cborTypeName(value));

            case MAP:
                return value.getType() == CBORType.MAP
                        ? ValidationResult.success()
                        : fail(ctx, "Expected major type 5 (map) but got " + cborTypeName(value));

            case TAG:
                return value.isTag()
                        ? ValidationResult.success()
                        : fail(ctx, "Expected major type 6 (tagged) but got " + cborTypeName(value));

            case SPECIAL:
                return isSpecial(value)
                        ? ValidationResult.success()
                        : fail(ctx, "Expected major type 7 (special) but got " + cborTypeName(value));

            default:
                throw new IllegalStateException("Unhandled CBOR major type: " + majorType);
        }
    }

    /**
     * Determines whether a CBOR value belongs to major type 7.
     *
     * @param value the CBOR value to inspect
     * @return {@code true} if the value is encoded using major type 7
     */
    private boolean isSpecial(CBORValue value) {
        return value.isFloatingPoint()
                || value.isBoolean()
                || value.isSimpleValue()
                || value.isNull()
                || value.isUndefined();
    }

    /**
     * Returns a human-readable name for the CBOR type represented by a value.
     *
     * @param value the CBOR value whose type is being described
     * @return a human-readable CBOR type name
     */
    static String cborTypeName(CBORValue value) {
        if (value.isNull()) {
            return "nil/null";
        }
        if (value.isUndefined()) {
            return "undefined";
        }
        if (value.isTag()) {
            return "tagged";
        }
        if (value.isInteger()) {
            return "int (integer)";
        }
        if (value.isFloatingPoint()) {
            return "float (floating-point number)";
        }
        if (value.isBoolean()) {
            return "boolean";
        }
        if (value.isTextString()) {
            return "tstr (text string)";
        }
        if (value.isByteString()) {
            return "bstr (byte string)";
        }
        if (value.isArray()) {
            return "CBOR array";
        }
        if (value.isMap()) {
            return "CBOR map";
        }
        return value.getType().toString();
    }

    /**
     * Returns the CDDL representation of this primitive type.
     *
     * <p>Raw major types are represented using the CDDL {@code #n} notation.
     * Prelude types are represented using their canonical CDDL names.
     *
     * @return the CDDL representation of this primitive type
     */
    @Override
    public String toString() {
        return kind == Kind.MAJOR_TYPE
                ? "#" + majorType.getValue()
                : kind.name().toLowerCase();
    }

}
