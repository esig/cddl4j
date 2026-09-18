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

class StructValidatorTest {

    @Test
    void ignoreArrayKeysTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Geography = [\n"
                        + "  city           : tstr,\n"
                        + "  gpsCoordinates : GpsCoordinates,\n"
                        + "]\n"
                        + "GpsCoordinates = {\n"
                        + "  longitude      : uint,            ; degrees, scaled by 10^7\n"
                        + "  latitude       : uint,            ; degrees, scaled by 10^7\n"
                        + "}");

        CBORValue value = CBOR.array()
                .add(CBOR.text("Kehlen"))
                .add(CBOR.map()
                        .put(CBOR.text("longitude"), CBOR.integer(10))
                        .put(CBOR.text("latitude"), CBOR.integer(20))
                        .value())
                .value();

        assertValid(schema, value);

        value = CBOR.array()
                .add(CBOR.text("Kehlen"))
                .add(CBOR.integer(10))
                .add(CBOR.integer(20))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void mapWithColonDefinitionTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "located-samples = {\n"
                        + "  sample-point: int,\n"
                        + "  samples: [+ float],\n"
                        + "}");

        CBORValue value = CBOR.map()
                .put(CBOR.text("sample-point"), CBOR.integer(10))
                .put(CBOR.text("samples"), CBOR.array()
                        .add(CBOR.floatingPoint(10.0))
                        .add(CBOR.floatingPoint(20.0))
                        .value())
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("sample-point"), CBOR.integer(10))
                .put(CBOR.text("samples"), CBOR.array()
                        .add(CBOR.floatingPoint(10.0))
                        .value())
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("sample-point"), CBOR.integer(10))
                .put(CBOR.text("samples"), CBOR.array().value())
                .value();

        assertInvalid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("samples"), CBOR.array()
                        .add(CBOR.floatingPoint(10.0))
                        .add(CBOR.floatingPoint(20.0))
                        .value())
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void mapWithDoubleArrowDefinitionTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "located-samples = {\n"
                        + "  \"sample-point\" => int,\n"
                        + "  \"samples\" => [+ float],\n"
                        + "}");

        CBORValue value = CBOR.map()
                .put(CBOR.text("sample-point"), CBOR.integer(10))
                .put(CBOR.text("samples"), CBOR.array()
                        .add(CBOR.floatingPoint(10.0))
                        .add(CBOR.floatingPoint(20.0))
                        .value())
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("sample-point"), CBOR.integer(10))
                .put(CBOR.text("samples"), CBOR.array()
                        .add(CBOR.floatingPoint(10.0))
                        .value())
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("sample-point"), CBOR.integer(10))
                .put(CBOR.text("samples"), CBOR.array().value())
                .value();

        assertInvalid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("samples"), CBOR.array()
                        .add(CBOR.floatingPoint(10.0))
                        .add(CBOR.floatingPoint(20.0))
                        .value())
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void mapWithDoubleArrowDefinitionByReferenceTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "located-samples = {\n"
                        + "  sample-point => int,\n"
                        + "  samples => [+ float],\n"
                        + "}\n"
                        + "sample-point = 0\n"
                        + "samples = 1");

        CBORValue value = CBOR.map()
                .put(CBOR.integer(0), CBOR.integer(10))
                .put(CBOR.integer(1), CBOR.array()
                        .add(CBOR.floatingPoint(10.0))
                        .add(CBOR.floatingPoint(20.0))
                        .value())
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("sample-point"), CBOR.integer(10))
                .put(CBOR.text("samples"), CBOR.array()
                        .add(CBOR.floatingPoint(10.0))
                        .add(CBOR.floatingPoint(20.0))
                        .value())
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void mapWithStarEntryTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "located-samples = {\n"
                        + "  sample-point: int,\n"
                        + "  samples: [+ float],\n"
                        + "  * equipment-type => equipment-tolerances,\n"
                        + "}\n"
                        + "equipment-type = [name: tstr, manufacturer: tstr]\n"
                        + "equipment-tolerances = [+ [float, float]]");

        CBORValue value = CBOR.map()
                .put(CBOR.text("sample-point"), CBOR.integer(10))
                .put(CBOR.text("samples"), CBOR.array()
                        .add(CBOR.floatingPoint(10.0))
                        .add(CBOR.floatingPoint(20.0))
                        .value())
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("sample-point"), CBOR.integer(10))
                .put(CBOR.text("samples"), CBOR.array()
                        .add(CBOR.floatingPoint(10.0))
                        .add(CBOR.floatingPoint(20.0))
                        .value())
                .put(CBOR.array().add("x").add("y").value(),
                        CBOR.array()
                                .add(CBOR.array()
                                        .add(CBOR.floatingPoint(10.0))
                                        .add(CBOR.floatingPoint(20.0))
                                        .value())
                                .value())
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("sample-point"), CBOR.integer(10))
                .put(CBOR.text("samples"), CBOR.array()
                        .add(CBOR.floatingPoint(10.0))
                        .add(CBOR.floatingPoint(20.0))
                        .value())
                .put(CBOR.array().add("x").add("y").value(),
                        CBOR.array()
                                .add(CBOR.array()
                                        .add(CBOR.floatingPoint(10.0))
                                        .add(CBOR.floatingPoint(20.0))
                                        .value())
                                .add(CBOR.array()
                                        .add(CBOR.floatingPoint(30.0))
                                        .add(CBOR.floatingPoint(40.0))
                                        .value())
                                .value())
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("sample-point"), CBOR.integer(10))
                .put(CBOR.text("samples"), CBOR.array()
                        .add(CBOR.floatingPoint(10.0))
                        .add(CBOR.floatingPoint(20.0))
                        .value())
                .put(CBOR.array().add("x").add("y").value(),
                        CBOR.array()
                                .add(CBOR.array()
                                        .add(CBOR.floatingPoint(10.0))
                                        .add(CBOR.floatingPoint(20.0))
                                        .value())
                                .value())
                .put(CBOR.array().add("k").add("m").value(),
                        CBOR.array()
                                .add(CBOR.array()
                                        .add(CBOR.floatingPoint(10.0))
                                        .add(CBOR.floatingPoint(20.0))
                                        .value())
                                .value())
                .value();

        assertValid(schema, value);


        value = CBOR.map()
                .put(CBOR.text("sample-point"), CBOR.integer(10))
                .put(CBOR.text("samples"), CBOR.array()
                        .add(CBOR.floatingPoint(10.0))
                        .add(CBOR.floatingPoint(20.0))
                        .value())
                .put(CBOR.array().add("x").add("y").value(),
                        CBOR.array().value())
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void mapWithAllOptionalEntriesTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "PersonalData = {\n"
                        + "  ? displayName: tstr,\n"
                        + "  NameComponents,\n"
                        + "  ? age: uint,\n"
                        + "  * tstr => any\n"
                        + "}\n"
                        + "NameComponents = (\n"
                        + "  ? firstName: tstr,\n"
                        + "  ? familyName: tstr,\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.text("familyName"), CBOR.text("agust"))
                .put(CBOR.text("antiforeignism"), CBOR.text("pretzel"))
                .put(CBOR.text("springbuck"), CBOR.text("illuminatingly"))
                .put(CBOR.text("exuviae"), CBOR.text("ephemeris"))
                .put(CBOR.text("kilometrage"), CBOR.text("kilometrage"))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("familyName"), CBOR.text("agust"))
                .put(CBOR.text("age"), CBOR.integer(20))
                .put(CBOR.text("antiforeignism"), CBOR.text("pretzel"))
                .put(CBOR.text("springbuck"), CBOR.text("illuminatingly"))
                .put(CBOR.text("exuviae"), CBOR.text("ephemeris"))
                .put(CBOR.text("kilometrage"), CBOR.text("kilometrage"))
                .put(CBOR.text("millieage"), CBOR.bytes(new byte[] { 0x00, 0x01 }))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("antiforeignism"), CBOR.text("pretzel"))
                .put(CBOR.text("springbuck"), CBOR.text("illuminatingly"))
                .put(CBOR.text("exuviae"), CBOR.text("ephemeris"))
                .put(CBOR.text("kilometrage"), CBOR.text("kilometrage"))
                .value();

        assertValid(schema, value);

        value = CBOR.map()
                .put(CBOR.text("familyName"), CBOR.text("agust"))
                .put(CBOR.text("age"), CBOR.text("20"))
                .put(CBOR.text("antiforeignism"), CBOR.text("pretzel"))
                .put(CBOR.text("springbuck"), CBOR.text("illuminatingly"))
                .put(CBOR.text("exuviae"), CBOR.text("ephemeris"))
                .put(CBOR.text("kilometrage"), CBOR.text("kilometrage"))
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
