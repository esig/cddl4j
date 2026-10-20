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
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationError;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * CDDL array type: {@code [entry, entry, ...]}.
 *
 * <p>Array entries are matched as a sequence. When an entry references a
 * CDDL group, its occurrence applies to the complete group sequence, as
 * required by RFC 8610. For example, {@code [* person]} repeats the complete
 * {@code person} group rather than repeating each member independently.
 */
public final class ArrayType extends CddlType {

    /** Array entries. */
    private final List<GroupEntry> entries;

    /**
     * Creates an array type from the supplied entries.
     *
     * @param entries a list of array entries
     */
    public ArrayType(List<GroupEntry> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * Returns the array entries.
     *
     * @return an immutable list of entries
     */
    public List<GroupEntry> getEntries() {
        return entries;
    }

    /**
     * Validates a CBOR array against this type.
     *
     * @param value the value to validate
     * @param ctx the validation context
     * @return the validation result
     */
    @Override
    public ValidationResult validate(CBORValue value, ValidationContext ctx) {
        if (value.getType() != CBORType.ARRAY) {
            return fail(ctx, "Expected array but got " + PrimitiveType.cborTypeName(value));
        }

        List<ValidationError> errors = new ArrayList<>();
        List<GroupEntry> source = expandUnwrappedReferences(entries, ctx, new HashSet<>());
        List<List<GroupEntry>> alternatives = GroupExpansion.expandForArray(new GroupType(source), ctx);
        for (List<GroupEntry> alternative : alternatives) {
            Set<Integer> matches = matchSequence(alternative, 0, 0, value, ctx, new HashSet<>(), errors);
            if (matches.contains(value.size())) {
                return ValidationResult.success();
            }
        }

        if (errors.isEmpty()) {
            errors.add(new ValidationError(ctx.getInstancePath(), "Array value did not match any allowed entry sequence"));
        }
        return ValidationResult.failure(errors);
    }

    /**
     * Returns all array indexes reachable after matching the remainder of a
     * group-entry sequence.
     *
     * @param sequence the sequence to match
     * @param entryIndex current entry index
     * @param itemIndex current array index
     * @param value array being validated
     * @param ctx validation context
     * @param active active recursive matcher states
     * @return reachable array indexes
     */
    private Set<Integer> matchSequence(List<GroupEntry> sequence, int entryIndex, int itemIndex,
                                       CBORValue value, ValidationContext ctx, Set<MatchState> active,
                                       List<ValidationError> errors) {
        if (entryIndex == sequence.size()) {
            return Collections.singleton(itemIndex);
        }
        MatchState state = new MatchState(sequence, entryIndex, itemIndex);
        if (!active.add(state)) {
            return Collections.emptySet();
        }

        GroupEntry entry = sequence.get(entryIndex);
        Set<Integer> result = new LinkedHashSet<>();
        List<Integer> starts = Collections.singletonList(itemIndex);
        List<Integer> ends = isGroupReference(entry, ctx)
                ? matchGroupOccurrences(entry, starts, value, ctx, active, errors)
                : matchValueOccurrences(entry, starts, value, ctx, errors);

        for (Integer end : ends) {
            result.addAll(matchSequence(sequence, entryIndex + 1, end, value, ctx, active, errors));
        }

        active.remove(state);
        return result;
    }

    private List<Integer> matchValueOccurrences(GroupEntry entry, List<Integer> starts, CBORValue value,
                                                ValidationContext ctx, List<ValidationError> errors) {
        List<Integer> current = starts;
        int max = entry.getMax() == OccurrenceType.UNBOUNDED ? value.size() : entry.getMax();
        List<Integer> result = new ArrayList<>();
        if (entry.getMin() == 0) {
            result.addAll(current);
        }

        for (int count = 1; count <= max && !current.isEmpty(); count++) {
            List<Integer> next = new ArrayList<>();
            for (Integer index : current) {
                if (index >= value.size()) {
                    continue;
                }
                ValidationResult validation = entry.getType().validate(
                        value.get(index.intValue()), ctx.item(String.valueOf(index)));
                if (validation.isValid()) {
                    next.add(index + 1);
                } else {
                    errors.addAll(validation.getErrors());
                }
            }
            current = next;
            if (count >= entry.getMin()) {
                result.addAll(current);
            }
        }
        return result;
    }

    private List<Integer> matchGroupOccurrences(GroupEntry entry, List<Integer> starts, CBORValue value,
                                                ValidationContext ctx, Set<MatchState> active,
                                                List<ValidationError> errors) {
        String groupName = groupName(entry);
        CddlType groupResolved = entry.isGroupReference() ? ctx.resolveRule(groupName) : entry.getType();
        CddlType group = GroupExpansion.resolveGroup(groupResolved, ctx);
        if (group == null) {
            throw new CddlParseException("Rule '" + groupName + "' is not a CDDL group");
        }

        List<List<GroupEntry>> alternatives = GroupExpansion.expandForArray(group, ctx);
        int max = entry.getMax() == OccurrenceType.UNBOUNDED ? value.size() : entry.getMax();
        List<Integer> current = new ArrayList<>(starts);
        List<Integer> result = new ArrayList<>();
        if (entry.getMin() == 0) {
            result.addAll(current);
        }

        for (int count = 1; count <= max && !current.isEmpty(); count++) {
            List<Integer> next = new ArrayList<>();
            for (Integer start : current) {
                for (List<GroupEntry> alternative : alternatives) {
                    next.addAll(matchSequence(alternative, 0, start, value, ctx, active, errors));
                }
            }
            current = unique(next);
            if (count >= entry.getMin()) {
                result.addAll(current);
            }
        }
        return unique(result);
    }

    private boolean isGroupReference(GroupEntry entry, ValidationContext ctx) {
        if (entry.isGroupReference()) {
            return true;
        }
        return !entry.hasKey() && GroupExpansion.resolveGroup(entry.getType(), ctx) != null;
    }

    private String groupName(GroupEntry entry) {
        if (entry.isGroupReference()) {
            return entry.getGroupReference();
        }
        if (entry.getType() instanceof ReferenceType) {
            return ((ReferenceType) entry.getType()).getRuleName();
        }
        return "<inline group>";
    }

    private List<GroupEntry> expandUnwrappedReferences(List<GroupEntry> source, ValidationContext ctx,
                                                       Set<String> expanding) {
        List<GroupEntry> result = new ArrayList<>();
        for (GroupEntry entry : source) {
            if (!entry.isUnwrappedReference()) {
                result.add(entry);
                continue;
            }

            String ruleName = entry.getGroupReference();
            CddlType referenced = ctx.resolveRule(ruleName);
            if (!(referenced instanceof ArrayType)) {
                result.add(entry);
                continue;
            }
            if (entry.getMin() != 1 || entry.getMax() != 1) {
                throw new CddlParseException(
                        "Occurrence modifiers are not supported for unwrapped rule '" + ruleName + "'");
            }
            if (!expanding.add(ruleName)) {
                throw new CddlParseException("Recursive unwrapped rule reference: '" + ruleName + "'");
            }
            result.addAll(expandUnwrappedReferences(((ArrayType) referenced).getEntries(), ctx, expanding));
            expanding.remove(ruleName);
        }
        return result;
    }

    private static List<Integer> unique(List<Integer> values) {
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    /**
     * Represents one recursive matcher state.
     */
    private static final class MatchState {

        /** Sequence being matched. */
        private final List<GroupEntry> sequence;

        /** Current sequence entry. */
        private final int entryIndex;

        /** Current array index. */
        private final int itemIndex;

        /**
         * Creates a matcher state.
         *
         * @param sequence sequence being matched
         * @param entryIndex current sequence entry
         * @param itemIndex current array index
         */
        private MatchState(List<GroupEntry> sequence, int entryIndex, int itemIndex) {
            this.sequence = sequence;
            this.entryIndex = entryIndex;
            this.itemIndex = itemIndex;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof MatchState)) {
                return false;
            }
            MatchState that = (MatchState) other;
            return sequence == that.sequence && entryIndex == that.entryIndex && itemIndex == that.itemIndex;
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(sequence);
            result = 31 * result + entryIndex;
            return 31 * result + itemIndex;
        }

    }

    /**
     * Returns the CDDL representation of this array type.
     *
     * @return the CDDL array representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(entries.get(i));
        }
        return sb.append("]").toString();
    }

}
