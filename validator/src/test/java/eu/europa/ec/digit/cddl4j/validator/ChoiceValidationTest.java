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
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationError;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for type choice validation: {@code a / b / c}.
 */
class ChoiceValidationTest {

    @Test
    void choiceTextEnumeration() {
        CddlSchema schema = CddlValidator.loadSchema("attire = \"bow tie\" / \"necktie\" / \"Internet attire\"");
        assertValid(schema, CBOR.text("bow tie"));
        assertValid(schema, CBOR.text("necktie"));
        assertValid(schema, CBOR.text("Internet attire"));
        assertInvalid(schema, CBOR.text("swimwear"));
        assertInvalid(schema, CBOR.text("bow tie necktie"));
        assertInvalid(schema, CBOR.text("Hello world"));
    }

    @Test
    void choiceIntEnumeration() {
        CddlSchema schema = CddlValidator.loadSchema("attire = 6 / 17");
        assertValid(schema, CBOR.integer(6));
        assertValid(schema, CBOR.integer(17));
        assertInvalid(schema, CBOR.integer(10));
        assertInvalid(schema, CBOR.text("6"));
        assertInvalid(schema, CBOR.text("17"));
    }

    @Test
    void choiceWithAdditiveEnumeration() {
        CddlSchema schema = CddlValidator.loadSchema(
                "attire = \"bow tie\" / \"necktie\" / \"Internet attire\"\n"
                        + "attire /= \"swimwear\"");
        assertValid(schema, CBOR.text("bow tie"));
        assertValid(schema, CBOR.text("necktie"));
        assertValid(schema, CBOR.text("Internet attire"));
        assertValid(schema, CBOR.text("swimwear"));
        assertInvalid(schema, CBOR.text("bow tie necktie"));
        assertInvalid(schema, CBOR.text("Hello world"));
    }

    @Test
    void choiceFirstAlternativeMatches() {
        CddlSchema schema = CddlValidator.loadSchema("root = uint / tstr");
        assertValid(schema, CBOR.integer(42));
    }

    @Test
    void choiceSecondAlternativeMatches() {
        CddlSchema schema = CddlValidator.loadSchema("root = uint / tstr");
        assertValid(schema, CBOR.text("hello"));
    }

    @Test
    void choiceNoAlternativeFails() {
        CddlSchema schema = CddlValidator.loadSchema("root = uint / tstr");
        assertInvalid(schema, CBOR.trueValue());
    }

    @Test
    void choiceDoesNotCoerceTypes() {
        CddlSchema schema = CddlValidator.loadSchema("root = uint / tstr");
        assertInvalid(schema, CBOR.floatingPoint(42.0));
    }

    @Test
    void choiceDistinguishesNullFromText() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = tstr / null");

