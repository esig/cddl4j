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

import eu.europa.ec.digit.cddl4j.validator.CddlParseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lexer for CDDL source text (RFC 8610).
 *
 * <p>Produces a flat list of {@link Token}s. Comments ({@code ;} to EOL)
 * and all whitespace are silently dropped.
 *
 * <p>Text literals use CDDL's JSON-style escapes. Unsupported escapes,
 * unescaped control characters, invalid Unicode escapes, and malformed UTF-8
 * source bytes are rejected rather than silently rewritten.
 *
 * <p>Numeric tokens use ASCII digits and accept decimal, hexadecimal, and
 * binary integer forms. Invalid or incomplete forms are rejected during
 * tokenization so they cannot be reinterpreted as adjacent tokens.
 */
public final class CddlTokenizer {

    /** Original string containing CDDL source. */
    private final String src;

    /** Current reading position. */
    private int pos;

    /** Current reading line. */
    private int line;

    /** Current reading column. */
    private int col;

    /**
     * Creates a tokenizer for CDDL source text.
     *
     * @param src the CDDL source
     * @throws NullPointerException if {@code src} is null
     */
    public CddlTokenizer(String src) {
        this.src = Objects.requireNonNull(src, "src");
        this.pos = 0;
        this.line = 1;
        this.col = 1;
    }

    /**
     * Creates a tokenizer for UTF-8 encoded CDDL source.
     *
     * @param src the UTF-8 encoded CDDL source
     * @throws NullPointerException if {@code src} is null
     * @throws CddlParseException if the source is not valid UTF-8
     */
    public CddlTokenizer(byte[] src) {
        this(decodeUtf8(Objects.requireNonNull(src, "CDDL source is null")));
    }

    /**
     * Creates a tokenizer for UTF-8 encoded CDDL source read from an input
     * stream.
     *
     * <p>The input stream is read completely during construction and converted
     * to a UTF-8 {@link String}. The supplied stream is not closed by this
     * tokenizer; ownership of the stream remains with the caller.
     *
     * @param src the input stream containing UTF-8 encoded CDDL source
     * @throws NullPointerException if {@code src} is null
     * @throws CddlParseException if the source is not valid UTF-8 or cannot be read
     */
    public CddlTokenizer(InputStream src) {
        // TODO: Tokenize directly from the stream to avoid materializing the entire source.
        // Keep the current String-backed implementation until streaming parser state is designed.
        this(readSource(src));
    }

