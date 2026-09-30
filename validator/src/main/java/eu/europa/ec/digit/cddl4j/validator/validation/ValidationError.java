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
package eu.europa.ec.digit.cddl4j.validator.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Describes a single constraint violation found during CBOR/CDDL validation.
 *
 * <p>Validation errors form a tree: a composite rule, such as a map or array,
 * can produce a top-level error that contains individual validation failures
 * as {@link #getCauses() causes}.
 */
public final class ValidationError {

    /** JSON-Pointer-style path to the invalid value within the CBOR instance. */
    private final String instancePath;

    /** Human-readable description of the validation failure. */
    private final String message;

    /** Validation errors that caused this error. */
    private final List<ValidationError> causes;

    /**
     * Creates a leaf validation error without causes.
     *
     * @param instancePath the JSON-Pointer-style path to the invalid CBOR
     *     value; the root is represented by an empty string
     * @param message a human-readable description of the validation failure
     */
    public ValidationError(String instancePath, String message) {
        this(instancePath, message, Collections.emptyList());
    }

    /**
     * Creates a validation error with the specified causes.
     *
     * <p>The supplied list is wrapped in an unmodifiable view.
     *
     * @param instancePath the JSON-Pointer-style path to the invalid CBOR
     *     value; the root is represented by an empty string
     * @param message a human-readable description of the validation failure
     * @param causes the validation errors that caused this error
     */
    public ValidationError(String instancePath, String message, List<ValidationError> causes) {
        this.instancePath = instancePath;
        this.message = message;
        this.causes = Collections.unmodifiableList(new ArrayList<>(causes));
    }

    /**
     * Returns the JSON-Pointer-style path into the CBOR instance.
     *
     * <p>For example, a nested value can have a path such as
     * {@code "/items/0/name"}. A root error is displayed as {@code "/"} for
     * readability, while nested paths use JSON Pointer escaping.
     * This root-display convention is retained for API compatibility.
     *
     * @return the instance path associated with this validation error
     */
    public String getInstancePath() {
        if (instancePath.isEmpty()) {
            return "/";
        }
        return instancePath;
    }

    /**
     * Returns the human-readable description of why validation failed.
     *
     * @return the validation failure message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the validation errors that caused this error.
     *
     * <p>The list is empty when this is a leaf error.
     *
     * @return an unmodifiable list of child validation errors
     */
    public List<ValidationError> getCauses() {
        return causes;
    }

    /**
     * Returns a formatted representation of this error and its causes.
     *
     * <p>Each cause is rendered on a separate line and indented according to
     * its depth in the validation error tree.
     *
     * @return the formatted validation error tree
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendTo(sb, 0);
        return sb.toString();
    }

    /**
     * Appends this error and its causes to the supplied string builder.
     *
     * @param sb the string builder to which the error is appended
     * @param indent the indentation depth of this error
     */
    private void appendTo(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
        sb.append('[')
                .append(getInstancePath())
                .append("] ")
                .append(getMessage());

        for (ValidationError cause : getCauses()) {
            sb.append('\n');
            cause.appendTo(sb, indent + 1);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ValidationError that = (ValidationError) o;
        return Objects.equals(instancePath, that.instancePath)
                && Objects.equals(message, that.message)
                && Objects.equals(causes, that.causes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(instancePath);
        result = 31 * result + Objects.hashCode(message);
        result = 31 * result + Objects.hashCode(causes);
        return result;
    }

}
