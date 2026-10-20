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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for RFC 8610 Appendix D standard prelude types.
 *
 * <p>Every type listed in Appendix D must be usable in a CDDL schema without
 * an explicit declaration by the user.
 */
class PreludeValidationTest {

    // -----------------------------------------------------------------------
    // Numeric super-types
    // -----------------------------------------------------------------------

    @Test
    void numberAcceptsUint() {
        assertValid("root = number", CBOR.integer(42));
    }

    @Test
    void numberAcceptsNint() {
        assertValid("root = number", CBOR.integer(-5));
    }

    @Test
    void numberAcceptsFloat() {
        assertValid("root = number", CBOR.floatingPoint(3.14));
    }

    @Test
    void numberRejectsText() {
        assertInvalid("root = number", CBOR.text("hello"));
    }

    // -----------------------------------------------------------------------
    // Float sub-ranges
    // -----------------------------------------------------------------------

    @Test
    void float16_32AcceptsFloat() {
        assertValid("root = float16-32", CBOR.floatingPoint(1.5f));
    }

    @Test
    void float32_64AcceptsFloat() {
        assertValid("root = float32-64", CBOR.floatingPoint(2.718281828));
    }

    @Test
    void float16RejectsValuesThatRequireMorePrecision() {
        CddlSchema schema = CddlValidator.loadSchema("root = float16");

        assertValid(schema, CBOR.floatingPoint(65504.0));
        assertInvalid(schema, CBOR.floatingPoint(65505.0));
    }

    @Test
    void float32RejectsValuesThatRequireMorePrecision() {
        CddlSchema schema = CddlValidator.loadSchema("root = float32");

        assertValid(schema, CBOR.floatingPoint(16777216.0));
        assertInvalid(schema, CBOR.floatingPoint(16777217.0));
    }

    @Test
    void float16_32RejectsInteger() {
        assertInvalid("root = float16-32", CBOR.integer(42));
    }

    @Test
    void float16_32AcceptsFloat32() {
        assertValid("root = float16-32", CBOR.floatingPoint(2.718281828f));
    }

    @Test
    void float32_64AcceptsFloat64() {
        assertValid("root = float32-64", CBOR.floatingPoint(2.718281828));
    }

    @Test
    void float32_64RejectsInteger() {
        assertInvalid("root = float32-64", CBOR.integer(42));
    }

    // -----------------------------------------------------------------------
    // Boolean singletons: true / false as types
    // -----------------------------------------------------------------------

    @Test
    void trueTypeAcceptsTrue() {
        assertValid("root = true", CBORValue.TRUE);
    }

    @Test
    void trueTypeRejectsFalse() {
        assertInvalid("root = true", CBORValue.FALSE);
    }

    @Test
    void falseTypeAcceptsFalse() {
        assertValid("root = false", CBORValue.FALSE);
    }

    @Test
    void falseTypeRejectsTrue() {
        assertInvalid("root = false", CBORValue.TRUE);
    }

    // -----------------------------------------------------------------------
    // "bstr" or "bytes"  A byte string (major type 2). For "bstr" tests see PrimitiveValidationTest
    // -----------------------------------------------------------------------

    @Test
    void bytesAcceptsByteString() {
        assertValid("root = bytes", CBOR.value(new byte[] {(byte) 0xde, (byte) 0xad}));
    }

    @Test
    void bytesAcceptsEmptyByteString() {
        assertValid("root = bytes", CBOR.value(new byte[0]));
    }

    @Test
    void bytesRejectsTextString() {
        assertInvalid("root = bytes", CBOR.value("not bytes"));
    }

    @Test
    void bytesRejectsInteger() {
        assertInvalid("root = bytes", CBOR.integer(0));
    }

    // -----------------------------------------------------------------------
    // ""tstr" or "text"  Text string (major type 3). For "tstr" tests see PrimitiveValidationTest
    // -----------------------------------------------------------------------

    @Test
    void textAcceptsString() {
        assertValid("root = text", CBOR.value("hello"));
    }

    @Test
    void textAcceptsEmptyString() {
        assertValid("root = text", CBOR.value(""));
    }

    @Test
    void textRejectsByteString() {
        assertInvalid("root = text", CBOR.value(new byte[] {0x01, 0x02}));
    }

    @Test
    void textRejectsInteger() {
        assertInvalid("root = text", CBOR.integer(123));
    }

    // -----------------------------------------------------------------------
    // Big integer types (RFC 8949 §3.4.3)
    // -----------------------------------------------------------------------

    @Test
    void biguintAcceptsTaggedBstrTag2() {
        // biguint = #6.2(bstr)
        byte[] bytes = { 0x01, 0x00 };

        CBORValue tagged = CBOR.tag(2, CBOR.bytes(bytes));
        assertValid("root = biguint", tagged);
    }