    /**
     * Tokenizes the complete source.
     *
     * @return the lexical tokens, including a final EOF token
     * @throws CddlParseException if the source contains invalid syntax or an
     *     invalid text literal
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < src.length()) {
            skipWhitespaceAndComments();
            if (pos >= src.length()) {
                break;
            }
            tokens.add(nextToken());
        }
        tokens.add(new Token(TokenType.EOF, "", line, col));
        return tokens;
    }

    // ------------------------------------------------------------------
    // Main dispatch
    // ------------------------------------------------------------------

    private Token nextToken() {
        int startLine = line;
        int startCol  = col;
        char c = peek();

        // --- Identifiers and keywords ---
        if (isIdentStart(c)) {
            return readIdent(startLine, startCol);
        }

        // --- Numbers ---
        if (isDecimalDigit(c) || (c == '-' && pos + 1 < src.length() && isDecimalDigit(src.charAt(pos + 1)))) {
            return readNumber(startLine, startCol);
        }

        // TODO: Support unprefixed CDDL byte-string literals (RFC 8610, Appendix B).
        // The current syntax intentionally requires the explicit h'...' or b64'...' qualifier.

        // --- Quoted text string ---
        if (c == '"') {
            return readTextString(startLine, startCol);
        }

        // --- Byte strings: h'...' and b64'...' ---
        // These are handled inside readIdent when we see 'h' or 'b' followed by '\''

        // --- Structural and operators ---
        advance();
        switch (c) {
            case '{':
                return token(TokenType.LBRACE,   "{", startLine, startCol);
            case '}':
                return token(TokenType.RBRACE,   "}", startLine, startCol);
            case '[':
                return token(TokenType.LBRACKET, "[", startLine, startCol);
            case ']':
                return token(TokenType.RBRACKET, "]", startLine, startCol);
            case '(':
                return token(TokenType.LPAREN,   "(", startLine, startCol);
            case ')':
                return token(TokenType.RPAREN,   ")", startLine, startCol);
            case ',':
                return token(TokenType.COMMA,    ",", startLine, startCol);
            case ':':
                return token(TokenType.COLON,    ":", startLine, startCol);
            case '^':
                return token(TokenType.CARET,    "^", startLine, startCol);
            case '~':
                return token(TokenType.TILDE,    "~", startLine, startCol);
            case '&':
                return token(TokenType.AMPERSAND,"&", startLine, startCol);
            case '*':
                return token(TokenType.STAR,     "*", startLine, startCol);
            case '+':
                return token(TokenType.PLUS,     "+", startLine, startCol);
            case '?':
                return token(TokenType.QUESTION, "?", startLine, startCol);

            case '#':
                return token(TokenType.HASH, "#", startLine, startCol);

            case '=':
                if (peek() == '>') {
                    advance();
                    return token(TokenType.FAT_ARROW, "=>", startLine, startCol);
                }
                return token(TokenType.ASSIGN, "=", startLine, startCol);

            case '/':
                if (peek() == '/') {
                    advance(); // second /
                    if (peek() == '=') {
                        advance();
                        return token(TokenType.ASSIGN_GROUP_ADDITIVE, "//=", startLine, startCol);
                    }
                    if (peek() == '/') {
                        advance();
                        return token(TokenType.DOUBLE_SLASH, "///", startLine, startCol);
                    }
                    // bare // is not standard; treat as // group choice open
                    return token(TokenType.DOUBLE_SLASH, "//", startLine, startCol);
                }
                if (peek() == '=') {
                    advance();
                    return token(TokenType.ASSIGN_ADDITIVE, "/=", startLine, startCol);
                }
                return token(TokenType.SLASH, "/", startLine, startCol);

            case '.':
                if (peek() == '.') {
                    advance();
                    if (peek() == '.') {
                        advance();
                        return token(TokenType.RANGE_EXCL, "...", startLine, startCol);
                    }
                    return token(TokenType.RANGE, "..", startLine, startCol);
                }
                // Control operator: .size, .regexp, etc.
                StringBuilder ctrlOp = new StringBuilder(".");
                while (pos < src.length() && (isAsciiLetterOrDigit(peek()) || peek() == '-')) {
                    ctrlOp.append(advance());
                }
                return token(TokenType.CTRL_OP, ctrlOp.toString(), startLine, startCol);

            default:
                throw new CddlParseException("Unexpected character '" + c + "'", startLine, startCol);
        }
    }

    // ------------------------------------------------------------------
    // Identifier / keyword / byte-string prefix readers
    // ------------------------------------------------------------------

    private Token readIdent(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        while (pos < src.length() && isIdentPart(peek())) {
            sb.append(advance());
        }
        String name = sb.toString();

        // Byte-string prefixes: h'...' or b64'...'
        if ((name.equals("h") || name.equals("b64")) && pos < src.length() && peek() == '\'') {
            return readBytesLiteral(name, startLine, startCol);
        }

        return token(TokenType.IDENT, name, startLine, startCol);
    }

    private Token readBytesLiteral(String prefix, int startLine, int startCol) {
        advance(); // consume opening '
        StringBuilder sb = new StringBuilder();
        while (pos < src.length() && peek() != '\'') {
            char c = advance();
            if (!Character.isWhitespace(c)) {
                sb.append(c); // ignore embedded whitespace
            }
        }
        if (pos >= src.length()) {
            throw new CddlParseException("Unterminated byte-string literal", startLine, startCol);
        }
        advance(); // consume closing '
        TokenType tt = prefix.equals("h") ? TokenType.BYTES_HEX : TokenType.BYTES_B64;
        return token(tt, sb.toString(), startLine, startCol);
    }

    // ------------------------------------------------------------------
    // Number reader
    // ------------------------------------------------------------------

    private Token readNumber(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        boolean isFloat = false;

        if (peek() == '-') {
            sb.append(advance());
        }

        boolean prefixedInteger = false;
        if (peek() == '0' && pos + 1 < src.length()
                && (src.charAt(pos + 1) == 'x' || src.charAt(pos + 1) == 'X')) {
            // TODO: Add CDDL hexadecimal floating-point number forms.
            // Keep the integer subset explicit until those forms have dedicated parsing rules.
            sb.append(advance());
            sb.append(advance());
            prefixedInteger = true;
            int firstDigit = sb.length();
            while (pos < src.length() && isHexDigit(peek())) {
                sb.append(advance());
            }
            if (sb.length() == firstDigit) {
                throw new CddlParseException("Hexadecimal integer requires at least one digit", startLine, startCol);
            }
            rejectPrefixedIntegerSuffix("hexadecimal", startLine, startCol);
        } else if (peek() == '0' && pos + 1 < src.length()
                && (src.charAt(pos + 1) == 'b' || src.charAt(pos + 1) == 'B')) {
            sb.append(advance());
            sb.append(advance());
            prefixedInteger = true;
            int firstDigit = sb.length();
            while (pos < src.length() && (peek() == '0' || peek() == '1')) {
                sb.append(advance());
            }
            if (sb.length() == firstDigit) {
                throw new CddlParseException("Binary integer requires at least one digit", startLine, startCol);
            }
            rejectPrefixedIntegerSuffix("binary", startLine, startCol);
        } else {
            // Decimal integer or float
            while (pos < src.length() && isDecimalDigit(peek())) {
                sb.append(advance());
            }
            if (pos < src.length() && peek() == '.') {
                // Could be a range operator — peek ahead
                if (pos + 1 < src.length() && isDecimalDigit(src.charAt(pos + 1))) {
                    isFloat = true;
                    sb.append(advance()); // '.'
                    while (pos < src.length() && isDecimalDigit(peek())) {
                        sb.append(advance());
                    }
                }
                // else leave '.' for the operator reader
            }
            if (pos < src.length() && (peek() == 'e' || peek() == 'E')) {
                isFloat = true;
                sb.append(advance());
                if (pos < src.length() && (peek() == '+' || peek() == '-')) {
                    sb.append(advance());
                }
                int exponentDigitStart = sb.length();
                while (pos < src.length() && isDecimalDigit(peek())) {
                    sb.append(advance());
                }
                if (sb.length() == exponentDigitStart) {
                    throw new CddlParseException("Exponent requires at least one digit", startLine, startCol);
                }
            }

            // In #6.0xNN and #6.0bNN, the radix prefix follows the decimal-looking major-type token.
            if (pos < src.length() && isIdentifierContinuation(peek())
                    && !("6.0".contentEquals(sb)
                    && (peek() == 'x' || peek() == 'X' || peek() == 'b' || peek() == 'B'))) {
                throw new CddlParseException("Invalid character after numeric literal", startLine, startCol);
            }
        }

        if (!prefixedInteger) {
            int firstDigit = sb.charAt(0) == '-' ? 1 : 0;
            if (firstDigit + 1 < sb.length() && sb.charAt(firstDigit) == '0'
                    && isDecimalDigit(sb.charAt(firstDigit + 1))) {
                throw new CddlParseException("Decimal numbers must not contain leading zeroes", startLine, startCol);
            }
        }

        TokenType tt = isFloat ? TokenType.NUMBER_FLOAT : TokenType.NUMBER_INT;
        return token(tt, sb.toString(), startLine, startCol);
    }

    /** Rejects suffixes that would otherwise split a malformed prefixed integer into adjacent tokens. */
    private void rejectPrefixedIntegerSuffix(String radix, int startLine, int startCol) {
        if (pos < src.length() && isIdentifierContinuation(peek())) {
            throw new CddlParseException("Invalid character after " + radix + " integer", startLine, startCol);
        }
    }

