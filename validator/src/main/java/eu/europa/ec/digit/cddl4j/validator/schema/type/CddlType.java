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
package eu.europa.ec.digit.cddl4j.validator.schema.type;

import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationError;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

/**
 * Base class for all CDDL type nodes produced by the parser.
 *
 * <p>Each subclass represents one syntactic construct in CDDL
 * (a primitive, a literal, a group, a choice, a reference, etc.)
 * and knows how to validate a {@link CBORValue} against itself.
 */
public abstract class CddlType {

    /**
     * Default constructor
     */
    protected CddlType() {
        // empty
    }

    /**
     * Validate the given CBOR value against this type.
     *
     * @param value the decoded CBOR value to check
     * @param ctx   the current validation context (carries path + rule set)
     * @return {@link ValidationResult#success()} or a failure with one or more errors
     */
    public abstract ValidationResult validate(CBORValue value, ValidationContext ctx);

    /**
     * A short human-readable description used in error messages.
     */
    @Override
    public abstract String toString();

    // ------------------------------------------------------------------
    // Helper shared across subclasses
    // ------------------------------------------------------------------

    /**
     * Creates a validation failure result
     *
     * @param ctx {@link ValidationContext}
     * @param message {@link String}
     * @return {@link ValidationResult}
     */
    protected ValidationResult fail(ValidationContext ctx, String message) {
        return ValidationResult.failure(new ValidationError(ctx.getInstancePath(), message));
    }

}
