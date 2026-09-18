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
package eu.europa.ec.digit.cddl4j.validator.schema.parser;

/**
 * The complete set of token types produced by {@link CddlTokenizer}.
 */
public enum TokenType {

    // --- Identifiers and literals ---

    /** An identifier used for rule names and type references. */
    IDENT,

    /** An integer literal, such as <code>0</code>, <code>42</code>, <code>-7</code>, or <code>0xFF</code>. */
    NUMBER_INT,

    /** A floating-point literal, such as <code>1.5</code> or <code>3.14e-2</code>. */
    NUMBER_FLOAT,

    /** A text-string literal, such as <code>"hello world"</code>. */
    TEXT,

    /** A hexadecimal byte-string literal, such as <code>h'deadbeef'</code>. */
    BYTES_HEX,

    /** A Base64-encoded byte-string literal, such as <code>b64'SGVsbG8='</code>. */
    BYTES_B64,

    // --- Structural ---

    /** An opening map delimiter, <code>{</code>. */
    LBRACE,

    /** A closing map delimiter, <code>}</code>. */
    RBRACE,

    /** An opening array delimiter, <code>[</code>. */
    LBRACKET,

    /** A closing array delimiter, <code>]</code>. */
    RBRACKET,

    /** An opening parenthesis, <code>(</code>. */
    LPAREN,

    /** A closing parenthesis, <code>)</code>. */
    RPAREN,

    // --- Separators and operators ---

    /** A comma separator, <code>,</code>. */
    COMMA,

    /** A colon separator, <code>:</code>. */
    COLON,

    /** A hash operator, <code>#</code>. */
    HASH,

    /** A caret operator, <code>^</code>. */
    CARET,

    /** A tilde operator, <code>~</code>. */
    TILDE,

    /** An ampersand operator, <code>&amp;</code>. */
    AMPERSAND,

    // --- Assignment ---

    /** A standard rule assignment operator, <code>=</code>. */
    ASSIGN,

    /** An additive rule assignment operator, <code>/=</code>. */
    ASSIGN_ADDITIVE,

    /** A group additive rule assignment operator, <code>//=</code>. */
    ASSIGN_GROUP_ADDITIVE,

    // --- Choice and range ---

    /** A type-choice operator, <code>/</code>. */
    SLASH,

    /** A group-choice operator, <code>//</code>. */
    DOUBLE_SLASH,

    /** An inclusive range operator, <code>..</code>. */
    RANGE,

    /** An exclusive-upper-bound range operator, <code>...</code>. */
    RANGE_EXCL,

    // --- Arrow ---

    /** A map key-to-value association operator, <code>=&gt;</code>. */
    FAT_ARROW,

    // --- Occurrence ---

    /** A zero-or-more occurrence indicator, <code>*</code>. */
    STAR,

    /** A one-or-more occurrence indicator, <code>+</code>. */
    PLUS,

    /** A zero-or-one occurrence indicator, <code>?</code>. */
    QUESTION,

    // --- Control operators (dot-prefixed identifiers) ---

    /**
     * A control operator represented by a dot-prefixed identifier, such as
     * <code>.size</code>, <code>.regexp</code>, <code>.bits</code>,
     * <code>.cbor</code>, <code>.cborseq</code>, <code>.within</code>,
     * <code>.and</code>, <code>.default</code>, <code>.lt</code>,
     * <code>.le</code>, <code>.gt</code>, <code>.ge</code>, <code>.eq</code>,
     * or <code>.ne</code>.
     */
    CTRL_OP,

    // --- End of input ---

    /** The end-of-input marker. */
    EOF

}
