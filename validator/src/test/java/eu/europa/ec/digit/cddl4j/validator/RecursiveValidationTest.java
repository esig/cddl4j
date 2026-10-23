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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for recursive (self-referential) CDDL rule resolution and forward references.
 */
class RecursiveValidationTest {

    @Test
    void forwardReferenceResolved() {
        // person references address which is defined after.
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { name: tstr, home: address }\n"
                        + "address = { city: tstr }");

        CBORValue addr = CBOR.map()
                .put(CBOR.value("city"), CBOR.value("Springfield"))
                .value();

        CBORValue person = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Homer"))
                .put(CBOR.value("home"), addr)
                .value();

        assertValid(schema, person);
    }

    @Test
    void selfReferentialSchemaDoesNotStackOverflow() {
        // A linked-list node: value: uint, next: node / null.
        CddlSchema schema = CddlValidator.loadSchema(
                "node = { value: uint, ? next: node }");

        // Node with no "next".
        CBORValue leaf = CBOR.map()
                .put(CBOR.value("value"), CBOR.value(3))
                .value();

        CBORValue mid = CBOR.map()
                .put(CBOR.value("value"), CBOR.value(2))
                .put(CBOR.value("next"), leaf)
                .value();

        CBORValue head = CBOR.map()
                .put(CBOR.value("value"), CBOR.value(1))
                .put(CBOR.value("next"), mid)
                .value();

        assertValid(schema, head);
    }

    @Test
    void deepRuleChainResolvesCorrectly() {
        CddlSchema schema = CddlValidator.loadSchema(
                "a = b\n"
                        + "b = c\n"
                        + "c = d\n"
                        + "d = uint");

        assertValid(schema, CBOR.value(42));
        assertInvalid(schema, CBOR.value("hello"));
    }

    @Test
    void unknownRuleReferenceProducesError() {
        CddlSchema schema = CddlValidator.loadSchema("root = nonexistent-rule");
        ValidationResult result = schema.validate(CBOR.value(42));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getMessage().contains("nonexistent-rule"));
    }

    @Test
    void zeroProgressRecursionDoesNotAcceptArbitraryValues() {
        CddlSchema schema = CddlValidator.loadSchema("root = root");

        assertInvalid(schema, CBOR.value(42));
    }

    @Test
    void zeroProgressMutualRecursionFails() {
        CddlSchema schema = CddlValidator.loadSchema("first = second\nsecond = first");

        assertInvalid(schema, CBOR.value(42));
    }

    @Test
    void zeroProgressRecursiveChoiceCanMatchLaterAlternative() {
        CddlSchema schema = CddlValidator.loadSchema("root = root / uint");

        assertValid(schema, CBOR.value(42));
        assertInvalid(schema, CBOR.value("not an integer"));
    }

    @Test
    void recursiveTreeAcceptsLeaf() {
        CddlSchema schema = CddlValidator.loadSchema(
                "tree = uint / { value: uint, children: [* tree] }");

        assertValid(schema, CBOR.value(42));
    }

    @Test
    void recursiveTreeAcceptsNestedNodes() {
        CddlSchema schema = CddlValidator.loadSchema(
                "tree = uint / { value: uint, children: [* tree] }");

        CBORValue tree = CBOR.map()
                .put(CBOR.value("value"), CBOR.integer(1))
                .put(
                        CBOR.value("children"),
                        CBOR.array()
                                .add(CBOR.integer(2))
                                .add(CBOR.map()
                                        .put(CBOR.value("value"), CBOR.integer(3))
                                        .put(
                                                CBOR.value("children"),
                                                CBOR.array()
                                                        .add(CBOR.integer(4))
                                                        .value())
                                        .value())
                                .value())
                .value();

        assertValid(schema, tree);
    }

    @Test
    void recursiveTreeReportsDeepInvalidValue() {
        CddlSchema schema = CddlValidator.loadSchema(
                "tree = uint / { value: uint, children: [* tree] }");

        CBORValue tree = CBOR.map()
                .put(CBOR.value("value"), CBOR.integer(1))
                .put(
                        CBOR.value("children"),
                        CBOR.array()
                                .add(CBOR.integer(2))
                                .add(CBOR.map()
                                        .put(CBOR.value("value"), CBOR.text("bad"))
                                        .put(
                                                CBOR.value("children"),
                                                CBOR.array().value())
                                        .value())
                                .value())
                .value();

        ValidationResult result = schema.validate(tree);

        assertFalse(result.isValid());
        assertEquals("/children[1]/value", result.getErrors().get(0).getInstancePath());
        assertTrue(result.getErrors().get(0).getMessage().contains("Expected uint"));
    }

    @Test
    void recursiveStructureHandlesSeveralLevels() {
        CddlSchema schema = CddlValidator.loadSchema(
                "tree = uint / { value: uint, children: [* tree] }");

        CBORValue level4 = CBOR.map()
                .put(CBOR.value("value"), CBOR.integer(4))
                .put(CBOR.value("children"), CBOR.array().value())
                .value();

        CBORValue level3 = CBOR.map()
                .put(CBOR.value("value"), CBOR.integer(3))
                .put(CBOR.value("children"),
                        CBOR.array().add(level4).value())
                .value();

        CBORValue level2 = CBOR.map()
                .put(CBOR.value("value"), CBOR.integer(2))
                .put(CBOR.value("children"),
                        CBOR.array().add(level3).value())
                .value();

        CBORValue root = CBOR.map()
                .put(CBOR.value("value"), CBOR.integer(1))
                .put(CBOR.value("children"),
                        CBOR.array().add(level2).value())
                .value();

        assertValid(schema, root);
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
