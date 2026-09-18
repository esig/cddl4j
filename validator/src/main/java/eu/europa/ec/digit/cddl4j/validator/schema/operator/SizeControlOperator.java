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

import eu.europa.ec.digit.cddl4j.cbor.CBOR;
import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.validator.schema.type.CddlType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.LiteralType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.ReferenceType;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationError;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Standard implementation of {@code .size}.
 *
 * <p>For byte and text strings, the controller is validated against the
 * number of bytes in the target. Text strings are measured using UTF-8,
 * which is the representation used by CBOR for text strings.
 *
 * <p>For unsigned integers, the controller is an integer literal {@code N}
 * and the target is restricted to values requiring at most {@code N} bytes.
 */
public final class SizeControlOperator implements ControlOperator {

    /** Singleton instance. */
    public static final SizeControlOperator INSTANCE = new SizeControlOperator();

    /** Number of bits in one byte. */
    private static final BigInteger BITS_PER_BYTE = BigInteger.valueOf(8);

    /**
     * Creates the operator.
     */
    private SizeControlOperator() {
        // Singleton.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return ".size";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidationResult validate(CBORValue value, CddlType controller, ValidationContext ctx) {
        if (value.isByteString()) {
            return validateStringSize(value.asBytes().length, controller, ctx);
        }

        if (value.isTextString()) {
            return validateStringSize(value.asString().getBytes(StandardCharsets.UTF_8).length, controller, ctx);
        }

        if (value.isUnsignedInteger()) {
            return validateIntegerSize(value.asInteger(), controller, ctx);
        }

        return failure(ctx, "Control .size requires a byte string, text string, or unsigned integer target");
    }

    /**
     * Validates a string length against the controller type.
     *
     * @param size the string size in bytes
     * @param controller the controller type
     * @param ctx the validation context
     * @return the controller validation result
     */
    private ValidationResult validateStringSize(int size, CddlType controller, ValidationContext ctx) {
        return controller.validate(CBOR.integer(size), ctx);
    }

    /**
     * Validates an unsigned integer against its maximum byte size.
     *
     * @param value the unsigned integer
     * @param controller the controller type
     * @param ctx the validation context
     * @return the validation result
     */
    private ValidationResult validateIntegerSize(BigInteger value, CddlType controller, ValidationContext ctx) {
        BigInteger size = resolveIntegerLiteral(controller, ctx);

        if (size == null) {
            return failure(ctx, "Control .size on an unsigned integer requires an integer literal controller");
        }

        if (size.signum() < 0) {
            return failure(ctx, "Control .size on an unsigned integer requires a non-negative size");
        }

        BigInteger maximumBits = size.multiply(BITS_PER_BYTE);

        if (BigInteger.valueOf(value.bitLength()).compareTo(maximumBits) > 0) {
            return failure(ctx, "Unsigned integer requires more than " + size + " bytes for control .size");
        }

        return ValidationResult.success();
    }

    /**
     * Resolves a controller to an integer literal.
     *
     * <p>References to rules containing an integer literal are supported.
     * Cyclic references are rejected by returning {@code null}.
     *
     * @param controller the controller type
     * @param ctx the validation context
     * @return the integer literal, or {@code null} if the controller is not
     *     an integer literal
     */
    private static BigInteger resolveIntegerLiteral(CddlType controller, ValidationContext ctx) {
        Set<String> visited = new HashSet<>();
        CddlType current = controller;

        while (current instanceof ReferenceType) {
            String ruleName = ((ReferenceType) current).getRuleName();

            if (!visited.add(ruleName)) {
                return null;
            }

            current = ctx.resolveRule(ruleName);

            if (current == null) {
                return null;
            }
        }

        if (!(current instanceof LiteralType)) {
            return null;
        }

        return ((LiteralType) current).getIntegerValueOrNull();
    }

    /**
     * Creates a control operator validation failure.
     *
     * @param ctx the validation context
     * @param message the failure message
     * @return a failed validation result
     */
    private static ValidationResult failure(ValidationContext ctx, String message) {
        return ValidationResult.failure(new ValidationError(ctx.getInstancePath(), message));
    }

}
