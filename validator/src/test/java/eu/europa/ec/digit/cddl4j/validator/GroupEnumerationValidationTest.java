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

class GroupEnumerationValidationTest {

    @Test
    void inlineGroupEnumerationMatchesMemberValues() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = &(red: 1, green: 2, blue: 3)");

        assertValid(schema, CBOR.integer(1));
        assertValid(schema, CBOR.integer(2));
        assertValid(schema, CBOR.integer(3));
        assertInvalid(schema, CBOR.integer(4));
    }

    @Test
    void inlineGroupEnumerationIgnoresMemberNames() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = &(first: 1, second: 2)");

        assertValid(schema, CBOR.integer(1));
        assertValid(schema, CBOR.integer(2));
        assertInvalid(schema, CBOR.integer(0));
        assertInvalid(schema, CBOR.text("first"));
        assertInvalid(schema, CBOR.text("second"));
    }

    @Test
    void inlineGroupEnumerationSupportsDifferentTypes() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = &(number: 42, text: \"hello\", enabled: true)");

        assertValid(schema, CBOR.integer(42));
        assertValid(schema, CBOR.text("hello"));
        assertValid(schema, CBOR.trueValue());

        assertInvalid(schema, CBOR.integer(43));
        assertInvalid(schema, CBOR.text("world"));
        assertInvalid(schema, CBOR.falseValue());
    }

    @Test
    void namedGroupEnumerationMatchesMemberValues() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = &colors\n"
                        + "colors = (red: 1, green: 2, blue: 3)");

        assertValid(schema, CBOR.integer(1));
        assertValid(schema, CBOR.integer(2));
        assertValid(schema, CBOR.integer(3));
        assertInvalid(schema, CBOR.integer(4));
    }

    @Test
    void namedGroupEnumerationSupportsForwardReference() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = &colors\n"
                        + "colors = (red: 1, green: 2)");

        assertValid(schema, CBOR.integer(1));
        assertValid(schema, CBOR.integer(2));
        assertInvalid(schema, CBOR.integer(3));
    }

    @Test
    void groupEnumerationSupportsReferencedGroups() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = &(base, orange: 8, pink: 9)\n"
                        + "base = (red: 1, green: 2, blue: 3)");

        assertValid(schema, CBOR.integer(1));
        assertValid(schema, CBOR.integer(2));
        assertValid(schema, CBOR.integer(3));
        assertValid(schema, CBOR.integer(8));
        assertValid(schema, CBOR.integer(9));
        assertInvalid(schema, CBOR.integer(10));
    }

    @Test
    void groupEnumerationSupportsNestedReferencedGroups() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = &colors\n"
                        + "colors = (primary, black: 0)\n"
                        + "primary = (red: 1, green: 2, blue: 3)");

        assertValid(schema, CBOR.integer(0));
        assertValid(schema, CBOR.integer(1));
        assertValid(schema, CBOR.integer(2));
        assertValid(schema, CBOR.integer(3));
        assertInvalid(schema, CBOR.integer(4));
    }

    @Test
    void groupEnumerationSupportsOrdinaryTypeReferences() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = &(positive, text: tstr)\n"
                        + "positive = 1..10");

        assertValid(schema, CBOR.integer(1));
        assertValid(schema, CBOR.integer(5));
        assertValid(schema, CBOR.integer(10));
        assertValid(schema, CBOR.text("hello"));

        assertInvalid(schema, CBOR.integer(0));
        assertInvalid(schema, CBOR.integer(11));
        assertInvalid(schema, CBOR.trueValue());
    }

    @Test
    void groupEnumerationDoesNotApplyOccurrenceConstraintsToValues() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = &(+ one: 1, ? two: 2)");

        assertValid(schema, CBOR.integer(1));
        assertValid(schema, CBOR.integer(2));
        assertInvalid(schema, CBOR.integer(3));
    }

    @Test
    void groupEnumerationUnknownGroupFails() {
        CddlSchema schema = CddlValidator.loadSchema("root = &unknown");

        assertInvalid(schema, CBOR.integer(1));
    }

    @Test
    void emptyGroupEnumerationFails() {
        CddlSchema schema = CddlValidator.loadSchema("root = &()");

        assertInvalid(schema, CBOR.integer(1));
    }

    @Test
    void groupEnumerationNoValueMatches() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = &(red: 1, green: 2)");

        ValidationResult result = schema.validate(CBOR.integer(3));

        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void groupEnumerationErrorContainsAlternativeFailures() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = &(red: 1, green: 2)");

        ValidationResult result = schema.validate(CBOR.integer(3));

        assertFalse(result.isValid());
        assertFalse(result.getErrors().get(0).getCauses().isEmpty());
    }

    @Test
    void rfc8610Test() {
        CddlSchema schema = CddlValidator.loadSchema(
                "terminal-color = &basecolors\n"
                        + "basecolors = (\n"
                        + "  black: 0,  red: 1,  green: 2,  yellow: 3,\n"
                        + "  blue: 4,  magenta: 5,  cyan: 6,  white: 7,\n"
                        + ")\n"
                        + "extended-color = &(\n"
                        + "  basecolors,\n"
                        + "  orange: 8,  pink: 9,  purple: 10,  brown: 11,\n"
                        + ")");

        assertValid(schema, CBOR.integer(0));
        assertValid(schema, CBOR.integer(1));
        assertValid(schema, CBOR.integer(2));
        assertValid(schema, CBOR.integer(3));
        assertValid(schema, CBOR.integer(4));
        assertValid(schema, CBOR.integer(5));
        assertValid(schema, CBOR.integer(6));
        assertValid(schema, CBOR.integer(7));

        assertInvalid(schema, CBOR.integer(8));
        assertInvalid(schema, CBOR.integer(9));
        assertInvalid(schema, CBOR.integer(10));
        assertInvalid(schema, CBOR.integer(11));

        assertInvalid(schema, CBOR.integer(12));

        assertInvalid(schema, CBOR.text("black"));
        assertInvalid(schema, CBOR.text("blue"));
        assertInvalid(schema, CBOR.text("orange"));
        assertInvalid(schema, CBOR.text("brown"));
    }

    @Test
    void rfc8610ExtendedColorsFirstTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "extended-color = &(\n"
                        + "  basecolors,\n"
                        + "  orange: 8,  pink: 9,  purple: 10,  brown: 11,\n"
                        + ")\n"
                        + "basecolors = (\n"
                        + "  black: 0,  red: 1,  green: 2,  yellow: 3,\n"
                        + "  blue: 4,  magenta: 5,  cyan: 6,  white: 7,\n"
                        + ")");

        assertValid(schema, CBOR.integer(0));
        assertValid(schema, CBOR.integer(1));
        assertValid(schema, CBOR.integer(2));
        assertValid(schema, CBOR.integer(3));
        assertValid(schema, CBOR.integer(4));
        assertValid(schema, CBOR.integer(5));
        assertValid(schema, CBOR.integer(6));
        assertValid(schema, CBOR.integer(7));

        assertValid(schema, CBOR.integer(8));
        assertValid(schema, CBOR.integer(9));
        assertValid(schema, CBOR.integer(10));
        assertValid(schema, CBOR.integer(11));

        assertInvalid(schema, CBOR.integer(12));

        assertInvalid(schema, CBOR.text("black"));
        assertInvalid(schema, CBOR.text("blue"));
        assertInvalid(schema, CBOR.text("orange"));
        assertInvalid(schema, CBOR.text("brown"));
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
