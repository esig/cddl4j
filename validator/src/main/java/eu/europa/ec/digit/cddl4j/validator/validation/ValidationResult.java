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
package eu.europa.ec.digit.cddl4j.validator.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The outcome of validating a CBOR payload against a CDDL schema.
 *
 * <p>Uses an immutable list of {@link ValidationError}s for the failure case. An empty list represents success.
 */
public final class ValidationResult {

    /** Successful validation result, indicating no issues have been identified. */
    private static final ValidationResult SUCCESS = new ValidationResult(Collections.emptyList());

    /** List of validation errors. */
    private final List<ValidationError> errors;

    /**
     * Creates a validation result.
     *
     * @param errors a list of {@link ValidationError}s, if any
     */
    private ValidationResult(List<ValidationError> errors) {
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /**
     * Returns a result representing a successful validation (no errors).
     *
     * @return the shared successful result
     */
    public static ValidationResult success() {
        return SUCCESS;
    }

    /**
     * Returns a result representing one or more validation failures.
     *
     * @param errors non-empty list of errors; must not be {@code null}
     * @return a failed validation result
     */
    public static ValidationResult failure(List<ValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException("errors must be non-empty for a failure result");
        }
        return new ValidationResult(errors);
    }

    /**
     * Returns a result representing a single validation failure.
     *
     * @param error {@link ValidationError}
     * @return {@link ValidationResult}
     */
    public static ValidationResult failure(ValidationError error) {
        return failure(Collections.singletonList(error));
    }

    /**
     * {@code true} when the CBOR payload fully satisfies the CDDL schema.
     *
     * @return {@code true} when no validation errors were found
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * The list of errors found.  Empty when {@link #isValid()} is {@code true}.
     *
     * @return an immutable list of validation errors
     */
    public List<ValidationError> getErrors() {
        return errors;
    }

    /**
     * Throws an {@link AssertionError} with a formatted message if the result
     * is invalid — useful in unit tests.
     *
     * @return this result when validation succeeded
     * @throws AssertionError if validation failed
     */
    public ValidationResult assertValid() {
        if (!isValid()) {
            StringBuilder sb = new StringBuilder("CBOR validation failed with ")
                    .append(errors.size())
                    .append(" error(s):\n");
            for (ValidationError e : errors) {
                sb.append(e.toString()).append('\n');
            }
            throw new AssertionError(sb.toString());
        }
        return this;
    }

    @Override
    public String toString() {
        return isValid() ? "ValidationResult{valid}" : "ValidationResult{invalid, " + errors.size() + " error(s)}";
    }

}
