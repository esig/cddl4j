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
package eu.europa.ec.digit.cddl4j.validator;

import eu.europa.ec.digit.cddl4j.cbor.CBOR;
import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.validator.schema.CddlSchema;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests validation of CDDL primitive types against CBOR values.
 */
class PrimitiveValidationTest {

    // ------------------------------------------------------------------
    // uint
    // ------------------------------------------------------------------

    @Test
    void uintAcceptsPositiveInteger() {
        CddlSchema schema = CddlValidator.loadSchema("value = uint");

        assertValid(schema, CBOR.value(42));
    }

    @Test
    void uintAcceptsZero() {
        CddlSchema schema = CddlValidator.loadSchema("root = uint");

        assertValid(schema, CBOR.value(0));
    }

    @Test
    void uintAcceptsLargePositiveInteger() {
        CddlSchema schema = CddlValidator.loadSchema("root = uint");

        assertValid(schema, CBOR.value(Long.MAX_VALUE));
    }

    @Test
    void uintAcceptsBigIntegerBeyondLongMaxValue() {
        CddlSchema schema = CddlValidator.loadSchema("root = uint");

        BigInteger value = BigInteger.ONE.shiftLeft(63);
        assertValid(schema, CBOR.integer(value));
    }

    @Test
    void uintRejectsNegativeInteger() {
        CddlSchema schema = CddlValidator.loadSchema("root = uint");

        assertInvalid(schema, CBOR.value(-1));

        BigInteger value = BigInteger.ONE.negate();
        assertInvalid(schema, CBOR.integer(value));
    }

    @Test
    void largeUintIsNotTruncatedToLong() {
        CddlSchema schema = CddlValidator.loadSchema("root = 18446744073709551615");

        BigInteger value = new BigInteger("18446744073709551615");
        assertValid(schema, CBOR.integer(value));
    }

    @Test
    void uintRejectsTextString() {
        CddlSchema schema = CddlValidator.loadSchema("root = uint");

        assertInvalid(schema, CBOR.value("hello"));
    }

    @Test
    void uintRejectsFloatingPoint() {
        CddlSchema schema = CddlValidator.loadSchema("value = uint");

        assertInvalid(schema, CBOR.floatingPoint(42.0));
    }

    @Test
    void uintRejectsBoolean() {
        CddlSchema schema = CddlValidator.loadSchema("value = uint");

        assertInvalid(schema, CBOR.bool(true));
    }

    @Test
    void uintRejectsNull() {
        CddlSchema schema = CddlValidator.loadSchema("value = uint");

        assertInvalid(schema, CBOR.nullValue());
    }

    @Test
    void uintRejectsUndefined() {
        CddlSchema schema = CddlValidator.loadSchema("value = uint");

        assertInvalid(schema, CBOR.undefined());
    }

    // ------------------------------------------------------------------
    // int
    // ------------------------------------------------------------------

    @Test
    void intAcceptsNegativeInteger() {
        CddlSchema schema = CddlValidator.loadSchema("root = int");

        assertValid(schema, CBOR.value(-42));
    }

    @Test
    void intAcceptsPositiveInteger() {
        CddlSchema schema = CddlValidator.loadSchema("root = int");

        assertValid(schema, CBOR.value(42));
    }

    @Test
    void intAcceptsZero() {
        CddlSchema schema = CddlValidator.loadSchema("root = int");

        assertValid(schema, CBOR.value(0));
    }

    @Test
    void intAcceptsLongMaxValue() {
        CddlSchema schema = CddlValidator.loadSchema("root = int");

        assertValid(schema, CBOR.integer(Long.MAX_VALUE));
    }

    @Test
    void intAcceptsLongMinValue() {
        CddlSchema schema = CddlValidator.loadSchema("root = int");

        assertValid(schema, CBOR.integer(Long.MIN_VALUE));
    }

    @Test
    void intAcceptsBigIntegerBeyondLongMaxValue() {
        CddlSchema schema = CddlValidator.loadSchema("root = int");

        BigInteger value = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        assertValid(schema, CBOR.integer(value));
    }

