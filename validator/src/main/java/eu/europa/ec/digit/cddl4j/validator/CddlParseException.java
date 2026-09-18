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

/**
 * Thrown when a CDDL source string cannot be parsed.
 */
public class CddlParseException extends RuntimeException {

    private static final long serialVersionUID = 295195974129900993L;

    /** Line position of the parsing error */
    private final int line;

    /** Column position of the parsing error */
    private final int column;

    /**
     * Creates a parse exception without source location information.
     *
     * @param message the parse error message
     */
    public CddlParseException(String message) {
        super(message);
        this.line = -1;
        this.column = -1;
    }

    /**
     * Creates a parse exception with a source location.
     *
     * @param message the parse error message
     * @param line the 1-based source line
     * @param column the 1-based source column
     */
    public CddlParseException(String message, int line, int column) {
        super(message + " (line " + line + ", col " + column + ")");
        this.line = line;
        this.column = column;
    }

    /**
     * Creates a parse exception that wraps another failure.
     *
     * @param message the parse error message
     * @param cause the underlying cause
     */
    public CddlParseException(String message, Throwable cause) {
        super(message, cause);
        this.line = -1;
        this.column = -1;
    }

    /**
     * Returns the 1-based line number where the parse error occurred.
     *
     * @return the source line, or -1 if unknown
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the 1-based column number where the parse error occurred.
     *
     * @return the source column, or -1 if unknown
     */
    public int getColumn() {
        return column;
    }
}
