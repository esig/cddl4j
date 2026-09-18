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
package eu.europa.ec.digit.cddl4j.validator.schema;

import eu.europa.ec.digit.cddl4j.cbor.CBORDecodeException;
import eu.europa.ec.digit.cddl4j.cbor.CBORDecoder;
import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.validator.schema.operator.ControlOperator;
import eu.europa.ec.digit.cddl4j.validator.schema.type.CddlType;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationError;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a compiled CDDL schema ready for CBOR validation.
 *
 * <p>A schema contains the compiled CDDL rules. Validation can be performed
 * directly on an already-decoded
 * {@link CBORValue} or on a byte array containing an encoded CBOR data item.
 *
 * <p>Instances are immutable and safe to share across threads.
 *
 * <p>Obtain instances through
 * {@link eu.europa.ec.digit.cddl4j.validator.CddlValidator}. For example:
 * <pre>{@code
 * // Use the first user-defined rule as the root rule.
 * CddlSchema schema = CddlValidator.loadSchema(
 *         "person = {\"name\": tstr, \"age\": uint}");
 *
 * // Explicitly select the root rule.
 * CddlSchema schema = CddlValidator.loadSchema(cddlSource, "person");
 *
 * // Override the root rule after loading.
 * CddlSchema schema = CddlValidator.loadSchema(cddlSource)
 *         .withRootRule("person");
 * }</pre>
 */
public final class CddlSchema {

    /** The compiled CDDL rule set. */
    private final CddlRuleSet ruleSet;

    /** Control operators used during validation. */
    private final ControlOperatorSet controlOperators;

    /**
     * The explicitly configured root rule.
     *
     * <p>A {@code null} value means that the default root rule from the rule
     * set is used.
     */
    private final String overrideRoot;

    /**
     * Creates a compiled schema using the rule set's default root rule.
     *
     * <p>The default root rule is the first user-defined rule in the CDDL
     * source.
     *
     * @param ruleSet the compiled CDDL rule set
     */
    public CddlSchema(CddlRuleSet ruleSet) {
        this(ruleSet, null);
    }

    /**
     * Creates a compiled schema with an optional root rule override.
     *
     * @param ruleSet the compiled CDDL rule set
     * @param overrideRoot the rule name to use as the root rule, or
     *                     {@code null} to use the rule set's default root
     */
    private CddlSchema(CddlRuleSet ruleSet, String overrideRoot) {
        this(ruleSet, ControlOperatorSet.defaults(), overrideRoot);
    }

    /**
     * Creates a compiled schema with an optional root rule and control operators override.
     *
     * @param ruleSet the compiled CDDL rule set
     * @param controlOperators control operators to be used
     * @param overrideRoot the rule name to use as the root rule, or
     *                     {@code null} to use the rule set's default root
     */
    private CddlSchema(CddlRuleSet ruleSet, ControlOperatorSet controlOperators, String overrideRoot) {
        Objects.requireNonNull(ruleSet, "ruleSet");
        Objects.requireNonNull(controlOperators, "controlOperators");
        this.ruleSet = ruleSet;
        this.controlOperators = controlOperators;
        this.overrideRoot = overrideRoot;
    }

    /**
     * Returns a new schema that uses the specified rule as its validation
     * entry point.
     *
     * <p>The original schema is not modified.
     *
     * <pre>{@code
     * CddlSchema schema = CddlValidator.loadSchema(source)
     *         .withRootRule("my-rule");
     * schema.validate(cborBytes);
     * }</pre>
     *
     * @param ruleName the name of the rule to use as the validation entry point
     * @return a new schema configured with the specified root rule
     * @throws IllegalArgumentException if {@code ruleName} is {@code null} or
     *                                  blank
     */
    public CddlSchema withRootRule(String ruleName) {
        if (ruleName == null || ruleName.trim().isEmpty()) {
            throw new IllegalArgumentException("ruleName must not be null or blank");
        }

        return new CddlSchema(ruleSet, controlOperators, ruleName);
    }

    /**
     * Returns a new schema with additional application-defined rules.
     *
     * <p>Application-defined rules are resolved after rules declared directly
     * in the CDDL source and before the RFC 8610 prelude.
     *
     * <p>The original schema is not modified.
     *
     * <pre>{@code
     * Map<String, CddlType> customRules = new HashMap<>();
     * customRules.put("my-type", PrimitiveType.TSTR);
     *
     * CddlSchema schema = CddlValidator.loadSchema(
     *         "root = my-type")
     *         .withRules(customRules);
     * }</pre>
     *
     * @param rules application-defined rules
     * @return a new schema configured with the supplied rules
     * @throws NullPointerException if {@code rules} is null
     */
    public CddlSchema withRules(Map<String, CddlType> rules) {
        Objects.requireNonNull(rules, "rules must not be null");
        return new CddlSchema(ruleSet.withCustomRules(rules), controlOperators, overrideRoot);
    }

