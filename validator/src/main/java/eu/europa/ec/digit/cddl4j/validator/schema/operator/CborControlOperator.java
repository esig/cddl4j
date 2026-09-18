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

import eu.europa.ec.digit.cddl4j.cbor.CBORDecodeException;
import eu.europa.ec.digit.cddl4j.cbor.CBORDecoder;
import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.validator.schema.type.CddlType;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationError;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

/**
 * Standard implementation of {@code .cbor}.
 */
public final class CborControlOperator implements ControlOperator {

    /** Singleton instance. */
    public static final CborControlOperator INSTANCE = new CborControlOperator();

    /**
     * Default constructor
     */
    private CborControlOperator() {
        // Singleton.
    }

    @Override
    public String getName() {
        return ".cbor";
    }

    @Override
    public ValidationResult validate(CBORValue value, CddlType controller, ValidationContext ctx) {
        if (!value.isByteString()) {
            return ValidationResult.failure(new ValidationError(ctx.getInstancePath(),
                    "Control .cbor requires a byte string target"));
        }
        try {
            return controller.validate(CBORDecoder.decode(value.asBytes()), ctx);

        } catch (CBORDecodeException exception) {
            return ValidationResult.failure(new ValidationError(ctx.getInstancePath(),
                    "Invalid CBOR for .cbor: " + exception.getMessage()));
        }
    }

}