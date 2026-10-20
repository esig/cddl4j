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
 * Represents a reference to a named CDDL rule.
 *
 * <p>A reference is resolved during validation rather than when the schema is
 * constructed. This allows forward references and recursive rule definitions
 * to be handled correctly.
 *
 * <p>For example:
 * <pre>{@code
 * person = {
 *     name: tstr,
 *     home: address
 * }
 * address = {
 *     street: tstr,
 *     city: tstr
 * }
 * }</pre>
 *
 * <p>In this example, {@code address} is represented by a
 * {@code ReferenceType} and resolved when the {@code person} rule is
 * validated.
 */
public final class ReferenceType extends CddlType {

    /** The name of the referenced CDDL rule. */
    private final String ruleName;

    /**
     * Creates a reference to a named CDDL rule.
     *
     * @param ruleName the name of the CDDL rule to reference
     */
    public ReferenceType(String ruleName) {
        this.ruleName = ruleName;
    }

    /**
     * Returns the name of the referenced CDDL rule.
     *
     * @return the referenced rule name
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * Validates a CBOR value against the referenced CDDL rule.
     *
     * <p>The referenced rule is resolved through the validation context at
     * validation time. If the same reference is already being evaluated at
     * the current instance path, validation fails to terminate an invalid
     * zero-progress recursion cycle.
     *
     * @param value the CBOR value to validate
     * @param ctx the validation context used to resolve the rule and track
     *            recursive validation
     * @return a successful validation result when the referenced rule accepts
     *         the value; otherwise, a failed validation result
     */
    @Override
    public ValidationResult validate(CBORValue value, ValidationContext ctx) {
        // Guard against zero-progress recursion in self-referential schemas.
        if (ctx.isInRecursion(ruleName)) {
            // Treat any value as valid to break the cycle.
            return ValidationResult.success(); // TODO : review
        }

        CddlType target = ctx.resolveRule(ruleName);
        if (target == null) {
            return fail(ctx, "Reference to unknown rule: '" + ruleName + "'");
        }

        ValidationContext innerCtx = ctx.enterRule(ruleName);
        return target.validate(value, innerCtx);
    }

    /**
     * Returns the CDDL rule name represented by this reference.
     *
     * @return the referenced rule name
     */
    @Override
    public String toString() {
        return ruleName;
    }

}
