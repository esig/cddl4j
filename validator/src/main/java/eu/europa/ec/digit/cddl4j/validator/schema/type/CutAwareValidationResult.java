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
package eu.europa.ec.digit.cddl4j.validator.schema.type;

import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

/**
 * Internal outcome of a type evaluation that preserves cut-match information.
 *
 * <p>The public {@link ValidationResult} represents validation status and diagnostics. This class
 * additionally records whether a validation failure occurred after a CDDL cut entry matched.
 * The additional state supports correct alternative selection during map and map-group-choice
 * evaluation without exposing control-flow information through the public validation API.
 *
 * <p>The cut-match flag does not identify the particular entry that matched, nor does it
 * automatically commit every enclosing choice. The evaluator consuming this outcome is
 * responsible for interpreting the flag according to the semantics of that choice.
 */
public final class CutAwareValidationResult {

    /** Target CddlType validation result */
    private final ValidationResult validationResult;

    /** Whether the cut rule has been applied */
    private final boolean matchedCutEntry;

    /**
     * Default constructor
     *
     * @param validationResult {@link ValidationResult} of the target CddlType
     * @param matchedCutEntry whether the cut rule has been applied
     */
    private CutAwareValidationResult(ValidationResult validationResult, boolean matchedCutEntry) {
        if (validationResult == null) {
            throw new IllegalArgumentException("validationResult must not be null");
        }
        this.validationResult = validationResult;
        this.matchedCutEntry = matchedCutEntry;
    }

    /**
     * Creates an evaluation outcome without cut-match information.
     *
     * @param validationResult the validation result
     * @return the evaluation outcome
     * @throws NullPointerException if {@code validationResult} is {@code null}
     */
    static CutAwareValidationResult of(ValidationResult validationResult) {
        if (validationResult.isValid()) {
            return new CutAwareValidationResult(ValidationResult.success(), false);
        }
        return new CutAwareValidationResult(validationResult, false);
    }

    /**
     * Creates an evaluation outcome for a failure that occurred after a cut entry matched.
     *
     * <p>This outcome allows the enclosing evaluator to apply the relevant cut semantics when
     * deciding whether another map-group alternative may be attempted.
     *
     * @param validationResult the failed validation result
     * @return the evaluation outcome carrying cut-match information
     * @throws NullPointerException if {@code validationResult} is {@code null}
     * @throws IllegalArgumentException if {@code validationResult} is valid
     */
    static CutAwareValidationResult ofCutMatchFailure(ValidationResult validationResult) {
        if (validationResult.isValid()) {
            throw new IllegalArgumentException("A successful outcome must not carry matched cut entries");
        }
        return new CutAwareValidationResult(validationResult, true);
    }

    /**
     * Returns the ordinary validation result, without exposing cut-match information.
     *
     * @return the validation result
     */
    ValidationResult getValidationResult() {
        return validationResult;
    }

    /**
     * Indicates whether validation failed after a cut entry matched.
     *
     * <p>This flag is internal evaluation metadata. It does not, by itself, determine whether
     * every enclosing choice must stop evaluating alternatives.
     *
     * @return {@code true} if validation failed after a cut match; {@code false} otherwise
     */
    boolean isMatchedCutEntry() {
        return matchedCutEntry;
    }

}
