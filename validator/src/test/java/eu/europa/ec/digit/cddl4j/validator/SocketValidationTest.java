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
 * Tests the RFC 8610 Section 3.9 socket/plug example.
 */
class SocketValidationTest {

    /** Complete RFC 8610 Section 3.9 example, represented as one schema. */
    private static final String TCP_SCHEMA =
            "tcp-header = {seq: uint, ack: uint, * $$tcp-option}\n"
                    + "\n"
                    + "; later, in a different file\n"
                    + "\n"
                    + "$$tcp-option //= (\n"
                    + "    sack: [+(left: uint, right: uint)]\n"
                    + ")\n"
                    + "\n"
                    + "; and, maybe in another file\n"
                    + "\n"
                    + "$$tcp-option //= (\n"
                    + "    sack-permitted: true\n"
                    + ")\n";

    @Test
    void tcpHeaderAcceptsSocketPlugOptions() {
        CddlSchema schema = CddlValidator.loadSchema(TCP_SCHEMA);

        assertValid(schema, CBOR.map()
                .put(CBOR.text("seq"), CBOR.integer(100))
                .put(CBOR.text("ack"), CBOR.integer(200))
                .value());

        assertValid(schema, CBOR.map()
                .put(CBOR.text("seq"), CBOR.integer(100))
                .put(CBOR.text("ack"), CBOR.integer(200))
                .put(CBOR.text("sack"), CBOR.array()
                        .add(CBOR.integer(10)).add(CBOR.integer(20))
                        .add(CBOR.integer(30)).add(CBOR.integer(40))
                        .value())
                .value());

        assertValid(schema, CBOR.map()
                .put(CBOR.text("seq"), CBOR.integer(100))
                .put(CBOR.text("ack"), CBOR.integer(200))
                .put(CBOR.text("sack-permitted"), CBOR.bool(true))
                .value());

        assertValid(schema, CBOR.map()
                .put(CBOR.text("seq"), CBOR.integer(100))
                .put(CBOR.text("ack"), CBOR.integer(200))
                .put(CBOR.text("sack"), CBOR.array()
                        .add(CBOR.integer(10)).add(CBOR.integer(20))
                        .value())
                .put(CBOR.text("sack-permitted"), CBOR.bool(true))
                .value());
    }

    @Test
    void tcpHeaderRejectsInvalidSocketPlugValues() {
        CddlSchema schema = CddlValidator.loadSchema(TCP_SCHEMA);

        assertInvalid(schema, CBOR.map()
                .put(CBOR.text("seq"), CBOR.integer(100))
                .put(CBOR.text("ack"), CBOR.integer(200))
                .put(CBOR.text("sack"), CBOR.array()
                        .add(CBOR.integer(10))
                        .value())
                .value());

        assertInvalid(schema, CBOR.map()
                .put(CBOR.text("seq"), CBOR.integer(100))
                .put(CBOR.text("ack"), CBOR.integer(200))
                .put(CBOR.text("sack-permitted"), CBOR.bool(false))
                .value());

        assertInvalid(schema, CBOR.map()
                .put(CBOR.text("seq"), CBOR.integer(100))
                .put(CBOR.text("ack"), CBOR.integer(200))
                .put(CBOR.text("unknown-option"), CBOR.bool(true))
                .value());
    }

    @Test
    void undefinedGroupSocketCanBeEmpty() {
        CddlSchema schema = CddlValidator.loadSchema(
                "tcp-header = {seq: uint, ack: uint, * $$tcp-option}");

        assertValid(schema, CBOR.map()
                .put(CBOR.text("seq"), CBOR.integer(100))
                .put(CBOR.text("ack"), CBOR.integer(200))
                .value());
    }

    @Test
    void undefinedGroupSocketAtLeastOneTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "tcp-header = {seq: uint, ack: uint, + $$tcp-option}");

        assertInvalid(schema, CBOR.map()
                .put(CBOR.text("seq"), CBOR.integer(100))
                .put(CBOR.text("ack"), CBOR.integer(200))
                .value());
    }

    @Test
    void undefinedGroupSocketCannotBeEmpty() {
        CddlSchema schema = CddlValidator.loadSchema(
                "tcp-header = {seq: uint, ack: uint, + $$tcp-option}\n"
                        + "\n"
                        + "; later, in a different file\n"
                        + "\n"
                        + "$$tcp-option //= (\n"
                        + "    sack: [+(left: uint, right: uint)]\n"
                        + ")\n"
                        + "\n"
                        + "; and, maybe in another file\n"
                        + "\n"
                        + "$$tcp-option //= (\n"
                        + "    sack-permitted: true\n"
                        + ")\n");

        assertValid(schema, CBOR.map()
                .put(CBOR.text("seq"), CBOR.integer(100))
                .put(CBOR.text("ack"), CBOR.integer(200))
                .put(CBOR.text("sack"), CBOR.array()
                        .add(CBOR.integer(10)).add(CBOR.integer(20))
                        .add(CBOR.integer(30)).add(CBOR.integer(40))
                        .value())
                .value());

        assertValid(schema, CBOR.map()
                .put(CBOR.text("seq"), CBOR.integer(100))
                .put(CBOR.text("ack"), CBOR.integer(200))
                .put(CBOR.text("sack-permitted"), CBOR.bool(true))
                .value());

        assertValid(schema, CBOR.map()
                .put(CBOR.text("seq"), CBOR.integer(100))
                .put(CBOR.text("ack"), CBOR.integer(200))
                .put(CBOR.text("sack"), CBOR.array()
                        .add(CBOR.integer(10)).add(CBOR.integer(20))
                        .value())
                .put(CBOR.text("sack-permitted"), CBOR.bool(true))
                .value());

        assertInvalid(schema, CBOR.map()
                .put(CBOR.text("seq"), CBOR.integer(100))
                .put(CBOR.text("ack"), CBOR.integer(200))
                .value());
    }

    private static void assertValid(CddlSchema schema, CBORValue value) {
        ValidationResult result = schema.validate(value);
        assertTrue(result.isValid(), "Expected valid value, got: " + result.getErrors());
    }

    private static void assertInvalid(CddlSchema schema, CBORValue value) {
        ValidationResult result = schema.validate(value);
        assertFalse(result.isValid(), "Expected invalid value");
    }

}
