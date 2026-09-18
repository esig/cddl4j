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
import eu.europa.ec.digit.cddl4j.validator.schema.parser.CddlParser;
import eu.europa.ec.digit.cddl4j.validator.schema.type.GroupChoiceType;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupChoiceValidationTest {

    @Test
    void parsesGroupChoice() {
        String cddl = "delivery = (street: tstr // per-pickup: true)";

        CddlSchema schema = new CddlParser().parse(cddl);

        GroupChoiceType choice = assertInstanceOf(GroupChoiceType.class, schema.getRuleSet().getRule("delivery"));
        assertEquals(2, choice.getAlternatives().size());
    }

    @Test
    void validatesGroupChoiceInsideArray() {
        String cddl = "root = [values]\n"
                + "values = (uint // tstr)";

        CddlSchema schema = new CddlParser().parse(cddl);

        assertValid(schema, CBOR.array().add(CBOR.value(42)).value());
        assertValid(schema, CBOR.array().add(CBOR.text("value")).value());
        assertInvalid(schema, CBOR.array().add(CBOR.trueValue()).value());
    }

    @Test
    void validatesAdditiveGroupChoiceRule() {
        String cddl = "address = { delivery }\n"
                + "delivery = (street: tstr)\n"
                + "delivery //= (per-pickup: true)";

        CddlSchema schema = new CddlParser().parse(cddl);

        assertValid(schema, CBOR.map().put(CBOR.text("street"), CBOR.text("Rue")).value());
        assertValid(schema, CBOR.map().put(CBOR.text("per-pickup"), CBOR.trueValue()).value());
    }

    @Test
    void deliveryStreetPass() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("street"), CBOR.text("Main Street 1"))
                .put(CBOR.value("name"), CBOR.text("Luxembourg"))
                .put(CBOR.value("zip-code"), CBOR.integer(1000))
                .value();

        assertValid(schema, value);
    }

    @Test
    void deliveryStreetWithOptionalNumberPass() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("street"), CBOR.text("Main Street"))
                .put(CBOR.value("number"), CBOR.integer(42))
                .put(CBOR.value("name"), CBOR.text("Luxembourg"))
                .put(CBOR.value("zip-code"), CBOR.integer(1000))
                .value();

        assertValid(schema, value);
    }

    @Test
    void deliveryPoBoxPass() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("po-box"), CBOR.integer(123))
                .put(CBOR.value("name"), CBOR.text("Luxembourg"))
                .put(CBOR.value("zip-code"), CBOR.integer(1000))
                .value();

        assertValid(schema, value);
    }

    @Test
    void deliveryPerPickupPass() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("per-pickup"), CBOR.bool(true))
                .value();

        assertValid(schema, value);
    }

    @Test
    void deliveryStreetWithoutNumberPass() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("street"), CBOR.text("Rue de la Gare"))
                .put(CBOR.value("name"), CBOR.text("Luxembourg"))
                .put(CBOR.value("zip-code"), CBOR.integer(1000))
                .value();

        assertValid(schema, value);
    }

    @Test
    void deliveryStreetMissingCityFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("street"), CBOR.text("Main Street"))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void deliveryStreetWithInvalidNumberFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("street"), CBOR.text("Main Street"))
                .put(CBOR.value("number"), CBOR.text("42"))
                .put(CBOR.value("name"), CBOR.text("Luxembourg"))
                .put(CBOR.value("zip-code"), CBOR.integer(1000))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void deliveryPoBoxMissingCityFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("po-box"), CBOR.integer(123))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void deliveryPerPickupFalseFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("per-pickup"), CBOR.bool(false))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void deliveryCityMissingNameFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("street"), CBOR.text("Main Street"))
                .put(CBOR.value("zip-code"), CBOR.integer(1000))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void deliveryCityMissingZipCodeFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("street"), CBOR.text("Main Street"))
                .put(CBOR.value("name"), CBOR.text("Luxembourg"))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void deliveryCityWithInvalidZipCodeFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("street"), CBOR.text("Main Street"))
                .put(CBOR.value("name"), CBOR.text("Luxembourg"))
                .put(CBOR.value("zip-code"), CBOR.text("1000"))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void deliveryMixedAlternativesFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("street"), CBOR.text("Main Street"))
                .put(CBOR.value("po-box"), CBOR.integer(123))
                .put(CBOR.value("name"), CBOR.text("Luxembourg"))
                .put(CBOR.value("zip-code"), CBOR.integer(1000))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void deliveryUnexpectedKeyFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("street"), CBOR.text("Main Street"))
                .put(CBOR.value("name"), CBOR.text("Luxembourg"))
                .put(CBOR.value("zip-code"), CBOR.integer(1000))
                .put(CBOR.value("country"), CBOR.text("LU"))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void deliveryPerPickupWithAdditionalKeyFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("per-pickup"), CBOR.bool(true))
                .put(CBOR.value("street"), CBOR.text("Main Street"))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void deliveryStreetWithAdditive() {
        CddlSchema schema = CddlValidator.loadSchema(
                "address = { delivery }\n"
                        + "delivery = (\n"
                        + "street: tstr, ? number: uint, city //\n"
                        + "po-box: uint, city //\n"
                        + "per-pickup: true )\n"
                        + "city = (\n"
                        + "name: tstr, zip-code: uint\n"
                        + ")\n"
                        + "delivery //= (\n"
                        + "lat: float, long: float, drone-type: tstr\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.value("street"), CBOR.text("Main Street 1"))
                .put(CBOR.value("name"), CBOR.text("Luxembourg"))
                .put(CBOR.value("zip-code"), CBOR.integer(1000))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.value("lat"), CBOR.floatingPoint(10.5))
                .put(CBOR.value("long"), CBOR.floatingPoint(1234))
                .put(CBOR.value("drone-type"), CBOR.text("alt"))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.value("street"), CBOR.text("Main Street 1"))
                .put(CBOR.value("name"), CBOR.text("Luxembourg"))
                .put(CBOR.value("lat"), CBOR.floatingPoint(10.5))
                .put(CBOR.value("long"), CBOR.floatingPoint(1234))
                .put(CBOR.value("drone-type"), CBOR.text("alt"))
                .value();

        assertInvalid(schema, value);

        value = CBOR.map()
                .put(CBOR.value("lat"), CBOR.floatingPoint(10.5))
                .put(CBOR.value("long"), CBOR.floatingPoint(1234))
                .value();

        assertInvalid(schema, value);
    }

    private static void assertValid(CddlSchema schema, CBORValue value) {
        ValidationResult result = schema.validate(value);
        assertTrue(result.isValid(), "Expected valid CBOR value, but validation failed: " + result);
    }

    private static void assertInvalid(CddlSchema schema, CBORValue value) {
        ValidationResult result = schema.validate(value);
        assertFalse(result.isValid(), "Expected invalid CBOR value, but validation succeeded");
    }

}
