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
package eu.europa.ec.digit.cddl4j.validator.schema.operator;

import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.validator.schema.type.CddlType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.ControlType;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

/**
 * Defines the validation semantics of a CDDL control operator.
 *
 * <p>Implementations should be stateless and thread-safe because a configured
 * operator may be shared by multiple schemas and validation operations.
 */
public interface ControlOperator {

    /**
     * Returns the CDDL spelling of this operator.
     *
     * <p>The returned name includes the leading dot, for example {@code .cbor}.
     *
     * @return the CDDL operator name
     */
    String getName();

    /**
     * Validates the target value against the controller type.
     *
     * <p>The target type has already been validated by {@link ControlType}
     * before this method is invoked.
     *
     * @param value the target value
     * @param controller the controller type
     * @param ctx the current validation context
     * @return the validation result
     */
    ValidationResult validate(CBORValue value, CddlType controller, ValidationContext ctx);

}