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

import eu.europa.ec.digit.cddl4j.cbor.CBOR;
import eu.europa.ec.digit.cddl4j.cbor.CBORType;
import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.validator.CddlParseException;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationError;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CDDL map type: {@code { entry, entry, ... }}
 *
 * <p>Validates that:
 * <ol>
 *   <li>The CBOR value is a map (major type 5).</li>
 *   <li>Every required entry has a matching key present in the map.</li>
 *   <li>Every value matches the declared value type for its key.</li>
 *   <li>No extra keys exist unless the schema includes a catch-all wildcard entry.</li>
 * </ol>
 */
public final class MapType extends CddlType {

    /** The map entries in schema order. */
    private final List<MapEntry> entries;

    /**
     * Creates a map type from the supplied entries.
     *
     * @param entries the map entries in schema order
     */
    public MapType(List<MapEntry> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * Returns the entries that define this map type.
     *
     * @return an immutable list of map entries in schema order
     */
    public List<MapEntry> getEntries() {
        return entries;
    }

    /**
     * Validates whether a CBOR value conforms to this map type.
     *
     * <p>The value must be a CBOR map. Required entries must have matching keys,
     * and every matched value must conform to the entry's declared value type.
     * Keys that are not consumed by a schema entry are rejected unless the
     * schema contains a catch-all entry.
     *
     * @param value the CBOR value to validate
     * @param ctx the validation context
     * @return a successful validation result when the value conforms to this
     *         map type; otherwise, a failed validation result containing the
     *         validation errors
     */
    @Override
    public ValidationResult validate(CBORValue value, ValidationContext ctx) {
        if (value.getType() != CBORType.MAP) {
            return fail(ctx, "Expected map but got " + PrimitiveType.cborTypeName(value));
        }

        List<ValidationError> errors = new ArrayList<>();
        Set<CBORValue> consumedKeys = new LinkedHashSet<>();

        List<MapEntry> unwrapped = expandUnwrappedReferences(entries, ctx, new HashSet<>());
        List<List<MapEntry>> alternatives = expandGroupReferences(unwrapped, ctx, value.size());
        if (alternatives.size() > 1) {
            return new ChoiceType(mapAlternatives(alternatives)).validate(value, ctx);
        }

        if (!alternatives.isEmpty()) {
            for (MapEntry entry : alternatives.get(0)) {
                if (entry.isBareWord()) {
                    validateBareWordEntry(entry, value, ctx, consumedKeys, errors);
                } else {
                    validateTypedEntry(entry, value, ctx, consumedKeys, errors);
                }
            }
        }

        for (CBORValue key : value.getKeys()) {
            if (!containsKey(consumedKeys, key)) {
                String keyStr = keyToString(key);
                errors.add(new ValidationError(ctx.getInstancePath(),
                        "Unexpected key '" + keyStr + "' not defined in schema"));
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    private List<MapEntry> expandUnwrappedReferences(List<MapEntry> source, ValidationContext ctx,
                                                     Set<String> expanding) {
        List<MapEntry> result = new ArrayList<>();
        for (MapEntry entry : source) {
            if (!entry.isUnwrappedReference()) {
                result.add(entry);
                continue;
            }

            String ruleName = entry.getGroupReference();
            CddlType referenced = ctx.resolveRule(ruleName);
            if (!(referenced instanceof MapType)) {
                throw new CddlParseException(
                        "Cannot unwrap rule '" + ruleName + "': expected a map but found " + referenced);
            }
            if (!expanding.add(ruleName)) {
                throw new CddlParseException("Recursive unwrapped rule reference: '" + ruleName + "'");
            }
            result.addAll(expandUnwrappedReferences(((MapType) referenced).getEntries(), ctx, expanding));
            expanding.remove(ruleName);
        }
        return result;
    }

    /**
     * Expands named groups recursively and converts each group alternative into map entries.
     *
     * @param source source entries containing possible group references
     * @param ctx validation context
     * @return concrete map-entry alternatives
     */
    private List<List<MapEntry>> expandGroupReferences(List<MapEntry> source, ValidationContext ctx, int mapSize) {
        List<GroupEntry> groupEntries = new ArrayList<>();
        for (MapEntry entry : source) {
            groupEntries.add(toGroupEntry(entry));
        }

        List<List<GroupEntry>> groups = GroupExpansion.expandForMap(groupEntries, ctx, mapSize);
        List<List<MapEntry>> result = new ArrayList<>();
        for (List<GroupEntry> group : groups) {
            result.add(toMapEntries(group));
        }
        return result;
    }

    private GroupEntry toGroupEntry(MapEntry entry) {
        if (entry.isGroupReference()) {
            return GroupEntry.groupReference(entry.getGroupReference(), entry.getMin(), entry.getMax());
        }
        if (entry.isBareWord()) {
            return GroupEntry.bareWord(entry.getBareWordKey(), entry.getValueType(), entry.getMin(), entry.getMax());
        }
        return GroupEntry.typed(entry.getKeyType(), entry.getValueType(), entry.getMin(), entry.getMax(),
                entry.isCut());
    }

    private List<MapEntry> toMapEntries(List<GroupEntry> group) {
        List<MapEntry> result = new ArrayList<>();
        for (GroupEntry entry : group) {
            if (!entry.hasKey()) {
                throw new CddlParseException("Group contains an unkeyed entry and cannot be used in a map");
            }
            if (entry.isBareWord()) {
                result.add(MapEntry.bareWord(entry.getBareWordKey(), entry.getType(), entry.getMin(), entry.getMax()));
            } else {
                result.add(MapEntry.typed(entry.getKeyType(), entry.getType(), entry.getMin(), entry.getMax(),
                        entry.isCut()));
            }
        }
        return result;
    }

    private List<CddlType> mapAlternatives(List<List<MapEntry>> alternatives) {
        List<CddlType> result = new ArrayList<>();
        for (List<MapEntry> alternative : alternatives) {
            result.add(new MapType(alternative));
        }
        return result;
    }

    /**
     * Validates a map entry with a bare-word key.
     *
     * <p>A bare-word key is represented by an exact CBOR text-string key.
     * When the key is present, its corresponding value is validated against
     * the entry's value type. A missing key is reported when the entry is
     * required.
     *
     * @param entry the map entry to validate
     * @param map the CBOR map being validated
     * @param ctx the validation context
     * @param consumed the set of keys already consumed by schema entries
     * @param errors the list to which validation errors are added
     */
    private void validateBareWordEntry(MapEntry entry, CBORValue map, ValidationContext ctx, Set<CBORValue> consumed,
                                       List<ValidationError> errors) {
        String keyStr = entry.getBareWordKey();
        CBORValue cborKey = CBOR.text(keyStr);
        CBORValue mapValue = map.get(cborKey);

        if (mapValue == null) {
            if (entry.getMin() > 0) {
                errors.add(new ValidationError(ctx.getInstancePath(),
                        "Required key '" + keyStr + "' is missing"));
            }
            return;
        }

        consumed.add(findKey(map, cborKey));
        ValidationResult result = entry.getValueType().validate(mapValue, ctx.child(keyStr));
        if (!result.isValid()) {
            errors.addAll(result.getErrors());
        }
    }

    /**
     * Validates a map entry whose key is defined by a CDDL type.
     *
     * <p>For a normal typed entry ({@code =>}), a map member is consumed only
     * when both its key and value match the entry. This allows a later map entry
     * to consume a member whose key matches but whose value does not.
     *
     * <p>For an entry with cut semantics, a matching key is consumed immediately
     * and its value is then validated. This prevents later entries from accepting
     * the same key when the value is invalid.
     *
     * @param entry the typed-key map entry to validate
     * @param map the CBOR map being validated
     * @param ctx the validation context
     * @param consumed the set of keys already consumed by schema entries
     * @param errors the list to which validation errors are added
     */
    private void validateTypedEntry(MapEntry entry, CBORValue map, ValidationContext ctx,
                                    Set<CBORValue> consumed, List<ValidationError> errors) {
        List<CBORValue> matchingKeys = new ArrayList<>();
        Map<CBORValue, List<ValidationError>> matchingKeyErrors = new HashMap<>();

        for (CBORValue key : map.getKeys()) {
            if (containsKey(consumed, key)) {
                continue;
            }

            ValidationResult keyResult = entry.getKeyType().validate(key, ctx);
            if (!keyResult.isValid()) {
                continue;
            }

            if (!entry.isCut()) {
                String keyStr = keyToString(key);
                ValidationResult valueResult = entry.getValueType().validate(map.get(key), ctx.child(keyStr));
                if (!valueResult.isValid()) {
                    matchingKeyErrors.put(key, valueResult.getErrors());
                    continue;
                }
            }

            matchingKeys.add(key);
        }

        int validCount = matchingKeys.size();
        int allCount = validCount + matchingKeyErrors.size();
        int min = entry.getMin();
        int max = entry.getMax();

        if (validCount < min) {
            if (!matchingKeyErrors.isEmpty()) {
                consumed.addAll(matchingKeyErrors.keySet());
            }
            List<ValidationError> causes = matchingKeyErrors.values().stream().flatMap(List::stream).collect(Collectors.toList());
            if (allCount < min) {
                errors.add(new ValidationError(ctx.getInstancePath(), "Expected at least " + min + " key(s) matching "
                        + entry.getKeyType() + " but found " + validCount, causes));
            } else {
                errors.addAll(causes);
            }
            return;
        }

        if (validCount > max) {
            if (!matchingKeyErrors.isEmpty()) {
                consumed.addAll(matchingKeyErrors.keySet());
            }
            List<ValidationError> causes = matchingKeyErrors.values().stream().flatMap(List::stream).collect(Collectors.toList());
            if (allCount > max) {
                errors.add(new ValidationError(ctx.getInstancePath(), "Expected at most " + max + " key(s) matching "
                        + entry.getKeyType() + " but found " + validCount, causes));
            } else {
                errors.addAll(causes);
            }
            return;
        }

        for (CBORValue key : matchingKeys) {
            consumed.add(key);

            if (entry.isCut()) {
                String keyStr = keyToString(key);
                CBORValue mapValue = map.get(key);
                ValidationResult result = entry.getValueType().validate(mapValue, ctx.child(keyStr));
                if (!result.isValid()) {
                    errors.addAll(result.getErrors());
                }
            }
        }
    }

    /**
     * Determines whether a set contains a CBOR key equal to the supplied key.
     *
     * @param set the set of CBOR keys to search
     * @param key the CBOR key to find
     * @return {@code true} if the set contains an equal key; {@code false}
     *         otherwise
     */
    private static boolean containsKey(Set<CBORValue> set, CBORValue key) {
        for (CBORValue candidate : set) {
            if (candidate.equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the key instance from a CBOR map that is equal to the supplied key.
     *
     * @param map the CBOR map whose keys are searched
     * @param key the key to find
     * @return the matching key instance from the map, or {@code key} if no
     *         matching instance is found
     */
    private static CBORValue findKey(CBORValue map, CBORValue key) {
        for (CBORValue candidate : map.getKeys()) {
            if (candidate.equals(key)) {
                return candidate;
            }
        }
        return key;
    }

    /**
     * Converts a CBOR map key to a string suitable for use in validation
     * messages and instance paths.
     *
     * @param key the CBOR map key to convert
     * @return the string value for a text-string key, or the CBOR representation
     *         for any other key type
     */
    private static String keyToString(CBORValue key) {
        if (key.getType() == CBORType.TEXT_STRING) {
            return key.asString();
        }
        return key.toString();
    }

    /**
     * Returns the CDDL representation of this map type.
     *
     * @return the CDDL map representation containing all map entries in schema
     *         order
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(entries.get(i));
        }
        sb.append("}");
        return sb.toString();
    }

}
