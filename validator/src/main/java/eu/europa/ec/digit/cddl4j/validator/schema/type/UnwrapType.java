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
 * CDDL unwrapping type represented by {@code ~typename}.
 *
 * <p>Unwrapping removes one layer from the referenced type. For a tagged type,
 * this exposes the tagged type's underlying type. Array and map unwrapping is
 * handled by the corresponding container type because their underlying groups
 * must be threaded into the containing group.
 *
 * <p>The referenced rule is resolved during validation, consistently with
 * {@link ReferenceType}, so forward references remain supported.
 */
public final class UnwrapType extends CddlType {

    /** Name of the type to unwrap. */
    private final String ruleName;

    /**
     * Creates an unwrapping type.
     *
     * @param ruleName name of the type to unwrap
     */
    public UnwrapType(String ruleName) {
        if (ruleName == null || ruleName.isEmpty()) {
            throw new IllegalArgumentException("ruleName must not be null or empty");
        }
        this.ruleName = ruleName;
    }

    /**
     * Returns the name of the referenced rule.
     *
     * @return referenced rule name
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * Resolves the referenced rule.
     *
     * @param ctx validation context
     * @return referenced type, or {@code null} when the rule is unknown
     */
    CddlType resolve(ValidationContext ctx) {
        return ctx.resolveRule(ruleName);
    }

    /**
     * Resolves and validates the unwrapped type.
     *
     * <p>For a tagged type, validation is delegated to the type contained
     * inside the tag. Array and map types cannot be unwrapped through ordinary
     * value validation because their underlying groups must be threaded into
     * the containing array or map group.
     *
     * @param value the CBOR value to validate
     * @param ctx the validation context
     * @return validation result
     */
    @Override
    public ValidationResult validate(CBORValue value, ValidationContext ctx) {
        CddlType target = ctx.resolveRule(ruleName);

        if (target == null) {
            return fail(ctx, "Reference to unknown rule: '" + ruleName + "'");
        }

        if (target instanceof TaggedType) {
            return ((TaggedType) target).getContentType().validate(value, ctx);
        }

        if (target instanceof ArrayType || target instanceof MapType) {
            return fail(ctx, "Unwrapped array or map '" + ruleName
                    + "' must be used inside an array or map group");
        }

        return fail(ctx, "Cannot unwrap rule '" + ruleName + "'");
    }

    /**
     * Returns the CDDL representation of this type.
     *
     * @return CDDL unwrapping representation
     */
    @Override
    public String toString() {
        return "~" + ruleName;
    }

}