    @Test
    void biguintRejectsWrongTag() {
        byte[] bytes = {0x01};

        CBORValue tagged = CBOR.tag(3, CBOR.bytes(bytes));
        assertInvalid("root = biguint", tagged);
    }

    @Test
    void bignintAcceptsTaggedBstrTag3() {
        // bignint = #6.3(bstr)
        byte[] bytes = {0x01};

        CBORValue tagged = CBOR.tag(3, CBOR.bytes(bytes));
        assertValid("root = bignint", tagged);
    }

    @Test
    void bigintAcceptsBiguint() {
        byte[] bytes = {0x01};

        CBORValue tagged = CBOR.tag(2, CBOR.bytes(bytes));
        assertValid("root = bigint", tagged);
    }

    @Test
    void bigintAcceptsBignint() {
        byte[] bytes = {0x01};

        CBORValue tagged = CBOR.tag(3, CBOR.bytes(bytes));
        assertValid("root = bigint", tagged);
    }

    @Test
    void bigintRejectsPlainInteger() {
        assertInvalid("root = bigint", CBOR.integer(42));
    }

    @Test
    void integerAcceptsPlainInt() {
        assertValid("root = integer", CBOR.integer(-7));
    }

    @Test
    void integerAcceptsBiguint() {
        byte[] bytes = {0x01};

        CBORValue tagged = CBOR.tag(2, CBOR.bytes(bytes));
        assertValid("root = integer", tagged);
    }

    @Test
    void unsignedAcceptsUint() {
        assertValid("root = unsigned", CBOR.integer(0));
    }

    @Test
    void unsignedRejectsNegative() {
        assertInvalid("root = unsigned", CBOR.integer(-1));
    }

    // -----------------------------------------------------------------------
    // decfrac / bigfloat (RFC 8949 §3.4.4)
    // -----------------------------------------------------------------------

    @Test
    void decfracAcceptsTag4WithIntBigintArray() {
        // decfrac = #6.4([int, bigint])
        CBORValue mantissa = CBOR.tag(2, CBOR.bytes(new byte[]{0x03, (byte) 0xE8}));

        CBORValue array = CBOR.array()
                .add(CBOR.integer(-2))
                .add(mantissa)
                .value();

        CBORValue tagged = CBOR.tag(4, array);

        assertValid("root = decfrac", tagged);
    }

    @Test
    void decfracRejectsWrongTag() {
        CBORValue mantissa = CBOR.tag(2, CBOR.bytes(new byte[]{0x01}));

        CBORValue array = CBOR.array()
                .add(CBOR.integer(-2))
                .add(mantissa)
                .value();

        CBORValue tagged = CBOR.tag(5, array);

        assertInvalid("root = decfrac", tagged);
    }

    @Test
    void bigfloatAcceptsTag5Array() {
        // bigfloat = #6.5([int, bigint])
        CBORValue mantissa = CBOR.tag(2, CBOR.bytes(new byte[]{0x01}));

        CBORValue array = CBOR.array()
                .add(CBOR.integer(1))
                .add(mantissa)
                .value();

        CBORValue tagged = CBOR.tag(5, array);

        assertValid("root = bigfloat", tagged);
    }

    // -----------------------------------------------------------------------
    // Date / time tags (RFC 8949 §3.4.1)
    // -----------------------------------------------------------------------

    @Test
    void tdateAcceptsTag0Tstr() {
        // tdate = #6.0(tstr)
        CBORValue tagged = CBOR.tag(0, CBOR.text("2024-01-15T12:00:00Z"));

        assertValid("root = tdate", tagged);
    }

    @Test
    void tdateRejectsUntaggedTstr() {
        assertInvalid("root = tdate", CBOR.text("2024-01-15T12:00:00Z"));
    }

    @Test
    void timeAcceptsTag1Uint() {
        // time = #6.1(uint / float)
        CBORValue tagged = CBOR.tag(1, CBOR.integer(1_700_000_000L));

        assertValid("root = time", tagged);
    }

    @Test
    void timeAcceptsTag1Float() {
        CBORValue tagged = CBOR.tag(1, CBOR.floatingPoint(1_700_000_000.5));

        assertValid("root = time", tagged);
    }

    @Test
    void timeRejectsUntaggedUint() {
        assertInvalid("root = time", CBOR.integer(1_700_000_000L));
    }

    // -----------------------------------------------------------------------
    // URI / encoding tags
    // -----------------------------------------------------------------------

    @Test
    void uriAcceptsTag32Tstr() {
        CBORValue tagged = CBOR.tag(32, CBOR.text("https://example.com"));
        assertValid("root = uri", tagged);
    }

    @Test
    void uriRejectsUntaggedTstr() {
        assertInvalid("root = uri", CBOR.text("https://example.com"));
    }

    @Test
    void uriRejectsTag32NonTstr() {
        CBORValue tagged = CBOR.tag(32, CBOR.integer(42));
        assertInvalid("root = uri", tagged);
    }

