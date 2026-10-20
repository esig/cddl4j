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
import java.util.List;

/**
 * CDDL group enumeration type — {@code &(group)} or {@code &groupname}.
 *
 * <p>A group enumeration turns the value types contributed by a CDDL group
 * into a type choice. Member names and occurrence constraints are not part of
 * the resulting enumeration.
 *
 * <p>Named groups are resolved during validation, consistently with
 * {@link ReferenceType}. A group reference occurring inside another group is
 * expanded recursively when its referenced rule resolves to a {@link GroupType}.
 */
public final class GroupEnumerationType extends CddlType {

    /** Inline group, or {@code null} when this enumeration references a named group. */
    private final CddlType group;

    /** Referenced group name, or {@code null} for an inline group. */
    private final String groupName;

    /**
     * Creates a group enumeration from an inline group.
     *
     * @param group the group whose values form the enumeration
     */
    public GroupEnumerationType(CddlType group) {
        if (!(group instanceof GroupType) && !(group instanceof GroupChoiceType)) {
            throw new IllegalArgumentException("group must be a GroupType or GroupChoiceType");
        }
        this.group = group;
        this.groupName = null;
    }

    /**
     * Creates a group enumeration from a named group.
     *
     * @param groupName the name of the group
     */
    public GroupEnumerationType(String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            throw new IllegalArgumentException("groupName must not be null or empty");
        }
        this.group = null;
        this.groupName = groupName;
    }

    /**
     * Validates a CBOR value against the values contributed by the group.
     *
     * <p>The group is resolved lazily so forward references work in the same
     * way as ordinary {@link ReferenceType} instances.
     *
     * @param value the CBOR value to validate
     * @param ctx the validation context
     * @return a successful result when the value matches one of the enumerated values
     */
    @Override
    public ValidationResult validate(CBORValue value, ValidationContext ctx) {
        CddlType resolvedGroup = resolveGroup(ctx);
        if (resolvedGroup == null) {
            return fail(ctx, "Group enumeration references unknown group: '" + groupName + "'");
        }

        List<CddlType> alternatives = new ArrayList<>();
        for (List<GroupEntry> alternative : GroupExpansion.expand(resolvedGroup, ctx)) {
            for (GroupEntry entry : alternative) {
                if (entry.getType() != null) {
                    alternatives.add(entry.getType());
                }
            }
        }

        if (alternatives.isEmpty()) {
            return fail(ctx, "Group enumeration '" + this + "' does not contain any values");
        }

        List<ValidationError> branchErrors = new ArrayList<>();
        for (CddlType alternative : alternatives) {
            ValidationResult result = alternative.validate(value, ctx);
            if (result.isValid()) {
                return ValidationResult.success();
            }

            branchErrors.add(new ValidationError(
                    ctx.getInstancePath(),
                    "Group enumeration alternative (" + alternative + ") validation failed",
                    result.getErrors()));
        }

        return ValidationResult.failure(new ValidationError(
                ctx.getInstancePath(),
                "Value did not match any value in group enumeration (" + this + ")",
                branchErrors));
    }

    /**
     * Resolves the group represented by this enumeration.
     *
     * @param ctx the current validation context
     * @return the resolved group
     */
    private CddlType resolveGroup(ValidationContext ctx) {
        if (group != null) {
            return group;
        }

        CddlType target = ctx.resolveRule(groupName);
        return target instanceof GroupType || target instanceof GroupChoiceType ? target : null;
    }

    /**
     * Returns the CDDL representation of this enumeration.
     *
     * @return CDDL group enumeration representation
     */
    @Override
    public String toString() {
        return "&" + (group != null ? group : groupName);
    }

}
