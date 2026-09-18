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

import eu.europa.ec.digit.cddl4j.validator.CddlParseException;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Expands context-dependent CDDL groups into concrete group-entry sequences.
 *
 * <p>The expansion is deliberately independent of maps and arrays. Array
 * validation preserves group occurrences as sequence operations, while map
 * validation can materialize the finite number of useful repetitions for the
 * current input map.
 */
final class GroupExpansion {

    /** Utility class. */
    private GroupExpansion() {
        // No instances.
    }

    /**
     * Expands a group without permitting occurrences on a referenced group.
     *
     * @param type group type or group choice
     * @param ctx validation context used to resolve named rules
     * @return all concrete group-entry alternatives
     */
    static List<List<GroupEntry>> expand(CddlType type, ValidationContext ctx) {
        return expand(type, ctx, new HashSet<>());
    }

    /**
     * Expands a group type or choice recursively while retaining group
     * references that carry occurrence modifiers for array matching.
     *
     * @param type group type or group choice
     * @param ctx validation context
     * @return concrete group alternatives
     */
    static List<List<GroupEntry>> expandForArray(CddlType type, ValidationContext ctx) {
        return expand(type, ctx, new HashSet<>());
    }

    /**
     * Expands group references in a map context and applies occurrences to the
     * complete referenced group. The input map size provides a finite upper
     * bound for unbounded occurrences.
     *
     * @param source source group entries
     * @param ctx validation context
     * @param maxOccurrences maximum useful number of group repetitions
     * @return concrete group-entry alternatives
     */
    static List<List<GroupEntry>> expandForMap(List<GroupEntry> source, ValidationContext ctx, int maxOccurrences) {
        return expandMapEntries(source, ctx, new HashSet<>(), Math.max(0, maxOccurrences));
    }

    private static List<List<GroupEntry>> expand(CddlType type, ValidationContext ctx, Set<String> expanding) {
        if (type instanceof GroupType) {
            return expandEntries(((GroupType) type).getEntries(), ctx, expanding);
        }
        if (type instanceof GroupChoiceType) {
            List<List<GroupEntry>> result = new ArrayList<>();
            for (GroupType alternative : ((GroupChoiceType) type).getAlternatives()) {
                result.addAll(expand(alternative, ctx, expanding));
            }
            return result;
        }
        throw new CddlParseException("Expected a CDDL group but found " + type);
    }

    private static List<List<GroupEntry>> expandEntries(List<GroupEntry> source, ValidationContext ctx,
                                                        Set<String> expanding) {
        List<List<GroupEntry>> result = new ArrayList<>();
        result.add(new ArrayList<>());

        for (GroupEntry entry : source) {
            CddlType referenced = referencedGroup(entry, ctx);
            if (referenced == null) {
                append(result, entry);
                continue;
            }

            String name = groupName(entry);
            if (entry.getMin() != 1 || entry.getMax() != 1) {
                append(result, entry);
                continue;
            }
            result = concatenate(result, expandReferencedGroup(name, referenced, ctx, expanding));
        }
        return result;
    }

    private static List<List<GroupEntry>> expandMapEntries(List<GroupEntry> source, ValidationContext ctx,
                                                            Set<String> expanding, int maxOccurrences) {
        List<List<GroupEntry>> result = new ArrayList<>();
        result.add(new ArrayList<>());

        for (GroupEntry entry : source) {
            CddlType referenced = referencedGroup(entry, ctx);
            if (referenced == null) {
                append(result, entry);
                continue;
            }

            String name = groupName(entry);
            List<List<GroupEntry>> replacement = repeat(referenced, name, entry.getMin(), entry.getMax(), ctx,
                    expanding, maxOccurrences);
            result = concatenate(result, replacement);
        }
        return result;
    }

    private static List<List<GroupEntry>> repeat(CddlType referenced, String name, int min, int max,
                                                  ValidationContext ctx, Set<String> expanding, int maxOccurrences) {
        int upper = max == OccurrenceType.UNBOUNDED ? maxOccurrences : Math.min(max, maxOccurrences);
        if (upper < min) {
            return Collections.emptyList();
        }
        List<List<GroupEntry>> one = expandReferencedGroup(name, referenced, ctx, expanding);
        if (one.isEmpty()) {
            return min == 0 ? Collections.singletonList(new ArrayList<>()) : Collections.emptyList();
        }

        List<List<GroupEntry>> result = new ArrayList<>();
        List<List<GroupEntry>> current = Collections.singletonList(new ArrayList<>());

        if (min == 0) {
            result.add(new ArrayList<>());
        }

        for (int count = 1; count <= upper; count++) {
            current = concatenate(current, one);
            if (count >= min) {
                result.addAll(copy(current));
            }
        }
        return result;
    }

    private static List<List<GroupEntry>> expandReferencedGroup(String name, CddlType referenced,
                                                                ValidationContext ctx, Set<String> expanding) {
        if (!expanding.add(name)) {
            throw new CddlParseException("Recursive group reference: '" + name + "'");
        }
        try {
            return expand(referenced, ctx, expanding);
        } finally {
            expanding.remove(name);
        }
    }

    private static CddlType referencedGroup(GroupEntry entry, ValidationContext ctx) {
        if (entry.isGroupReference()) {
            CddlType target = ctx.resolveRule(entry.getGroupReference());
            if (target == null && entry.getGroupReference().startsWith("$$")) {
                return new GroupChoiceType(Collections.emptyList());
            }
            if (!(target instanceof GroupType) && !(target instanceof GroupChoiceType)) {
                throw new CddlParseException("Rule '" + entry.getGroupReference() + "' is not a CDDL group");
            }
            return target;
        }

        if (!entry.hasKey() && entry.getType() instanceof ReferenceType) {
            String name = ((ReferenceType) entry.getType()).getRuleName();
            CddlType target = ctx.resolveRule(name);
            return target instanceof GroupType || target instanceof GroupChoiceType ? target : null;
        }
        return null;
    }

    private static String groupName(GroupEntry entry) {
        return entry.isGroupReference() ? entry.getGroupReference() : ((ReferenceType) entry.getType()).getRuleName();
    }

    private static void append(List<List<GroupEntry>> alternatives, GroupEntry entry) {
        for (List<GroupEntry> alternative : alternatives) {
            alternative.add(entry);
        }
    }

    private static List<List<GroupEntry>> concatenate(List<List<GroupEntry>> left, List<List<GroupEntry>> right) {
        List<List<GroupEntry>> result = new ArrayList<>();
        for (List<GroupEntry> a : left) {
            for (List<GroupEntry> b : right) {
                List<GroupEntry> combined = new ArrayList<>(a);
                combined.addAll(b);
                result.add(combined);
            }
        }
        return result;
    }

    private static List<List<GroupEntry>> copy(List<List<GroupEntry>> source) {
        List<List<GroupEntry>> result = new ArrayList<>();
        for (List<GroupEntry> entries : source) {
            result.add(new ArrayList<>(entries));
        }
        return result;
    }

}