        assertValid(schema, CBOR.text("null"));
        assertValid(schema, CBOR.nullValue());
    }

    @Test
    void threeWayChoice() {
        CddlSchema schema = CddlValidator.loadSchema("root = uint / tstr / bool");

        assertValid(schema, CBOR.trueValue());
        assertValid(schema, CBOR.integer(0));
        assertValid(schema, CBOR.text("text"));
        assertInvalid(schema, CBOR.nullValue());
    }

    @Test
    void fourWaysChoice() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = uint / tstr / bool / null");

        assertValid(schema, CBOR.integer(1));
        assertValid(schema, CBOR.text("one"));
        assertValid(schema, CBOR.bool(true));
        assertValid(schema, CBOR.nullValue());
        assertInvalid(schema, CBOR.bytes(new byte[] {1, 2, 3}));
        assertInvalid(schema, CBOR.undefined());
        assertInvalid(schema, CBOR.floatingPoint(1.5));
    }

    @Test
    void mapTypeChoiceBacktracksAfterCutFailure() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = { 0 ^=> int } / { 0 ^=> tstr }");

        assertValid(schema, CBOR.map().put(CBOR.integer(0), CBOR.text("second alternative")).value());
    }

    @Test
    void cutFailuresWithoutSharedDiagnosticsRetainBothFailures() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = { 0 ^=> uint, * int => any } / { 1 ^=> uint, * int => any }");
        CBORValue value = CBOR.map()
                .put(CBOR.integer(0), CBOR.text("not a uint"))
                .put(CBOR.integer(1), CBOR.text("also not a uint"))
                .value();

        ValidationResult result = schema.validate(value);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        ValidationError choiceError = result.getErrors().get(0);
        assertEquals("/", choiceError.getInstancePath());
        assertTrue(choiceError.getMessage().contains("Value did not match any alternative in choice"));
        assertEquals(2, choiceError.getCauses().size());
        assertEquals("/", choiceError.getCauses().get(0).getInstancePath());
        assertEquals(1, choiceError.getCauses().get(0).getCauses().size());
        assertEquals("/0", choiceError.getCauses().get(0).getCauses().get(0).getInstancePath());
        assertEquals("/", choiceError.getCauses().get(1).getInstancePath());
        assertEquals(1, choiceError.getCauses().get(1).getCauses().size());
        assertEquals("/1", choiceError.getCauses().get(1).getCauses().get(0).getInstancePath());
    }

    @Test
    void nfcRetrievalOptionsSucceedAfterWifiAndBleCutFailures() {
        CddlSchema schema = CddlValidator.loadSchema(
                "RetrievalOptions = WifiOptions / BleOptions / NfcOptions\n" +
                        "WifiOptions = { ? 0: tstr, * uint => any }\n" +
                        "BleOptions = { ? 0: bool, * uint => any }\n" +
                        "NfcOptions = { 0: uint, 1: uint, * uint => any }");

        CBORValue nfcOptions = CBOR.map()
                .put(CBOR.integer(0), CBOR.integer(10))
                .put(CBOR.integer(1), CBOR.integer(20))
                .value();

        assertValid(schema, nfcOptions);
    }

    @Test
    void choiceWithLiterals() {
        CddlSchema schema = CddlValidator.loadSchema("status = 0 / 1 / 2");

        assertValid(schema, CBOR.integer(0));
        assertValid(schema, CBOR.integer(1));
        assertValid(schema, CBOR.integer(2));
        assertInvalid(schema, CBOR.integer(3));
    }

    @Test
    void choiceErrorsMentionAllAlternatives() {
        CddlSchema schema = CddlValidator.loadSchema("root = uint / tstr");

        ValidationResult result = schema.validate(CBOR.trueValue());

        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());

        // The top-level choice error should retain the validation
        // failures produced by its alternatives.
        assertFalse(result.getErrors().get(0).getCauses().isEmpty());
    }

    @Test
    void namedChoiceRuleReference() {
        CddlSchema schema = CddlValidator.loadSchema(
                        "payload = text-payload / binary-payload\n"
                                + "text-payload = tstr\n"
                                + "binary-payload = bstr");

        assertValid(schema, CBOR.text("hello"));
        assertValid(schema, CBOR.bytes(new byte[] {1, 2, 3}));
        assertInvalid(schema, CBOR.integer(42));
    }

    @Test
    void additiveRuleUpdateCreatesChoice() {
        CddlSchema schema = CddlValidator.loadSchema(
                        "value = uint\n"
                                + "value /= tstr");

        assertValid(schema, CBOR.integer(42));
        assertValid(schema, CBOR.text("hello"));
        assertInvalid(schema, CBOR.trueValue());
    }

    @Test
    void choiceAcceptsValueFromFirstNumericRange() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = 1..10 / 20..30");

        assertValid(schema, CBOR.integer(5));
    }

    @Test
    void choiceAcceptsValueFromSecondNumericRange() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = 1..10 / 20..30");

        assertValid(schema, CBOR.integer(25));
    }

    @Test
    void choiceRejectsValueBetweenNumericAlternatives() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = 1..10 / 20..30");

        assertInvalid(schema, CBOR.integer(15));
    }

    @Test
    void choiceRejectsValueOutsideBothNumericRanges() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = 1..10 / 20..30");

        assertInvalid(schema, CBOR.integer(100));
    }

    @Test
    void choiceAcceptsBothRangeBoundaries() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = 1..10 / 20..30");

        assertValid(schema, CBOR.integer(1));
        assertValid(schema, CBOR.integer(10));
        assertValid(schema, CBOR.integer(20));
        assertValid(schema, CBOR.integer(30));
    }

    @Test
    void choiceFallsBackFromScalarToMap() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = uint / person\n"
                        + "person = { name: tstr }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("name"), CBOR.text("Alice"))
                .value();

        assertValid(schema, value);
    }

    @Test
    void choiceFallsBackFromMapToScalar() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = uint / person\n"
                        + "person = { name: tstr }");

        assertValid(schema, CBOR.integer(42));
    }

    @Test
    void choiceRejectsScalarAndMapWhenNeitherMatches() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = uint / person\n"
                        + "person = { name: tstr }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("name"), CBOR.integer(42))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void choiceRejectsWrongScalarTypeAndWrongMapShape() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = uint / person\n"
                        + "person = { name: tstr }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("age"), CBOR.integer(42))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void choiceAcceptsFirstMapAlternative() {
        CddlSchema schema = CddlValidator.loadSchema(
                "contact = email / phone\n"
                        + "email = { email: tstr }\n"
                        + "phone = { phone: tstr }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("email"), CBOR.text("a@example.com"))
                .value();

        assertValid(schema, value);
    }

    @Test
    void choiceAcceptsSecondMapAlternative() {
        CddlSchema schema = CddlValidator.loadSchema(
                "contact = email / phone\n"
                        + "email = { email: tstr }\n"
                        + "phone = { phone: tstr }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("phone"), CBOR.text("123456"))
                .value();

        assertValid(schema, value);
    }

    @Test
    void choiceRejectsMapMatchingNeitherShape() {
        CddlSchema schema = CddlValidator.loadSchema(
                "contact = email / phone\n"
                        + "email = { email: tstr }\n"
                        + "phone = { phone: tstr }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("address"), CBOR.text("Somewhere"))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void choiceTriesSecondAlternativeAfterFirstMapFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = person / address\n"
                        + "address = { street: tstr }\n"
                        + "person = { name: tstr }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("street"), CBOR.text("Main Street"))
                .value();

        assertValid(schema, value);
    }

    @Test
    void choiceDoesNotFailJustBecauseFirstAlternativeHasWrongField() {
        CddlSchema schema = CddlValidator.loadSchema(
                "contact = email / phone\n"
                        + "email = { email: tstr }\n"
                        + "phone = { phone: tstr }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("phone"), CBOR.text("123"))
                .value();

        assertValid(schema, value);
    }

    @Test
    void choiceFailDifferentType() {
        CddlSchema schema = CddlValidator.loadSchema(
                "contact = email / phone\n"
                        + "email = { email: tstr }\n"
                        + "phone = { phone: tstr }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("phone"), CBOR.integer(123))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void nestedChoiceAcceptsFirstNestedAlternative() {
        CddlSchema schema = CddlValidator.loadSchema(
                "contact = { value: uint / tstr }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("value"), CBOR.integer(123))
                .value();

        assertValid(schema, value);
    }

    @Test
    void nestedChoiceAcceptsSecondNestedAlternative() {
        CddlSchema schema = CddlValidator.loadSchema(
                "contact = { value: uint / tstr }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("value"), CBOR.text("123"))
                .value();

        assertValid(schema, value);
    }

    @Test
    void nestedChoiceRejectsInvalidAlternative() {
        CddlSchema schema = CddlValidator.loadSchema(
                "contact = { value: uint / tstr }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("value"), CBOR.bool(true))
                .value();

        ValidationResult result = schema.validate(value);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getInstancePath().contains("value"));
    }

    @Test
    void arrayChoiceAcceptsDifferentTypesPerElement() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [* uint / tstr]");

        CBORValue values = CBOR.array()
                .add(CBOR.integer(1))
                .add(CBOR.text("two"))
                .add(CBOR.integer(3))
                .add(CBOR.text("four"))
                .value();

        assertValid(schema, values);
    }

    @Test
    void arrayChoiceRejectsElementMatchingNeitherAlternative() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [* uint / tstr]");

        CBORValue values = CBOR.array()
                .add(CBOR.integer(1))
                .add(CBOR.text("two"))
                .add(CBOR.bool(true))
                .value();

        ValidationResult result = schema.validate(values);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getInstancePath().contains("2"));
    }

    @Test
    void arrayChoiceAcceptsAllElementsFromOneAlternative() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [* uint / tstr]");

        CBORValue values = CBOR.array()
                .add(CBOR.integer(1))
                .add(CBOR.integer(2))
                .add(CBOR.integer(3))
                .value();

        assertValid(schema, values);
    }

    @Test
    void arrayChoiceAcceptsAllElementsFromSecondAlternative() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [* uint / tstr]");

        CBORValue values = CBOR.array()
                .add(CBOR.text("one"))
                .add(CBOR.text("two"))
                .add(CBOR.text("three"))
                .value();

        assertValid(schema, values);
    }

    @Test
    void nestedStructureChoiceAcceptsScalarAlternative() {
        CddlSchema schema = CddlValidator.loadSchema(
                "shipping = address / tstr\n"
                        + "address = { street: tstr, zip: tstr }");

        assertValid(schema, CBOR.text("pickup"));
    }

    @Test
    void nestedStructureChoiceAcceptsMapAlternative() {
        CddlSchema schema = CddlValidator.loadSchema(
                "shipping = address / tstr\n"
                        + "address = { street: tstr, zip: tstr }");

        CBORValue address = CBOR.map()
                .put(CBOR.value("street"), CBOR.text("Main"))
                .put(CBOR.value("zip"), CBOR.text("12345"))
                .value();

        assertValid(schema, address);
    }

    @Test
    void nestedStructureChoiceReportsPathWhenAllAlternativesFail() {
        CddlSchema schema = CddlValidator.loadSchema(
                "order = { shipping: address / tstr }\n"
                        + "address = { street: tstr, zip: tstr }");

        CBORValue order = CBOR.map()
                .put(
                        CBOR.value("shipping"),
                        CBOR.integer(42))
                .value();

        ValidationResult result = schema.validate(order);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getInstancePath().contains("shipping"));
    }

    @Test
    void choiceDoesNotAcceptInvalidMapJustBecauseMapAlternativeStartsMatching() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = user / tstr\n"
                        + "user = { name: tstr, age: uint }");

        CBORValue invalidUser = CBOR.map()
                .put(CBOR.value("name"), CBOR.text("Alice"))
                .put(CBOR.value("age"), CBOR.text("not-a-number"))
                .value();

        assertInvalid(schema, invalidUser);
    }

    @Test
    void choiceStillRejectsInvalidValueWhenEveryAlternativeFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = user / uint\n"
                        + "user = { name: tstr }");

        CBORValue invalidUser = CBOR.map()
                .put(CBOR.value("name"), CBOR.integer(123))
                .value();

        assertInvalid(schema, invalidUser);
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