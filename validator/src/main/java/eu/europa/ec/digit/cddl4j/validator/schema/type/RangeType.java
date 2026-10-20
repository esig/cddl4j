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

import eu.europa.ec.digit.cddl4j.cbor.CBORType;
import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.validator.CddlParseException;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a CDDL numeric range.
 *
 * <p>An inclusive range uses the {@code min..max} notation, while an exclusive
 * upper bound uses the {@code min...max} notation. The lower bound is always
 * inclusive.
 *
 * <p>Ranges can constrain integer or floating-point CBOR values. Both bounds
 * may be integer or floating-point literals.
 *
 * <p>Examples include {@code 0..255}, {@code 1..10}, and {@code 0...1}.
 */
public final class RangeType extends CddlType {

    /** The inclusive lower bound of the range. */
    private final CddlType lowerBound;

    /** The upper bound of the range, inclusive or exclusive according to {@link #exclusive}. */
    private final CddlType upperBound;

    /** Whether the upper bound is exclusive. */
    private final boolean exclusive;

    /**
     * Creates a numeric range type.
     *
     * @param lowerBound the inclusive lower bound; expected to be an integer or
     *                   floating-point {@link LiteralType}
     * @param upperBound the upper bound; expected to be an integer or
     *                   floating-point {@link LiteralType}
     * @param exclusive {@code true} to exclude the upper bound, corresponding
     *                  to CDDL {@code ...}; {@code false} to include it,
     *                  corresponding to CDDL {@code ..}
     */
    public RangeType(CddlType lowerBound, CddlType upperBound, boolean exclusive) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.exclusive = exclusive;
    }

    /**
     * Validates whether a CBOR value falls within this numeric range.
     *
     * <p>The value must be an integer or floating-point value. The lower bound
     * is always inclusive. The upper bound is inclusive when this range uses
     * {@code ..} and exclusive when it uses {@code ...}.
     *
     * @param value the CBOR value to validate
     * @param ctx the validation context
     * @return a successful validation result if the value is within the range;
     *         otherwise, a failed validation result describing why the value
     *         is outside the range
     */
    @Override
    public ValidationResult validate(CBORValue value, ValidationContext ctx) {
        CddlType minRule = resolve(lowerBound, ctx);
        CddlType maxRule = resolve(upperBound, ctx);

        CBORType type = value.getType();
        switch (type) {
            case INTEGER:
                if (!isIntegerTypeAllowed(minRule) && !isIntegerTypeAllowed(maxRule)) {
                    return fail(ctx, "Range " + this + " requires an integer value but got " + type);
                }
                break;
            case FLOATING_POINT:
                if (!isFloatTypeAllowed(minRule) && !isFloatTypeAllowed(maxRule)) {
                    return fail(ctx, "Range " + this + " requires a floating point value but got " + type);
                }
                break;
            default:
                return fail(ctx, "Range " + this + " requires a numeric value but got " + PrimitiveType.cborTypeName(value));
        }

        BigDecimal actual = toBigDecimal(value);
        BigDecimal lo = boundAsBigDecimal(minRule);
        BigDecimal hi = boundAsBigDecimal(maxRule);

        if (actual.compareTo(lo) < 0) {
            return fail(ctx, "Value " + actual + " is below lower bound " + lo + " in range " + this);
        }

        if (exclusive) {
            if (actual.compareTo(hi) >= 0) {
                return fail(ctx, "Value " + actual + " must be strictly less than " + hi + " in range " + this);
            }
        } else if (actual.compareTo(hi) > 0) {
            return fail(ctx, "Value " + actual + " exceeds upper bound " + hi + " in range " + this);
        }

        return ValidationResult.success();
    }

    private boolean isIntegerTypeAllowed(CddlType lowerBound) {
        LiteralType.Kind dataKind = getDataKind(lowerBound);
        return LiteralType.Kind.INTEGER == dataKind;
    }

    private boolean isFloatTypeAllowed(CddlType lowerBound) {
        LiteralType.Kind dataKind = getDataKind(lowerBound);
        return LiteralType.Kind.FLOAT == dataKind;
    }

    private LiteralType.Kind getDataKind(CddlType cddlType) {
        if (cddlType instanceof LiteralType) {
            return ((LiteralType) cddlType).getKind();
        }
        throw new UnsupportedOperationException(String.format(
                "Range of type '%s' is not supported.", cddlType.getClass().getSimpleName()));
    }

    private CddlType resolve(CddlType cddlType, ValidationContext ctx) {
        return resolve(cddlType, ctx, new HashSet<>());
    }

    private CddlType resolve(CddlType cddlType, ValidationContext ctx, Set<String> recursionGuard) {
        if (cddlType instanceof ReferenceType) {
            String ruleName = ((ReferenceType) cddlType).getRuleName();
            if (!recursionGuard.add(ruleName)) {
                throw new CddlParseException("Recursive unwrapped rule reference: '" + ruleName + "'");
            }
            CddlType referencedRule = ctx.resolveRule(ruleName);
            if (referencedRule == null) {
                throw new CddlParseException("Reference to unknown rule: '" + ruleName + "'");
            }
            return resolve(referencedRule, ctx, recursionGuard);
        }
        return cddlType;
    }

    /**
     * Converts a numeric CBOR value to a {@link BigDecimal}.
     *
     * @param value the integer or floating-point CBOR value
     * @return the numeric value represented as a {@link BigDecimal}
     */
    private BigDecimal toBigDecimal(CBORValue value) {
        if (value.getType() == CBORType.FLOATING_POINT) {
            return BigDecimal.valueOf(value.asDouble());
        }

        BigInteger integer = value.asInteger();
        return new BigDecimal(integer);
    }

    /**
     * Converts a numeric range bound to a {@link BigDecimal}.
     *
     * <p>Range bounds are expected to be {@link LiteralType} instances
     * produced by the parser. Their CDDL representation is parsed as a
     * decimal number.
     *
     * @param bound the numeric range bound
     * @return the bound represented as a {@link BigDecimal}
     * @throws IllegalStateException if the bound does not have a valid numeric
     *                               representation
     */
    private BigDecimal boundAsBigDecimal(CddlType bound) {
        String value = bound.toString();
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Range bound is not numeric: '" + value + "'", e);
        }
    }

    /**
     * Returns the CDDL representation of this range.
     *
     * @return the CDDL range representation using {@code ..} for an inclusive
     *         upper bound or {@code ...} for an exclusive upper bound
     */
    @Override
    public String toString() {
        return lowerBound + (exclusive ? "..." : "..") + upperBound;
    }

}
