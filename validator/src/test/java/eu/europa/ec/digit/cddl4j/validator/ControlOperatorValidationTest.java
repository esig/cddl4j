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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlOperatorValidationTest {

    private static final String CBOR_SCHEMA =
            "root = bstr .cbor issuerSignedItem\n"
                    + "issuerSignedItem = {\n"
                    + "    \"digestID\" : uint,\n"
                    + "    \"random\" : bstr,\n"
                    + "    \"elementIdentifier\" : tstr,\n"
                    + "    \"elementValue\" : any\n"
                    + "}";

    private static final String CBORSEQ_SCHEMA =
            "root = bstr .cborseq items\n"
                    + "items = [* item]\n"
                    + "item = { \"id\" : uint }";

    /*
     * .cbor tests
     */

    @Test
    void shouldAcceptValidCbor() {
        CddlSchema schema = new CddlParser().parse(
                "root = bstr .cbor uint"
        );

        CBORValue value = CBOR.value(new byte[] { 0x18, 0x2a });
        assertTrue(schema.validate(value).isValid());
    }

    @Test
    void shouldRejectInvalidCborType() {
        CddlSchema schema = new CddlParser().parse(
                "root = bstr .cbor uint"
        );

        /*
         * Encoded CBOR text string "hello".
         */
        CBORValue value = CBOR.value(new byte[] { 0x65, 0x68, 0x65, 0x6c, 0x6c, 0x6f });
        assertFalse(schema.validate(value).isValid());
    }

    @Test
    void shouldRejectMalformedCbor() {
        CddlSchema schema = new CddlParser().parse(
                "root = bstr .cbor uint"
        );

        /*
         * 0x18 means uint8 follows, but the value is missing.
         */
        CBORValue value = CBOR.value(new byte[] { 0x18 });
        assertFalse(schema.validate(value).isValid());
    }

    @Test
    void shouldRejectTrailingCborData() {
        CddlSchema schema = new CddlParser().parse(
                "root = bstr .cbor uint"
        );

        /*
         * Two CBOR data items:
         *
         * 0x01 -> 1
         * 0x02 -> 2
         *
         * .cbor must contain exactly one data item.
         */
        CBORValue value = CBOR.value(new byte[]{ 0x01, 0x02 });

        assertFalse(schema.validate(value).isValid());
    }

    @Test
    void shouldRejectNonByteStringTarget() {
        CddlSchema schema = new CddlParser().parse(
                "root = bstr .cbor uint"
        );

        /*
         * The outer value itself must be a byte string.
         */
        CBORValue value = CBOR.value(42);
        assertFalse(schema.validate(value).isValid());
    }

    @Test
    void shouldValidateNestedCborMap() {
        CddlSchema schema = new CddlParser().parse(CBOR_SCHEMA);

        CBORValue issuerSignedItem = CBOR.map()
                .put("digestID", 42)
                .put("random", new byte[] {0x01, 0x02, 0x03})
                .put("elementIdentifier", "given_name")
                .put("elementValue", "Alice")
                .value();

        CBORValue value = CBOR.bytes(issuerSignedItem.encodeToBytes());
        assertTrue(schema.validate(value).isValid());
    }

    @Test
    void shouldRejectNestedCborMapWithInvalidDigestId() {
        CddlSchema schema = new CddlParser().parse(CBOR_SCHEMA);

        CBORValue issuerSignedItem = CBOR.map()
                .put("digestID", "not-a-number")
                .put("random", new byte[] {0x01, 0x02, 0x03})
                .put("elementIdentifier", "given_name")
                .put("elementValue", "Alice")
                .value();

        CBORValue value = CBOR.bytes(issuerSignedItem.encodeToBytes());
        assertFalse(schema.validate(value).isValid());
    }

    @Test
    void shouldRejectNestedCborMapWithInvalidRandom() {
        CddlSchema schema = new CddlParser().parse(CBOR_SCHEMA);

        CBORValue issuerSignedItem = CBOR.map()
                .put("digestID", 42)
                .put("random", "not-a-byte-string")
                .put("elementIdentifier", "given_name")
                .put("elementValue", "Alice")
                .value();

        CBORValue value = CBOR.bytes(issuerSignedItem.encodeToBytes());
        assertFalse(schema.validate(value).isValid());
    }

    @Test
    void shouldRejectNestedCborMapWithInvalidElementIdentifier() {
        CddlSchema schema = new CddlParser().parse(CBOR_SCHEMA);

        CBORValue issuerSignedItem = CBOR.map()
                .put("digestID", 42)
                .put("random", new byte[] {0x01, 0x02, 0x03})
                .put("elementIdentifier", 123)
                .put("elementValue", "Alice")
                .value();

        CBORValue value = CBOR.bytes(issuerSignedItem.encodeToBytes());
        assertFalse(schema.validate(value).isValid());
    }

    @Test
    void shouldValidateNestedCborWithNestedArray() {
        CddlSchema schema = new CddlParser().parse(
                "root = bstr .cbor payload\n"
                        + "payload = {\n"
                        + "    \"id\" : uint,\n"
                        + "    \"names\" : [* tstr]\n"
                        + "}"
        );

        CBORValue payload = CBOR.map()
                .put("id", 42)
                .put("names", CBOR.array()
                        .add("Alice")
                        .add("Bob")
                        .add("Charlie")
                        .value()
                ).value();
        CBORValue value = CBOR.bytes(payload.encodeToBytes());
        assertTrue(schema.validate(value).isValid());
    }

    /*
     * .cborseq tests
     */

    @Test
    void shouldAcceptValidCborSequence() {
        CddlSchema schema = new CddlParser().parse(
                "root = bstr .cborseq values\n"
                        + "values = [* uint]"
        );

        /*
         * CBOR sequence:
         *
         * 1
         * 2
         * 3
         */
        CBORValue value = CBOR.value(new byte[] { 0x01, 0x02, 0x03 });
        assertTrue(schema.validate(value).isValid());
    }

    @Test
    void shouldAcceptEmptyCborSequence() {
        CddlSchema schema = new CddlParser().parse(
                "root = bstr .cborseq values\n"
                        + "values = [* uint]"
        );

        CBORValue value = CBOR.value(new byte[0]);
        assertTrue(schema.validate(value).isValid());
    }

    @Test
    void shouldRejectInvalidElementInCborSequence() {
        CddlSchema schema = new CddlParser().parse(
                "root = bstr .cborseq values\n"
                        + "values = [* uint]"
        );

        /*
         * CBOR sequence:
         *
         * 1
         * "hello"
         */
        CBORValue value = CBOR.value(new byte[] { 0x01, 0x65, 0x68, 0x65, 0x6c, 0x6c, 0x6f });
        assertFalse(schema.validate(value).isValid());
    }

    @Test
    void shouldRejectMalformedCborSequence() {
        CddlSchema schema = new CddlParser().parse(
                "root = bstr .cborseq values\n"
                        + "values = [* uint]"
        );

        /*
         * Valid uint followed by an incomplete uint encoding.
         */
        CBORValue value = CBOR.value(new byte[] { 0x01, 0x18 });
        assertFalse(schema.validate(value).isValid());
    }

    @Test
    void shouldRejectEmptySequenceWhenAtLeastOneElementIsRequired() {
        CddlSchema schema = new CddlParser().parse(
                "root = bstr .cborseq values\n"
                        + "values = [+ uint]"
        );

        CBORValue value = CBOR.value(new byte[0]);
        assertFalse(schema.validate(value).isValid());
    }

    @Test
    void shouldAcceptSequenceWithChoiceElements() {
        CddlSchema schema = new CddlParser().parse(
                "root = bstr .cborseq values\n"
                        + "values = [* (uint / tstr)]"
        );

        /*
         * CBOR sequence:
         *
         * 1
         * "hello"
         * 2
         */
        CBORValue value = CBOR.value(new byte[] { 0x01, 0x65, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x02 });
        assertTrue(schema.validate(value).isValid());
    }

    @Test
    void shouldValidateSequenceOfMaps() {
        CddlSchema schema = new CddlParser().parse(CBORSEQ_SCHEMA);

        CBORValue firstItem = CBOR.map()
                .put("id", 1)
                .value();

        CBORValue secondItem = CBOR.map()
                .put("id", 2)
                .value();

        CBORValue thirdItem = CBOR.map()
                .put("id", 3)
                .value();

        CBORValue value = CBOR.encodeSequence(firstItem, secondItem, thirdItem);
        assertTrue(schema.validate(value).isValid());
    }

    @Test
    void shouldRejectSequenceContainingInvalidMap() {
        CddlSchema schema = new CddlParser().parse(CBORSEQ_SCHEMA);

        CBORValue validItem = CBOR.map()
                .put("id", 1)
                .value();

        CBORValue invalidItem = CBOR.map()
                .put("id", "not-a-number")
                .value();

        CBORValue value = CBOR.encodeSequence(validItem, invalidItem);
        assertFalse(schema.validate(value).isValid());
    }

    @Test
    void shouldRejectSequenceContainingNonMap() {
        CddlSchema schema = new CddlParser().parse(CBORSEQ_SCHEMA);

        CBORValue validItem = CBOR.map()
                .put("id", 1)
                .value();

        CBORValue invalidItem = CBOR.text("not-a-map");

        CBORValue value = CBOR.encodeSequence(validItem, invalidItem);
        assertFalse(schema.validate(value).isValid());
    }

    @Test
    void shouldAcceptEmptySequence() {
        CddlSchema schema = new CddlParser().parse(CBORSEQ_SCHEMA);

        CBORValue value = CBOR.bytes(new byte[0]);
        assertTrue(schema.validate(value).isValid());
    }

    @Test
    void shouldValidateSequenceContainingNestedStructures() {
        CddlSchema schema = new CddlParser().parse(
                "root = bstr .cborseq items\n"
                        + "items = [* item]\n"
                        + "item = {\n"
                        + "    \"id\" : uint,\n"
                        + "    \"name\" : tstr,\n"
                        + "    \"tags\" : [* tstr]\n"
                        + "}"
        );

        CBORValue alice = CBOR.map()
                .put("id", 1)
                .put("name", "Alice")
                .put("tags", CBOR.array()
                        .add("admin")
                        .add("user")
                        .value()
                ).value();

        CBORValue bob = CBOR.map()
                .put("id", 2)
                .put("name", "Bob")
                .put("tags", CBOR.array()
                        .add("user")
                        .value()
                ).value();

        CBORValue value = CBOR.encodeSequence(alice, bob);
        assertTrue(schema.validate(value).isValid());
    }

    @Test
    void shouldRejectNestedInvalidStructureInSequence() {
        CddlSchema schema = new CddlParser().parse(
                "root = bstr .cborseq items\n"
                        + "items = [* item]\n"
                        + "item = {\n"
                        + "    \"id\" : uint,\n"
                        + "    \"name\" : tstr,\n"
                        + "    \"tags\" : [* tstr]\n"
                        + "}"
        );

        CBORValue validItem = CBOR.map()
                .put("id", 1)
                .put("name", "Alice")
                .put("tags", CBOR.array()
                        .add("admin")
                        .value()
                ).value();

        CBORValue invalidItem = CBOR.map()
                .put("id", "not-a-number")
                .put("name", "Bob")
                .put("tags", CBOR.array()
                        .add("user")
                        .value()
                ).value();

        CBORValue value = CBOR.encodeSequence(validItem, invalidItem);
        assertFalse(schema.validate(value).isValid());
    }

}
