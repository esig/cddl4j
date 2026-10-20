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
import eu.europa.ec.digit.cddl4j.validator.schema.operator.ControlOperator;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

import java.util.Objects;

/**
 * CDDL control type connecting a target type to a controller through a
 * named control operator.
 */
public final class ControlType extends CddlType {

    /** Target type. */
    private final CddlType target;

    /** CDDL operator name. */
    private final String operator;

    /** Controller type. */
    private final CddlType controller;

    /**
     * Creates a control type.
     *
     * @param target target type
     * @param operator CDDL operator name
     * @param controller controller type
     */
    public ControlType(CddlType target, String operator, CddlType controller) {
        this.target = Objects.requireNonNull(target, "target");
        this.operator = Objects.requireNonNull(operator, "operator");
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    /**
     * Returns the target type.
     *
     * @return target type
     */
    public CddlType getTarget() {
        return target;
    }

    /**
     * Returns the CDDL operator name.
     *
     * @return operator name
     */
    public String getOperator() {
        return operator;
    }

    /**
     * Returns the controller type.
     *
     * @return controller type
     */
    public CddlType getController() {
        return controller;
    }

    @Override
    public ValidationResult validate(CBORValue value, ValidationContext ctx) {
        ValidationResult targetResult = target.validate(value, ctx);
        if (!targetResult.isValid()) {
            return targetResult;
        }

        ControlOperator implementation = ctx.getControlOperator(operator);
        if (implementation == null) {
            throw new IllegalStateException(String.format("No control operator registered for '%s'", operator));
        }

        return implementation.validate(value, controller, ctx.child(operator));
    }

    @Override
    public String toString() {
        return target + " " + operator + " " + controller;
    }

}