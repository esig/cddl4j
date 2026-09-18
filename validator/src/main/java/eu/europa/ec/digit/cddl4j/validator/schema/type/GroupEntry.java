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

/**
 * A single entry in a CDDL group.
 *
 * <p>A group entry may optionally contain a member key. When used in a map,
 * the member key is matched against the CBOR map key. When used in an array,
 * the key is annotation only and the value type is matched against the array
 * element.
 */
public final class GroupEntry {

    /** The optional bare-word member key. */
    private final String bareWordKey;

    /** The optional typed member key. */
    private final CddlType keyType;

    /** The CDDL type expected for the entry value. */
    private final CddlType type;

    /** Minimum occurrence count. */
    private final int min;

    /** Maximum occurrence count. */
    private final int max;

    /** Optional array annotation label. */
    private final String label;

    /** Whether this entry uses map cut semantics. */
    private final boolean cut;

    /** The referenced group name, or {@code null} for normal map entries. */
    private final String groupReference;

    /** Whether this entry uses the CDDL unwrapping operator. */
    private final boolean unwrappedReference;

    /**
     * Creates an array group entry.
     *
     * @param type the CDDL type expected for this entry
     * @param min the minimum number of occurrences allowed
     * @param max the maximum number of occurrences allowed, or
     *            {@link OccurrenceType#UNBOUNDED} for no upper limit
     * @param label the optional label associated with this entry, or {@code null}
     */
    public GroupEntry(CddlType type, int min, int max, String label) {
        this(null, null, type, min, max, label);
    }

    /**
     * Creates a group entry.
     *
     * @param bareWordKey optional bare-word key
     * @param keyType optional typed key
     * @param type value type
     * @param min minimum occurrence
     * @param max maximum occurrence
     * @param label optional array annotation
     */
    public GroupEntry(String bareWordKey, CddlType keyType, CddlType type, int min, int max, String label) {
        this(bareWordKey, keyType, type, min, max, label, null, false);
    }

    /**
     * Creates a group entry.
     *
     * @param bareWordKey optional bare-word key
     * @param keyType optional typed key
     * @param type value type
     * @param min minimum occurrence
     * @param max maximum occurrence
     * @param label optional array annotation
     * @param groupReference group reference
     */
    private GroupEntry(String bareWordKey, CddlType keyType, CddlType type, int min, int max, String label,
                       String groupReference, boolean unwrappedReference) {
        this(bareWordKey, keyType, type, min, max, label, groupReference, unwrappedReference, false);
    }

    /**
     * Creates a group entry with optional map cut semantics.
     *
     * @param bareWordKey optional bare-word key
     * @param keyType optional typed key
     * @param type value type
     * @param min minimum occurrence
     * @param max maximum occurrence
     * @param label optional array annotation
     * @param groupReference referenced group name
     * @param unwrappedReference whether the entry unwraps a referenced type
     * @param cut whether the entry uses map cut semantics
     */
    private GroupEntry(String bareWordKey, CddlType keyType, CddlType type, int min, int max, String label,
                       String groupReference, boolean unwrappedReference, boolean cut) {
        if (bareWordKey != null && keyType != null) {
            throw new IllegalArgumentException("A group entry cannot have both key forms");
        }
        this.bareWordKey = bareWordKey;
        this.keyType = keyType;
        this.type = type;
        this.min = min;
        this.max = max;
        this.label = label;
        this.cut = cut;
        this.groupReference = groupReference;
        this.unwrappedReference = unwrappedReference;
    }

    /**
     * Creates an unkeyed group entry.
     *
     * @param type value type
     * @param min minimum occurrence
     * @param max maximum occurrence
     * @return the group entry
     */
    public static GroupEntry value(CddlType type, int min, int max) {
        return new GroupEntry(null, null, type, min, max, null);
    }

    /**
     * Creates a bare-word-keyed group entry.
     *
     * @param key the member key
     * @param type value type
     * @param min minimum occurrence
     * @param max maximum occurrence
     * @return the group entry
     */
    public static GroupEntry bareWord(String key, CddlType type, int min, int max) {
        return bareWord(key, type, min, max, false);
    }

    /**
     * Creates a bare-word group entry with map cut semantics.
     *
     * @param key the member key
     * @param type value type
     * @param min minimum occurrence
     * @param max maximum occurrence
     * @param cut whether map cut semantics are enabled
     * @return the group entry
     */
    public static GroupEntry bareWord(String key, CddlType type, int min, int max, boolean cut) {
        return new GroupEntry(key, null, type, min, max, null, null, false, cut);
    }

    /**
     * Creates a typed-key group entry.
     *
     * @param keyType member key type
     * @param type value type
     * @param min minimum occurrence
     * @param max maximum occurrence
     * @return the group entry
     */
    public static GroupEntry typed(CddlType keyType, CddlType type, int min, int max) {
        return typed(keyType, type, min, max, false);
    }

