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
 * Wraps another CDDL type with an occurrence indicator that constrains how
 * many times the type may appear within an array or map group.
 *
 * <p>The supported occurrence indicators are:
 * <ul>
 *   <li>{@code ?} — zero or one occurrence ({@code min=0}, {@code max=1})</li>
 *   <li>{@code *} — zero or more occurrences ({@code min=0},
 *       {@code max=UNBOUNDED})</li>
 *   <li>{@code +} — one or more occurrences ({@code min=1},
 *       {@code max=UNBOUNDED})</li>
 *   <li>{@code n*m} — between {@code n} and {@code m} occurrences</li>
 * </ul>
 *
 * <p>An {@code OccurrenceType} does not directly validate the number of
 * occurrences in a CBOR value. Array and map validators inspect its bounds
 * and delegate value validation to the wrapped type.
 *
 * <p>When an occurrence type appears at the top level, {@link #validate}
 * validates the single supplied value against the wrapped type. Occurrence
 * bounds are not applied at the top level.
 */
public final class OccurrenceType extends CddlType {

    /** Sentinel used to represent an unlimited maximum occurrence count. */
    public static final int UNBOUNDED = Integer.MAX_VALUE;

    /** The minimum number of occurrences allowed. */
    private final int min;

    /** The maximum number of occurrences allowed. */
    private final int max;

    /** The CDDL type to which the occurrence constraint applies. */
    private final CddlType wrapped;

    /**
     * Creates an optional occurrence of a type.
     *
     * <p>The resulting occurrence constraint is equivalent to the CDDL
     * {@code ?} indicator and permits zero or one occurrence.
     *
     * @param wrapped the CDDL type whose occurrence is optional
     * @return an occurrence type with a minimum of zero and a maximum of one
     */
    public static OccurrenceType optional(CddlType wrapped) {
        return new OccurrenceType(0, 1, wrapped);
    }

    /**
     * Creates a zero-or-more occurrence of a type.
     *
     * <p>The resulting occurrence constraint is equivalent to the CDDL
     * {@code *} indicator and permits any number of occurrences.
     *
     * @param wrapped the CDDL type that may occur zero or more times
     * @return an occurrence type with a minimum of zero and no upper limit
     */
    public static OccurrenceType zeroOrMore(CddlType wrapped) {
        return new OccurrenceType(0, UNBOUNDED, wrapped);
    }

    /**
     * Creates a one-or-more occurrence of a type.
     *
     * <p>The resulting occurrence constraint is equivalent to the CDDL
     * {@code +} indicator and requires at least one occurrence.
     *
     * @param wrapped the CDDL type that must occur at least once
     * @return an occurrence type with a minimum of one and no upper limit
     */
    public static OccurrenceType oneOrMore(CddlType wrapped) {
        return new OccurrenceType(1, UNBOUNDED, wrapped);
    }

    /**
     * Creates an occurrence constraint requiring exactly the specified number
     * of occurrences.
     *
     * <p>The resulting occurrence constraint is represented as
     * {@code n*n} by {@link #toString()}.
     *
     * @param n the exact number of occurrences required
     * @param wrapped the CDDL type to which the occurrence constraint applies
     * @return an occurrence type requiring exactly {@code n} occurrences
     */
    public static OccurrenceType exactly(int n, CddlType wrapped) {
        return new OccurrenceType(n, n, wrapped);
    }

    /**
     * Creates an occurrence constraint with the specified minimum and maximum
     * number of occurrences.
     *
     * <p>Use {@link #UNBOUNDED} as the maximum to represent an unlimited upper
     * bound.
     *
     * @param min the minimum number of occurrences
     * @param max the maximum number of occurrences, or
     *            {@link #UNBOUNDED} for no upper limit
     * @param wrapped the CDDL type to which the occurrence constraint applies
     * @return an occurrence type with the specified bounds
     */
    public static OccurrenceType range(int min, int max, CddlType wrapped) {
        return new OccurrenceType(min, max, wrapped);
    }

    /**
     * Creates an occurrence type with the specified occurrence bounds.
     *
     * @param min the minimum number of occurrences
     * @param max the maximum number of occurrences, or
     *            {@link #UNBOUNDED} for no upper limit
     * @param wrapped the CDDL type to which the occurrence constraint applies
     */
    public OccurrenceType(int min, int max, CddlType wrapped) {
        this.min = min;
        this.max = max;
        this.wrapped = wrapped;
    }

    /**
     * Returns the minimum number of occurrences allowed by this constraint.
     *
     * @return the minimum occurrence count
     */
    public int getMin() {
        return min;
    }

    /**
     * Returns the maximum number of occurrences allowed by this constraint.
     *
     * @return the maximum occurrence count, or {@link #UNBOUNDED} for no upper limit
     */
    public int getMax() {
        return max;
    }

    /**
     * Returns the CDDL type wrapped by this occurrence constraint.
     *
     * @return the wrapped CDDL type
     */
    public CddlType getWrapped() {
        return wrapped;
    }

    /**
     * Validates a single CBOR value against the wrapped CDDL type.
     *
     * <p>Occurrence bounds are not evaluated by this method. They are applied
     * by the array and map validators when processing groups. At the top level,
     * the supplied value is therefore validated as a single occurrence of the
     * wrapped type.
     *
     * @param value the CBOR value to validate
     * @param ctx the validation context
     * @return the validation result produced by the wrapped type
     */
    @Override
    public ValidationResult validate(CBORValue value, ValidationContext ctx) {
        return wrapped.validate(value, ctx);
    }

    /**
     * Returns the CDDL representation of this occurrence type.
     *
     * @return the CDDL occurrence indicator followed by the wrapped type
     */
    @Override
    public String toString() {
        String occur;
        if (min == 0 && max == 1) {
            occur = "?";
        } else if (min == 0 && max == UNBOUNDED) {
            occur = "*";
        } else if (min == 1 && max == UNBOUNDED) {
            occur = "+";
        } else if (max == UNBOUNDED) {
            occur = min + "*";
        } else {
            occur = min + "*" + max;
        }
        return occur + " " + wrapped;
    }

}
