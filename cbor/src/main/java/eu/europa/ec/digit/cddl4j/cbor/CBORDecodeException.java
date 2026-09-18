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
package eu.europa.ec.digit.cddl4j.cbor;

/**
 * Checked exception raised when CBOR bytes are malformed or truncated.
 */
public final class CBORDecodeException extends Exception {

    private static final long serialVersionUID = 1797327719315082986L;

    /** The byte offset at which the error was detected */
    private final int offset;

    /**
     * Creates a decoding exception with zero offset.
     *
     * @param message the error message
     */
    public CBORDecodeException(String message) {
        this(message, 0);
    }

    /**
     * Creates a decoding exception.
     *
     * @param message the error message
     * @param offset the byte offset at which the error was detected
     */
    public CBORDecodeException(String message, int offset) {
        super(message);
        this.offset = offset;
    }

    /**
     * Creates a decoding exception with a cause.
     *
     * @param message the error message
     * @param offset the byte offset at which the error was detected
     * @param cause the underlying cause
     */
    public CBORDecodeException(String message, int offset, Throwable cause) {
        super(message, cause);
        this.offset = offset;
    }

    /**
     * Returns the byte offset at which the error was detected.
     *
     * @return the byte offset
     */
    public int getOffset() {
        return offset;
    }

}