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

/**
 * A single entry in a CDDL map.
 *
 * <p>This class is retained as the map-oriented representation used by
 * {@link MapType}. The parser internally represents entries as
 * {@link GroupEntry} instances.
 */
public final class MapEntry {

    /** Underlying neutral group entry. */
    private final GroupEntry entry;

    /** Whether this map entry has CDDL cut semantics. */
    private final boolean cut;

    /**
     * Default constructor
     *
     * @param entry {@link GroupEntry}
     */
    private MapEntry(GroupEntry entry) {
        this(entry, false);
    }

    /**
     * Constructor with a cut flag
     *
     * @param entry {@link GroupEntry}
     * @param cut boolean flag
     */
    private MapEntry(GroupEntry entry, boolean cut) {
        this.entry = entry;
        this.cut = cut;
    }

    /**
     * Creates a bare-word map entry.
     *
     * @param key exact text key
     * @param value value type
     * @param min minimum occurrence
     * @param max maximum occurrence
     * @return map entry
     */
    public static MapEntry bareWord(String key, CddlType value, int min, int max) {
        return new MapEntry(GroupEntry.bareWord(key, value, min, max, true), true);
    }

    /**
     * Creates a typed map entry without cut semantics.
     *
     * @param keyType the type of the map key
     * @param valueType the type of the map value
     * @param min minimum occurrence count
     * @param max maximum occurrence count
     * @return the typed map entry
     */
    public static MapEntry typed(CddlType keyType, CddlType valueType, int min, int max) {
        return typed(keyType, valueType, min, max, false);
    }

    /**
     * Creates a typed map entry.
     *
     * @param keyType the type of the map key
     * @param valueType the type of the map value
     * @param min minimum occurrence count
     * @param max maximum occurrence count
     * @param cut whether the entry has CDDL cut semantics
     * @return the typed map entry
     */
    public static MapEntry typed(CddlType keyType, CddlType valueType, int min, int max, boolean cut) {
        // Use the existing MapEntry constructor here, adding the cut argument.
        return new MapEntry(GroupEntry.typed(keyType, valueType, min, max, cut), cut);
    }

    /**
     * Creates a reference to a named CDDL group.
     *
     * @param groupName the name of the referenced group
     * @return a group-reference map entry
     */
    public static MapEntry groupReference(String groupName) {
        return groupReference(groupName, 1, 1);
    }

    /**
     * Creates a reference to a named CDDL group with an occurrence modifier.
     *
     * @param groupName the name of the referenced group
     * @param min minimum number of group occurrences
     * @param max maximum number of group occurrences, or unbounded
     * @return a group-reference map entry
     */
    public static MapEntry groupReference(String groupName, int min, int max) {
        return new MapEntry(GroupEntry.groupReference(groupName, min, max));
    }

    /**
     * Returns the bare-word key.
     *
     * @return the key, or {@code null}
     */
    public String getBareWordKey() {
        return entry.getBareWordKey();
    }

    /**
     * Returns the typed key.
     *
     * @return the key type, or {@code null}
     */
    public CddlType getKeyType() {
        return entry.getKeyType();
    }

    /**
     * Returns the value type.
     *
     * @return the value type
     */
    public CddlType getValueType() {
        return entry.getType();
    }

    /**
     * Returns minimum occurrence.
     *
     * @return minimum occurrence
     */
    public int getMin() {
        return entry.getMin();
    }

    /**
     * Returns maximum occurrence.
     *
     * @return maximum occurrence
     */
    public int getMax() {
        return entry.getMax();
    }

    /**
     * Determines whether this entry uses a bare-word key.
     *
     * @return {@code true} when the key is a bare word
     */
    public boolean isBareWord() {
        return entry.isBareWord();
    }

    /**
     * Determines whether this entry references a CDDL group.
     *
     * @return {@code true} when this entry is a group reference
     */
    public boolean isGroupReference() {
        return entry.isGroupReference();
    }

    /**
     * Returns the referenced group name.
     *
     * @return the group name, or {@code null} when this is not a group reference
     */
    public String getGroupReference() {
        return entry.getGroupReference();
    }

    /**
     * Determines whether this entry uses the CDDL unwrapping operator.
     *
     * @return {@code true} when this entry is an unwrapped reference
     */
    public boolean isUnwrappedReference() {
        return entry.isUnwrappedReference();
    }

    /**
     * Returns whether this entry has cut semantics.
     *
     * <p>A cut prevents later map entries from matching a key once this entry
     * matches the key, regardless of whether the value subsequently matches.
     *
     * @return {@code true} if this entry has cut semantics
     */
    public boolean isCut() {
        return entry.isCut();
    }

    /**
     * Creates an unwrapped reference to a named CDDL type.
     *
     * @param ruleName the referenced CDDL type
     * @return an unwrapped-reference map entry
     */
    public static MapEntry unwrappedReference(String ruleName) {
        return new MapEntry(GroupEntry.unwrappedReference(ruleName));
    }

    /**
     * Returns the CDDL representation.
     *
     * @return CDDL map entry
     */
    @Override
    public String toString() {
        return entry.toString();
    }

}