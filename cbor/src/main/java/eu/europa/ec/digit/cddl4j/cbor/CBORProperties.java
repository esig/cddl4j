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
package eu.europa.ec.digit.cddl4j.cbor;

/**
 * Contains a list of supported System properties applying on CBOR processing.
 *
 */
public final class CBORProperties {

    /* System properties */

    /** Defines maximum byte string length obtained from the untrusted sources */
    public static final String MAX_BYTE_STRING_LENGTH = "eu.europa.ec.digit.cddl4j.cbor.max_byte_string_length";

    /** Defines maximum number of nesting structure levels */
    public static final String MAX_NESTING_DEPTH = "eu.europa.ec.digit.cddl4j.cbor.max_nesting_depth";

    /* System property defaults */

    /** Default max byte string length is limited to 10 MB */
    public static final int DEFAULT_MAX_BYTE_STRING_LENGTH = 10 * 1024 * 1024;

    /** Default max nesting depth is limited to 32 levels */
    public static final int DEFAULT_MAX_NESTING_DEPTH = 32;

    /**
     * Default constructor
     */
    private CBORProperties() {
        // empty
    }

}
