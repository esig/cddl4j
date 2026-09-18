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
package eu.europa.ec.digit.cddl4j.cbor;

import java.util.Objects;

/**
 * An immutable key/value entry in a CBOR map.
 *
 */
public final class CBORMapEntry {

    /** Key of the map entry */
    private final CBORValue key;

    /** Value of the map entry */
    private final CBORValue value;

    /**
     * Default constructor
     *
     * @param key {@link CBORValue}
     * @param value {@link CBORValue}
     */
    public CBORMapEntry(CBORValue key, CBORValue value) {
        this.key = Objects.requireNonNull(key, "key");
        this.value = Objects.requireNonNull(value, "value");
    }

    /**
     * Returns the map key.
     *
     * @return the key
     */
    public CBORValue key() {
        return key;
    }

    /**
     * Returns the map value.
     *
     * @return the value
     */
    public CBORValue value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof CBORMapEntry)) {
            return false;
        }

        CBORMapEntry that = (CBORMapEntry) other;
        return key.equals(that.key) && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return 31 * key.hashCode() + value.hashCode();
    }

}
