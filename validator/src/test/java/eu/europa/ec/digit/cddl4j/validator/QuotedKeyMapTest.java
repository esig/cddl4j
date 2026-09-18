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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for quoted-string map keys: {@code "name": type}.
 *
 * <p>RFC 8610 allows map member keys to be expressed as quoted text literals
 * ({@code "name": type}) or as bare identifiers ({@code name: type}).
 * Both forms are semantically identical and produce a text-string key lookup.
 */
class QuotedKeyMapTest {

    @Test
    void quotedKeysAcceptValidMap() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { \"name\": tstr, \"age\": uint }");

        CBORValue map = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Alice"))
                .put(CBOR.value("age"), CBOR.value(30))
                .value();

        assertValid(schema, map);
    }

    @Test
    void quotedKeysRejectWrongValueType() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { \"name\": tstr, \"age\": uint }");

        CBORValue map = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Bob"))
                .put(CBOR.value("age"), CBOR.value("thirty"))
                .value();

        assertInvalid(schema, map);
    }

    @Test
    void quotedKeysDetectMissingRequiredKey() {
        CddlSchema schema = CddlValidator.loadSchema(
                "msg = { \"id\": uint, \"body\": tstr }");

        CBORValue onlyId = CBOR.map()
                .put(CBOR.value("id"), CBOR.value(1))
                .value();

        assertInvalid(schema, onlyId);
    }

    @Test
    void quotedKeyWithHyphenAccepted() {
        CddlSchema schema = CddlValidator.loadSchema(
                "headers = { \"Content-Type\": tstr, \"Accept\": tstr }");

        CBORValue map = CBOR.map()
                .put(CBOR.value("Content-Type"), CBOR.value("application/json"))
                .put(CBOR.value("Accept"), CBOR.value("*/*"))
                .value();

        assertValid(schema, map);
    }

    @Test
    void quotedKeyWithHyphenRejectsMissingKey() {
        CddlSchema schema = CddlValidator.loadSchema(
                "headers = { \"Content-Type\": tstr }");

        CBORValue empty = CBOR.map().value();
        assertInvalid(schema, empty);
    }

    @Test
    void quotedAndBarewordKeysAreEquivalent() {
        CddlSchema quotedSchema = CddlValidator.loadSchema("r = { \"status\": uint }");
        CddlSchema barewordSchema = CddlValidator.loadSchema("r = { status: uint }");

        CBORValue map = CBOR.map()
                .put(CBOR.value("status"), CBOR.value(200))
                .value();

        assertValid(quotedSchema, map);
        assertValid(barewordSchema, map);
    }

    @Test
    void optionalQuotedKeyMayBeAbsent() {
        CddlSchema schema = CddlValidator.loadSchema(
                "response = { \"status\": uint, ? \"message\": tstr }");

        CBORValue noMsg = CBOR.map()
                .put(CBOR.value("status"), CBOR.value(200))
                .value();

        assertValid(schema, noMsg);
    }

    @Test
    void optionalQuotedKeyPresentMustHaveCorrectType() {
        CddlSchema schema = CddlValidator.loadSchema(
                "response = { \"status\": uint, ? \"message\": tstr }");

        CBORValue withBadMsg = CBOR.map()
                .put(CBOR.value("status"), CBOR.value(200))
                .put(CBOR.value("message"), CBOR.value(42))
                .value();

        assertInvalid(schema, withBadMsg);
    }

    @Test
    void quotedKeyFatArrowSyntaxAccepted() {
        CddlSchema schema = CddlValidator.loadSchema(
                "lookup = { \"code\" => uint }");

        CBORValue map = CBOR.map()
                .put(CBOR.value("code"), CBOR.value(404))
                .value();

        assertValid(schema, map);
    }

    @Test
    void quotedKeyFatArrowRejectsWrongValue() {
        CddlSchema schema = CddlValidator.loadSchema(
                "lookup = { \"code\" => uint }");

        CBORValue map = CBOR.map()
                .put(CBOR.value("code"), CBOR.value("not-a-uint"))
                .value();

        assertInvalid(schema, map);
    }

    @Test
    void mixedQuotedAndBarewordKeysInSameMap() {
        // "name" is quoted; status is bare-word; both refer to text-string keys.
        CddlSchema schema = CddlValidator.loadSchema(
                "msg = { \"name\": tstr, status: uint }");

        CBORValue map = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("sensor-1"))
                .put(CBOR.value("status"), CBOR.value(1))
                .value();

        assertValid(schema, map);
    }

    @Test
    void quotedKeyMapWithWildcardAllowsExtras() {
        CddlSchema schema = CddlValidator.loadSchema(
                "msg = { \"id\": uint, * tstr => any }");

        CBORValue map = CBOR.map()
                .put(CBOR.value("id"), CBOR.value(1))
                .put(CBOR.value("extra-1"), CBOR.value("foo"))
                .put(CBOR.value("extra-2"), CBOR.value(true))
                .value();

        assertValid(schema, map);
    }

    @Test
    void quotedKeysInNestedMap() {
        CddlSchema schema = CddlValidator.loadSchema(
                "order = { \"id\": uint, \"address\": address }\n"
                        + "address = { \"street\": tstr, \"zip\": tstr }");

        CBORValue addr = CBOR.map()
                .put(CBOR.value("street"), CBOR.value("123 Main St"))
                .put(CBOR.value("zip"), CBOR.value("12345"))
                .value();

        CBORValue order = CBOR.map()
                .put(CBOR.value("id"), CBOR.value(42))
                .put(CBOR.value("address"), addr)
                .value();

        assertValid(schema, order);
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