    @Test
    void intAcceptsBigIntegerBelowLongMinValue() {
        CddlSchema schema = CddlValidator.loadSchema("root = int");

        BigInteger value = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE);
        assertValid(schema, CBOR.integer(value));
    }

    @Test
    void intRejectsFloat() {
        CddlSchema schema = CddlValidator.loadSchema("root = int");

        assertInvalid(schema, CBOR.value(3.14));
    }

    @Test
    void intRejectsText() {
        CddlSchema schema = CddlValidator.loadSchema("root = int");

        assertInvalid(schema, CBOR.value("-42"));
    }

    // ------------------------------------------------------------------
    // nint
    // ------------------------------------------------------------------

    @Test
    void nintAcceptsNegativeInteger() {
        CddlSchema schema = CddlValidator.loadSchema("root = nint");

        assertValid(schema, CBOR.value(-1));
    }

    @Test
    void nintRejectsZero() {
        CddlSchema schema = CddlValidator.loadSchema("root = nint");

        assertInvalid(schema, CBOR.value(0));
    }

    // ------------------------------------------------------------------
    // float
    // ------------------------------------------------------------------

    @Test
    void floatAcceptsDouble() {
        CddlSchema schema = CddlValidator.loadSchema("root = float");

        assertValid(schema, CBOR.value(1.5));
    }

    @Test
    void floatAcceptsFloatingPoint() {
        CddlSchema schema = CddlValidator.loadSchema("root = float");

        assertValid(schema, CBOR.floatingPoint(3.14));
    }

    @Test
    void floatAcceptsFloat() {
        CddlSchema schema = CddlValidator.loadSchema("root = float");

        assertValid(schema, CBOR.floatingPoint(2.718281828));
    }

    @Test
    void floatRejectsInteger() {
        CddlSchema schema = CddlValidator.loadSchema("root = float");

        assertInvalid(schema, CBOR.integer(42));
    }

    @Test
    void floatRejectsText() {
        CddlSchema schema = CddlValidator.loadSchema("root = float");

        assertInvalid(schema, CBOR.text("3.14"));
    }

    @Test
    void floatAcceptsZero() {
        CddlSchema schema = CddlValidator.loadSchema("root = float");

        assertValid(schema, CBOR.value(0.0));
        assertValid(schema, CBOR.value(-0.0));
    }

    @Test
    void floatAcceptsSmallestPositiveDouble() {
        CddlSchema schema = CddlValidator.loadSchema("root = float");

        assertValid(schema, CBOR.floatingPoint(Double.MIN_VALUE));
    }

    @Test
    void floatAcceptsLargestFiniteDouble() {
        CddlSchema schema = CddlValidator.loadSchema("root = float");

        assertValid(schema, CBOR.floatingPoint(Double.MAX_VALUE));
    }

    @Test
    void floatInfinity() {
        CddlSchema schema = CddlValidator.loadSchema("root = float");

        assertValid(schema, CBOR.floatingPoint(Double.POSITIVE_INFINITY));
    }

    @Test
    void float16AcceptsFloat() {
        CddlSchema schema = CddlValidator.loadSchema("root = float16");

        assertValid(schema, CBOR.floatingPoint(1.5f));
    }

    @Test
    void float16RejectsInteger() {
        CddlSchema schema = CddlValidator.loadSchema("root = float16");

        assertInvalid(schema, CBOR.integer(42));
    }

    @Test
    void float16RejectsText() {
        CddlSchema schema = CddlValidator.loadSchema("root = float16");

        assertInvalid(schema, CBOR.text("1.5"));
    }

    @Test
    void float32AcceptsFloat() {
        CddlSchema schema = CddlValidator.loadSchema("root = float32");

        assertValid(schema, CBOR.floatingPoint(2.718281828f));
    }

    @Test
    void float32RejectsInteger() {
        CddlSchema schema = CddlValidator.loadSchema("root = float32");

        assertInvalid(schema, CBOR.integer(42));
    }

    @Test
    void float32RejectsText() {
        CddlSchema schema = CddlValidator.loadSchema("root = float32");

        assertInvalid(schema, CBOR.text("2.718281828"));
    }

    @Test
    void float64AcceptsFloat() {
        CddlSchema schema = CddlValidator.loadSchema("root = float64");

        assertValid(schema, CBOR.floatingPoint(2.718281828));
    }

    @Test
    void float64RejectsInteger() {
        CddlSchema schema = CddlValidator.loadSchema("root = float64");

        assertInvalid(schema, CBOR.integer(42));
    }

    @Test
    void float64RejectsText() {
        CddlSchema schema = CddlValidator.loadSchema("root = float64");

        assertInvalid(schema, CBOR.text("2.718281828"));
    }

    // ------------------------------------------------------------------
    // tstr
    // ------------------------------------------------------------------

    @Test
    void tstrAcceptsString() {
        CddlSchema schema = CddlValidator.loadSchema("root = tstr");

        assertValid(schema, CBOR.value("hello"));
    }

    @Test
    void tstrAcceptsEmptyString() {
        CddlSchema schema = CddlValidator.loadSchema("root = tstr");

        assertValid(schema, CBOR.value(""));
    }

    @Test
    void tstrRejectsByteString() {
        CddlSchema schema = CddlValidator.loadSchema("root = tstr");
        assertInvalid(schema, CBOR.value(new byte[] {0x01, 0x02}));
    }

    @Test
    void tstrRejectsInteger() {
        CddlSchema schema = CddlValidator.loadSchema("root = tstr");
        assertInvalid(schema, CBOR.integer(123));
    }

    @Test
    void tstrRejectsNull() {
        CddlSchema schema = CddlValidator.loadSchema("root = tstr");
        assertInvalid(schema, CBOR.nullValue());
    }

    @Test
    void unicodeTextIsValidTstr() {
        CddlSchema schema = CddlValidator.loadSchema("value = tstr");

        assertValid(schema, CBOR.text("héllo 世界 🌍"));
    }

    @Test
    void longTextIsValidTstr() {
        CddlSchema schema = CddlValidator.loadSchema("value = tstr");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10_000; i++) {
            sb.append("x");
        }
        assertValid(schema, CBOR.text(sb.toString()));
    }

    @Test
    void exactTextValueIgnoreWhitespace() {
        CddlSchema schema = CddlValidator.loadSchema("value = \"hello\"");

        assertValid(schema, CBOR.text("hello"));
        assertInvalid(schema, CBOR.text(" hello"));
        assertInvalid(schema, CBOR.text("hello "));
        assertInvalid(schema, CBOR.text("hello\n"));
    }

    // ------------------------------------------------------------------
    // bstr
    // ------------------------------------------------------------------

    @Test
    void bstrAcceptsByteString() {
        CddlSchema schema = CddlValidator.loadSchema("root = bstr");
        assertValid(schema, CBOR.value(new byte[] {(byte) 0xde, (byte) 0xad}));
    }

    @Test
    void bstrAcceptsEmptyByteString() {
        CddlSchema schema = CddlValidator.loadSchema("root = bstr");
        assertValid(schema, CBOR.value(new byte[0]));
    }

    @Test
    void bstrRejectsTextString() {
        CddlSchema schema = CddlValidator.loadSchema("root = bstr");

        assertInvalid(schema, CBOR.value("not bytes"));
    }

    @Test
    void bstrRejectsInteger() {
        CddlSchema schema = CddlValidator.loadSchema("root = bstr");

        assertInvalid(schema, CBOR.integer(0));
    }

    @Test
    void largeByteStringIsAccepted() {
        CddlSchema schema = CddlValidator.loadSchema("value = bstr");

        byte[] data = new byte[4096];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        assertValid(schema, CBOR.bytes(data));
    }

    // ------------------------------------------------------------------
    // bool
    // ------------------------------------------------------------------

    @Test
    void boolAcceptsTrue() {
        CddlSchema schema = CddlValidator.loadSchema("root = bool");

        assertValid(schema, CBORValue.TRUE);
    }

    @Test
    void boolAcceptsFalse() {
        CddlSchema schema = CddlValidator.loadSchema("root = bool");

        assertValid(schema, CBORValue.FALSE);
    }

    @Test
    void boolRejectsInteger() {
        CddlSchema schema = CddlValidator.loadSchema("root = bool");

        assertInvalid(schema, CBOR.value(0));
        assertInvalid(schema, CBOR.value(1));
    }

    // ------------------------------------------------------------------
    // nil
    // ------------------------------------------------------------------

    @Test
    void nilAcceptsNull() {
        CddlSchema schema = CddlValidator.loadSchema("root = nil");

        assertValid(schema, CBOR.nullValue());
    }

    @Test
    void nilRejectsFalse() {
        CddlSchema schema = CddlValidator.loadSchema("root = nil");

        assertInvalid(schema, CBOR.falseValue());
    }

    @Test
    void nullAcceptsNullValue() {
        CddlSchema schema = CddlValidator.loadSchema("root = null");

        assertValid(schema, CBOR.nullValue());
    }

    @Test
    void nullRejectsUndefined() {
        CddlSchema schema = CddlValidator.loadSchema("root = null");

        assertInvalid(schema, CBOR.undefined());
    }

    @Test
    void nullRejectsTextNull() {
        CddlSchema schema = CddlValidator.loadSchema("root = null");

        assertInvalid(schema, CBOR.value("null"));
    }

    // ------------------------------------------------------------------
    // undefined
    // ------------------------------------------------------------------

    @Test
    void undefinedAcceptsUndefined() {
        CddlSchema schema = CddlValidator.loadSchema("root = undefined");

        assertValid(schema, CBOR.undefined());
    }

    @Test
    void undefinedRejectsNull() {
        CddlSchema schema = CddlValidator.loadSchema("root = undefined");

        assertInvalid(schema, CBOR.nullValue());
    }

    // ------------------------------------------------------------------
    // simple values
    // ------------------------------------------------------------------

    @Test
    void simpleValueIsNotAcceptedAsInteger() {
        CddlSchema schema = CddlValidator.loadSchema("value = uint");

        assertInvalid(schema, CBOR.simpleValue(10));
    }

    @Test
    void simpleValueIsNotAcceptedAsBoolean() {
        CddlSchema schema = CddlValidator.loadSchema("value = bool");

        assertInvalid(schema, CBOR.simpleValue(20));
    }

    @Test
    void differentSimpleValuesDoNotCompareAsEqual() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = 10");

        assertInvalid(schema, CBOR.simpleValue(10));
    }

    @Test
    void simpleValuesDoNotSatisfyRepeatedIntegerConstraint() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [* uint]");

        CBORValue values = CBOR.array()
                .add(CBOR.integer(1))
                .add(CBOR.simpleValue(10))
                .add(CBOR.integer(3))
                .value();

        assertInvalid(schema, values);
    }

    // ------------------------------------------------------------------
    // any
    // ------------------------------------------------------------------

    @Test
    void anyAcceptsInteger() {
        CddlSchema schema = CddlValidator.loadSchema("root = any");

        assertValid(schema, CBOR.value(99));
    }

    @Test
    void anyAcceptsNull() {
        CddlSchema schema = CddlValidator.loadSchema("root = any");

        assertValid(schema, CBORValue.NULL);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void assertValid(CddlSchema schema, CBORValue value) {
        ValidationResult result = schema.validate(value);
        assertTrue(result.isValid(), "Expected valid but got errors: " + result.getErrors());
    }

    private static void assertInvalid(CddlSchema schema, CBORValue value) {
        ValidationResult result = schema.validate(value);

        assertFalse(result.isValid(), "Expected invalid but validation passed");
        assertFalse(result.getErrors().isEmpty());
    }
    
}