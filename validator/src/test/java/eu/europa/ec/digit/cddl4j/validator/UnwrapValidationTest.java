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
import eu.europa.ec.digit.cddl4j.validator.schema.parser.CddlParser;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnwrapValidationTest {

    @Test
    void unwrapTaggedTypeMatchesUnderlyingType() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = ~tagged\n"
                        + "tagged = #6.1(tstr)");

        assertValid(schema, CBOR.text("hello"));
        assertValid(schema, CBOR.text(""));
        assertInvalid(schema, CBOR.integer(42));
    }

    @Test
    void unwrapTaggedTypeDoesNotRequireTag() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = ~tagged\n"
                        + "tagged = #6.1(tstr)");

        assertValid(schema, CBOR.text("hello"));
        assertInvalid(schema, CBOR.tag(1, CBOR.text("hello")));
    }

    @Test
    void unwrapSupportsForwardReference() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = ~value\n"
                        + "value = #6.32(uint)");

        assertValid(schema, CBOR.integer(42));
        assertInvalid(schema, CBOR.text("hello"));
    }

    @Test
    void unwrapArrayGroup() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = [~base, third: bool]\n"
                        + "base = [first: uint, second: tstr]");

        assertValid(schema, CBOR.array()
                .add(CBOR.integer(42))
                .add(CBOR.text("hello"))
                .add(CBOR.trueValue())
                .value());

        assertInvalid(schema, CBOR.array()
                .add(CBOR.integer(42))
                .add(CBOR.trueValue())
                .add(CBOR.trueValue())
                .value());

        assertInvalid(schema, CBOR.array()
                .add(CBOR.integer(42))
                .add(CBOR.text("hello"))
                .value());
    }

    @Test
    void unwrapArrayGroupThreadsEntriesIntoContainingArray() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = [~base, third: bool]\n"
                        + "base = [first: uint, second: tstr]");

        assertValid(schema, CBOR.array()
                .add(CBOR.integer(1))
                .add(CBOR.text("text"))
                .add(CBOR.falseValue())
                .value());

        assertInvalid(schema, CBOR.array()
                .add(CBOR.array()
                        .add(CBOR.integer(1))
                        .add(CBOR.text("text"))
                        .value())
                .add(CBOR.falseValue())
                .value());
    }

    @Test
    void unwrapMapGroup() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = {~base, enabled: bool}\n"
                        + "base = {name: tstr, age: uint}");

        assertValid(schema, CBOR.map()
                .put("name", CBOR.text("Alice"))
                .put("age", CBOR.integer(42))
                .put("enabled", CBOR.trueValue())
                .value());

        assertInvalid(schema, CBOR.map()
                .put("name", CBOR.text("Alice"))
                .put("age", CBOR.text("42"))
                .put("enabled", CBOR.trueValue())
                .value());
    }

    @Test
    void unwrapMapGroupThreadsEntriesIntoContainingMap() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = {~base, enabled: bool}\n"
                        + "base = {name: tstr, age: uint}");

        assertValid(schema, CBOR.map()
                .put("name", CBOR.text("Alice"))
                .put("age", CBOR.integer(42))
                .put("enabled", CBOR.falseValue())
                .value());

        assertInvalid(schema, CBOR.map()
                .put("base", CBOR.map()
                        .put("name", CBOR.text("Alice"))
                        .put("age", CBOR.integer(42))
                        .value())
                .put("enabled", CBOR.trueValue())
                .value());
    }

    @Test
    void unwrapNestedArrayGroup() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = [~extended]\n"
                        + "extended = [~base, third: bool]\n"
                        + "base = [first: uint, second: tstr]");

        assertValid(schema, CBOR.array()
                .add(CBOR.integer(1))
                .add(CBOR.text("text"))
                .add(CBOR.trueValue())
                .value());

        assertInvalid(schema, CBOR.array()
                .add(CBOR.array()
                        .add(CBOR.integer(1))
                        .add(CBOR.text("text"))
                        .value())
                .add(CBOR.trueValue())
                .value());
    }

    @Test
    void unwrapNestedMaps() {
        CddlSchema schema = new CddlParser().parse(
                "outer = {~middle, outer-value: uint}\n"
                        + "middle = {~inner, middle-value: uint}\n"
                        + "inner = {inner-value: uint}");

        assertValid(schema, CBOR.map()
                .put("inner-value", CBOR.integer(1))
                .put("middle-value", CBOR.integer(2))
                .put("outer-value", CBOR.integer(3))
                .value());

        assertInvalid(schema, CBOR.map()
                .put("outer-value", CBOR.map()
                        .put("middle-value", CBOR.integer(2))
                        .put("inner-value", CBOR.integer(3))
                        .value())
                .value());
    }

    @Test
    void unwrapUnknownRuleFails() {
        CddlSchema schema = CddlValidator.loadSchema("root = ~unknown");

        assertInvalid(schema, CBOR.integer(42));
    }

    @Test
    void unwrapPrimitiveTypeFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = ~value\n"
                        + "value = uint");

        assertInvalid(schema, CBOR.integer(42));
    }

    @Test
    void unwrapChoiceTypeFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = ~value\n"
                        + "value = uint / tstr");

        assertInvalid(schema, CBOR.integer(42));
        assertInvalid(schema, CBOR.text("hello"));
    }

    @Test
    void unwrapPreservesUnderlyingTaggedTypeConstraints() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = ~tagged\n"
                        + "tagged = #6.1(tstr)");

        assertValid(schema, CBOR.text("hello"));
        assertInvalid(schema, CBOR.integer(42));
        assertInvalid(schema, CBOR.bytes(new byte[] {1, 2, 3}));
    }

    @Test
    void unwrapErrorIsReported() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = ~value\n"
                        + "value = uint");

        assertInvalid(schema, CBOR.integer(42));
    }

    @Test
    void tildePreservesConstraintsOfReferencedType() {
        CddlSchema schema = CddlValidator.loadSchema(
                "base = 1..10\n"
                        + "value = ~base");

        assertValid(schema, CBOR.integer(1));
        assertValid(schema, CBOR.integer(10));

        assertInvalid(schema, CBOR.integer(0));
        assertInvalid(schema, CBOR.integer(11));
    }

    @Test
    void rfc8610NoUnwrapTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "advanced-header = [\n"
                        + "  basic-header-group,\n"
                        + "  field3: bytes,\n"
                        + "  field4: number, ; as in the tagged type \"time\"\n"
                        + "]\n"
                        + "basic-header-group = (\n"
                        + "  field1: int,\n"
                        + "  field2: text,\n"
                        + ")\n"
                        + "basic-header = [ basic-header-group ]");

        CBORValue value = CBOR.array()
                .add(CBOR.integer(1))
                .add(CBOR.text("txt"))
                .add(CBOR.bytes(new byte[] { 0x00, 0x01 }))
                .add(CBOR.floatingPoint(0))
                .value();

        assertValid(schema, value);

        value = CBOR.array()
                .add(CBOR.bytes(new byte[] { 0x00, 0x01 }))
                .add(CBOR.floatingPoint(0))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void rfc8610UnwrapTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "advanced-header = [\n"
                        + "  ~basic-header,\n"
                        + "  field3: bytes,\n"
                        + "  field4: number, ; as in the tagged type \"time\"\n"
                        + "]\n"
                        + "basic-header = [\n"
                        + "  field1: int,\n"
                        + "  field2: text,\n"
                        + "]");

        CBORValue value = CBOR.array()
                .add(CBOR.integer(1))
                .add(CBOR.text("txt"))
                .add(CBOR.bytes(new byte[] { 0x00, 0x01 }))
                .add(CBOR.floatingPoint(0))
                .value();

        assertValid(schema, value);

        value = CBOR.array()
                .add(CBOR.bytes(new byte[] { 0x00, 0x01 }))
                .add(CBOR.floatingPoint(0))
                .value();

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