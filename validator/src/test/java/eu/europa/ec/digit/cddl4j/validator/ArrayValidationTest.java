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
 * Tests for array type validation: positional entries and occurrence indicators.
 */
class ArrayValidationTest {

    @Test
    void simpleTupleValid() {
        CddlSchema schema = CddlValidator.loadSchema(
                "point = [uint, uint]");

        CBORValue array = CBOR.array()
                .add(10)
                .add(20)
                .value();

        assertValid(schema, array);
    }

    @Test
    void tupleWrongElementTypeFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "point = [uint, uint]");

        CBORValue array = CBOR.array()
                .add(10)
                .add("twenty")
                .value();

        assertInvalid(schema, array);
    }

    @Test
    void tupleTooFewElementsFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "point = [uint, uint]");

        CBORValue array = CBOR.array()
                .add(10)
                .value();

        assertInvalid(schema, array);
    }

    @Test
    void tupleTooManyElementsFails() {
        CddlSchema schema = CddlValidator.loadSchema(
                "point = [uint, uint]");

        CBORValue array = CBOR.array()
                .add(10)
                .add(20)
                .add(30)
                .value();

        assertInvalid(schema, array);
    }

    @Test
    void starOccurrenceAllowsZeroItems() {
        CddlSchema schema = CddlValidator.loadSchema(
                "nums = [* uint]");

        assertValid(schema, CBOR.array().value());
    }

    @Test
    void starOccurrenceAllowsManyItems() {
        CddlSchema schema = CddlValidator.loadSchema(
                "nums = [* uint]");

        CBORValue.CBORArrayEditor builder = CBOR.array();

        for (int i = 0; i < 100; i++) {
            builder.add(i);
        }

        assertValid(schema, builder.value());
    }

    @Test
    void starOccurrenceRejectsWrongType() {
        CddlSchema schema = CddlValidator.loadSchema(
                "nums = [* uint]");

        CBORValue array = CBOR.array()
                .add(1)
                .add("oops")
                .value();

        ValidationResult result = schema.validate(array);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.getInstancePath().contains("1")));
    }

    @Test
    void starOccurrenceRejectsFirstWrongType() {
        CddlSchema schema = CddlValidator.loadSchema(
                "nums = [* uint]");

        CBORValue array = CBOR.array()
                .add("oops")
                .add(1)
                .value();

        ValidationResult result = schema.validate(array);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.getInstancePath().contains("0")));
    }

    @Test
    void mapIsRejectedWhereArrayIsExpected() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [* uint]");

        CBORValue value = CBOR.map()
                .put(CBOR.value("value"), CBOR.integer(42))
                .value();

        assertInvalid(schema, value);
    }

    @Test
    void textIsRejectedWhereArrayIsExpected() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [* uint]");

        assertInvalid(schema, CBOR.value("123"));
    }

    @Test
    void plusOccurrenceRequiresAtLeastOne() {
        CddlSchema schema = CddlValidator.loadSchema(
                "nums = [+ uint]");

        assertInvalid(schema, CBOR.array().value());
    }

    @Test
    void plusOccurrenceAcceptsOne() {
        CddlSchema schema = CddlValidator.loadSchema(
                "nums = [+ uint]");

        CBORValue array = CBOR.array()
                .add(7)
                .value();

        assertValid(schema, array);
    }

    @Test
    void plusOccurrenceAllowsManyItems() {
        CddlSchema schema = CddlValidator.loadSchema(
                "nums = [+ uint]");

        CBORValue.CBORArrayEditor builder = CBOR.array();

        for (int i = 0; i < 100; i++) {
            builder.add(i);
        }

        assertValid(schema, builder.value());
    }

    @Test
    void plusOccurrenceManyItemsInvalidNearEnd() {
        CddlSchema schema = CddlValidator.loadSchema(
                "nums = [+ uint]");

        CBORValue.CBORArrayEditor builder = CBOR.array();

        for (int i = 0; i < 100; i++) {
            builder.add(i);
        }
        builder.add(CBOR.value("invalid"));

        assertInvalid(schema, builder.value());
    }

    @Test
    void optionalEntryMayBeAbsent() {
        CddlSchema schema = CddlValidator.loadSchema(
                "pair = [uint, ? tstr]");

        CBORValue array = CBOR.array()
                .add(1)
                .value();

        assertValid(schema, array);
    }

    @Test
    void optionalEntryMayBePresent() {
        CddlSchema schema = CddlValidator.loadSchema(
                "pair = [uint, ? tstr]");

        CBORValue array = CBOR.array()
                .add(1)
                .add("hello")
                .value();

        assertValid(schema, array);
    }

    @Test
    void labelledEntriesValidatedByType() {
        CddlSchema schema = CddlValidator.loadSchema(
                "point = [x: uint, y: uint]");

        CBORValue array = CBOR.array()
                .add(5)
                .add(10)
                .value();

        assertValid(schema, array);
    }

    @Test
    void nonArrayFailsArraySchema() {
        CddlSchema schema = CddlValidator.loadSchema(
                "nums = [* uint]");

        assertInvalid(schema, CBOR.integer(42));
    }

    @Test
    void arrayRejectsNullForIntegerElement() {
        CddlSchema schema = CddlValidator.loadSchema(
                "nums = [* uint]");

        CBORValue values = CBOR.array()
                .add(CBOR.integer(1))
                .add(CBOR.nullValue())
                .add(CBOR.integer(3))
                .value();

        assertInvalid(schema, values);
    }

    @Test
    void arrayTextElementsPositive() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [* tstr]");

        CBORValue values = CBOR.array()
                .add(CBOR.value("one"))
                .add(CBOR.value("two"))
                .value();

        assertValid(schema, values);
    }

    @Test
    void arrayRejectsUndefinedForTextElement() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [* tstr]");

        CBORValue values = CBOR.array()
                .add(CBOR.value("one"))
                .add(CBOR.undefined())
                .value();

        assertInvalid(schema, values);
    }

    @Test
    void emptyArraySchemaMatchesEmpty() {
        CddlSchema schema = CddlValidator.loadSchema(
                "empty = []");

        assertValid(schema, CBOR.array().value());
    }

    @Test
    void fixedLengthArrayAcceptsExactLength() {
        CddlSchema schema = CddlValidator.loadSchema(
                "point = [int, int]");

        CBORValue point = CBOR.array()
                .add(CBOR.integer(10))
                .add(CBOR.integer(20))
                .value();

        assertValid(schema, point);
    }

    @Test
    void fixedLengthArrayRejectsTooFewElements() {
        CddlSchema schema = CddlValidator.loadSchema(
                "point = [int, int]");

        CBORValue point = CBOR.array()
                .add(CBOR.integer(10))
                .value();

        assertInvalid(schema, point);
    }

    @Test
    void fixedLengthArrayRejectsEmptyArray() {
        CddlSchema schema = CddlValidator.loadSchema(
                "point = [int, int]");

        assertInvalid(schema, CBOR.array().value());
    }

    @Test
    void fixedLengthArrayRejectsTooManyElements() {
        CddlSchema schema = CddlValidator.loadSchema(
                "point = [int, int]");

        CBORValue point = CBOR.array()
                .add(CBOR.integer(10))
                .add(CBOR.integer(20))
                .add(CBOR.integer(30))
                .value();

        assertInvalid(schema, point);
    }

    @Test
    void heterogeneousArrayAcceptsExpectedTypes() {
        CddlSchema schema = CddlValidator.loadSchema(
                "record = [uint, tstr, bool]");

        CBORValue record = CBOR.array()
                .add(CBOR.integer(42))
                .add(CBOR.value("Alice"))
                .add(CBOR.bool(true))
                .value();

        assertValid(schema, record);
    }

    @Test
    void heterogeneousArrayRejectsWrongFirstType() {
        CddlSchema schema = CddlValidator.loadSchema(
                "record = [uint, tstr, bool]");

        CBORValue record = CBOR.array()
                .add(CBOR.value("42"))
                .add(CBOR.value("Alice"))
                .add(CBOR.bool(true))
                .value();

        assertInvalid(schema, record);
    }

    @Test
    void heterogeneousArrayRejectsWrongMiddleType() {
        CddlSchema schema = CddlValidator.loadSchema(
                "record = [uint, tstr, bool]");

        CBORValue record = CBOR.array()
                .add(CBOR.integer(42))
                .add(CBOR.integer(99))
                .add(CBOR.bool(true))
                .value();

        assertInvalid(schema, record);
    }

    @Test
    void heterogeneousArrayRejectsWrongLastType() {
        CddlSchema schema = CddlValidator.loadSchema(
                "record = [uint, tstr, bool]");

        CBORValue record = CBOR.array()
                .add(CBOR.integer(42))
                .add(CBOR.value("Alice"))
                .add(CBOR.integer(1))
                .value();

        assertInvalid(schema, record);
    }

    @Test
    void headerPlusBodyArray() {
        CddlSchema schema = CddlValidator.loadSchema(
                "message = [version: uint, type: tstr, * uint]");

        CBORValue array = CBOR.array()
                .add(1)
                .add("data")
                .add(10)
                .add(20)
                .value();

        assertValid(schema, array);
    }

    @Test
    void oneOrMoreElementsAccepted() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [1* uint]");

        CBORValue values = CBOR.array().add(CBOR.value(20)).value();
        assertValid(schema, values);
    }

    @Test
    void emptyArrayIsRejectedForOneOrMoreElements() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [1* uint]");

        CBORValue values = CBOR.array().value();
        assertInvalid(schema, values);
    }

    @Test
    void numericConstraintAcceptsArrayBoundaries() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [* 1..10]");

        CBORValue array = CBOR.array()
                .add(CBOR.integer(1))
                .add(CBOR.integer(5))
                .add(CBOR.integer(10))
                .value();

        assertValid(schema, array);
    }

    @Test
    void numericConstraintRejectsInvalidFirstArrayElement() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [* 1..10]");

        CBORValue array = CBOR.array()
                .add(CBOR.integer(0))
                .add(CBOR.integer(5))
                .add(CBOR.integer(10))
                .value();

        assertInvalid(schema, array);
    }

    @Test
    void numericConstraintRejectsInvalidMiddleArrayElement() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [* 1..10]");

        CBORValue array = CBOR.array()
                .add(CBOR.integer(1))
                .add(CBOR.integer(11))
                .add(CBOR.integer(10))
                .value();

        assertInvalid(schema, array);
    }

    @Test
    void numericConstraintRejectsInvalidFinalArrayElement() {
        CddlSchema schema = CddlValidator.loadSchema(
                "values = [* 1..10]");

        CBORValue array = CBOR.array()
                .add(CBOR.integer(1))
                .add(CBOR.integer(5))
                .add(CBOR.integer(11))
                .value();

        assertInvalid(schema, array);
    }

    @Test
    void nestedArrayErrorReportsFullPath() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = { values: values }\n"
                        + "values = [* [* uint]]");

        CBORValue inner = CBOR.array()
                .add(CBOR.integer(1))
                .add(CBOR.value("bad"))
                .value();

        CBORValue values = CBOR.array()
                .add(inner)
                .value();

        CBORValue root = CBOR.map()
                .put(CBOR.value("values"), values)
                .value();

        ValidationResult result = schema.validate(root);

        assertFalse(result.isValid());

        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.getInstancePath().contains("values")
                        && error.getInstancePath().contains("0")
                        && error.getInstancePath().contains("1")));
    }

    @Test
    void arrayOfMapsIsValid() {
        CddlSchema schema = CddlValidator.loadSchema(
                "people = [* person]\n"
                        + "person = { name: tstr, age: uint }");

        CBORValue alice = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Alice"))
                .put(CBOR.value("age"), CBOR.integer(30))
                .value();

        CBORValue bob = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Bob"))
                .put(CBOR.value("age"), CBOR.integer(40))
                .value();

        CBORValue people = CBOR.array()
                .add(alice)
                .add(bob)
                .value();

        assertValid(schema, people);
    }

    @Test
    void invalidMapInsideArrayIsRejected() {
        CddlSchema schema = CddlValidator.loadSchema(
                "people = [* person]\n"
                        + "person = { name: tstr, age: uint }");

        CBORValue validPerson = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Alice"))
                .put(CBOR.value("age"), CBOR.integer(30))
                .value();

        CBORValue invalidPerson = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Bob"))
                .put(CBOR.value("age"), CBOR.value("forty"))
                .value();

        CBORValue people = CBOR.array()
                .add(validPerson)
                .add(invalidPerson)
                .value();

        assertInvalid(schema, people);
    }

    @Test
    void arrayOfMapsReportsNestedFieldPath() {
        CddlSchema schema = CddlValidator.loadSchema(
                "people = [* person]\n"
                        + "person = { name: tstr, age: uint }");

        CBORValue person = CBOR.map()
                .put(CBOR.value("name"), CBOR.value("Alice"))
                .put(CBOR.value("age"), CBOR.value("thirty"))
                .value();

        CBORValue people = CBOR.array()
                .add(person)
                .value();

        ValidationResult result = schema.validate(people);

        assertFalse(result.isValid());

        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.getInstancePath().contains("0")
                        && error.getInstancePath().contains("age")));
    }

    @Test
    void nestedMapArrayMapStructureIsValid() {
        CddlSchema schema = CddlValidator.loadSchema(
                "order = { items: [* item] }\n"
                        + "item = { id: uint, tags: [* tstr] }");

        CBORValue item1 = CBOR.map()
                .put(CBOR.value("id"), CBOR.integer(1))
                .put(CBOR.value("tags"), CBOR.array()
                        .add(CBOR.value("first"))
                        .add(CBOR.value("important"))
                        .value())
                .value();

        CBORValue item2 = CBOR.map()
                .put(CBOR.value("id"), CBOR.integer(2))
                .put(CBOR.value("tags"), CBOR.array()
                        .add(CBOR.value("second"))
                        .value())
                .value();

        CBORValue order = CBOR.map()
                .put(CBOR.value("items"), CBOR.array()
                        .add(item1)
                        .add(item2)
                        .value())
                .value();

        assertValid(schema, order);
    }

    @Test
    void nestedMapArrayMapReportsDeepArrayElementError() {
        CddlSchema schema = CddlValidator.loadSchema(
                "order = { items: [* item] }\n"
                        + "item = { id: uint, tags: [* tstr] }");

        CBORValue invalidItem = CBOR.map()
                .put(CBOR.value("id"), CBOR.integer(1))
                .put(CBOR.value("tags"), CBOR.array()
                        .add(CBOR.value("valid"))
                        .add(CBOR.integer(999))
                        .value())
                .value();

        CBORValue order = CBOR.map()
                .put(CBOR.value("items"), CBOR.array()
                        .add(invalidItem)
                        .value())
                .value();

        ValidationResult result = schema.validate(order);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> "/items/0/tags/1".equals(error.getInstancePath())));
    }

    @Test
    void unlimitedArrayOfObjectsValid() {
        CddlSchema schema = CddlValidator.loadSchema(
                "unlimited-people = [* person]\n"
                        + "person = (\n"
                        + "    name: tstr,\n"
                        + "    age: uint,\n"
                        + ")\n");

        CBORValue array = CBOR.array()
                .add(CBOR.text("roundlet")).add(CBOR.integer(1047))
                .add(CBOR.text("psychurgy")).add(CBOR.integer(2204))
                .add(CBOR.text("extrarhythmical")).add(CBOR.integer(2231))
                .value();

        assertValid(schema, array);

        array = CBOR.array().value(); // empty

        assertValid(schema, array);

        array = CBOR.array()
                .add(CBOR.text("aluminize")).add(CBOR.integer(212))
                .add(CBOR.text("climograph")).add(CBOR.integer(4124))
                .value();

        assertValid(schema, array);

        array = CBOR.array()
                .add(CBOR.text("penintime")).add(CBOR.integer(1513))
                .add(CBOR.text("endocarditis")).add(CBOR.integer(4084))
                .add(CBOR.text("impermeator")).add(CBOR.integer(1669))
                .add(CBOR.text("coextension")).add(CBOR.integer(865))
                .value();

        assertValid(schema, array);

        array = CBOR.array()
                .add(CBOR.text("aluminize"))
                .add(CBOR.text("climograph")).add(CBOR.integer(4124))
                .value();

        assertInvalid(schema, array);

        array = CBOR.array()
                .add(CBOR.text("aluminize")).add(CBOR.integer(212))
                .add(CBOR.text("climograph"))
                .value();

        assertInvalid(schema, array);

        array = CBOR.array()
                .add(CBOR.text("aluminize")).add(CBOR.text("climograph"))
                .value();

        assertInvalid(schema, array);
    }

    @Test
    void boundedGroupOccurrenceUsesWholeGroupAsOneOccurrence() {
        CddlSchema schema = CddlValidator.loadSchema(
                "one-or-two-people = [1*2 person]\n"
                        + "person = (\n"
                        + "    name: tstr,\n"
                        + "    age: uint,\n"
                        + ")\n");

        CBORValue array = CBOR.array()
                .add(CBOR.text("aluminize")).add(CBOR.integer(212))
                .add(CBOR.text("climograph")).add(CBOR.integer(4124))
                .value();

        assertValid(schema, array);

        array = CBOR.array()
                .add(CBOR.text("aluminize")).add(CBOR.integer(212))
                .value();

        assertValid(schema, array);

        array = CBOR.array().value(); // empty

        assertInvalid(schema, array);

        array = CBOR.array()
                .add(CBOR.text("roundlet")).add(CBOR.integer(1047))
                .add(CBOR.text("psychurgy")).add(CBOR.integer(2204))
                .add(CBOR.text("extrarhythmical")).add(CBOR.integer(2231))
                .value();

        assertInvalid(schema, array);
    }

    @Test
    void atLeastTwoGroupOccurrencesRequireTwoCompleteGroups() {
        CddlSchema schema = CddlValidator.loadSchema(
                "at-least-two-people = [2* person]\n"
                        + "person = (\n"
                        + "    name: tstr,\n"
                        + "    age: uint,\n"
                        + ")\n");

        CBORValue array = CBOR.array()
                .add(CBOR.text("aluminize")).add(CBOR.integer(212))
                .add(CBOR.text("climograph")).add(CBOR.integer(4124))
                .value();

        assertValid(schema, array);

        array = CBOR.array()
                .add(CBOR.text("roundlet")).add(CBOR.integer(1047))
                .add(CBOR.text("psychurgy")).add(CBOR.integer(2204))
                .add(CBOR.text("extrarhythmical")).add(CBOR.integer(2231))
                .value();

        assertValid(schema, array);

        array = CBOR.array().value(); // empty

        assertInvalid(schema, array);

        array = CBOR.array()
                .add(CBOR.text("aluminize")).add(CBOR.integer(212))
                .value();

        assertInvalid(schema, array);
    }

    @Test
    void groupOccurrenceBacktracksBeforeFollowingArrayEntry() {
        CddlSchema schema = CddlValidator.loadSchema(
                "unlimited-people = [* person, bool]\n"
                        + "person = (\n"
                        + "    name: tstr,\n"
                        + "    age: uint,\n"
                        + ")\n");

        CBORValue array = CBOR.array()
                .add(CBOR.text("roundlet")).add(CBOR.integer(1047))
                .add(CBOR.text("psychurgy")).add(CBOR.integer(2204))
                .add(CBOR.text("extrarhythmical")).add(CBOR.integer(2231))
                .add(CBOR.trueValue())
                .value();

        assertValid(schema, array);

        array = CBOR.array()
                .add(CBOR.trueValue())
                .value();

        assertValid(schema, array);

        array = CBOR.array()
                .add(CBOR.text("roundlet")).add(CBOR.integer(1047))
                .add(CBOR.text("psychurgy")).add(CBOR.integer(2204))
                .add(CBOR.text("extrarhythmical")).add(CBOR.integer(2231))
                .value();

        assertInvalid(schema, array);

        array = CBOR.array()
                .add(CBOR.text("roundlet")).add(CBOR.integer(1047)).add(CBOR.trueValue())
                .add(CBOR.text("psychurgy")).add(CBOR.integer(2204)).add(CBOR.falseValue())
                .add(CBOR.text("extrarhythmical")).add(CBOR.integer(2231)).add(CBOR.trueValue())
                .value();

        assertInvalid(schema, array);
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