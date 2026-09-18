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
 * Thrown when a CDDL schema file or resource cannot be loaded from disk / classpath.
 */
public class CddlLoadException extends RuntimeException {

    private static final long serialVersionUID = -8852889700264485388L;

    /**
     * Constructor with an exception message
     *
     * @param message {@link String}
     */
    public CddlLoadException(String message) {
        super(message);
    }

    /**
     * Constructor with an exception message and the cause
     *
     * @param message {@link String}
     * @param cause {@link Throwable}
     */
    public CddlLoadException(String message, Throwable cause) {
        super(message, cause);
    }

}
