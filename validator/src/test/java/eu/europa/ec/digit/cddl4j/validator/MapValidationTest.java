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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for map type validation: required/optional keys, typed keys, catch-all entries.
 */
class MapValidationTest {

    @Test
    void simpleMapWithRequiredKeys() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { name: tstr, age: uint }");

        CBORValue valid = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Alice"))
                .put(CBOR.value("age"), CBOR.value(30))
                .value();

        assertValid(schema, valid);
    }

    @Test
    void mapMissingRequiredKeyFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { name: tstr, age: uint }");

        CBORValue onlyName = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Bob"))
                .value();

        assertInvalid(schema, onlyName);
    }

    @Test
    void mapMissingFirstRequiredFieldIsInvalid() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { name: tstr, age: uint }");

        CBORValue onlyName = CBOR.map()
                .put(CBOR.value("age"), CBOR.integer(30))
                .value();

        assertInvalid(schema, onlyName);
    }

    @Test
    void emptyMapMissingAllRequiredFieldsIsInvalid() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { name: tstr, age: uint }");

        CBORValue onlyName = CBOR.map().value();
        assertInvalid(schema, onlyName);
    }

    @Test
    void mapWrongValueTypeFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { name: tstr, age: uint }");

        CBORValue bad = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Carol"))
                .put(CBOR.value("age"), CBOR.value("thirty"))
                .value();

        assertInvalid(schema, bad);
    }

    @Test
    void mapExtraKeyWithoutWildcardFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "device = { id: uint }");

        CBORValue extra = CBOR.map()
                .put(CBOR.value("id"), CBOR.value(1))
                .put(CBOR.value("name"), CBOR.value("extra"))
                .value();

        assertInvalid(schema, extra);
    }

    @Test
    void multipleUnexpectedMapMembersFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "device = { id: uint }");

        CBORValue extra = CBOR.map()
                .put(CBOR.value("id"), CBOR.value(1))
                .put(CBOR.value("name"), CBOR.value("extra"))
                .put(CBOR.value("active"), CBOR.bool(true))
                .put(CBOR.value("extra"), CBOR.value("x"))
                .value();

        assertInvalid(schema, extra);
    }

    @Test
    void mapExtraKeyWithWildcardIsOk() {
        CddlSchema schema = CddlValidator.loadSchema(
                "device = { id: uint, * tstr => any }");

        CBORValue obj = CBOR.map()
                .put(CBOR.value("id"), CBOR.value(1))
                .put(CBOR.value("name"), CBOR.value("sensor"))
                .put(CBOR.value("loc"), CBOR.value("room-1"))
                .value();

        assertValid(schema, obj);
    }

    @Test
    void mapExtraKeyWithWildcardIsKo() {
        CddlSchema schema = CddlValidator.loadSchema(
                "device = { id: uint, * tstr => any }");

        CBORValue obj = CBOR.map()
                .put(CBOR.value("id"), CBOR.value(1))
                .put(CBOR.value("name"), CBOR.value("sensor"))
                .put(CBOR.value(12), CBOR.value("room-1"))
                .value();

        assertInvalid(schema, obj);
    }

    @Test
    void optionalKeyMayBePresent() {
        CddlSchema schema = CddlValidator.loadSchema(
                "response = { status: uint, ? message: tstr }");

        CBORValue noMsg = CBOR.map()
                .put(CBOR.value("status"), CBOR.value(200))
                .put(CBOR.value("message"), CBOR.value("42"))
                .value();

        assertValid(schema, noMsg);
    }

    @Test
    void optionalKeyMayBeAbsent() {
        CddlSchema schema = CddlValidator.loadSchema(
                "response = { status: uint, ? message: tstr }");

        CBORValue noMsg = CBOR.map()
                .put(CBOR.value("status"), CBOR.value(200))
                .value();

        assertValid(schema, noMsg);
    }

    @Test
    void optionalKeyPresentMustHaveCorrectType() {
        CddlSchema schema = CddlValidator.loadSchema(
                "response = { status: uint, ? message: tstr }");

        CBORValue withBadMsg = CBOR.map()
                .put(CBOR.value("status"), CBOR.value(200))
                .put(CBOR.value("message"), CBOR.value(42))
                .value();

        assertInvalid(schema, withBadMsg);
    }

    @Test
    void emptyMapMatchesEmptySchema() {
        CddlSchema schema = CddlValidator.loadSchema("empty = {}");
        assertValid(schema, CBOR.map().value());
    }

    @Test
    void nonMapCborFailsMapSchema() {
        CddlSchema schema = CddlValidator.loadSchema("m = { id: uint }");
        assertInvalid(schema, CBOR.value(42));
    }

    @Test
    void otherKeyDefinition() {
        CddlSchema schema = CddlValidator.loadSchema(
                "lookup = { \"mandatory-key\" => int }");

        CBORValue obj = CBOR.map()
                .put(CBOR.text("mandatory-key"), CBOR.integer(0))
                .value();

        assertValid(schema, obj);
    }

    @Test
    void optionalKeyDefinitionEmptyMap() {
        CddlSchema schema = CddlValidator.loadSchema(
                "lookup = { ? \"optional-key\" => int }");

        CBORValue obj = CBOR.map().value();
        assertValid(schema, obj);
    }

    @Test
    void optionalKeyDefinitionWithWildcardMap() {
        CddlSchema schema = CddlValidator.loadSchema(
                "lookup = {\n" +
                            "? \"optional-key\" => int,\n" +
                            "* tstr => any\n"
                        + "}");


        CBORValue obj = CBOR.map()
                .put(CBOR.text("mandatory-key"), CBOR.integer(0))
                .value();
        assertValid(schema, obj);
    }

    @Test
    void optionalKeyDefinitionDiffTypeMap() {
        CddlSchema schema = CddlValidator.loadSchema(
                "lookup = {\n" +
                        "? \"optional-key\" => int,\n" +
                        "* tstr => any\n"
                        + "}");

        CBORValue obj = CBOR.map()
                .put(CBOR.text("optional-key"), CBOR.text("nonsense"))
                .value();
        assertValid(schema, obj);
    }

    @Test
    void optionalKeyDefinitionDiffTypeWithCutMap() {
        CddlSchema schema = CddlValidator.loadSchema(
                "lookup = {\n" +
                        "? \"optional-key\" ^ => int,\n" +
                        "* tstr => any\n"
                        + "}");

        CBORValue obj = CBOR.map()
                .put(CBOR.text("optional-key"), CBOR.text("nonsense"))
                .value();
        assertInvalid(schema, obj);
    }

    @Test
    void typedKeyMatchesAnyKeyOfThatType() {
        CddlSchema schema = CddlValidator.loadSchema(
                "lookup = { * uint => tstr }");

        CBORValue obj = CBOR.map()
                .put(CBOR.value(0), CBOR.value("zero"))
                .put(CBOR.value(1), CBOR.value("one"))
                .value();

        assertValid(schema, obj);
    }

    @Test
    void typedKeyRejectsWrongValueType() {
        CddlSchema schema = CddlValidator.loadSchema(
                "lookup = { * uint => tstr }");

        CBORValue bad = CBOR.map()
                .put(CBOR.value(0), CBOR.value(99))
                .value();

        assertInvalid(schema, bad);
    }

    @Test
    void byteStringMapKeyDoesNotMatchTextKey() {
        CddlSchema schema = CddlValidator.loadSchema(
                "object = { id: uint }");

        CBORValue obj = CBOR.map()
                .put(CBOR.bytes(new byte[] { 'i', 'd' }), CBOR.integer(42))
                .value();

        assertInvalid(schema, obj);
    }

    @Test
    void booleanMapKeyDoesNotMatchTextKey() {
        CddlSchema schema = CddlValidator.loadSchema(
                "object = { id: uint }");

        CBORValue obj = CBOR.map()
                .put(CBOR.bool(true), CBOR.integer(42))
                .value();

        assertInvalid(schema, obj);
    }

    @Test
    void sameNamedTypeCanBeUsedInMultipleMembers() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { id: id, manager: id }\n"
                        + "id = uint");

        CBORValue value = CBOR.map()
                .put(CBOR.value("id"), CBOR.integer(10))
                .put(CBOR.value("manager"), CBOR.integer(20))
                .value();

        assertValid(schema, value);
    }

    @Test
    void namedTypeFailureIsReportedAtReferenceLocation() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { id: id, manager: id }\n"
                        + "id = uint");

        CBORValue value = CBOR.map()
                .put(CBOR.value("id"), CBOR.integer(10))
                .put(CBOR.value("manager"), CBOR.text("bad"))
                .value();

        ValidationResult result = schema.validate(value);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getInstancePath().contains("manager"));
    }

    @Test
    void namedNestedTypeValidatesNormally() {
        CddlSchema schema = CddlValidator.loadSchema(
                "location = { point: coordinates }\n"
                        + "coordinates = [int, int]");

        CBORValue value = CBOR.map()
                .put(
                        CBOR.value("point"),
                        CBOR.array()
                                .add(CBOR.integer(10))
                                .add(CBOR.integer(-20))
                                .value())
                .value();

        assertValid(schema, value);
    }

    @Test
    void nestedMapValidation() {
        CddlSchema schema = CddlValidator.loadSchema(
                "order = { id: uint, shipping: address }\n"
                        + "address = { street: tstr, zip: tstr }");

        CBORValue addr = CBOR.map()
                .put(CBOR.value("street"), CBOR.value("123 Main St"))
                .put(CBOR.value("zip"), CBOR.value("12345"))
                .value();

        CBORValue order = CBOR.map()
                .put(CBOR.value("id"), CBOR.value(1))
                .put(CBOR.value("shipping"), addr)
                .value();

        assertValid(schema, order);
    }

    @Test
    void nestedMapInnerErrorReported() {
        CddlSchema schema = CddlValidator.loadSchema(
                "order = { id: uint, shipping: address }\n"
                        + "address = { street: tstr, zip: tstr }");

        CBORValue badAddr = CBOR.map()
                .put(CBOR.value("street"), CBOR.value(42))
                .put(CBOR.value("zip"), CBOR.value("12345"))
                .value();

        CBORValue order = CBOR.map()
                .put(CBOR.value("id"), CBOR.value(1))
                .put(CBOR.value("shipping"), badAddr)
                .value();

        ValidationResult result = schema.validate(order);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getInstancePath().contains("street"));
    }

    @Test
    void missingNestedRequiredFieldIsRejected() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { name: tstr, address: address }\n"
                        + "address = { street: tstr, zip: tstr }");

        CBORValue address = CBOR.map()
                .put(CBOR.value("street"), CBOR.value("Main Street"))
                .value();

        CBORValue person = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Alice"))
                .put(CBOR.value("address"), address)
                .value();

        ValidationResult result = schema.validate(person);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(
                error -> error.getInstancePath().contains("address")));
    }

    @Test
    void unexpectedNestedMapFieldIsRejected() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { address: address }\n"
                        + "address = { street: tstr }");

        CBORValue address = CBOR.map()
                .put(CBOR.value("street"), CBOR.value("Main Street"))
                .put(CBOR.value("country"), CBOR.value("LU"))
                .value();

        CBORValue person = CBOR.map()
                .put(CBOR.value("address"), address)
                .value();

        assertInvalid(schema, person);
    }

    @Test
    void nestedMapErrorReportsNestedFieldPath() {
        CddlSchema schema = CddlValidator.loadSchema(
                "order = { shipping: address }\n"
                        + "address = { street: tstr, zip: tstr }");

        CBORValue address = CBOR.map()
                .put(CBOR.value("street"), CBOR.integer(42))
                .put(CBOR.value("zip"), CBOR.value("12345"))
                .value();

        CBORValue order = CBOR.map()
                .put(CBOR.value("shipping"), address)
                .value();

        ValidationResult result = schema.validate(order);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(error ->
                        error.getInstancePath().contains("shipping") && error.getInstancePath().contains("street")));
    }

    @Test
    void multipleNestedFieldsProduceMultipleErrors() {
        CddlSchema schema = CddlValidator.loadSchema(
                "order = { shipping: address }\n"
                        + "address = { street: tstr, zip: tstr }");

        CBORValue address = CBOR.map()
                .put(CBOR.value("street"), CBOR.integer(42))
                .put(CBOR.value("zip"), CBOR.integer(12345))
                .value();

        CBORValue order = CBOR.map()
                .put(CBOR.value("shipping"), address)
                .value();

        ValidationResult result = schema.validate(order);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.getInstancePath().contains("street")));
        assertTrue(result.getErrors().stream().anyMatch(error -> error.getInstancePath().contains("zip")));
    }

    @Test
    void wrongIntegerTypeReportsFieldPath() {
        CddlSchema schema = CddlValidator.loadSchema(
                "message = { id: uint, name: tstr }");

        CBORValue message = CBOR.map()
                .put(CBOR.value("id"), CBOR.value("not-an-integer"))
                .put(CBOR.value("name"), CBOR.value("Alice"))
                .value();

        ValidationResult result = schema.validate(message);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.getInstancePath().contains("id")));
    }

    @Test
    void wrongTextTypeReportsFieldPath() {
        CddlSchema schema = CddlValidator.loadSchema(
                "message = { id: uint, name: tstr }");

        CBORValue message = CBOR.map()
                .put(CBOR.value("id"), CBOR.integer(1))
                .put(CBOR.value("name"), CBOR.integer(42))
                .value();

        ValidationResult result = schema.validate(message);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.getInstancePath().contains("name")));
    }

    @Test
    void multiplePrimitiveTypeErrorsAreReported() {
        CddlSchema schema = CddlValidator.loadSchema(
                "message = { id: uint, name: tstr, active: bool }");

        CBORValue message = CBOR.map()
                .put(CBOR.value("id"), CBOR.value("bad-id"))
                .put(CBOR.value("name"), CBOR.integer(42))
                .put(CBOR.value("active"), CBOR.value("yes"))
                .value();

        ValidationResult result = schema.validate(message);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() >= 3);

        assertTrue(result.getErrors().stream().anyMatch(error -> error.getInstancePath().contains("id")));
        assertTrue(result.getErrors().stream().anyMatch(error -> error.getInstancePath().contains("name")));
        assertTrue(result.getErrors().stream().anyMatch(error -> error.getInstancePath().contains("active")));
    }

    @Test
    void numericBoundariesInsideMapAreValid() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { min-age: 18..120, max-age: 18..120 }");

        CBORValue person = CBOR.map()
                .put(CBOR.value("min-age"), CBOR.integer(18))
                .put(CBOR.value("max-age"), CBOR.integer(120))
                .value();

        ValidationResult result = schema.validate(person);
        assertTrue(result.isValid());
    }

    @Test
    void numericLowerBoundViolationReportsFieldPath() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { age: 18..120 }");

        CBORValue person = CBOR.map()
                .put(CBOR.value("age"), CBOR.integer(17))
                .value();

        ValidationResult result = schema.validate(person);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.getInstancePath().contains("age")));
    }

    @Test
    void numericUpperBoundViolationReportsFieldPath() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { age: 18..120 }");

        CBORValue person = CBOR.map()
                .put(CBOR.value("age"), CBOR.integer(121))
                .value();

        ValidationResult result = schema.validate(person);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.getInstancePath().contains("age")));
    }

    @Test
    void deeplyNestedNumericRangeErrorReportsLeafPath() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = { payload: payload }\n"
                        + "payload = { metadata: metadata }\n"
                        + "metadata = { count: 1..10 }");

        CBORValue metadata = CBOR.map()
                .put(CBOR.value("count"), CBOR.integer(11))
                .value();

        CBORValue payload = CBOR.map()
                .put(CBOR.value("metadata"), metadata)
                .value();

        CBORValue root = CBOR.map()
                .put(CBOR.value("payload"), payload)
                .value();

        ValidationResult result = schema.validate(root);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> {
                    String path = error.getInstancePath();
                    return path.contains("payload")
                            && path.contains("metadata")
                            && path.contains("count");
                }));
    }

    @Test
    void largeIntegerRemainsValidWhenNested() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = { value: uint }");

        BigInteger largeValue = BigInteger.ONE.shiftLeft(63);

        CBORValue root = CBOR.map()
                .put(CBOR.value("value"), CBOR.integer(largeValue))
                .value();

        ValidationResult result = schema.validate(root);

        assertTrue(result.isValid());
    }

    @Test
    void threeLevelNestedMapReportsLeafError() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = { level1: level1 }\n"
                        + "level1 = { level2: level2 }\n"
                        + "level2 = { value: uint }");

        CBORValue level2 = CBOR.map()
                .put(CBOR.value("value"), CBOR.value("invalid"))
                .value();

        CBORValue level1 = CBOR.map()
                .put(CBOR.value("level2"), level2)
                .value();

        CBORValue root = CBOR.map()
                .put(CBOR.value("level1"), level1)
                .value();

        ValidationResult result = schema.validate(root);

        assertFalse(result.isValid());

        assertTrue(result.getErrors().stream()
                .anyMatch(error ->
                        error.getInstancePath().contains("level1")
                                && error.getInstancePath().contains("level2")
                                && error.getInstancePath().contains("value")));
    }

    @Test
    void nestedMapRejectsScalarInsteadOfMap() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = { address: address }\n"
                        + "address = { street: tstr }");

        CBORValue root = CBOR.map()
                .put(CBOR.value("address"), CBOR.value("not-a-map"))
                .value();

        ValidationResult result = schema.validate(root);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.getInstancePath().contains("address")));
    }

    @Test
    void mapContainingArrayIsValid() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { name: tstr, scores: [* uint] }");

        CBORValue scores = CBOR.array()
                .add(CBOR.integer(10))
                .add(CBOR.integer(20))
                .add(CBOR.integer(30))
                .value();

        CBORValue person = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Alice"))
                .put(CBOR.value("scores"), scores)
                .value();

        assertValid(schema, person);
    }

    @Test
    void arrayFieldRejectsMap() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { scores: [* uint] }");

        CBORValue scores = CBOR.map()
                .put(CBOR.value("value"), CBOR.integer(10))
                .value();

        CBORValue person = CBOR.map()
                .put(CBOR.value("scores"), scores)
                .value();

        ValidationResult result = schema.validate(person);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.getInstancePath().contains("scores")));
    }

    @Test
    void requiredNestedMapRejectsEmptyMapWhenFieldsRequired() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = { address: address }\n"
                        + "address = { street: tstr, zip: tstr }");

        CBORValue root = CBOR.map()
                .put(CBOR.value("address"), CBOR.map().value())
                .value();

        assertInvalid(schema, root);
    }

    @Test
    void mapFieldRejectsNull() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = { address: address }\n"
                        + "address = { street: tstr }");

        CBORValue root = CBOR.map()
                .put(CBOR.value("address"), CBOR.nullValue())
                .value();

        assertInvalid(schema, root);
    }

    @Test
    void mapFieldRejectsUndefined() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = { address: address }\n"
                        + "address = { street: tstr }");

        CBORValue root = CBOR.map()
                .put(CBOR.value("address"), CBOR.undefined())
                .value();

        assertInvalid(schema, root);
    }

    @Test
    void complexNestedMapValidatesCorrectly() {
        CddlSchema schema = CddlValidator.loadSchema(
                "order = {"
                        + " id: uint,"
                        + " customer: customer,"
                        + " shipping: address"
                        + " }\n"
                        + "customer = { name: tstr, active: bool }\n"
                        + "address = { street: tstr, zip: tstr }");

        CBORValue customer = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Alice"))
                .put(CBOR.value("active"), CBOR.trueValue())
                .value();

        CBORValue shipping = CBOR.map()
                .put(CBOR.value("street"), CBOR.value("Main Street"))
                .put(CBOR.value("zip"), CBOR.value("12345"))
                .value();

        CBORValue order = CBOR.map()
                .put(CBOR.value("id"), CBOR.integer(1001))
                .put(CBOR.value("customer"), customer)
                .put(CBOR.value("shipping"), shipping)
                .value();

        assertValid(schema, order);
    }

    @Test
    void complexNestedMapReportsDeepInvalidField() {
        CddlSchema schema = CddlValidator.loadSchema(
                "order = {"
                        + " id: uint,"
                        + " customer: customer,"
                        + " shipping: address"
                        + " }\n"
                        + "customer = { name: tstr, active: bool }\n"
                        + "address = { street: tstr, zip: tstr }");

        CBORValue customer = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Alice"))
                .put(CBOR.value("active"), CBOR.trueValue())
                .value();

        CBORValue shipping = CBOR.map()
                .put(CBOR.value("street"), CBOR.value("Main Street"))
                .put(CBOR.value("zip"), CBOR.integer(12345))
                .value();

        CBORValue order = CBOR.map()
                .put(CBOR.value("id"), CBOR.integer(1001))
                .put(CBOR.value("customer"), customer)
                .put(CBOR.value("shipping"), shipping)
                .value();

        ValidationResult result = schema.validate(order);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.getInstancePath().contains("shipping")
                        && error.getInstancePath().contains("zip")));
    }

    @Test
    void mapCanConstrainMemberToExactLiteral() {
        CddlSchema schema = CddlValidator.loadSchema(
                "request = { version: 1, id: uint }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("version"), CBOR.integer(1))
                .put(CBOR.value("id"), CBOR.integer(100))
                .value();

        assertValid(schema, value);
    }

    @Test
    void mapRejectsWrongLiteralWhileOtherMembersAreValid() {
        CddlSchema schema = CddlValidator.loadSchema(
                "request = { version: 1, id: uint }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("version"), CBOR.integer(2))
                .put(CBOR.value("id"), CBOR.integer(100))
                .value();

        ValidationResult result = schema.validate(value);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getInstancePath().contains("version"));
    }

    @Test
    void keyWithMultipleOccurrencesTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "apartment = {\n"
                        + "  kitchen: size,\n"
                        + "  * bedroom: size,\n"
                        + "}\n"
                        + "size = float ; in m2\n");

        CBORValue value = CBOR.map()
                .put(CBOR.value("kitchen"), CBOR.floatingPoint(12))
                .put(CBOR.value("bedroom"), CBOR.floatingPoint(15.5))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.value("kitchen"), CBOR.floatingPoint(12))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.value("bedroom"), CBOR.floatingPoint(15.5))
                .value();

        assertInvalid(schema, value);

        CBORValue.CBORMapEditor mapEditor = CBOR.map()
                .put(CBOR.value("kitchen"), CBOR.floatingPoint(12))
                .put(CBOR.value("bedroom"), CBOR.floatingPoint(15.5));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            mapEditor.put(CBOR.value("bedroom"), CBOR.floatingPoint(18.0));
        });
        assertEquals("Duplicate CBOR map key: \"bedroom\"", exception.getMessage());
    }

    @Test
    void mapKeysOrderTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = {\n"
                        + "  1: tstr,\n"
                        + "  3: tstr,\n"
                        + "  4: tstr,\n"
                        + "  7: tstr,\n"
                        + "}");

        CBORValue value = CBOR.map()
                .put(CBOR.integer(1), CBOR.text("1"))
                .put(CBOR.integer(3), CBOR.text("3"))
                .put(CBOR.integer(4), CBOR.text("4"))
                .put(CBOR.integer(7), CBOR.text("7"))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.integer(1), CBOR.text("1"))
                .put(CBOR.integer(4), CBOR.text("4"))
                .put(CBOR.integer(3), CBOR.text("3"))
                .put(CBOR.integer(7), CBOR.text("7"))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.integer(7), CBOR.text("7"))
                .put(CBOR.integer(4), CBOR.text("4"))
                .put(CBOR.integer(3), CBOR.text("3"))
                .put(CBOR.integer(1), CBOR.text("1"))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.integer(1), CBOR.text("1"))
                .put(CBOR.integer(4), CBOR.text("4"))
                .put(CBOR.integer(7), CBOR.text("7"))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void tablePredefinedTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "square-roots = {* x => y}\n"
                        + "x = int\n"
                        + "y = float");

        CBORValue value = CBOR.map()
                .put(CBOR.integer(1), CBOR.floatingPoint(10.0))
                .put(CBOR.integer(2), CBOR.floatingPoint(20.0))
                .put(CBOR.integer(3), CBOR.floatingPoint(30.0))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.integer(1), CBOR.floatingPoint(10.0))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.integer(1), CBOR.text("10.0"))
                .put(CBOR.integer(2), CBOR.floatingPoint(20.0))
                .put(CBOR.integer(3), CBOR.floatingPoint(30.0))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void tableAnyTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "square-roots = {* x => y}\n"
                        + "x = any\n"
                        + "y = any");

        CBORValue value = CBOR.map()
                .put(CBOR.integer(1), CBOR.floatingPoint(10.0))
                .put(CBOR.integer(2), CBOR.floatingPoint(20.0))
                .put(CBOR.integer(3), CBOR.floatingPoint(30.0))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.integer(1), CBOR.floatingPoint(10.0))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.integer(1), CBOR.text("10.0"))
                .put(CBOR.text("2"), CBOR.floatingPoint(20.0))
                .put(CBOR.integer(3), CBOR.floatingPoint(30.0))
                .value();

        assertValid(schema, value);

        value = CBOR.array()
                .add(CBOR.text("10.0"))
                .add(CBOR.floatingPoint(20.0))
                .add(CBOR.floatingPoint(30.0))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void tableKeyChoiceTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "tostring = {* mynumber => tstr}\n"
                        + "mynumber = int / float\n");

        CBORValue value = CBOR.map()
                .put(CBOR.floatingPoint(10.0), CBOR.text("10.0"))
                .put(CBOR.floatingPoint(20.0), CBOR.text("20.0"))
                .put(CBOR.floatingPoint(30.0), CBOR.text("30.0"))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.integer(10), CBOR.text("10"))
                .put(CBOR.integer(20), CBOR.text("20"))
                .put(CBOR.integer(30), CBOR.text("30"))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.integer(10), CBOR.text("10"))
                .put(CBOR.integer(20), CBOR.text("20"))
                .put(CBOR.floatingPoint(30.0), CBOR.text("30.0"))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.integer(10), CBOR.floatingPoint(10.0))
                .put(CBOR.integer(20), CBOR.text("20"))
                .put(CBOR.integer(30), CBOR.text("30"))
                .value();

        assertInvalid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("10"), CBOR.text("10"))
                .put(CBOR.integer(20), CBOR.text("20"))
                .put(CBOR.integer(30), CBOR.text("30"))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void labeledValuesTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "labeled-values = {\n"
                        + "  ? fritz: number,\n"
                        + "  * label => value\n"
                        + "}\n"
                        + "label = text\n"
                        + "value = number");

        CBORValue value = CBOR.map()
                .put(CBOR.text("fritz"), CBOR.integer(0))
                .put(CBOR.text("fox"), CBOR.integer(1))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("fritz"), CBOR.integer(0))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("fox"), CBOR.integer(1))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("fritz"), CBOR.floatingPoint(-10))
                .put(CBOR.text("fox"), CBOR.floatingPoint(15.2))
                .put(CBOR.text("wolf"), CBOR.floatingPoint(266.3))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("fritz"), CBOR.text("0"))
                .put(CBOR.text("fox"), CBOR.integer(1))
                .value();

        assertInvalid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("fritz"), CBOR.integer(0))
                .put(CBOR.text("fox"), CBOR.text("1"))
                .value();

        assertInvalid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("fritz"), CBOR.integer(0))
                .put(CBOR.integer(5), CBOR.integer(1))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void indeterminateKeyDefinitionTest() {
        // Illegal example, used to verify the validation behaviour

        CddlSchema schema = CddlValidator.loadSchema(
                "do-not-do-this = {\n"
                        + "  int => int,\n"
                        + "  int => 6,\n"
                        + "}");

        CBORValue value = CBOR.map()
                .put(CBOR.integer(3), CBOR.integer(5))
                .put(CBOR.integer(4), CBOR.integer(6))
                .value();

        ValidationResult result = schema.validate(value);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> "Expected at most 1 key(s) matching int but found 2".equals(e.getMessage())));

        value = CBOR.map()
                .put(CBOR.integer(4), CBOR.integer(6))
                .put(CBOR.integer(3), CBOR.integer(5))
                .value();

        result = schema.validate(value);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> "Expected at most 1 key(s) matching int but found 2".equals(e.getMessage())));

        value = CBOR.map()
                .put(CBOR.integer(4), CBOR.integer(6))
                .value();

        result = schema.validate(value);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> "Expected at least 1 key(s) matching int but found 0".equals(e.getMessage()))); // TODO : review error
    }

    @Test
    void jsonPointerEscapesMapKeySegments() {
        CddlSchema schema = CddlValidator.loadSchema("root = {\"a/b~c\": uint}");
        ValidationResult result = schema.validate(CBOR.map().put("a/b~c", "bad").value());

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .flatMap(error -> java.util.stream.Stream.concat(java.util.stream.Stream.of(error), error.getCauses().stream()))
                .anyMatch(error -> "/a/b~c".equals(error.getInstancePath())), result.getErrors().toString());
    }
    
    @Test
    void typedLiteralKeyColonHasImplicitCut() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = { ? 1 : int, * label => values }\n" +
                        "label = int / tstr\n" +
                        "values = any");

        assertValid(schema, CBOR.map()
                .put(CBOR.integer(1), CBOR.integer(42))
                .value());

        assertInvalid(schema, CBOR.map()
                .put(CBOR.integer(1), CBOR.bytes(new byte[] {0x00}))
                .value());
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
