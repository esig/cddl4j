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
package eu.europa.ec.digit.cddl4j.validator;

import eu.europa.ec.digit.cddl4j.validator.schema.CddlSchema;
import eu.europa.ec.digit.cddl4j.validator.schema.parser.CddlParser;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Main entry point for CBOR-against-CDDL validation.
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * // Root rule = first rule in the schema
 * String source = "sensor = { \"device-id\": tstr, \"value\": float }";
 * CddlSchema schema = CddlValidator.loadSchema(source);
 * ValidationResult r = schema.validate(cborBytes);
 *
 * // Specify the root rule at load time
 * CddlSchema schema2 = CddlValidator.loadSchema(source, "inner-rule");
 *
 * // Or override it later (returns a new schema; original unchanged)
 * CddlSchema schema3 = schema.withRootRule("inner-rule");
 * }</pre>
 */
public final class CddlValidator {

    /**
     * Default constructor
     */
    private CddlValidator() {
        // empty
    }

    // -----------------------------------------------------------------------
    // loadSchema — from String
    // -----------------------------------------------------------------------

    /**
     * Parses a CDDL schema from a raw string.
     * The root rule defaults to the first user-defined rule in the source.
     *
     * @param cddlSource the CDDL text
     * @return a compiled {@link CddlSchema} ready for validation
     * @throws CddlParseException if the CDDL is syntactically invalid
     */
    public static CddlSchema loadSchema(String cddlSource) {
        Objects.requireNonNull(cddlSource, "CDDL cannot be null");
        return new CddlParser().parse(cddlSource);
    }

    /**
     * Parses a CDDL schema from a raw string, using an explicit root rule name.
     *
     * <p>Equivalent to
     * {@code loadSchema(cddlSource).withRootRule(rootRuleName)}.
     *
     * @param cddlSource   the CDDL text
     * @param rootRuleName the rule to use as the validation entry point
     * @return a compiled {@link CddlSchema} with the given root rule
     * @throws CddlParseException if the CDDL is syntactically invalid
     */
    public static CddlSchema loadSchema(String cddlSource, String rootRuleName) {
        return loadSchema(cddlSource).withRootRule(rootRuleName);
    }

    /**
     * Parses a CDDL schema from UTF-8 encoded bytes.
     * The root rule defaults to the first user-defined rule in the source.
     *
     * @param cddlSource the UTF-8 encoded CDDL source
     * @return a compiled {@link CddlSchema} ready for validation
     * @throws CddlParseException if the CDDL is syntactically invalid
     */
    public static CddlSchema loadSchema(byte[] cddlSource) {
        Objects.requireNonNull(cddlSource, "CDDL cannot be null");
        return new CddlParser().parse(cddlSource);
    }

    /**
     * Parses a CDDL schema from UTF-8 encoded bytes, using an explicit root
     * rule name.
     *
     * <p>Equivalent to
     * {@code loadSchema(cddlSource).withRootRule(rootRuleName)}.
     *
     * @param cddlSource   the UTF-8 encoded CDDL source
     * @param rootRuleName the rule to use as the validation entry point
     * @return a compiled {@link CddlSchema} with the given root rule
     * @throws CddlParseException if the CDDL is syntactically invalid
     */
    public static CddlSchema loadSchema(byte[] cddlSource, String rootRuleName) {
        return loadSchema(cddlSource).withRootRule(rootRuleName);
    }

    /**
     * Parses a CDDL schema from a UTF-8 encoded input stream.
     * The root rule defaults to the first user-defined rule in the source.
     *
     * <p>The input stream is consumed but not closed by this method. Ownership
     * of the input stream remains with the caller.
     *
     * @param cddlSource the input stream containing UTF-8 encoded CDDL source
     * @return a compiled {@link CddlSchema} ready for validation
     * @throws CddlParseException if the CDDL is syntactically invalid or the
     *         source cannot be read
     */
    public static CddlSchema loadSchema(InputStream cddlSource) {
        Objects.requireNonNull(cddlSource, "CDDL cannot be null");
        return new CddlParser().parse(cddlSource);
    }

