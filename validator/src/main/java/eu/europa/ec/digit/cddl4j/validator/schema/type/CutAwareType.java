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

import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

/**
 * Internal contract for types that support cut-aware evaluation.
 *
 * <p>Implementations preserve cut-match information needed by map and map-group-choice
 * evaluators. Ordinary callers should use the public type-validation API, which returns
 * {@link ValidationResult} without exposing this internal control-flow state.
 */
interface CutAwareType {

    /**
     * Validates a value while preserving cut-match information for internal evaluation.
     *
     * <p>The returned outcome may indicate that validation failed after a cut entry matched.
     * Enclosing evaluators must interpret that information according to the semantics of the
     * choice being evaluated. In particular, cut state must not unintentionally prevent
     * backtracking across unrelated type-choice alternatives.
     *
     * @param value the decoded CBOR value to validate
     * @param ctx the current validation context
     * @return the validation outcome, including any relevant cut-match information
     */
    CutAwareValidationResult validateCutAware(CBORValue value, ValidationContext ctx);
    
}
