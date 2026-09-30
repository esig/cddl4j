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
import eu.europa.ec.digit.cddl4j.validator.schema.type.ChoiceType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.GroupType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.MapType;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupValidatorTest {

    @Test
    void parsesParenthesizedGroup() {
        // A parenthesized map-like definition is parsed as a group.
        CddlSchema schema = CddlValidator.loadSchema(
                "Headers = (\n"
                    + "    protected : empty_or_serialized_map,\n"
                    + "    unprotected : header_map\n"
                    + ")");

        GroupType group = assertInstanceOf(GroupType.class, schema.getRuleSet().getRule("Headers"));
        assertEquals(2, group.getEntries().size());
        assertEquals("protected", group.getEntries().get(0).getBareWordKey());
        assertEquals("unprotected", group.getEntries().get(1).getBareWordKey());
    }

    @Test
    void parsesParenthesizedType() {
        // A normal parenthesized type remains a type expression.
        CddlSchema schema = CddlValidator.loadSchema(
                "value = (uint / tstr)");
        assertInstanceOf(ChoiceType.class, schema.getRuleSet().getRule("value"));
    }

    @Test
    void parsesGenericHeadersGroup() {
        // RFC 8152 generic headers are parsed as a group.
        CddlSchema schema = CddlValidator.loadSchema(
                "Generic_Headers = (\n"
                    + "    ? 1 => int / tstr,\n"
                    + "    ? 2 => [+label],\n"
                    + "    ? 3 => tstr / int,\n"
                    + "    ? 4 => bstr,\n"
                    + "    ? 5 => bstr,\n"
                    + "    ? 6 => bstr,\n"
                    + "    ? 7 => COSE_Signature / [+COSE_Signature]\n"
                    + ")");

        GroupType group = assertInstanceOf(GroupType.class, schema.getRuleSet().getRule("Generic_Headers"));
        assertEquals(7, group.getEntries().size());
        assertTrue(group.getEntries().get(0).isOptional());
        assertEquals(1, group.getEntries().get(0).getKeyType()
                .toString().equals("1") ? 1 : 0);
    }

    @Test
    void parsesNamedGroupInsideMap() {
        // A parenthesized group can be referenced from a map group.
        CddlSchema schema = CddlValidator.loadSchema(
                "Generic_Headers = (\n"
                    + "    ? 1 => int / tstr,\n"
                    + "    ? 2 => [+label]\n"
                    + ")\n"
                    + "header_map = {\n"
                    + "    Generic_Headers,\n"
                    + "    * label => values\n"
                    + "}");

        assertInstanceOf(GroupType.class, schema.getRuleSet().getRule("Generic_Headers"));
        assertInstanceOf(MapType.class, schema.getRuleSet().getRule("header_map"));
    }

    @Test
    void expandsNamedGroupInsideMap() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Generic_Headers = (\n"
                    + "    ? 1 => int / tstr,\n"
                    + "    ? 2 => [+label]\n"
                    + ")\n"
                    + "header_map = {\n"
                    + "    Generic_Headers,\n"
                    + "    * label => values\n"
                    + "}");

        assertInstanceOf(GroupType.class, schema.getRuleSet().getRule("Generic_Headers"));
        assertInstanceOf(MapType.class, schema.getRuleSet().getRule("header_map"));
    }

    @Test
    void headerMapIsRootAndGenericHeadersIsGroup() {
        CddlSchema schema = CddlValidator.loadSchema(
                "header_map = {\n"
                    + "    Generic_Headers,\n"
                    + "    * label => values\n"
                    + "}\n"
                    + "Generic_Headers = (\n"
                    + "    ? 1 => int / tstr,\n"
                    + "    ? 2 => [+label]\n"
                    + ")\n"
                    + "label = int / tstr\n"
                    + "values = any");
        assertInstanceOf(GroupType.class, schema.getRuleSet().getRule("Generic_Headers"));
    }

    @Test
    void simpleMapGroupPass() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = {\n"
                + "        age: int,\n"
                + "        name: tstr,\n"
                + "        employer: tstr\n"
                + "    }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("age"), CBOR.integer(42))
                .put(CBOR.value("name"), CBOR.text("John"))
                .put(CBOR.value("employer"), CBOR.text("Nowina"))
                .value();

        assertValid(schema, value);
    }

    @Test
    void simpleMapGroupsFail() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = {\n"
                + "        age: int,\n"
                + "        name: tstr,\n"
                + "        employer: tstr\n"
                + "    }");

        CBORValue value = CBOR.map()
                .put(CBOR.value("age"), CBOR.text("42"))
                .put(CBOR.value("name"), CBOR.text("John"))
                .put(CBOR.value("employer"), CBOR.text("Nowina"))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void groupAloneTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "pii = (\n"
                + "        age: int,\n"
                + "        name: tstr,\n"
                + "        employer: tstr\n"
                + "    )");

        CBORValue value = CBOR.map()
                .put(CBOR.value("age"), CBOR.integer(42))
                .put(CBOR.value("name"), CBOR.text("John"))
                .put(CBOR.value("employer"), CBOR.text("Nowina"))
                .value();

        Exception exception = assertThrows(UnsupportedOperationException.class, () -> assertValid(schema, value));
        assertEquals("A CDDL group must be used inside an array or map context. " +
                "Rule : (age: int, name: tstr, employer: tstr)", exception.getMessage());
    }

    @Test
    void groupInMapPass() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = {\n"
                + "        pii\n"
                + "    }"
                + "    pii = (\n"
                + "        age: int,\n"
                + "        name: tstr,\n"
                + "        employer: tstr\n"
                + "    )");

        CBORValue value = CBOR.map()
                .put(CBOR.value("age"), CBOR.integer(42))
                .put(CBOR.value("name"), CBOR.text("John"))
                .put(CBOR.value("employer"), CBOR.text("Nowina"))
                .value();

        assertValid(schema, value);
    }

    @Test
    void groupInMapFail() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = {\n"
                + "        pii\n"
                + "    }"
                + "    pii = (\n"
                + "        age: int,\n"
                + "        name: tstr,\n"
                + "        employer: tstr\n"
                + "    )");

        CBORValue value = CBOR.map()
                .put(CBOR.value("age"), CBOR.text("42"))
                .put(CBOR.value("name"), CBOR.text("John"))
                .put(CBOR.value("employer"), CBOR.text("Nowina"))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void groupInArrayPass() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = [ pii ]\n"
                + "    pii = (\n"
                + "        age: int,\n"
                + "        name: tstr,\n"
                + "        employer: tstr\n"
                + "    )");

        CBORValue value = CBOR.array()
                .add(CBOR.integer(42))
                .add(CBOR.text("John"))
                .add(CBOR.text("Nowina"))
                .value();

        assertValid(schema, value);
    }

    @Test
    void groupInArrayFail() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = [ pii ]\n"
                + "    pii = (\n"
                + "        age: int,\n"
                + "        name: tstr,\n"
                + "        employer: tstr\n"
                + "    )");

        CBORValue value = CBOR.map()
                .put(CBOR.value("age"), CBOR.integer(42))
                .put(CBOR.value("name"), CBOR.text("John"))
                .put(CBOR.value("employer"), CBOR.text("Nowina"))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void groupInsideMapPass() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = {(\n"
                + "        age: int,\n"
                + "        name: tstr,\n"
                + "        employer: tstr\n"
                + "    )}");

        CBORValue value = CBOR.map()
                .put(CBOR.value("age"), CBOR.integer(42))
                .put(CBOR.value("name"), CBOR.text("John"))
                .put(CBOR.value("employer"), CBOR.text("Nowina"))
                .value();

        assertValid(schema, value);
    }

    @Test
    void groupInsideMapFail() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = {(\n"
                + "        age: int,\n"
                + "        name: tstr,\n"
                + "        employer: tstr\n"
                + "    )}");

        CBORValue value = CBOR.map()
                .put(CBOR.value("age"), CBOR.text("42"))
                .put(CBOR.value("name"), CBOR.text("John"))
                .put(CBOR.value("employer"), CBOR.text("Nowina"))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void groupInsideArrayPass() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = [(\n"
                + "        age: int,\n"
                + "        name: tstr,\n"
                + "        employer: tstr\n"
                + "    )]");

        CBORValue value = CBOR.array()
                .add(CBOR.integer(42))
                .add(CBOR.text("John"))
                .add(CBOR.text("Nowina"))
                .value();

        assertValid(schema, value);
    }

    @Test
    void groupInsideArrayFail() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = [(\n"
                + "        age: int,\n"
                + "        name: tstr,\n"
                + "        employer: tstr\n"
                + "    )]");

        CBORValue value = CBOR.array()
                .add(CBOR.text("42"))
                .add(CBOR.text("John"))
                .add(CBOR.text("Nowina"))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void namedGroupInsideMapValidatesActualValues() {
        CddlSchema schema = CddlValidator.loadSchema(
                "header_map = {\n"
                    + "    Generic_Headers,\n"
                    + "    * label => values\n"
                    + "}\n"
                    + "Generic_Headers = (\n"
                    + "    ? 1 => int / tstr,\n"
                    + "    ? 2 => [+label]\n"
                    + ")\n"
                    + "label = int / tstr\n"
                    + "values = any");

        CBORValue value = CBOR.map()
                .put(CBOR.value(1), CBOR.value(42))
                .put(
                        CBOR.value(2),
                        CBOR.array()
                                .add(CBOR.value("content-type"))
                                .add(CBOR.value("accept"))
                                .value()
                )
                .put(
                        CBOR.value("custom"),
                        CBOR.value("custom-value")
                )
                .value();

        assertValid(schema, value);
    }

    @Test
    void namedGroupInsideMapAcceptWildcardTypeForGenericHeader() {
        CddlSchema schema = CddlValidator.loadSchema(
                "header_map = {\n"
                    + "    Generic_Headers,\n"
                    + "    * label => values\n"
                    + "}\n"
                    + "Generic_Headers = (\n"
                    + "    ? 1 => int / tstr,\n"
                    + "    ? 2 => [+label]\n"
                    + ")\n"
                    + "label = int / tstr\n"
                    + "values = any");

        CBORValue value = CBOR.map()
                // Header 1 only accepts int / tstr.
                .put(CBOR.value(1), CBOR.value(true))
                .value();

        assertValid(schema, value); // Groups handling is aligned with RFC 8610 "3.5.4. Cuts in Maps".
    }

    @Test
    void namedGroupInsideMapAcceptWildcardLabelArray() {
        CddlSchema schema = CddlValidator.loadSchema(
                "header_map = {\n"
                    + "    Generic_Headers,\n"
                    + "    * label => values\n"
                    + "}\n"
                    + "Generic_Headers = (\n"
                    + "    ? 1 => int / tstr,\n"
                    + "    ? 2 => [+label]\n"
                    + ")\n"
                    + "label = int / tstr\n"
                    + "values = any");

        CBORValue value = CBOR.map()
                .put(
                        CBOR.value(2),
                        CBOR.array()
                                // 123 is a valid label because label = int / tstr.
                                .add(CBOR.value(123))
                                // Boolean is not a valid label.
                                .add(CBOR.value(true))
                                .value()
                )
                .value();

        assertValid(schema, value);
    }

    @Test
    void namedGroupOptionalEntriesMayBeAbsent() {
        CddlSchema schema = CddlValidator.loadSchema(
                "header_map = {\n"
                    + "    Generic_Headers,\n"
                    + "    * label => values\n"
                    + "}\n"
                    + "Generic_Headers = (\n"
                    + "    ? 1 => int / tstr,\n"
                    + "    ? 2 => [+label]\n"
                    + ")\n"
                    + "label = int / tstr\n"
                    + "values = any");

        // Both Generic_Headers entries are optional.
        CBORValue value = CBOR.map()
                .put(CBOR.value("custom"), CBOR.value("value"))
                .value();

        assertValid(schema, value);
    }

    @Test
    void namedGroupOptionalIntegerOrTextHeaderIsValid() {
        CddlSchema schema = CddlValidator.loadSchema(
                "header_map = {\n"
                    + "    Generic_Headers,\n"
                    + "    * label => values\n"
                    + "}\n"
                    + "Generic_Headers = (\n"
                    + "    ? 1 => int / tstr,\n"
                    + "    ? 2 => [+label]\n"
                    + ")\n"
                    + "label = int / tstr\n"
                    + "values = any");

        assertValid(
                schema,
                CBOR.map()
                        .put(CBOR.value(1), CBOR.value("text-value"))
                        .value()
        );

        assertValid(
                schema,
                CBOR.map()
                        .put(CBOR.value(1), CBOR.value(123))
                        .value()
        );
    }

    @Test
    void namedGroupLabelArrayAcceptWildcardLabel() {
        CddlSchema schema = CddlValidator.loadSchema(
                "header_map = {\n"
                    + "    Generic_Headers,\n"
                    + "    * label => values\n"
                    + "}\n"
                    + "Generic_Headers = (\n"
                    + "    ? 1 => int / tstr,\n"
                    + "    ? 2 => [+label]\n"
                    + ")\n"
                    + "label = int / tstr\n"
                    + "values = any");

        // [+label] requires one or more labels.
        CBORValue value = CBOR.map()
                .put(
                        CBOR.value(2),
                        CBOR.array().value()
                )
                .value();

        assertValid(schema, value);
    }

    @Test
    void wildcardMapEntryAcceptsAdditionalLabels() {
        CddlSchema schema = CddlValidator.loadSchema(
                "header_map = {\n"
                    + "    Generic_Headers,\n"
                    + "    * label => values\n"
                    + "}\n"
                    + "Generic_Headers = (\n"
                    + "    ? 1 => int / tstr,\n"
                    + "    ? 2 => [+label]\n"
                    + ")\n"
                    + "label = int / tstr\n"
                    + "values = any");

        CBORValue value = CBOR.map()
                .put(CBOR.value(1), CBOR.value(42))
                .put(CBOR.value(2),
                        CBOR.array()
                                .add(CBOR.value(10))
                                .add(CBOR.value("label"))
                                .value())
                .put(CBOR.value(100), CBOR.value("some-value"))
                .put(CBOR.value("custom"), CBOR.value(true))
                .value();

        assertValid(schema, value);

    }

    @Test
    void wildcardMapEntryAcceptsLabelKey() {
        CddlSchema schema = CddlValidator.loadSchema(
                "header_map = {\n"
                    + "    Generic_Headers,\n"
                    + "    * label => values\n"
                    + "}\n"
                    + "Generic_Headers = (\n"
                    + "    ? 1 => int / tstr,\n"
                    + "    ? 2 => [+label]\n"
                    + ")\n"
                    + "label = int / tstr\n"
                    + "values = any");

        CBORValue value = CBOR.map()
                .put(CBOR.value(123), CBOR.value("integer-label"))
                .put(CBOR.value("custom"), CBOR.value("text-label"))
                .value();

        assertValid(schema, value);
    }

    @Test
    void wildcardMapEntryRejectsNonLabelKey() {
        CddlSchema schema = CddlValidator.loadSchema(
                "header_map = {\n"
                    + "    Generic_Headers,\n"
                    + "    * label => values\n"
                    + "}\n"
                    + "Generic_Headers = (\n"
                    + "    ? 1 => int / tstr,\n"
                    + "    ? 2 => [+label]\n"
                    + ")\n"
                    + "label = int / tstr\n"
                    + "values = any");

        CBORValue value = CBOR.map()
                .put(CBOR.value(true), CBOR.value("not-a-valid-label"))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void arrayExpandsGroupReference() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Test = [ Headers, payload : bstr, signature : bstr ]\n"
                        + "Headers = (\n"
                        + "    protected : bstr,\n"
                        + "    unprotected : bstr\n"
                        + ")");

        CBORValue value = CBOR.array()
                .add(CBOR.value(new byte[] {1})) // Headers.protected
                .add(CBOR.value(new byte[] {2})) // Headers.unprotected
                .add(CBOR.value(new byte[] {3})) // payload
                .add(CBOR.value(new byte[] {4})) // signature
                .value();

        assertValid(schema, value);
    }

    @Test
    void arrayRejectsInvalidValueAfterExpandedGroup() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Test = [ Headers, payload : bstr, signature : bstr ]\n"
                        + "Headers = (\n"
                        + "    protected : bstr,\n"
                        + "    unprotected : bstr\n"
                        + ")");

        CBORValue value = CBOR.array()
                .add(CBOR.value(new byte[] {1}))
                .add(CBOR.value(new byte[] {2}))
                .add(CBOR.value(123)) // payload must be bstr
                .add(CBOR.value(new byte[] {3}))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void arrayExpandsGroupReferenceTransitive() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Test = [ Headers, payload : bstr, signature : bstr ]\n"
                        + "Headers = Headers_New\n"
                        + "Headers_New = (\n"
                        + "    protected : bstr,\n"
                        + "    unprotected : bstr\n"
                        + ")");

        CBORValue value = CBOR.array()
                .add(CBOR.value(new byte[] {1})) // Headers.protected
                .add(CBOR.value(new byte[] {2})) // Headers.unprotected
                .add(CBOR.value(new byte[] {3})) // payload
                .add(CBOR.value(new byte[] {4})) // signature
                .value();

        assertValid(schema, value);
    }

    @Test
    void arrayExpandsGroupReferenceTransitiveInvalid() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Test = [ Headers, payload : bstr, signature : bstr ]\n"
                        + "Headers = Headers_New\n"
                        + "Headers_New = (\n"
                        + "    protected : bstr,\n"
                        + "    unprotected : bstr\n"
                        + ")");

        CBORValue value = CBOR.array()
                .add(CBOR.value("String")) // Headers.protected
                .add(CBOR.value(new byte[] {2})) // Headers.unprotected
                .add(CBOR.value(new byte[] {3})) // payload
                .add(CBOR.value(new byte[] {4})) // signature
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void arrayMatchesRepeatedTransitiveGroupReference() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Test = [ * Headers, payload : bstr ]\n"
                        + "Headers = Headers_New\n"
                        + "Headers_New = (\n"
                        + "    protected : bstr,\n"
                        + "    unprotected : bstr\n"
                        + ")");

        CBORValue value = CBOR.array()
                .add(CBOR.value(new byte[] {1}))
                .add(CBOR.value(new byte[] {2}))
                .add(CBOR.value(new byte[] {3}))
                .add(CBOR.value(new byte[] {4}))
                .add(CBOR.value(new byte[] {5}))
                .value();

        assertValid(schema, value);
    }

    @Test
    void arrayKeepsTransitiveTypeReferenceAsValue() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Test = [ payload : Payload ]\n"
                        + "Payload = Payload_New\n"
                        + "Payload_New = bstr");

        assertValid(schema, CBOR.array()
                .add(CBOR.value(new byte[] {1}))
                .value());

        assertInvalid(schema, CBOR.array()
                .add(CBOR.integer(1))
                .value());
    }

    @Test
    void mapExpandsTransitiveGroupReference() {
        CddlSchema schema = CddlValidator.loadSchema(
                "header_map = { Headers }\n"
                        + "Headers = Headers_New\n"
                        + "Headers_New = ( protected : bstr, unprotected : bstr )");

        CBORValue value = CBOR.map()
                .put(CBOR.text("protected"), CBOR.value(new byte[] {1}))
                .put(CBOR.text("unprotected"), CBOR.value(new byte[] {2}))
                .value();

        assertValid(schema, value);
    }

    @Test
    void mapExpandsTransitiveGroupReferenceInvalid() {
        CddlSchema schema = CddlValidator.loadSchema(
                "header_map = { Headers }\n"
                        + "Headers = Headers_New\n"
                        + "Headers_New = ( protected : bstr, unprotected : bstr )");

        CBORValue value = CBOR.map()
                .put(CBOR.text("protected"), CBOR.value("1"))
                .put(CBOR.text("unprotected"), CBOR.value(new byte[] {2}))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void mapExpandsMultipleTransitiveGroupReference() {
        CddlSchema schema = CddlValidator.loadSchema(
                "header_map = { Headers }\n"
                        + "Headers = Headers_New\n"
                        + "Headers_New = Headers_New_2\n"
                        + "Headers_New_2 = ( protected : bstr, unprotected : bstr )");

        CBORValue value = CBOR.map()
                .put(CBOR.text("protected"), CBOR.value(new byte[] {1}))
                .put(CBOR.text("unprotected"), CBOR.value(new byte[] {2}))
                .value();

        assertValid(schema, value);
    }

    @Test
    void mapExpandsMultipleTransitiveGroupReferenceInvalid() {
        CddlSchema schema = CddlValidator.loadSchema(
                "header_map = { Headers }\n"
                        + "Headers = Headers_New\n"
                        + "Headers_New = Headers_New_2\n"
                        + "Headers_New_2 = ( protected : bstr, unprotected : bstr )");

        CBORValue value = CBOR.map()
                .put(CBOR.text("protected"), CBOR.value("1"))
                .put(CBOR.text("unprotected"), CBOR.value(new byte[] {2}))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void coseHeadersAcceptAdditionalIntegerHeader() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Test = [\n"
                        + "    Headers,\n"
                        + "    payload : bstr / nil,\n"
                        + "    signature : bstr\n"
                        + "]\n"
                        + "Headers = (\n"
                        + "    protected : bstr,\n"
                        + "    unprotected : header_map\n"
                        + ")\n"
                        + "header_map = {\n"
                        + "    Generic_Headers,\n"
                        + "    * label => values\n"
                        + "}\n"
                        + "Generic_Headers = (\n"
                        + "    ? 1 => int / tstr,\n"
                        + "    ? 2 => [+label],\n"
                        + "    ? 3 => tstr / int,\n"
                        + "    ? 4 => bstr\n"
                        + ")\n"
                        + "label = int / tstr\n"
                        + "values = any");

        CBORValue value = CBOR.array()
                .add(CBOR.value(new byte[] {1}))
                .add(CBOR.map()
                        .put(CBOR.value(33), CBOR.value(new byte[] {2}))
                        .value())
                .add(CBOR.value(new byte[] {3}))
                .add(CBOR.value(new byte[] {4}))
                .value();

        assertValid(schema, value);
    }

    @Test
    void coseHeadersRejectNonLabelAdditionalHeader() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Test = [\n"
                        + "    Headers,\n"
                        + "    payload : bstr / nil,\n"
                        + "    signature : bstr\n"
                        + "]\n"
                        + "Headers = (\n"
                        + "    protected : bstr,\n"
                        + "    unprotected : header_map\n"
                        + ")\n"
                        + "header_map = {\n"
                        + "    Generic_Headers,\n"
                        + "    * label => values\n"
                        + "}\n"
                        + "Generic_Headers = (\n"
                        + "    ? 1 => int / tstr,\n"
                        + "    ? 2 => [+label],\n"
                        + "    ? 3 => tstr / int,\n"
                        + "    ? 4 => bstr\n"
                        + ")\n"
                        + "label = int / tstr\n"
                        + "values = any");

        CBORValue value = CBOR.array()
                .add(CBOR.value(new byte[] {1}))
                .add(CBOR.map()
                        .put(CBOR.value(true), CBOR.value("invalid-key"))
                        .value())
                .add(CBOR.value(new byte[] {3}))
                .add(CBOR.value(new byte[] {4}))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void coseSign1AcceptsNilPayload() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Test = [\n"
                        + "    Headers,\n"
                        + "    payload : bstr / nil,\n"
                        + "    signature : bstr\n"
                        + "]\n"
                        + "Headers = (\n"
                        + "    protected : bstr,\n"
                        + "    unprotected : header_map\n"
                        + ")\n"
                        + "header_map = {\n"
                        + "    * label => values\n"
                        + "}\n"
                        + "label = int / tstr\n"
                        + "values = any");

        CBORValue value = CBOR.array()
                .add(CBOR.value(new byte[] {1}))
                .add(CBOR.map().value())
                .add(CBOR.nullValue())
                .add(CBOR.value(new byte[] {3}))
                .value();

        assertValid(schema, value);
    }

    @Test
    void coseSign1RejectsNonBstrSignature() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Test = [\n"
                        + "    Headers,\n"
                        + "    payload : bstr / nil,\n"
                        + "    signature : bstr\n"
                        + "]\n"
                        + "Headers = (\n"
                        + "    protected : bstr,\n"
                        + "    unprotected : header_map\n"
                        + ")\n"
                        + "header_map = {\n"
                        + "    * label => values\n"
                        + "}\n"
                        + "label = int / tstr\n"
                        + "values = any");

        CBORValue value = CBOR.array()
                .add(CBOR.value(new byte[] {1}))
                .add(CBOR.map().value())
                .add(CBOR.value(new byte[] {2}))
                .add(CBOR.value(123)) // signature must be bstr
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void coseSign1ExpandsHeadersAndValidatesPayloadAndSignature() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Test = [\n"
                        + "    Headers,\n"
                        + "    payload : bstr / nil,\n"
                        + "    signature : bstr\n"
                        + "]\n"
                        + "Headers = (\n"
                        + "    protected : empty_or_serialized_map,\n"
                        + "    unprotected : header_map\n"
                        + ")\n"
                        + "empty_or_serialized_map = bstr\n"
                        + "header_map = {\n"
                        + "    Generic_Headers,\n"
                        + "    * label => values\n"
                        + "}\n"
                        + "Generic_Headers = (\n"
                        + "    ? 1 => int / tstr,\n"
                        + "    ? 2 => [+label],\n"
                        + "    ? 3 => tstr / int,\n"
                        + "    ? 4 => bstr,\n"
                        + "    ? 5 => bstr,\n"
                        + "    ? 6 => bstr,\n"
                        + "    ? 7 => bstr\n"
                        + ")\n"
                        + "label = int / tstr\n"
                        + "values = any");

        CBORValue value = CBOR.array()
                // Headers.protected
                .add(CBOR.value(new byte[] {1, 2, 3}))
                // Headers.unprotected
                .add(CBOR.map()
                        .put(CBOR.value(33), CBOR.value(new byte[] {4, 5, 6}))
                        .value())
                // payload
                .add(CBOR.value(new byte[] {7, 8, 9}))
                // signature
                .add(CBOR.value(new byte[] {10, 11, 12}))
                .value();

        assertValid(schema, value);
    }

    @Test
    void coseSign1RejectsInvalidHeaderKey() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Test = [\n"
                        + "    Headers,\n"
                        + "    payload : bstr / nil,\n"
                        + "    signature : bstr\n"
                        + "]\n"
                        + "Headers = (\n"
                        + "    protected : empty_or_serialized_map,\n"
                        + "    unprotected : header_map\n"
                        + ")\n"
                        + "empty_or_serialized_map = bstr\n"
                        + "header_map = {\n"
                        + "    * label => values\n"
                        + "}\n"
                        + "label = int / tstr\n"
                        + "values = any");

        CBORValue value = CBOR.array()
                .add(CBOR.value(new byte[] {1}))
                .add(CBOR.map()
                        .put(CBOR.value(true), CBOR.value("bad"))
                        .value())
                .add(CBOR.value(new byte[] {2}))
                .add(CBOR.value(new byte[] {3}))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void coseSign1RejectsInvalidPayloadAndSignatureTypes() {
        CddlSchema schema = CddlValidator.loadSchema(
                "Test = [\n"
                        + "    Headers,\n"
                        + "    payload : bstr / nil,\n"
                        + "    signature : bstr\n"
                        + "]\n"
                        + "Headers = (\n"
                        + "    protected : empty_or_serialized_map,\n"
                        + "    unprotected : header_map\n"
                        + ")\n"
                        + "empty_or_serialized_map = bstr\n"
                        + "header_map = {\n"
                        + "    * label => values\n"
                        + "}\n"
                        + "label = int / tstr\n"
                        + "values = any");

        CBORValue value = CBOR.array()
                .add(CBOR.value(new byte[] {1}))
                .add(CBOR.map().value())
                .add(CBOR.value("payload must not be tstr"))
                .add(CBOR.value(123))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void commentFirstTest() {
        CddlSchema schema = CddlValidator.loadSchema(
                "; This is a comment\n"
                        + "person = { g },\n"
                        + "g = (\n"
                        + "  \"name\": tstr,\n"
                        + "  age: int,  ; \"age\" is a bareword\n"
                        + ")");

        CBORValue value = CBOR.map()
                .put(CBOR.text("name"), CBOR.text("John"))
                .put(CBOR.text("age"), CBOR.integer(42))
                .value();

        assertValid(schema, value);
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
