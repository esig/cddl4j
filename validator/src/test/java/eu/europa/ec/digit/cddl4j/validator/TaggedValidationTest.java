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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for CBOR tagged-value validation: {@code #6.<tag>(<type>)}.
 */
class TaggedValidationTest {

    @Test
    void noTagFail() {
        CddlSchema schema = CddlValidator.loadSchema("value = uint");

        CBORValue tagged = CBOR.tag(0, CBOR.integer(42));
        assertInvalid(schema, tagged);
    }

    @Test
    void taggedUintAcceptsTaggedInteger() {
        // Tag 1 = epoch-based date/time (uint or float).
        CddlSchema schema = CddlValidator.loadSchema("timestamp = #6.1(uint)");

        CBORValue tagged = CBOR.tag(1, CBOR.value(1_700_000_000L));
        assertValid(schema, tagged);
    }

    @Test
    void taggedUintRejectsWrongTag() {
        CddlSchema schema = CddlValidator.loadSchema("timestamp = #6.1(uint)");
        CBORValue tagged = CBOR.tag(2, CBOR.value(42));
        assertInvalid(schema, tagged);
    }

    @Test
    void taggedSchemaRejectsUntagged() {
        CddlSchema schema = CddlValidator.loadSchema("ts = #6.1(uint)");
        assertInvalid(schema, CBOR.value(42));
    }

    @Test
    void taggedAnyContent() {
        // #6 without a tag number = any CBOR tag.
        CddlSchema schema = CddlValidator.loadSchema("tagged-any = #6(any)");
        CBORValue tagged = CBOR.tag(99, CBOR.value("hello"));
        assertValid(schema, tagged);
    }

    @Test
    void taggedContentTypeValidated() {
        CddlSchema schema = CddlValidator.loadSchema("uri = #6.32(tstr)");
        CBORValue tagged = CBOR.tag(32, CBOR.value("https://example.com"));
        assertValid(schema, tagged);
    }

    @Test
    void taggedWrongContentTypeFails() {
        CddlSchema schema = CddlValidator.loadSchema("uri = #6.32(tstr)");
        CBORValue tagged = CBOR.tag(32, CBOR.value(42));
        assertInvalid(schema, tagged);
    }

    @Test
    void bareHashMatchesAnyCborValue() {
        CddlSchema schema = CddlValidator.loadSchema("root = #");
        assertValid(schema, CBOR.map()
                                .put("nested", CBOR.tag(7, CBOR.integer(1)))
                                .value());
    }

    @Test
    void majorTypeSelectorsMatchTheirCborMajorType() {
        assertValid(CddlValidator.loadSchema("root = #0"), CBOR.integer(42));
        assertInvalid(CddlValidator.loadSchema("root = #0"), CBOR.integer(-1));
        assertValid(CddlValidator.loadSchema("root = #6"), CBOR.tag(99, CBOR.text("tagged")));
        assertInvalid(CddlValidator.loadSchema("root = #6"), CBOR.text("untagged"));
    }

    @Test
    void parserRejectsNegativeAndOutOfRangeTagNumbers() {
        assertThrows(CddlParseException.class, () -> CddlValidator.loadSchema("root = #6.-1"));
        assertThrows(CddlParseException.class, () -> CddlValidator.loadSchema("root = #6.18446744073709551616"));
    }

    @Test
    void largeTagNumberIsPreserved() {
        CddlSchema schema = CddlValidator.loadSchema("value = #6.1000000(uint)");

        CBORValue value = CBOR.tag(1_000_000L, CBOR.integer(42));
        assertValid(schema, value);
    }

    @Test
    void taggedOptional() {
        CddlSchema schema = CddlValidator.loadSchema("uri = #6.32(tstr) / tstr");
        assertValid(schema, CBOR.tag(32, CBOR.value("https://example.com")));
        assertValid(schema, CBOR.value("https://example.com"));
        assertInvalid(schema, CBOR.tag(34, CBOR.value("https://example.com")));
    }

    @Test
    void rfc8610MyBreakfastTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "my_breakfast = #6.55799(breakfast)   ; cbor-any is too general!\n"
                        + "breakfast = cereal / porridge\n"
                        + "cereal = #6.998(tstr)\n"
                        + "porridge = #6.999([liquid, solid])\n"
                        + "liquid = milk / water\n"
                        + "milk = 0\n"
                        + "water = 1\n"
                        + "solid = tstr");

        CBORValue value = CBOR.tag(55799, CBOR.tag(998, CBOR.text("cereal")));
        assertValid(schema,  value);

        value = CBOR.tag(55799, CBOR.tag(999, CBOR.array().add(CBOR.integer(0)).add("strawberry").value()));
        assertValid(schema,  value);

        value = CBOR.tag(55799, CBOR.tag(999, CBOR.array().add(CBOR.integer(1)).add("strawberry").value()));
        assertValid(schema,  value);

        value = CBOR.tag(55798, CBOR.tag(998, CBOR.text("cereal")));
        assertInvalid(schema,  value);

        value = CBOR.tag(55799, CBOR.tag(999, CBOR.text("cereal")));
        assertInvalid(schema,  value);

        value = CBOR.tag(55799, CBOR.tag(998, CBOR.array().add(CBOR.integer(0)).add("strawberry").value()));
        assertInvalid(schema,  value);

        value = CBOR.tag(55799, CBOR.tag(999, CBOR.array().add(CBOR.integer(0)).value()));
        assertInvalid(schema,  value);
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
