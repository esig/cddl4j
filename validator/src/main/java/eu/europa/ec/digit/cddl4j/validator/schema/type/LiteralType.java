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
package eu.europa.ec.digit.cddl4j.validator.schema.type;

import eu.europa.ec.digit.cddl4j.cbor.CBORType;
import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * A CDDL literal value: a specific integer, floating-point number,
 * text string, byte string, or boolean constant.
 *
 * <p>Examples: {@code 0}, {@code 42}, {@code 3.14}, {@code "hello"},
 * {@code h'deadbeef'}, {@code true}, {@code false}.
 */
public final class LiteralType extends CddlType {

    /** The kinds of literal values supported by CDDL. */
    public enum Kind {

        /** An integer literal. */
        INTEGER,

        /** A floating-point literal. */
        FLOAT,

        /** A text-string literal. */
        TEXT,

        /** A byte-string literal. */
        BYTES,

        /** A boolean literal. */
        BOOL

    }

    /** The kind of literal represented by this type. */
    private final Kind kind;

    /** The integer value, or {@code null} when this is not an integer literal. */
    private final BigInteger intValue;

    /** The floating-point value when this is a floating-point literal. */
    private final double floatValue;

    /** The text value, or {@code null} when this is not a text literal. */
    private final String textValue;

    /** The byte-string value, or {@code null} when this is not a byte-string literal. */
    private final byte[] bytesValue;

    /** The boolean value when this is a boolean literal. */
    private final boolean boolValue;

    /**
     * Creates an integer literal type.
     *
     * @param value the integer literal value
     * @return an integer literal type representing {@code value}
     */
    public static LiteralType ofInt(BigInteger value) {
        return new LiteralType(value);
    }

    /**
     * Creates a floating-point literal type.
     *
     * @param value the floating-point literal value
     * @return a floating-point literal type representing {@code value}
     */
    public static LiteralType ofFloat(double value) {
        return new LiteralType(value);
    }

    /**
     * Creates a text-string literal type.
     *
     * @param value the text-string literal value
     * @return a text-string literal type representing {@code value}
     */
    public static LiteralType ofText(String value) {
        return new LiteralType(value, Kind.TEXT);
    }

    /**
     * Creates a byte-string literal type.
     *
     * @param value the byte-string literal value
     * @return a byte-string literal type representing a copy of {@code value}
     */
    public static LiteralType ofBytes(byte[] value) {
        return new LiteralType(value);
    }

    /**
     * Creates a boolean literal type.
     *
     * @param value the boolean literal value
     * @return a boolean literal type representing {@code value}
     */
    public static LiteralType ofBool(boolean value) {
        return new LiteralType(value);
    }

    /**
     * Creates an integer literal type.
     *
     * @param value the integer literal value
     */
    private LiteralType(BigInteger value) {
        kind = Kind.INTEGER;
        intValue = value;
        floatValue = 0;
        textValue = null;
        bytesValue = null;
        boolValue = false;
    }

    /**
     * Creates a floating-point literal type.
     *
     * @param value the floating-point literal value
     */
    private LiteralType(double value) {
        kind = Kind.FLOAT;
        intValue = null;
        floatValue = value;
        textValue = null;
        bytesValue = null;
        boolValue = false;
    }

    /**
     * Creates a text-string literal type.
     *
     * @param value the text-string literal value
     * @param kind the literal kind
     */
    private LiteralType(String value, Kind kind) {
        this.kind = kind;
        intValue = null;
        floatValue = 0;
        textValue = value;
        bytesValue = null;
        boolValue = false;
    }

    /**
     * Creates a byte-string literal type.
     *
     * @param value the byte-string literal value
     */
    private LiteralType(byte[] value) {
        kind = Kind.BYTES;
        intValue = null;
        floatValue = 0;
        textValue = null;
        bytesValue = value.clone();
        boolValue = false;
    }

