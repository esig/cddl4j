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
package eu.europa.ec.digit.cddl4j.validator.schema.parser;

import eu.europa.ec.digit.cddl4j.cbor.CBOR;
import eu.europa.ec.digit.cddl4j.validator.CddlParseException;
import eu.europa.ec.digit.cddl4j.validator.schema.CddlSchema;
import eu.europa.ec.digit.cddl4j.validator.schema.type.ArrayType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.GroupEntry;
import eu.europa.ec.digit.cddl4j.validator.schema.type.TaggedType;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the parser integration for CDDL integer forms, tag numbers, and occurrence bounds. */
class CddlParserNumericTest {

    @Test
    void parsesPositiveAndNegativeHexadecimalAndBinaryIntegerLiterals() {
        assertTrue(parse("root = -0x2A").validate(CBOR.integer(-42)).isValid());
        assertTrue(parse("root = 0X2A").validate(CBOR.integer(42)).isValid());
        assertTrue(parse("root = 0b101010").validate(CBOR.integer(42)).isValid());
        assertTrue(parse("root = -0B101010").validate(CBOR.integer(-42)).isValid());
    }

    @Test
    void parsesHexadecimalAndBinaryOccurrenceBounds() {
        CddlSchema schema = parse("root = [0x2*0b11 int]");
        ArrayType array = assertInstanceOf(ArrayType.class, schema.getRuleSet().getRule("root"));
        GroupEntry entry = array.getEntries().get(0);

        assertEquals(2, entry.getMin());
        assertEquals(3, entry.getMax());
    }

    @Test
    void rejectsNegativeAndOutOfRangeOccurrenceBounds() {
        assertThrows(CddlParseException.class, () -> parse("root = [-1* int]"));
        assertThrows(CddlParseException.class, () -> parse("root = [4294967296* int]"));
        assertThrows(CddlParseException.class, () -> parse("root = [1*4294967296 int]"));
        assertThrows(CddlParseException.class, () -> parse("root = [1*2147483647 int]"));
    }

    @Test
    void parsesHexadecimalAndBinaryTagNumbers() {
        CddlSchema hexadecimal = parse("root = #6.0x20(tstr)");
        CddlSchema binary = parse("root = #6.0b101(tstr)");
        CddlSchema decimal = parse("root = #6.32(tstr)");

        assertTrue(hexadecimal.validate(CBOR.tag(32, CBOR.text("value"))).isValid());
        assertFalse(hexadecimal.validate(CBOR.tag(33, CBOR.text("value"))).isValid());
        assertTrue(binary.validate(CBOR.tag(5, CBOR.text("value"))).isValid());
        assertTrue(decimal.validate(CBOR.tag(32, CBOR.text("value"))).isValid());
    }

    @Test
    void parsesFullUnsigned64BitTagNumber() {
        BigInteger maximumUnsignedLong = new BigInteger("18446744073709551615");
        CddlSchema schema = parse("root = #6.0xFFFFFFFFFFFFFFFF(tstr)");
        assertInstanceOf(TaggedType.class, schema.getRuleSet().getRule("root"));
        assertTrue(schema.validate(CBOR.tag(maximumUnsignedLong, CBOR.text("value"))).isValid());
    }

    @Test
    void reportsInvalidIntegerAndTagValuesAsCddlParseExceptions() {
        assertThrows(CddlParseException.class, () -> parse("root = 0b102"));
        assertThrows(CddlParseException.class, () -> parse("root = #6.-1(tstr)"));
        assertThrows(CddlParseException.class, () -> parse("root = #6.0xGG(tstr)"));
        assertThrows(CddlParseException.class, () -> parse("root = #6.0x10000000000000000(tstr)"));
    }

    private CddlSchema parse(String source) {
        return new CddlParser().parse(source);
    }
}