    /**
     * Creates a typed group entry with optional map cut semantics.
     *
     * @param keyType member key type
     * @param type value type
     * @param min minimum occurrence
     * @param max maximum occurrence
     * @param cut whether map cut semantics are enabled
     * @return the group entry
     */
    public static GroupEntry typed(CddlType keyType, CddlType type, int min, int max, boolean cut) {
        return new GroupEntry(null, keyType, type, min, max, null, null, false, cut);
    }

    /**
     * Creates an array entry with an optional label.
     *
     * @param type value type
     * @param min minimum occurrence
     * @param max maximum occurrence
     * @param label optional label
     * @return the group entry
     */
    public static GroupEntry arrayEntry(CddlType type, int min, int max, String label) {
        return new GroupEntry(null, null, type, min, max, label);
    }

    /**
     * Creates a reference to a named CDDL group.
     *
     * @param groupName the name of the referenced group
     * @return a group-reference map entry
     */
    public static GroupEntry groupReference(String groupName) {
        return groupReference(groupName, 1, 1);
    }

    /**
     * Creates a reference to a named CDDL group with an occurrence modifier.
     *
     * @param groupName the name of the referenced group
     * @param min minimum number of group occurrences
     * @param max maximum number of group occurrences, or unbounded
     * @return a group-reference entry
     */
    public static GroupEntry groupReference(String groupName, int min, int max) {
        return new GroupEntry(null, null, null, min, max, null, groupName, false);
    }

    /**
     * Creates an unwrapped reference to a named CDDL type.
     *
     * @param ruleName the referenced CDDL type
     * @return an unwrapped group entry
     */
    public static GroupEntry unwrappedReference(String ruleName) {
        return new GroupEntry(null, null, new ReferenceType(ruleName), 1, 1, null, ruleName, true);
    }

    /**
     * Returns the value type.
     *
     * @return the value type
     */
    public CddlType getType() {
        return type;
    }

    /**
     * Returns the minimum occurrence.
     *
     * @return minimum occurrence
     */
    public int getMin() {
        return min;
    }

    /**
     * Returns the maximum occurrence.
     *
     * @return maximum occurrence
     */
    public int getMax() {
        return max;
    }

    /**
     * Returns the optional annotation label.
     *
     * @return the label, or {@code null}
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the bare-word key.
     *
     * @return the key, or {@code null}
     */
    public String getBareWordKey() {
        return bareWordKey;
    }

    /**
     * Returns the typed key.
     *
     * @return the key type, or {@code null}
     */
    public CddlType getKeyType() {
        return keyType;
    }

    /**
     * Determines whether this entry has a member key.
     *
     * @return {@code true} when a key is present
     */
    public boolean hasKey() {
        return bareWordKey != null || keyType != null;
    }

    /**
     * Determines whether the entry has an exact bare-word key.
     *
     * @return {@code true} for a bare-word key
     */
    public boolean isBareWord() {
        return bareWordKey != null;
    }

    /**
     * Determines whether the entry may be omitted.
     *
     * @return {@code true} when minimum occurrence is zero
     */
    public boolean isOptional() {
        return min == 0;
    }

    /**
     * Determines whether this entry uses map cut semantics.
     *
     * @return {@code true} when cut semantics are enabled
     */
    public boolean isCut() {
        return cut;
    }

    /**
     * Returns the referenced group name.
     *
     * @return the group name, or {@code null} when this is not a group reference
     */
    public String getGroupReference() {
        return groupReference;
    }

    /**
     * Determines whether this entry references a CDDL group.
     *
     * @return {@code true} when this entry is a group reference
     */
    public boolean isGroupReference() {
        return groupReference != null;
    }

    /**
     * Determines whether this entry uses the CDDL unwrapping operator.
     *
     * @return {@code true} when this entry is an unwrapped reference
     */
    public boolean isUnwrappedReference() {
        return unwrappedReference;
    }

    /**
     * Returns the CDDL representation.
     *
     * @return CDDL entry
     */
    @Override
    public String toString() {
        if (isGroupReference()) {
            return groupReference;
        }

        String occur = "";
        if (min == 0 && max == 1) {
            occur = "? ";
        } else if (min == 0 && max == OccurrenceType.UNBOUNDED) {
            occur = "* ";
        } else if (min == 1 && max == OccurrenceType.UNBOUNDED) {
            occur = "+ ";
        } else if (min != 1 || max != 1) {
            occur = min + "*" + (max == OccurrenceType.UNBOUNDED ? "" : max) + " ";
        }

        String key = "";
        if (bareWordKey != null) {
            key = bareWordKey + ": ";
        } else if (keyType != null) {
            key = keyType + " => ";
        } else if (label != null) {
            key = label + ": ";
        }

        return occur + key + type;
    }

}