    /**
     * Returns a new schema with the supplied control operators.
     *
     * <p>The original schema is not modified. Operators with names matching
     * existing operators replace those implementations.
     *
     * @param operators control operators to add or replace
     * @return a new schema using the supplied operators
     * @throws NullPointerException if {@code operators} is null
     */
    public CddlSchema withControlOperators(Collection<ControlOperator> operators) {
        Objects.requireNonNull(operators, "operators must not be null");
        return new CddlSchema(ruleSet, controlOperators.withOperators(operators), overrideRoot);
    }

    /**
     * Returns the effective root rule name used by validation methods that do
     * not explicitly specify a rule.
     *
     * <p>The explicitly configured root rule takes precedence over the default
     * root rule from the rule set.
     *
     * @return the effective root rule name
     */
    public String getRootRuleName() {
        return overrideRoot != null ? overrideRoot : ruleSet.getRootRuleName();
    }

    /**
     * Validates an encoded CBOR data item against the effective root rule.
     *
     * @param cborBytes the raw CBOR-encoded data item
     * @return a validation result indicating whether the CBOR data satisfies
     *         the effective root rule
     */
    public ValidationResult validate(byte[] cborBytes) {
        return validate(cborBytes, getRootRuleName());
    }

    /**
     * Validates an encoded CBOR data item against a specific named rule.
     *
     * @param cborBytes the raw CBOR-encoded data item
     * @param ruleName the name of the rule to use as the validation entry point
     * @return a validation result indicating whether the CBOR data satisfies
     *         the specified rule, or a failed result if decoding fails or the
     *         rule does not exist
     */
    public ValidationResult validate(byte[] cborBytes, String ruleName) {
        CBORValue cbor;

        try {
            cbor = CBORDecoder.decode(cborBytes);
        } catch (CBORDecodeException e) {
            return ValidationResult.failure(
                    new ValidationError("", "Failed to decode CBOR input: " + e.getMessage()));
        }

        return validate(cbor, ruleName);
    }

    /**
     * Validates an encoded CBOR data item from an input stream against the
     * effective root rule.
     *
     * @param cborInput the input stream containing the raw CBOR-encoded data item
     * @return a validation result indicating whether the CBOR data satisfies
     *         the effective root rule
     */
    public ValidationResult validate(InputStream cborInput) {
        return validate(cborInput, getRootRuleName());
    }

    /**
     * Validates an encoded CBOR data item from an input stream against a specific
     * named rule.
     *
     * @param cborInput the input stream containing the raw CBOR-encoded data item
     * @param ruleName the name of the rule to use as the validation entry point
     * @return a validation result indicating whether the CBOR data satisfies the
     *         specified rule, or a failed result if decoding fails or the rule
     *         does not exist
     */
    public ValidationResult validate(InputStream cborInput, String ruleName) {
        CBORValue cbor;

        try {
            cbor = CBORDecoder.decode(cborInput);
        } catch (CBORDecodeException e) {
            return ValidationResult.failure(
                    new ValidationError("", "Failed to decode CBOR input: " + e.getMessage()));
        }

        return validate(cbor, ruleName);
    }

    /**
     * Validates an already-decoded CBOR value against the effective root rule.
     *
     * @param cborValue the pre-decoded CBOR value to validate
     * @return a validation result indicating whether the value satisfies the
     *         effective root rule
     */
    public ValidationResult validate(CBORValue cborValue) {
        return validate(cborValue, getRootRuleName());
    }

    /**
     * Validates an already-decoded CBOR value against a specific named rule.
     *
     * @param cborValue the pre-decoded CBOR value to validate
     * @param ruleName the name of the rule to use as the validation entry point
     * @return a validation result indicating whether the value satisfies the
     *         specified rule, or a failed result if the rule does not exist
     */
    public ValidationResult validate(CBORValue cborValue, String ruleName) {
        CddlType rootType = ruleSet.getRule(ruleName);

        if (rootType == null) {
            return ValidationResult.failure(
                    new ValidationError("", "Unknown rule: '" + ruleName + "'"));
        }

        ValidationContext ctx = new ValidationContext(ruleSet, controlOperators);
        return rootType.validate(cborValue, ctx);
    }

    /**
     * Returns the compiled CDDL rule set used by this schema.
     *
     * <p>The rule set contains the CDDL prelude types and all user-defined
     * rules.
     *
     * @return the compiled CDDL rule set
     */
    public CddlRuleSet getRuleSet() {
        return ruleSet;
    }

    /**
     * Returns the control operator registry used by this schema.
     *
     * @return control operator registry
     */
    public ControlOperatorSet getControlOperators() {
        return controlOperators;
    }

}