    /**
     * Parses a CDDL schema from a UTF-8 encoded input stream, using an explicit
     * root rule name.
     *
     * <p>Equivalent to
     * {@code loadSchema(cddlSource).withRootRule(rootRuleName)}.
     *
     * <p>The input stream is consumed but not closed by this method. Ownership
     * of the input stream remains with the caller.
     *
     * @param cddlSource   the input stream containing UTF-8 encoded CDDL source
     * @param rootRuleName the rule to use as the validation entry point
     * @return a compiled {@link CddlSchema} with the given root rule
     * @throws CddlParseException if the CDDL is syntactically invalid or the
     *         source cannot be read
     */
    public static CddlSchema loadSchema(InputStream cddlSource, String rootRuleName) {
        return loadSchema(cddlSource).withRootRule(rootRuleName);
    }

    // -----------------------------------------------------------------------
    // loadSchemaFromResource — from classpath
    // -----------------------------------------------------------------------

    /**
     * Parse a CDDL schema from a classpath resource.
     * The root rule defaults to the first user-defined rule in the source.
     *
     * @param resourcePath classpath path, e.g. {@code "/schemas/my-device.cddl"}
     * @return a compiled {@link CddlSchema}
     * @throws CddlParseException if the CDDL is syntactically invalid
     * @throws CddlLoadException  if the resource cannot be read
     */
    public static CddlSchema loadSchemaFromResource(String resourcePath) {
        Objects.requireNonNull(resourcePath, "Resource path cannot be null");
        try (InputStream is = CddlValidator.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new CddlLoadException("Classpath resource not found: " + resourcePath);
            }
            return loadSchema(is);
        } catch (IOException e) {
            throw new CddlLoadException("Failed to read classpath resource: " + resourcePath, e);
        }
    }

    /**
     * Parse a CDDL schema from a classpath resource, using an explicit root rule name.
     *
     * @param resourcePath classpath path
     * @param rootRuleName the rule to use as the validation entry point
     * @return a compiled {@link CddlSchema} with the given root rule
     * @throws CddlParseException if the CDDL is syntactically invalid
     * @throws CddlLoadException  if the resource cannot be read
     */
    public static CddlSchema loadSchemaFromResource(String resourcePath, String rootRuleName) {
        return loadSchemaFromResource(resourcePath).withRootRule(rootRuleName);
    }

    // -----------------------------------------------------------------------
    // loadSchemaFromFile — from filesystem
    // -----------------------------------------------------------------------

    /**
     * Parse a CDDL schema from a file on disk.
     * The root rule defaults to the first user-defined rule in the source.
     *
     * @param path path to a {@code .cddl} file
     * @return a compiled {@link CddlSchema}
     * @throws CddlParseException if the CDDL is syntactically invalid
     * @throws CddlLoadException  if the file cannot be read
     */
    public static CddlSchema loadSchemaFromFile(Path path) {
        Objects.requireNonNull(path, "Path cannot be null");
        try {
            return loadSchema(Files.newInputStream(path));
        } catch (IOException e) {
            throw new CddlLoadException("Failed to read CDDL file: " + path, e);
        }
    }

    /**
     * Parse a CDDL schema from a file on disk, using an explicit root rule name.
     *
     * @param path         path to a {@code .cddl} file
     * @param rootRuleName the rule to use as the validation entry point
     * @return a compiled {@link CddlSchema} with the given root rule
     * @throws CddlParseException if the CDDL is syntactically invalid
     * @throws CddlLoadException  if the file cannot be read
     */
    public static CddlSchema loadSchemaFromFile(Path path, String rootRuleName) {
        return loadSchemaFromFile(path).withRootRule(rootRuleName);
    }

    /**
     * Parse a CDDL schema from a file on disk (string path overload).
     *
     * @param filePath path string to a {@code .cddl} file
     * @return a compiled {@link CddlSchema}
     */
    public static CddlSchema loadSchemaFromFile(String filePath) {
        Objects.requireNonNull(filePath, "File path cannot be null");
        return loadSchemaFromFile(Paths.get(filePath));
    }

    /**
     * Parse a CDDL schema from a file on disk (string path overload), with an explicit root rule.
     *
     * @param filePath     path string to a {@code .cddl} file
     * @param rootRuleName the rule to use as the validation entry point
     * @return a compiled {@link CddlSchema} with the given root rule
     */
    public static CddlSchema loadSchemaFromFile(String filePath, String rootRuleName) {
        Objects.requireNonNull(filePath, "File path cannot be null");
        return loadSchemaFromFile(Paths.get(filePath), rootRuleName);
    }
    /**
     * Parse a CDDL schema from a file on disk.
     * The root rule defaults to the first user-defined rule in the source.
     *
     * @param file a {@code .cddl} file
     * @return a compiled {@link CddlSchema}
     * @throws CddlParseException if the CDDL is syntactically invalid
     * @throws CddlLoadException  if the file cannot be read
     */
    public static CddlSchema loadSchemaFromFile(File file) {
        Objects.requireNonNull(file, "File cannot be null");
        return loadSchemaFromFile(file.toPath());
    }

    /**
     * Parse a CDDL schema from a file on disk, using an explicit root rule name.
     *
     * @param file a {@code .cddl} file
     * @param rootRuleName the rule to use as the validation entry point
     * @return a compiled {@link CddlSchema} with the given root rule
     * @throws CddlParseException if the CDDL is syntactically invalid
     * @throws CddlLoadException  if the file cannot be read
     */
    public static CddlSchema loadSchemaFromFile(File file, String rootRuleName) {
        Objects.requireNonNull(file, "File cannot be null");
        return loadSchemaFromFile(file.toPath(), rootRuleName);
    }


    // -----------------------------------------------------------------------
    // Convenience: validate inline
    // -----------------------------------------------------------------------

    /**
     * Convenience method: validates CBOR bytes directly using an inline CDDL
     * string.
     *
     * <p>Validates against the first rule in the schema.
     *
     * @param cddlSource the CDDL text
     * @param cborBytes the CBOR-encoded payload to validate
     * @return a {@link ValidationResult}
     */
    public static ValidationResult validate(String cddlSource, byte[] cborBytes) {
        return loadSchema(cddlSource).validate(cborBytes);
    }

    /**
     * Convenience method: validates CBOR bytes directly using UTF-8 encoded
     * CDDL source.
     *
     * <p>Validates against the first rule in the schema.
     *
     * @param cddlSource the UTF-8 encoded CDDL source
     * @param cborBytes the CBOR-encoded payload to validate
     * @return a {@link ValidationResult}
     */
    public static ValidationResult validate(byte[] cddlSource, byte[] cborBytes) {
        return loadSchema(cddlSource).validate(cborBytes);
    }

    /**
     * Convenience method: validates CBOR bytes directly using CDDL source read
     * from an input stream.
     *
     * <p>The CDDL source is interpreted as UTF-8. The input stream is consumed
     * but not closed by this method. Ownership of the stream remains with the
     * caller.
     *
     * <p>Validates against the first rule in the schema.
     *
     * @param cddlSource the input stream containing UTF-8 encoded CDDL source
     * @param cborBytes the CBOR-encoded payload to validate
     * @return a {@link ValidationResult}
     */
    public static ValidationResult validate(InputStream cddlSource, byte[] cborBytes) {
        return loadSchema(cddlSource).validate(cborBytes);
    }

    /**
     * Convenience method: validates a CBOR input stream directly using an inline
     * CDDL string.
     *
     * <p>Validates against the first rule in the schema.
     *
     * <p>The CBOR input stream is consumed but not closed by this method.
     * Ownership of the stream remains with the caller.
     *
     * @param cddlSource the CDDL text
     * @param cborInput the input stream containing the CBOR-encoded payload
     * @return a {@link ValidationResult}
     */
    public static ValidationResult validate(String cddlSource, InputStream cborInput) {
        return loadSchema(cddlSource).validate(cborInput);
    }

    /**
     * Convenience method: validates a CBOR input stream directly using UTF-8
     * encoded CDDL source.
     *
     * <p>Validates against the first rule in the schema.
     *
     * <p>The CBOR input stream is consumed but not closed by this method.
     * Ownership of the stream remains with the caller.
     *
     * @param cddlSource the UTF-8 encoded CDDL source
     * @param cborInput the input stream containing the CBOR-encoded payload
     * @return a {@link ValidationResult}
     */
    public static ValidationResult validate(byte[] cddlSource, InputStream cborInput) {
        return loadSchema(cddlSource).validate(cborInput);
    }

    /**
     * Convenience method: validates a CBOR input stream directly using CDDL
     * source read from an input stream.
     *
     * <p>The CDDL source is interpreted as UTF-8. Both input streams are
     * consumed but neither is closed by this method. Ownership of the streams
     * remains with the caller.
     *
     * <p>Validates against the first rule in the schema.
     *
     * @param cddlSource the input stream containing UTF-8 encoded CDDL source
     * @param cborInput the input stream containing the CBOR-encoded payload
     * @return a {@link ValidationResult}
     */
    public static ValidationResult validate(InputStream cddlSource, InputStream cborInput) {
        return loadSchema(cddlSource).validate(cborInput);
    }

}