    // ------------------------------------------------------------------
    // Text string reader
    // ------------------------------------------------------------------

    private Token readTextString(int startLine, int startCol) {
        advance(); // consume opening "
        StringBuilder sb = new StringBuilder();
        while (pos < src.length() && peek() != '"') {
            char c = advance();
            if (c == '\\') {
                if (pos >= src.length()) {
                    throw new CddlParseException("Unterminated escape sequence", startLine, startCol);
                }

                char esc = advance();
                switch (esc) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'u':
                        sb.append(readUnicode(startLine, startCol));
                        break;
                    default:
                        throw new CddlParseException(
                                "Invalid text-string escape: \\" + esc,
                                startLine,
                                startCol);
                }
            } else {
                if (c < 0x20) {
                    throw new CddlParseException(
                            "Unescaped control character in text string",
                            startLine,
                            startCol);
                }
                sb.append(c);
            }
        }
        if (pos >= src.length()) {
            throw new CddlParseException("Unterminated text string", startLine, startCol);
        }
        advance(); // consume closing "
        return token(TokenType.TEXT, sb.toString(), startLine, startCol);
    }

    private char readUnicode(int startLine, int startCol) {
        if (pos + 3 >= src.length()) {
            throw new CddlParseException("Invalid unicode escape", startLine, startCol);
        }
        String hex = "" + advance() + advance() + advance() + advance();
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException exception) {
            throw new CddlParseException("Invalid unicode escape", startLine, startCol);
        }
    }

    // ------------------------------------------------------------------
    // Whitespace and comments
    // ------------------------------------------------------------------

    private void skipWhitespaceAndComments() {
        while (pos < src.length()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r') {
                advance();
            } else if (c == '\n') {
                advance();
                line++;
                col = 1;
            } else if (c == ';') {
                // comment to end-of-line
                while (pos < src.length() && peek() != '\n') {
                    advance();
                }
            } else {
                break;
            }
        }
    }

    // ------------------------------------------------------------------
    // Primitive helpers
    // ------------------------------------------------------------------

    private char peek() {
        return src.charAt(pos);
    }

    private char advance() {
        char c = src.charAt(pos++);
        col++;
        return c;
    }

    private static Token token(TokenType type, String value, int line, int col) {
        return new Token(type, value, line, col);
    }

    private static boolean isIdentStart(char c) {
        return isAsciiLetter(c) || c == '_' || c == '$' || c == '@';
    }

    private static boolean isIdentPart(char c) {
        return isAsciiLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == '@' || c == '$';
    }

    private static boolean isAsciiLetterOrDigit(char c) {
        return isAsciiLetter(c) || isDecimalDigit(c);
    }

    private static boolean isIdentifierContinuation(char c) {
        return isAsciiLetterOrDigit(c) || c == '_' || c == '$' || c == '@';
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDecimalDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    // ------------------------------------------------------------------
    // Source decoding helpers
    // ------------------------------------------------------------------

    /**
     * Decodes UTF-8 encoded CDDL source.
     *
     * @param data UTF-8 encoded CDDL source
     * @return decoded CDDL source text
     */
    private static String decodeUtf8(byte[] data) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new CddlParseException("CDDL source is not valid UTF-8", exception);
        }
    }

    /**
     * Reads the complete CDDL source from an input stream and decodes it as
     * UTF-8.
     *
     * <p>The supplied stream is not closed.
     *
     * @param input the input stream containing UTF-8 encoded CDDL source
     * @return decoded CDDL source text
     * @throws NullPointerException if {@code input} is null
     * @throws CddlParseException if an I/O error occurs
     */
    private static String readSource(InputStream input) {
        Objects.requireNonNull(input, "CDDL source is null");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];

        try {
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        } catch (IOException exception) {
            throw new CddlParseException(
                    "Unable to read CDDL source: " + exception.getMessage(),
                    exception);
        }
        return decodeUtf8(output.toByteArray());
    }

}

