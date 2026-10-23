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
 * You should have received a copy of the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 * along with this program. If not, see
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12 .
 */
package eu.europa.ec.digit.cddl4j.validator;

import eu.europa.ec.digit.cddl4j.cbor.CBOR;
import eu.europa.ec.digit.cddl4j.validator.schema.parser.CddlTokenizer;
import eu.europa.ec.digit.cddl4j.validator.schema.parser.Token;
import eu.europa.ec.digit.cddl4j.validator.schema.parser.TokenType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests strict UTF-8 source decoding and CDDL text-literal escape handling.
 */
class CddlTokenizerTest {

    @Test
    void acceptsJsonStyleTextEscapes() {
        List<Token> tokens = new CddlTokenizer("\"line\\n\\u0041\"").tokenize();

        assertEquals(TokenType.TEXT, tokens.get(0).getType());
        assertEquals("line\nA", tokens.get(0).getValue());
    }

    @Test
    void rejectsUnknownTextEscape() {
        assertThrows(
                CddlParseException.class,
                () -> new CddlTokenizer("\"bad\\q\"").tokenize());
    }

    @Test
    void rejectsInvalidUnicodeEscape() {
        assertThrows(
                CddlParseException.class,
                () -> new CddlTokenizer("\"\\u12x4\"").tokenize());
    }

    @Test
    void rejectsUnescapedControlCharacter() {
        assertThrows(
                CddlParseException.class,
                () -> new CddlTokenizer("\"line\nbreak\"").tokenize());
    }

    @Test
    void rejectsMalformedUtf8ByteSource() {
        byte[] invalidUtf8 = {(byte) 0xc3, 0x28};

        assertThrows(CddlParseException.class, () -> new CddlTokenizer(invalidUtf8));
        assertThrows(
                CddlParseException.class,
                () -> new CddlTokenizer(new ByteArrayInputStream(invalidUtf8)));
    }

    @Test
    void parsesBinaryAndNegativeHexadecimalIntegerLiterals() {
        assertTrue(
                CddlValidator.loadSchema("root = 0b101")
                        .validate(CBOR.integer(5))
                        .isValid());
        assertTrue(
                CddlValidator.loadSchema("root = -0x10")
                        .validate(CBOR.integer(-16))
                        .isValid());
        assertTrue(
                CddlValidator.loadSchema("root = -0b100")
                        .validate(CBOR.integer(-4))
                        .isValid());
    }

    @Test
    void rejectsInvalidIntegerLiteralSyntax() {
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("0x").tokenize());
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("0b2").tokenize());
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("01").tokenize());
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("1e+").tokenize());
        assertThrows(CddlParseException.class, () -> new CddlTokenizer("root = ١").tokenize());
    }

}
