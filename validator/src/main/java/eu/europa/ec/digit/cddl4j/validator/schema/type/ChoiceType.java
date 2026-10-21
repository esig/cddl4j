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
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationError;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CDDL union type — {@code typeA / typeB / typeC}.
 *
 * <p>The CBOR value is valid if it matches <em>at least one</em> of the alternatives. Each alternative is evaluated
 * independently. CDDL map cuts govern key allocation within a single map-group evaluation; they do not prevent a
 * separate type or group-choice alternative from being tried.
 *
 * <p>If multiple alternatives fail after matching a cut entry, the choice intersects their error lists. Errors shared
 * by alternatives are retained as the common failure, while unrelated cut failures retain their distinct diagnostics.
 * This avoids reporting branch-specific missing-key errors alongside a failure common to expanded alternatives.
 */
public final class ChoiceType extends CddlType implements CutAwareType {

    /** Possible choice options */
    private final List<CddlType> alternatives;

    /**
     * Creates a choice from its alternatives.
     *
     * @param alternatives the possible types, in schema order
     */
    public ChoiceType(List<CddlType> alternatives) {
        this.alternatives = Collections.unmodifiableList(new ArrayList<>(alternatives));
    }

    /**
     * Returns the alternatives in schema order.
     *
     * @return an immutable list of alternatives
     */
    public List<CddlType> getAlternatives() {
        return alternatives;
    }

    @Override
    public ValidationResult validate(CBORValue value, ValidationContext ctx) {
        return validateCutAware(value, ctx).getValidationResult();
    }

    @Override
    public CutAwareValidationResult validateCutAware(CBORValue value, ValidationContext ctx) {
        List<ValidationError> branchErrors = new ArrayList<>();
        List<ValidationError> cutFailures = new ArrayList<>();
        for (CddlType alt : alternatives) {
            ValidationResult result;
            boolean matchedCutEntry = false;
            if (alt instanceof CutAwareType) {
                CutAwareValidationResult entryBasedResult = ((CutAwareType) alt).validateCutAware(value, ctx);
                result = entryBasedResult.getValidationResult();
                matchedCutEntry = entryBasedResult.isMatchedCutEntry();
            } else {
                result = alt.validate(value, ctx);
            }

            if (result.isValid()) {
                return CutAwareValidationResult.of(ValidationResult.success());
            }

            ValidationError branchError = new ValidationError(ctx.getInstancePath(),
                    "Choice alternative (" + alt + ") validation failed", result.getErrors());
            branchErrors.add(branchError);

            if (matchedCutEntry) {
                cutFailures.add(branchError);
            }
        }

        // Cut metadata affects diagnostic selection, never whether alternatives are attempted.
        if (!cutFailures.isEmpty()) {
            ValidationResult result;
            if (cutFailures.size() == 1) {
                // return the cause, as there are no alternative options for the failure
                result = ValidationResult.failure(cutFailures.get(0).getCauses());
            } else {
                result = ValidationResult.failure(new ValidationError(ctx.getInstancePath(),
                        "Value did not match any alternative in choice (" + this + ")", cutFailures));
            }
            return CutAwareValidationResult.ofCutMatchFailure(result);
        }

        ValidationResult result = ValidationResult.failure(new ValidationError(ctx.getInstancePath(),
                "Value did not match any alternative in choice (" + this + ")", branchErrors));
        return CutAwareValidationResult.of(result);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < alternatives.size(); i++) {
            if (i > 0) {
                sb.append(" / ");
            }
            sb.append(alternatives.get(i));
        }
        return sb.toString();
    }

}