    @Test
    void b64urlAcceptsTag33Tstr() {
        CBORValue tagged = CBOR.tag(33, CBOR.text("SGVsbG8="));
        assertValid("root = b64url", tagged);
    }

    @Test
    void b64legacyAcceptsTag34Tstr() {
        CBORValue tagged = CBOR.tag(34, CBOR.text("SGVsbG8="));
        assertValid("root = b64legacy", tagged);
    }

    @Test
    void regexpAcceptsTag35Tstr() {
        CBORValue tagged = CBOR.tag(35, CBOR.text("^[a-z]+$"));
        assertValid("root = regexp", tagged);
    }

    @Test
    void mimeMessageAcceptsTag36Tstr() {
        CBORValue tagged = CBOR.tag(36, CBOR.text("From: alice@example.com\r\nSubject: hello"));
        assertValid("root = mime-message", tagged);
    }

    // -----------------------------------------------------------------------
    // encoded-cbor (tag 24)
    // -----------------------------------------------------------------------

    @Test
    void encodedCborAcceptsTag24Bstr() {
        // encoded-cbor = #6.24(bstr)
        //
        // The actual byte encoding is deliberately not tested here.
        // The validation test only needs a byte string payload.
        byte[] innerCbor = {0x18, 0x2A};

        CBORValue tagged = CBOR.tag(24, CBOR.bytes(innerCbor));
        assertValid("root = encoded-cbor", tagged);
    }

    @Test
    void encodedCborRejectsTag24Tstr() {
        CBORValue tagged = CBOR.tag(24, CBOR.text("not-bytes"));
        assertInvalid("root = encoded-cbor", tagged);
    }

    // -----------------------------------------------------------------------
    // eb64url / eb64legacy / eb16
    // -----------------------------------------------------------------------

    @Test
    void eb64urlAcceptsTag21AnyPayload() {
        CBORValue tagged = CBOR.tag(21, CBOR.text("base64url-encoded"));
        assertValid("root = eb64url", tagged);
    }

    @Test
    void eb64legacyAcceptsTag22AnyPayload() {
        CBORValue tagged = CBOR.tag(22, CBOR.bytes(new byte[]{0x01, 0x02}));
        assertValid("root = eb64legacy", tagged);
    }

    @Test
    void eb16AcceptsTag23AnyPayload() {
        CBORValue tagged = CBOR.tag(23, CBOR.integer(42));
        assertValid("root = eb16", tagged);
    }

    // -----------------------------------------------------------------------
    // cbor-any (tag 55799)
    // -----------------------------------------------------------------------

    @Test
    void cborAnyAcceptsTag55799() {
        CBORValue tagged = CBOR.tag(55799, CBOR.text("payload"));
        assertValid("root = cbor-any", tagged);
    }

    @Test
    void cborAnyRejectsUntaggedValue() {
        assertInvalid("root = cbor-any", CBOR.text("payload"));
    }

    // -----------------------------------------------------------------------
    // Prelude types usable inside user schemas
    // -----------------------------------------------------------------------

    @Test
    void preludeTypesUsableInsideUserMap() {
        CddlSchema schema = CddlValidator.loadSchema(
                "event = { \"url\": uri, \"ts\": time, \"count\": unsigned }");

        CBORValue url = CBOR.tag(32, CBOR.text("https://example.com/event"));

        CBORValue timestamp = CBOR.tag(1, CBOR.integer(1_700_000_000L));

        CBORValue map = CBOR.map()
                .put("url", url)
                .put("ts", timestamp)
                .put("count", CBOR.integer(0))
                .value();

        ValidationResult result = schema.validate(map);
        assertTrue(result.isValid(), "Expected valid but got: " + result.getErrors());
    }

    @Test
    void preludeTypeNameCanBeShadowedByUserRule() {
        // User defines "uri" as plain tstr — overrides the prelude.
        CddlSchema schema = CddlValidator.loadSchema(
                "uri = tstr\n"
                        + "root = uri");
        assertValid(schema, CBOR.text("https://example.com"));

        CBORValue tagged = CBOR.tag(32, CBOR.text("https://example.com"));
        assertInvalid(schema, tagged);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static void assertValid(String cddl, CBORValue value) {
        CddlSchema schema = CddlValidator.loadSchema(cddl);
        assertValid(schema, value);
    }

    private static void assertInvalid(String cddl, CBORValue value) {
        CddlSchema schema = CddlValidator.loadSchema(cddl);
        assertInvalid(schema, value);
    }

    private static void assertValid(CddlSchema schema, CBORValue value) {
        ValidationResult result = schema.validate(value);
        assertTrue(result.isValid(), "Expected valid but got: " + result.getErrors());
    }

    private static void assertInvalid(CddlSchema schema, CBORValue value) {
        ValidationResult result = schema.validate(value);
        assertFalse(result.isValid(), "Expected invalid but validation passed");
    }

}