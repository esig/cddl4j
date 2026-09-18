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
 * A single lexical token produced by {@link CddlTokenizer}.
 */
public final class Token {

    /** Type of the token */
    private final TokenType type;

    /** Value of the token */
    private final String value;

    /** Line position of the token */
    private final int line;

    /** Column position (within the line) of the token */
    private final int column;

    /**
     * Default constructor
     *
     * @param type {@link TokenType} type of the token
     * @param value {@link String} value of the token
     * @param line line position of the token
     * @param column column position of the token
     */
    public Token(TokenType type, String value, int line, int column) {
        this.type   = type;
        this.value  = value;
        this.line   = line;
        this.column = column;
    }

    /**
     * Gets type of the token
     *
     * @return {@link TokenType}
     */
    public TokenType getType() { return type; }

    /**
     * Gets value of the token
     *
     * @return {@link String}
     */
    public String getValue() { return value; }

    /**
     * Gets line position of the token
     *
     * @return integer
     */
    public int getLine() { return line; }
    /**
     * Gets column position (within the line) of the token
     *
     * @return integer
     */
    public int getColumn() { return column; }

    /**
     * Checks if the {@code t} is the current token
     *
     * @param t {@link TokenType} to compare
     * @return TRUE if the token is the same, FALSE otherwise
     */
    public boolean is(TokenType t) {
        return this.type == t;
    }

    @Override
    public String toString() {
        return type + "('" + value + "' @" + line + ":" + column + ")";
    }

}
