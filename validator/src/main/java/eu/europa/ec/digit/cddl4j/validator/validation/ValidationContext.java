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

import eu.europa.ec.digit.cddl4j.validator.schema.CddlRuleSet;
import eu.europa.ec.digit.cddl4j.validator.schema.operator.ControlOperator;
import eu.europa.ec.digit.cddl4j.validator.schema.ControlOperatorSet;
import eu.europa.ec.digit.cddl4j.validator.schema.type.CddlType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Carries contextual information through recursive CDDL validation.
 *
 * <p>The context tracks the current instance path for validation error
 * reporting and provides access to the complete CDDL rule set. It also
 * maintains a recursion guard used by
 * {@link eu.europa.ec.digit.cddl4j.validator.schema.type.ReferenceType}
 * to prevent infinite recursion when validating self-referential rules.
 *
 * <p>Instances are immutable by convention. Operations that modify the
 * instance path or recursion guard return a new context while preserving the
 * original context.
 */
public final class ValidationContext {

    /** The CDDL rule set used to resolve named rules. */
    private final CddlRuleSet ruleSet;

    /** Control operators available during validation. */
    private final ControlOperatorSet controlOperators;

    /** The current JSON-Pointer-style path into the validated CBOR instance. */
    private final String instancePath;

    /** Rule/path pairs currently being validated. */
    private final Set<String> recursionGuard;

    /**
     * Creates a root validation context.
     *
     * <p>The instance path is initialized to the empty path and the recursion
     * guard is initially empty.
     *
     * @param ruleSet the CDDL rule set used during validation
     */
    public ValidationContext(CddlRuleSet ruleSet) {
        this(ruleSet, ControlOperatorSet.defaults());
    }

    /**
     * Creates a validation context.
     *
     * @param ruleSet rules used to resolve references
     * @param controlOperators operators available to control types
     */
    public ValidationContext(CddlRuleSet ruleSet, ControlOperatorSet controlOperators) {
        this(ruleSet, controlOperators, "", new HashSet<>());
    }

    /**
     * Creates a validation context with the specified state.
     *
     * @param ruleSet the CDDL rule set used during validation
     * @param instancePath the current instance path
     * @param recursionGuard the rule/path pairs currently being validated
     */
    private ValidationContext(CddlRuleSet ruleSet, ControlOperatorSet controlOperators, String instancePath,
                              Set<String> recursionGuard) {
        Objects.requireNonNull(ruleSet, "ruleSet");
        Objects.requireNonNull(controlOperators, "controlOperators");
        this.ruleSet = ruleSet;
        this.controlOperators = controlOperators;
        this.instancePath = instancePath;
        this.recursionGuard = recursionGuard;
    }

    /**
     * Creates a child context for a nested array element or map value.
     *
     * <p>The supplied segment is appended to the current instance path using
     * a slash separator. If the current path is empty, the resulting path
     * starts with {@code /}.
     *
     * @param segment the array index or map key identifying the child value
     * @return a new validation context with the child instance path
     */
    public ValidationContext child(String segment) {
        String newPath = instancePath.isEmpty() ? "/" + segment : instancePath + "/" + segment;
        return new ValidationContext(ruleSet, controlOperators, newPath, recursionGuard);
    }

    /**
     * Returns the control operator matching the given name available to this validation.
     *
     * <p>NOTE: The method does not perform null check, thus if a control operator is
     * not defined for the given name, a null maybe returned.
     *
     * @param operatorName {@link String} to get operator for
     * @return {@link ControlOperator}
     */
    public ControlOperator getControlOperator(String operatorName) {
        return controlOperators.get(operatorName);
    }

    /**
     * Returns the current path into the validated CBOR instance.
     *
     * <p>The path uses a JSON-Pointer-style representation. The root context
     * is represented by an empty string.
     *
     * @return the current instance path
     */
    public String getInstancePath() {
        return instancePath;
    }

    /**
     * Resolves a named CDDL rule from the rule set.
     *
     * @param name the name of the CDDL rule to resolve
     * @return the resolved CDDL type, or {@code null} if the rule is not
     *         defined
     */
    public CddlType resolveRule(String name) {
        return ruleSet.getRule(name);
    }

    /**
     * Determines whether a CDDL rule is currently being validated.
     *
     * <p>This method is used as a recursion guard to prevent unbounded
     * recursion when a schema contains self-referential or mutually recursive
     * rules. A rule is considered recursive only when the same rule is already
     * active at the current instance path; recursive data structures therefore
     * continue to validate normally at nested paths.
     *
     * @param ruleName the name of the rule to check
     * @return {@code true} if the rule is currently active at this instance
     *         path; {@code false} otherwise
     */
    public boolean isInRecursion(String ruleName) {
        Objects.requireNonNull(ruleName, "ruleName");
        return recursionGuard.contains(ruleName);
    }

    /**
     * Creates a context with the specified rule added to the recursion guard.
     *
     * <p>The current context remains unchanged.
     *
     * @param ruleName the name of the rule being entered
     * @return a new validation context containing the specified rule/path pair
     *         in its recursion guard
     */
    public ValidationContext enterRule(String ruleName) {
        Objects.requireNonNull(ruleName, "ruleName");
        Set<String> next = new HashSet<>(recursionGuard);
        next.add(ruleName);
        return new ValidationContext(ruleSet, controlOperators, instancePath, next);
    }

    /**
     * Creates a context with the specified rule removed from the recursion
     * guard.
     *
     * <p>The current context remains unchanged.
     *
     * @param ruleName the name of the rule being left
     * @return a new validation context with the specified rule/path pair removed
     *         from its recursion guard
     */
    public ValidationContext leaveRule(String ruleName) {
        Objects.requireNonNull(ruleName, "ruleName");
        Set<String> next = new HashSet<>(recursionGuard);
        next.remove(ruleName);
        return new ValidationContext(ruleSet, controlOperators, instancePath, next);
    }

}
