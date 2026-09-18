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
package eu.europa.ec.digit.cddl4j.validator.schema.type;

import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Represents a CDDL type for a tagged CBOR value.
 *
 * <p>A tagged type validates the outermost CBOR tag and then validates the
 * value contained by that tag against the specified content type. A specific
 * tag number may be required, or any tag number may be accepted.
 *
 * <p>For example, {@code #6.1(uint)} accepts a tagged CBOR value such as
 * {@code 1(42)}, provided that the value contained by tag {@code 1} is an
 * unsigned integer.
 *
 * <p>Nested tags are handled by nesting {@code TaggedType} instances. For
 * example, {@code #6.1(#6.2(uint))} requires tag {@code 1} containing tag
 * {@code 2} containing an unsigned integer.
 */
public final class TaggedType extends CddlType {

    /**
     * Indicates that any CBOR tag number is accepted.
     *
     * <p>A {@code null} value is used to represent the CDDL generic tag
     * notation {@code #6(type)}.
     */
    public static final BigInteger ANY_TAG = null;

    /** The expected CBOR tag number, or {@link #ANY_TAG} for any tag. */
    private final BigInteger tagNumber;

    /** The CDDL type expected inside the tag. */
    private final CddlType contentType;

    /**
     * Creates a tagged type that accepts any CBOR tag number.
     *
     * <p>The resulting type corresponds to the CDDL notation
     * {@code #6(type)}, where {@code type} is the supplied content type.
     *
     * @param contentType the type expected inside the tag
     */
    public TaggedType(CddlType contentType) {
        this(null, contentType);
    }

    /**
     * Creates a tagged type with a specific tag number.
     *
     * <p>The resulting type corresponds to the CDDL notation
     * {@code #6.tag(type)}, where {@code tag} is {@code tagNumber} and
     * {@code type} is the supplied content type.
     *
     * @param tagNumber the expected unsigned CBOR tag number
     * @param contentType the type expected inside the tag
     */
    public TaggedType(long tagNumber, CddlType contentType) {
        this(BigInteger.valueOf(tagNumber), contentType);
    }

    /**
     * Creates a tagged type with a specific or unrestricted tag number.
     *
     * <p>The tag number must be an unsigned 64-bit integer when specified.
     * A {@code null} tag number means that any CBOR tag number is accepted.
     *
     * @param tagNumber the expected unsigned 64-bit CBOR tag number, or
     *                  {@code null} to accept any tag number
     * @param contentType the type expected inside the tag
     * @throws IllegalArgumentException if {@code tagNumber} is negative or
     *                                  does not fit in an unsigned 64-bit
     *                                  integer
     * @throws NullPointerException if {@code contentType} is {@code null}
     */
    public TaggedType(BigInteger tagNumber, CddlType contentType) {
        if (tagNumber != null && (tagNumber.signum() < 0 || tagNumber.bitLength() > 64)) {
            throw new IllegalArgumentException("CBOR tag number must be an unsigned 64-bit integer");
        }

        this.tagNumber = tagNumber;
        this.contentType = Objects.requireNonNull(contentType, "contentType");
    }

    /**
     * Returns the type contained by this CBOR tag.
     *
     * @return the contained type
     */
    public CddlType getContentType() {
        return contentType;
    }

    /**
     * Validates a CBOR value against this tagged type.
     *
     * <p>The value must be a CBOR tag. If a specific tag number was configured,
     * the outermost tag must have that number. The value contained by the tag
     * is then validated against the configured content type.
     *
     * @param value the CBOR value to validate
     * @param ctx the validation context
     * @return a successful validation result if the value has an accepted tag
     *         and its tagged content satisfies the content type; otherwise,
     *         a failed validation result
     */
    @Override
    public ValidationResult validate(CBORValue value, ValidationContext ctx) {
        if (!value.isTag()) {
            return fail(ctx, "Expected tagged value (#6" + tagSuffix() + ") but got untagged " + PrimitiveType.cborTypeName(value));
        }

        BigInteger actualTag = value.getTag();

        if (tagNumber != ANY_TAG && !tagNumber.equals(actualTag)) {
            return fail(ctx, "Expected CBOR tag " + tagNumber + " but got tag " + actualTag);
        }

        return contentType.validate(value.getTaggedValue(), ctx);
    }

    /**
     * Returns the CDDL representation of this tagged type.
     *
     * <p>A specific tag is represented as {@code #6.tag(type)}, while an
     * unrestricted tag is represented as {@code #6(type)}.
     *
     * @return the CDDL tagged-type representation
     */
    @Override
    public String toString() {
        return "#6" + tagSuffix() + "(" + contentType + ")";
    }

    /**
     * Returns the CDDL suffix representing the configured tag number.
     *
     * @return an empty string when any tag is accepted; otherwise, a dot
     *         followed by the tag number
     */
    private String tagSuffix() {
        return tagNumber == ANY_TAG ? "" : "." + tagNumber;
    }

}
