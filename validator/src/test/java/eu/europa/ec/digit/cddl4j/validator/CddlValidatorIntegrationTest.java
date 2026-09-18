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
 * End-to-end integration tests that encode CBOR bytes and validate them.
 *
 * <p>These tests verify the full pipeline: CDDL text → schema compilation →
 * CBOR byte decoding → structural validation.
 */
class CddlValidatorIntegrationTest {

    // -------------------------------------------------------------------
    // IoT sensor schema used throughout
    // -------------------------------------------------------------------

    private static final String SENSOR_CDDL =
            String.join(
                    "\n",
                    "sensor-reading = {",
                    "  device-id: tstr,",
                    "  timestamp: uint,",
                    "  value: float,",
                    "  unit: unit-type,",
                    "  ? tags: [* tstr]",
                    "}",
                    "unit-type = \"celsius\" / \"fahrenheit\" / \"humidity\" / \"ppm\"");

    @Test
    void validSensorReadingPasses() {
        CddlSchema schema = CddlValidator.loadSchema(SENSOR_CDDL);

        CBORValue reading =
                buildSensorReading(
                        "sensor-001",
                        1_700_000_000L,
                        23.5,
                        "celsius");

        byte[] cbor = reading.encodeToBytes();

        ValidationResult result = schema.validate(cbor);
        assertTrue(result.isValid(), "Expected valid but got: " + result.getErrors());
    }

    @Test
    void sensorReadingWithTagsPasses() {
        CddlSchema schema = CddlValidator.loadSchema(SENSOR_CDDL);

        CBORValue tags = CBOR.array()
                                .add("building-A")
                                .add("floor-3")
                                .value();

        // The map builder is used when the object is constructed. Since
        // CBORObject maps are immutable, build the complete map here.
        CBORValue reading = CBOR.map()
                                .put("device-id", "sensor-002")
                                .put("timestamp", 1_700_000_001L)
                                .put("value", 45.0)
                                .put("unit", "humidity")
                                .put("tags", tags)
                                .value();

        byte[] cbor = reading.encodeToBytes();

        ValidationResult result = schema.validate(cbor);
        assertTrue(result.isValid(), "Expected valid but got: " + result.getErrors());
    }

    @Test
    void sensorReadingWithUnknownUnitFails() {
        CddlSchema schema = CddlValidator.loadSchema(SENSOR_CDDL);

        CBORValue reading = buildSensorReading("sensor-001", 1_700_000_000L, 23.5, "kelvin");

        byte[] cbor = reading.encodeToBytes();

        ValidationResult result = schema.validate(cbor);

        assertFalse(result.isValid());
    }

    @Test
    void sensorReadingWithMissingRequiredFieldFails() {
        CddlSchema schema = CddlValidator.loadSchema(SENSOR_CDDL);

        // Missing "value".
        CBORValue reading = CBOR.map()
                                .put("device-id", "x")
                                .put("timestamp", 0L)
                                .put("unit", "celsius")
                                .value();

        byte[] cbor = reading.encodeToBytes();

        ValidationResult result = schema.validate(cbor);
        assertFalse(result.isValid());
    }

    // -------------------------------------------------------------------
    // CoAP-style message schema
    // -------------------------------------------------------------------

    private static final String COAP_CDDL =
            String.join(
                    "\n",
                    "coap-message = [",
                    "  version: 0..3,",
                    "  type: 0..3,",
                    "  code: uint,",
                    "  message-id: uint,",
                    "  ? token: bstr,",
                    "  * uint",
                    "]");

    @Test
    void validCoapMessagePasses() {
        CddlSchema schema = CddlValidator.loadSchema(COAP_CDDL);

        CBORValue message = CBOR.array()
                                .add(1)
                                .add(0)
                                .add(1)
                                .add(0x1234)
                                .value();

        byte[] cbor = message.encodeToBytes();

        ValidationResult result = schema.validate(cbor);
        assertTrue(result.isValid(), "Expected valid but got: " + result.getErrors());
    }

    @Test
    void coapMessageVersionOutOfRangeFails() {
        CddlSchema schema = CddlValidator.loadSchema(COAP_CDDL);

        CBORValue message = CBOR.array()
                                .add(4)
                                .add(0)
                                .add(1)
                                .add(0x1)
                                .value();

        byte[] cbor = message.encodeToBytes();

        ValidationResult result = schema.validate(cbor);
        assertFalse(result.isValid());
    }

    // -------------------------------------------------------------------
    // Convenience static factory
    // -------------------------------------------------------------------

    @Test
    void staticConvenienceMethodValid() {
        String cddl = "root = { x: uint, y: uint }";

        CBORValue point = CBOR.map()
                                .put("x", 1)
                                .put("y", 2)
                                .value();

        byte[] cbor = point.encodeToBytes();

        ValidationResult result = CddlValidator.validate(cddl, cbor);
        assertTrue(result.isValid());
    }

    @Test
    void validateSpecificRule() {
        CddlSchema schema = CddlValidator.loadSchema(
                        "message = { id: uint, body: tstr }\n"
                                + "id-only = uint");

        CBORValue id = CBOR.integer(99);

        byte[] cbor = id.encodeToBytes();

        ValidationResult result = schema.validate(cbor, "id-only");
        assertTrue(result.isValid());
    }

    @Test
    void malformedCborBytesProduceError() {
        CddlSchema schema = CddlValidator.loadSchema("root = uint");

        byte[] garbage = { (byte) 0xFF, (byte) 0xFE, (byte) 0xFD };

        ValidationResult result = schema.validate(garbage);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getMessage()
                        .toLowerCase(java.util.Locale.ROOT)
                        .contains("decode"));
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private static CBORValue buildSensorReading(String id, long timestamp, double value, String unit) {
        return CBOR.map()
                .put("device-id", id)
                .put("timestamp", timestamp)
                .put("value", value)
                .put("unit", unit)
                .value();
    }

}