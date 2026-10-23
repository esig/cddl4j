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
import eu.europa.ec.digit.cddl4j.validator.CddlValidator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests lexical handling of numeric literals, strings, and encoded CDDL source. */
class CddlTokenizerTest {

    @Test
    void acceptsJsonStyleTextEscapes() {
        List<Token> tokens = new CddlTokenizer("\"line\\n\\u0041\"").tokenize();

        assertEquals(TokenType.TEXT, tokens.get(0).getType());
        assertEquals("line\nA", tokens.get(0).getValue());
    }

    @Test
    void rejectsUnknownTextEscape() {
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("\"bad\\q\"").tokenize());
    }

    @Test
    void rejectsInvalidUnicodeEscape() {
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("\"\\u12x4\"").tokenize());
    }

    @Test
    void rejectsUnescapedControlCharacter() {
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("\"line\nbreak\"").tokenize());
    }

    @Test
    void rejectsMalformedUtf8ByteSource() {
        byte[] invalidUtf8 = {(byte) 0xc3, 0x28};

        assertThrows(CddlParseException.class, () -> new CddlTokenizer(invalidUtf8));
        assertThrows(CddlParseException.class, () -> new CddlTokenizer(new ByteArrayInputStream(invalidUtf8)));
    }

    @Test
    void parsesBinaryAndNegativeHexadecimalIntegerLiterals() {
        assertTrue(CddlValidator.loadSchema("root = 0b101").validate(CBOR.integer(5)).isValid());
        assertTrue(CddlValidator.loadSchema("root = -0x10").validate(CBOR.integer(-16)).isValid());
        assertTrue(CddlValidator.loadSchema("root = -0b100").validate(CBOR.integer(-4)).isValid());
    }

    @Test
    void rejectsInvalidIntegerLiteralSyntax() {
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("0x").tokenize());
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("0b2").tokenize());
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("01").tokenize());
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("1e+").tokenize());
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("root = ١").tokenize());
    }

    @Test
    void tokenizesDecimalHexadecimalAndBinaryIntegers() {
        List<Token> tokens = new CddlTokenizer("42 -42 0x2A 0X2A -0x2A 0b101 0B101 -0b101").tokenize();
        String[] expected = {"42", "-42", "0x2A", "0X2A", "-0x2A", "0b101", "0B101", "-0b101"};

        assertEquals(expected.length + 1, tokens.size());
        for (int index = 0; index < expected.length; index++) {
            assertEquals(TokenType.NUMBER_INT, tokens.get(index).getType());
            assertEquals(expected[index], tokens.get(index).getValue());
        }
        assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).getType());
    }

    @Test
    void tokenizesDecimalFloatsWithFractionAndExponent() {
        List<Token> tokens = new CddlTokenizer("1.5e2 3E-2 1..10, 0xFF]").tokenize();

        assertEquals(TokenType.NUMBER_FLOAT, tokens.get(0).getType());
        assertEquals("1.5e2", tokens.get(0).getValue());
        assertEquals(TokenType.NUMBER_FLOAT, tokens.get(1).getType());
        assertEquals("3E-2", tokens.get(1).getValue());
        assertEquals(TokenType.NUMBER_INT, tokens.get(2).getType());
        assertEquals(TokenType.RANGE, tokens.get(3).getType());
        assertEquals(TokenType.NUMBER_INT, tokens.get(4).getType());
        assertEquals(TokenType.COMMA, tokens.get(5).getType());
        assertEquals(TokenType.NUMBER_INT, tokens.get(6).getType());
        assertEquals(TokenType.RBRACKET, tokens.get(7).getType());
    }

    @Test
    void rejectsIncompleteOrMalformedNumericLiterals() {
        for (String source : new String[] {"0x", "-0X", "0b", "0b102", "0b10g", "0x12z", "01", "-01", "1e", "1e+"}) {
            assertThrows(CddlParseException.class, () -> new CddlTokenizer(source).tokenize(), source);
        }
    }

    @Test
    void tokenizesCddlTaggedHexadecimalAndBinaryNumbersWithoutSplittingTheirSuffix() {
        List<Token> hexadecimal = new CddlTokenizer("#6.0x20(tstr)").tokenize();
        List<Token> binary = new CddlTokenizer("#6.0b101(tstr)").tokenize();

        assertEquals("6.0", hexadecimal.get(1).getValue());
        assertEquals("x20", hexadecimal.get(2).getValue());
        assertEquals("6.0", binary.get(1).getValue());
        assertEquals("b101", binary.get(2).getValue());
    }

    @Test
    void rejectsUnsupportedTextEscapesAndMalformedUnicodeEscapes() {
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("root = \"bad\\q\"").tokenize());
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("root = \"bad\\uZZZZ\"").tokenize());
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("root = \"trailing\\").tokenize());
    }

    @Test
    void rejectsNonAsciiIdentifierCharacters() {
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("root = café").tokenize());
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("root = ١").tokenize());
    }

    @Test
    void rejectsUnescapedControlCharactersInTextLiterals() {
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("root = \"first\nsecond\"").tokenize());
    }

    @Test
    void rejectsMalformedUtf8InByteArrayAndInputStreamSources() {
        byte[] invalidUtf8 = {(byte) 0xC3, 0x28};

        assertThrows(CddlParseException.class, () -> new CddlTokenizer(invalidUtf8));
        assertThrows(CddlParseException.class,
                () -> new CddlTokenizer(new ByteArrayInputStream(invalidUtf8)));
    }

    @Test
    void acceptsValidUtf8SourceAndJsonStyleEscapes() {
        String source = "root = \"café\\nline\\u0021\"";
        List<Token> tokens = new CddlTokenizer(source.getBytes(StandardCharsets.UTF_8)).tokenize();

        assertEquals(TokenType.TEXT, tokens.get(2).getType());
        assertEquals("café\nline!", tokens.get(2).getValue());
    }

}
