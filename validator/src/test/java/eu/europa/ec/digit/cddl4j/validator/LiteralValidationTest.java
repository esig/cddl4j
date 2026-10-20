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
 * Tests literal types: specific integer, float, text, boolean, and byte values.
 */
class LiteralValidationTest {

    @Test
    void integerLiteralMatchesExact() {
        CddlSchema schema = CddlValidator.loadSchema("root = 42");

        assertValid(schema, CBOR.integer(42));
    }

    @Test
    void integerLiteralRejectsDifferentValue() {
        CddlSchema schema = CddlValidator.loadSchema("root = 42");

        assertInvalid(schema, CBOR.integer(43));
    }

    @Test
    void integerLiteralRejectsText() {
        CddlSchema schema = CddlValidator.loadSchema("root = 42");

        assertInvalid(schema, CBOR.text("42"));
    }

    @Test
    void hexademicalIntegerLiteralMatches() {
        CddlSchema schema = CddlValidator.loadSchema("root = 0xFF");

        assertValid(schema, CBOR.integer(255));
    }

    @Test
    void hexademicalIntegerLiteralRejects() {
        CddlSchema schema = CddlValidator.loadSchema("root = 0xFF");

        assertInvalid(schema, CBOR.integer(0));
    }

    @Test
    void hexademicalIntegerLiteralRejectsBinaries() {
        CddlSchema schema = CddlValidator.loadSchema("root = 0xFF");

        assertInvalid(schema, CBOR.text("0xFF"));
    }

    @Test
    void floatingPointMatchesExact() {
        CddlSchema schema = CddlValidator.loadSchema("root = 1.5");

        assertValid(schema, CBOR.floatingPoint(1.5));
    }

    @Test
    void floatingPointRejectsInvalid() {
        CddlSchema schema = CddlValidator.loadSchema("root = 1.5");

        assertInvalid(schema, CBOR.floatingPoint(3.14));
    }

    @Test
    void floatingPointRejectsInteger() {
        CddlSchema schema = CddlValidator.loadSchema("root = 1.0");

        assertInvalid(schema, CBOR.integer(1));
    }

    @Test
    void textLiteralMatchesExact() {
        CddlSchema schema = CddlValidator.loadSchema("root = \"hello\"");

        assertValid(schema, CBOR.text("hello"));
    }

    @Test
    void textLiteralRejectsDifferentString() {
        CddlSchema schema = CddlValidator.loadSchema("root = \"hello\"");

        assertInvalid(schema, CBOR.text("world"));
    }

    @Test
    void boolTrueLiteralMatches() {
        CddlSchema schema = CddlValidator.loadSchema("root = true");

        assertValid(schema, CBOR.trueValue());
    }

    @Test
    void boolTrueLiteralRejectsFalse() {
        CddlSchema schema = CddlValidator.loadSchema("root = true");

        assertInvalid(schema, CBOR.falseValue());
    }

    @Test
    void boolFalseLiteralMatches() {
        CddlSchema schema = CddlValidator.loadSchema("root = false");

        assertValid(schema, CBOR.falseValue());
    }

    @Test
    void hexBytesLiteralMatches() {
        CddlSchema schema = CddlValidator.loadSchema("root = h'deadbeef'");

        assertValid(schema, CBOR.bytes(new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF }));
    }

    @Test
    void hexBytesLiteralRejectsDifferent() {
        CddlSchema schema = CddlValidator.loadSchema("root = h'deadbeef'");

        assertInvalid(schema, CBOR.bytes(new byte[] {0x00}));
    }

    @Test
    void b64BytesLiteralMatches() {
        CddlSchema schema = CddlValidator.loadSchema("root = b64'aGl3'");

        assertValid(schema, CBOR.bytes(new byte[] { 104, 105, 119 }));
    }

    @Test
    void b64BytesLiteralRejectsInvalid() {
        CddlSchema schema = CddlValidator.loadSchema("root = b64'aGl3'");

        assertInvalid(schema, CBOR.bytes(new byte[] {0x00}));
    }

    @Test
    void b64BytesLiteralRejectsString() {
        CddlSchema schema = CddlValidator.loadSchema("root = b64'aGl3'");

        assertInvalid(schema, CBOR.text("aGl3"));
    }

    @Test
    void negativeIntegerLiteralMatches() {
        CddlSchema schema = CddlValidator.loadSchema("root = -1");

        assertValid(schema, CBOR.integer(-1));
    }

    @Test
    void zeroLiteralMatches() {
        CddlSchema schema = CddlValidator.loadSchema("root = 0");

        assertValid(schema, CBOR.integer(0));
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