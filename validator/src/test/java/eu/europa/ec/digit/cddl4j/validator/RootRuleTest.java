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
package eu.europa.ec.digit.cddl4j.validator;

import eu.europa.ec.digit.cddl4j.cbor.CBOR;
import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.validator.schema.CddlSchema;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for configurable root rule:
 * {@link CddlSchema#withRootRule(String)} and
 * {@link CddlValidator#loadSchema(String, String)}.
 */
class RootRuleTest {

    private static final String MULTI_RULE_CDDL =
            "message = { \"id\": uint, \"body\": tstr }\n"
                    + "id-only = uint\n"
                    + "flag = bool";

    @Test
    void defaultRootIsFirstUserRule() {
        CddlSchema schema = CddlValidator.loadSchema(MULTI_RULE_CDDL);
        assertEquals("message", schema.getRootRuleName());
    }

    @Test
    void defaultRootValidatesAgainstFirstRule() {
        CddlSchema schema = CddlValidator.loadSchema(MULTI_RULE_CDDL);

        CBORValue msg = CBOR.map()
                .put(CBOR.value("id"), CBOR.value(1))
                .put(CBOR.value("body"), CBOR.value("hello"))
                .value();

        assertValid(schema, msg);
    }

    @Test
    void plainUintFailsFirstRuleWhichExpectsMap() {
        CddlSchema schema = CddlValidator.loadSchema(MULTI_RULE_CDDL);
        assertInvalid(schema, CBOR.value(99));
    }

    @Test
    void withRootRuleChangesEntryPoint() {
        CddlSchema schema = CddlValidator.loadSchema(MULTI_RULE_CDDL)
                .withRootRule("id-only");

        assertEquals("id-only", schema.getRootRuleName());
        assertValid(schema, CBOR.value(42));
    }

    @Test
    void withRootRuleRejectsWrongType() {
        CddlSchema schema = CddlValidator.loadSchema(MULTI_RULE_CDDL)
                .withRootRule("id-only");

        CBORValue map = CBOR.map()
                .put(CBOR.value("id"), CBOR.value(1))
                .value();
        assertInvalid(schema, map);
    }

    @Test
    void withRootRuleOriginalSchemaIsUnchanged() {
        CddlSchema original = CddlValidator.loadSchema(MULTI_RULE_CDDL);
        CddlSchema alternate = original.withRootRule("id-only");

        assertEquals("message", original.getRootRuleName());
        assertEquals("id-only", alternate.getRootRuleName());
    }

    @Test
    void withRootRuleCanBeChained() {
        CddlSchema schema = CddlValidator.loadSchema(MULTI_RULE_CDDL)
                .withRootRule("id-only")
                .withRootRule("flag");

        assertEquals("flag", schema.getRootRuleName());
        assertValid(schema, CBOR.value(true));
    }

    @Test
    void withRootRuleNullThrows() {
        CddlSchema schema = CddlValidator.loadSchema(MULTI_RULE_CDDL);
        assertThrows(IllegalArgumentException.class, () -> schema.withRootRule(null));
    }

    @Test
    void withRootRuleBlankThrows() {
        CddlSchema schema = CddlValidator.loadSchema(MULTI_RULE_CDDL);
        assertThrows(IllegalArgumentException.class, () -> schema.withRootRule("  "));
    }

    @Test
    void loadSchemaWithRootRuleName() {
        CddlSchema schema = CddlValidator.loadSchema(MULTI_RULE_CDDL, "id-only");
        assertEquals("id-only", schema.getRootRuleName());
        assertValid(schema, CBOR.value(7));
    }

    @Test
    void loadSchemaRootRuleUnknownReportsErrorAtValidationTime() {
        CddlSchema schema = CddlValidator.loadSchema(MULTI_RULE_CDDL, "nonexistent");
        ValidationResult result = schema.validate(CBOR.value(1).encodeToBytes());

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getMessage().contains("nonexistent"));
    }

    @Test
    void preludeRuleAsRoot() {
        // "uint" is a prelude rule; schema has no user rules of the same name.
        CddlSchema schema = CddlValidator.loadSchema("my-type = tstr", "uint");
        assertValid(schema, CBOR.value(42));
        assertInvalid(schema, CBOR.value("hello"));
    }

    @Test
    void validateWithExplicitRuleNameIgnoresRoot() {
        CddlSchema schema = CddlValidator.loadSchema(MULTI_RULE_CDDL);
        ValidationResult result = schema.validate(
                CBOR.value(false).encodeToBytes(), "flag");

        assertTrue(result.isValid());
    }

    @Test
    void duplicatePlainRuleDefinitionIsRejected() {
        assertThrows(CddlParseException.class, () -> CddlValidator.loadSchema("root = uint\nroot = tstr"));
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
