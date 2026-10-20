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
 * A CDDL group enclosed in parentheses.
 *
 * <p>A group is a context-dependent sequence of group entries. In a map
 * context, member keys are used to match map entries. In an array context,
 * only the value part of each entry is significant.
 *
 * <p>A group normally does not constitute a standalone CDDL type; it becomes
 * part of a concrete type when embedded in a map or array.
 */
public final class GroupType extends CddlType {

    /** Group entries in source order. */
    private final List<GroupEntry> entries;

    /**
     * Creates a group.
     *
     * @param entries the group entries in source order
     */
    public GroupType(List<GroupEntry> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * Returns the entries in this group.
     *
     * @return immutable group entries
     */
    public List<GroupEntry> getEntries() {
        return entries;
    }

    /**
     * Groups are context-dependent and therefore cannot be validated directly.
     *
     * @param value the CBOR value
     * @param ctx the validation context
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    @Override
    public ValidationResult validate(CBORValue value, ValidationContext ctx) {
        throw new UnsupportedOperationException(String.format(
                "A CDDL group must be used inside an array or map context. Rule : %s", toString()));
    }

    /**
     * Returns the CDDL representation of this group.
     *
     * @return parenthesized group representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(entries.get(i));
        }
        return sb.append(")").toString();
    }

}