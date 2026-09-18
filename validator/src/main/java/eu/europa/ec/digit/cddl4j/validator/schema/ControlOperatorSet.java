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

import eu.europa.ec.digit.cddl4j.validator.schema.operator.CborControlOperator;
import eu.europa.ec.digit.cddl4j.validator.schema.operator.CborSequenceControlOperator;
import eu.europa.ec.digit.cddl4j.validator.schema.operator.ControlOperator;
import eu.europa.ec.digit.cddl4j.validator.schema.operator.SizeControlOperator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable registry of CDDL control operators.
 *
 * <p>The default registry contains the control operators implemented by
 * cddl4j. Application-defined operators can be added or replaced without
 * modifying the global defaults.
 */
public final class ControlOperatorSet {

    /** Registered control operators. */
    private final Map<String, ControlOperator> operators;

    /**
     * Creates an empty registry.
     */
    public ControlOperatorSet() {
        this(Collections.emptyList());
    }

    /**
     * Creates a registry from the supplied operators.
     *
     * @param operators control operators
     */
    public ControlOperatorSet(Collection<ControlOperator> operators) {
        Objects.requireNonNull(operators, "operators must not be null");

        LinkedHashMap<String, ControlOperator> copy = new LinkedHashMap<>();
        for (ControlOperator operator : operators) {
            Objects.requireNonNull(operator, "operators must not contain null values");
            validateName(operator.getName());
            copy.put(operator.getName(), operator);
        }

        this.operators = Collections.unmodifiableMap(copy);
    }

    /**
     * Returns a registry containing the standard cddl4j control operators.
     *
     * @return the default registry
     */
    public static ControlOperatorSet defaults() {
        List<ControlOperator> operators = new ArrayList<>();
        operators.add(SizeControlOperator.INSTANCE);
        operators.add(CborControlOperator.INSTANCE);
        operators.add(CborSequenceControlOperator.INSTANCE);
        return new ControlOperatorSet(operators);
    }

    /**
     * Returns a new registry containing the supplied operator.
     *
     * @param operator the operator to add or replace
     * @return a new registry
     */
    public ControlOperatorSet withOperator(ControlOperator operator) {
        Objects.requireNonNull(operator, "operator must not be null");
        validateName(operator.getName());

        List<ControlOperator> values = new ArrayList<>(operators.values());
        values.add(operator);
        return new ControlOperatorSet(values);
    }

    /**
     * Returns a new registry containing all supplied operators.
     *
     * @param additionalOperators operators to add or replace
     * @return a new registry
     */
    public ControlOperatorSet withOperators(Collection<ControlOperator> additionalOperators) {
        Objects.requireNonNull(additionalOperators, "additionalOperators must not be null");

        List<ControlOperator> values = new ArrayList<>(operators.values());
        values.addAll(additionalOperators);
        return new ControlOperatorSet(values);
    }

    /**
     * Returns the operator with the supplied CDDL name.
     *
     * @param name operator name
     * @return the operator, or {@code null} if it is not registered
     */
    public ControlOperator get(String name) {
        return operators.get(name);
    }

    /**
     * Returns all registered operators.
     *
     * @return an immutable operator map
     */
    public Map<String, ControlOperator> getOperators() {
        return operators;
    }

    private static void validateName(String name) {
        if (name == null || name.length() < 2 || name.charAt(0) != '.') {
            throw new IllegalArgumentException(
                    "Control operator name must start with '.' and contain a name");
        }
    }

}