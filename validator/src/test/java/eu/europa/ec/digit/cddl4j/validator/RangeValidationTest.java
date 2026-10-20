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
 * Tests for range type validation: {@code min..max} and {@code min...max}.
 */
class RangeValidationTest {

    @Test
    void inclusiveRangeLowerBound() {
        CddlSchema schema = CddlValidator.loadSchema("byte = 0..255");
        assertValid(schema, CBOR.value(0));
    }

    @Test
    void inclusiveRangeUpperBound() {
        CddlSchema schema = CddlValidator.loadSchema("byte = 0..255");
        assertValid(schema, CBOR.value(255));
    }

    @Test
    void inclusiveRangeMiddle() {
        CddlSchema schema = CddlValidator.loadSchema("byte = 0..255");
        assertValid(schema, CBOR.value(128));
    }

    @Test
    void inclusiveRangeBelowLowerFails() {
        CddlSchema schema = CddlValidator.loadSchema("byte = 0..255");
        assertInvalid(schema, CBOR.value(-1));
    }

    @Test
    void inclusiveRangeAboveUpperFails() {
        CddlSchema schema = CddlValidator.loadSchema("byte = 0..255");
        assertInvalid(schema, CBOR.value(256));
    }

    @Test
    void exclusiveUpperBoundExcludesMax() {
        CddlSchema schema = CddlValidator.loadSchema("exclusive = 0...256");
        assertInvalid(schema, CBOR.value(256));
    }

    @Test
    void exclusiveUpperBoundAcceptsJustBelow() {
        CddlSchema schema = CddlValidator.loadSchema("exclusive = 0...256");
        assertValid(schema, CBOR.value(255));
    }

    @Test
    void negativeRange() {
        CddlSchema schema = CddlValidator.loadSchema("neg = -10..-1");
        assertValid(schema, CBOR.value(-5));
        assertInvalid(schema, CBOR.value(0));
        assertInvalid(schema, CBOR.value(-11));
    }

    @Test
    void rangeRejectsNonNumeric() {
        CddlSchema schema = CddlValidator.loadSchema("byte = 0..255");
        assertInvalid(schema, CBOR.value("hello"));
    }

    @Test
    void rangeInMapValue() {
        CddlSchema schema = CddlValidator.loadSchema("packet = { ttl: 0..255, id: uint }");

        CBORValue valid = CBOR.map()
                .put(CBOR.value("ttl"), CBOR.value(64))
                .put(CBOR.value("id"), CBOR.value(1234))
                .value();

        assertValid(schema, valid);
    }

    @Test
    void rangeInMapValueOutOfBounds() {
        CddlSchema schema = CddlValidator.loadSchema("packet = { ttl: 0..255, id: uint }");

        CBORValue bad = CBOR.map()
                .put(CBOR.value("ttl"), CBOR.value(300))
                .put(CBOR.value("id"), CBOR.value(1))
                .value();

        assertInvalid(schema, bad);
    }

    @Test
    void integerRangeAcceptsBounds() {
        CddlSchema schema = CddlValidator.loadSchema("value = 10..20");

        assertValid(schema, CBOR.integer(10));
        assertValid(schema, CBOR.integer(15));
        assertValid(schema, CBOR.integer(20));

        assertInvalid(schema, CBOR.integer(9));
        assertInvalid(schema, CBOR.integer(21));

        assertInvalid(schema, CBOR.floatingPoint(15.0));
        assertInvalid(schema, CBOR.text("15"));
    }

    @Test
    void booleanIsNotCoercedToNumericValue() {
        CddlSchema schema = CddlValidator.loadSchema("value = 0..1");

        assertInvalid(schema, CBOR.bool(true));
        assertInvalid(schema, CBOR.bool(false));
    }

    @Test
    void floatRangeAcceptsBounds() {
        CddlSchema schema = CddlValidator.loadSchema("value = 0.0..10.0");

        assertValid(schema, CBOR.floatingPoint(0));
        assertValid(schema, CBOR.floatingPoint(5));
        assertValid(schema, CBOR.floatingPoint(10));

        assertInvalid(schema, CBOR.integer(0));
        assertInvalid(schema, CBOR.integer(5));
        assertInvalid(schema, CBOR.integer(10));

        assertInvalid(schema, CBOR.text("5"));
    }

    @Test
    void intAndFloatRangeAcceptsBounds() {
        CddlSchema schema = CddlValidator.loadSchema("value = 0..10.0");

        assertValid(schema, CBOR.floatingPoint(0));
        assertValid(schema, CBOR.floatingPoint(5));
        assertValid(schema, CBOR.floatingPoint(10));

        assertValid(schema, CBOR.integer(0));
        assertValid(schema, CBOR.integer(5));
        assertValid(schema, CBOR.integer(10));

        assertInvalid(schema, CBOR.text("5"));
    }

    @Test
    void floatAndIntRangeAcceptsBounds() {
        CddlSchema schema = CddlValidator.loadSchema("value = 0.0..10");

        assertValid(schema, CBOR.floatingPoint(0));
        assertValid(schema, CBOR.floatingPoint(5));
        assertValid(schema, CBOR.floatingPoint(10));

        assertValid(schema, CBOR.integer(0));
        assertValid(schema, CBOR.integer(5));
        assertValid(schema, CBOR.integer(10));

        assertInvalid(schema, CBOR.text("5"));
    }

    @Test
    void singleNameReferenceConfusionTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "id = min..max\n"
                        + "min..max = tstr");

        assertValid(schema, CBOR.text("123456"));
        assertInvalid(schema, CBOR.integer(10));
    }

    @Test
    void rangeReferencesTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "id = min .. max\n"
                        + "min = 10\n"
                        + "max = 20\n");

        assertValid(schema, CBOR.integer(10));
    }

    @Test
    void rangeReferencesCombinationTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "device-address = byte\n"
                        + "max-byte = 255\n"
                        + "byte = 0..max-byte ; inclusive range\n");

        assertValid(schema, CBOR.integer(0));
        assertValid(schema, CBOR.integer(10));
        assertValid(schema, CBOR.integer(255));
        assertInvalid(schema, CBOR.integer(-1));
        assertInvalid(schema, CBOR.integer(256));
    }

    @Test
    void rangeReferencesCombinationDiffTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "device-address = byte1\n"
                        + "first-non-byte = 256\n"
                        + "byte1 = 0...first-non-byte ; byte1 is equivalent to byte\n");

        assertValid(schema, CBOR.integer(0));
        assertValid(schema, CBOR.integer(10));
        assertValid(schema, CBOR.integer(255));
        assertInvalid(schema, CBOR.integer(-1));
        assertInvalid(schema, CBOR.integer(256));
    }

    @Test
    void rangeEmptySetTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "id = 10..5");

        assertInvalid(schema, CBOR.integer(0));
        assertInvalid(schema, CBOR.integer(5));
        assertInvalid(schema, CBOR.integer(7));
        assertInvalid(schema, CBOR.integer(10));
        assertInvalid(schema, CBOR.integer(15));
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
