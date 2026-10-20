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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A CDDL group choice, such as {@code (foo // bar)}.
 *
 * <p>A group choice is context-dependent in exactly the same way as a
 * {@link GroupType}: its alternatives describe sequences of group entries,
 * rather than standalone CBOR values. Concrete validation is therefore
 * performed by the surrounding array or map type.
 */
public final class GroupChoiceType extends CddlType {

    /** Group alternatives in source order. */
    private final List<GroupType> alternatives;

    /**
     * Creates a group choice. An empty list represents an empty group socket,
     * which is valid for an unextended {@code $$} socket as defined by RFC 8610.
     *
     * @param alternatives the group alternatives in source order
     */
    public GroupChoiceType(List<GroupType> alternatives) {
        if (alternatives == null) {
            throw new IllegalArgumentException("Group choice alternatives must not be null");
        }
        this.alternatives = Collections.unmodifiableList(new ArrayList<>(alternatives));
    }

    /**
     * Returns the group alternatives.
     *
     * @return immutable group alternatives
     */
    public List<GroupType> getAlternatives() {
        return alternatives;
    }

    /**
     * Groups are context-dependent and cannot be validated directly.
     *
     * @param value the CBOR value
     * @param ctx the validation context
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    @Override
    public ValidationResult validate(CBORValue value, ValidationContext ctx) {
        throw new UnsupportedOperationException(String.format(
                "A CDDL group choice must be used inside an array or map context. Rule : %s", toString()));
    }

    /**
     * Returns the CDDL representation of this group choice.
     *
     * @return group choice representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < alternatives.size(); i++) {
            if (i > 0) {
                sb.append(" // ");
            }
            String value = alternatives.get(i).toString();
            sb.append(value, 1, value.length() - 1);
        }
        return sb.append(')').toString();
    }

}