    /**
     * Creates a boolean literal type.
     *
     * @param value the boolean literal value
     */
    private LiteralType(boolean value) {
        kind = Kind.BOOL;
        intValue = null;
        floatValue = 0;
        textValue = null;
        bytesValue = null;
        boolValue = value;
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
     * Validates whether a CBOR value matches this literal value.
     *
     * @param value the CBOR value to validate
     * @param ctx the validation context
     * @return a successful validation result if the value matches this literal;
     *         otherwise, a failed validation result describing the mismatch
     */
    @Override
    public ValidationResult validate(CBORValue value, ValidationContext ctx) {
        switch (kind) {
            case INTEGER: {
                if (value.getType() != CBORType.INTEGER) {
                    return fail(ctx, "Expected integer literal " + intValue
                            + " but got " + PrimitiveType.cborTypeName(value));
                }
                BigInteger actual;
                try {
                    actual = value.asInteger();
                } catch (Exception e) {
                    return fail(ctx, "Cannot read integer value: " + e.getMessage());
                }
                if (!intValue.equals(actual)) {
                    return fail(ctx, "Expected integer literal " + intValue + " but got " + actual);
                }
                return ValidationResult.success();
            }
            case FLOAT: {
                if (value.getType() != CBORType.FLOATING_POINT) {
                    return fail(ctx, "Expected float literal " + floatValue
                            + " but got " + PrimitiveType.cborTypeName(value));
                }
                double actual = value.asDouble();
                if (Double.compare(floatValue, actual) != 0) {
                    return fail(ctx, "Expected float literal " + floatValue + " but got " + actual);
                }
                return ValidationResult.success();
            }
            case TEXT: {
                if (value.getType() != CBORType.TEXT_STRING) {
                    return fail(ctx, "Expected text literal \"" + textValue
                            + "\" but got " + PrimitiveType.cborTypeName(value));
                }
                String actual = value.asString();
                if (!textValue.equals(actual)) {
                    return fail(ctx, "Expected text literal \"" + textValue
                            + "\" but got \"" + actual + "\"");
                }
                return ValidationResult.success();
            }
            case BYTES: {
                if (value.getType() != CBORType.BYTE_STRING) {
                    return fail(ctx, "Expected byte-string literal but got "
                            + PrimitiveType.cborTypeName(value));
                }
                byte[] actual = value.asBytes();
                if (!Arrays.equals(bytesValue, actual)) {
                    return fail(ctx, "Expected byte-string literal " + bytesToHex(bytesValue)
                            + " but got " + bytesToHex(actual));
                }
                return ValidationResult.success();
            }
            case BOOL: {
                if (value.getType() != CBORType.BOOLEAN) {
                    return fail(ctx, "Expected bool literal " + boolValue
                            + " but got " + PrimitiveType.cborTypeName(value));
                }
                boolean actual = value.asBoolean();
                if (boolValue != actual) {
                    return fail(ctx, "Expected bool literal " + boolValue + " but got " + actual);
                }
                return ValidationResult.success();
            }
            default:
                return fail(ctx, "Internal error: unknown literal kind " + kind);
        }
    }

    /**
     * Returns the integer value of this literal.
     *
     * @return the integer value, or {@code null} when this is not an integer literal
     */
    public BigInteger getIntegerValueOrNull() {
        return kind == Kind.INTEGER ? intValue : null;
    }

    /**
     * Returns the double value of this literal.
     *
     * @return the double value, or {@code null} when this is not a double literal
     */
    public Double getDoubleValueOrNull() {
        return kind == Kind.FLOAT ? floatValue : null;
    }

    /**
     * Returns the string value of this literal.
     *
     * @return the string value, or {@code null} when this is not a string literal
     */
    public String getStringValueOrNull() {
        return kind == Kind.TEXT ? textValue : null;
    }

    /**
     * Returns the bytes value of this literal.
     *
     * @return the bytes value, or {@code null} when this is not a bytes literal
     */
    public byte[] getBytesValueOrNull() {
        return kind == Kind.BYTES ? bytesValue : null;
    }

    /**
     * Returns the boolean value of this literal.
     *
     * @return the boolean value, or {@code null} when this is not a boolean literal
     */
    public Boolean getBooleanValueOrNull() {
        return kind == Kind.BOOL ? boolValue : null;
    }

    /**
     * Returns the CDDL representation of this literal.
     *
     * @return the literal value formatted according to CDDL syntax
     */
    @Override
    public String toString() {
        switch (kind) {
            case INTEGER:
                return intValue.toString();
            case FLOAT:
                return Double.toString(floatValue);
            case TEXT:
                return "\"" + textValue + "\"";
            case BYTES:
                return "h'" + bytesToHex(bytesValue) + "'";
            case BOOL:
                return Boolean.toString(boolValue);
            default:
                return "literal";
        }
    }

    /**
     * Converts a byte array to its lowercase hexadecimal representation.
     *
     * @param bytes the bytes to convert
     * @return the lowercase hexadecimal representation of {@code bytes}
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

}
