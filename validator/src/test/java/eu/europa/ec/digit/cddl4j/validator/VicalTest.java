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
package eu.europa.ec.digit.cddl4j.validator;

import eu.europa.ec.digit.cddl4j.validator.schema.CddlSchema;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationError;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VicalTest {

    @Test
    void validVical() throws IOException {
        CddlSchema schema = CddlValidator.loadSchemaFromFile(new File("src/test/resources/schema/vical.cddl").toPath());

        ValidationResult result = schema.validate(Files.readAllBytes(new File("src/test/resources/cbor/vical_valid.cbor").toPath()), "VICAL");
        assertTrue(result.isValid(), result.getErrors().toString());
    }

    @Test
    void validVicalNoRoot() throws IOException {
        CddlSchema schema = CddlValidator.loadSchemaFromFile(new File("src/test/resources/schema/vical.cddl").toPath());

        ValidationResult result = schema.validate(Files.readAllBytes(new File("src/test/resources/cbor/vical_valid.cbor").toPath()));
        assertTrue(result.isValid(), result.getErrors().toString());
    }

    @Test
    void invalidVical() throws IOException {
        CddlSchema schema = CddlValidator.loadSchemaFromFile(new File("src/test/resources/schema/vical.cddl").toPath());

        ValidationResult result = schema.validate(Files.readAllBytes(new File("src/test/resources/cbor/vical_invalid.cbor").toPath()), "VICAL");
        assertFalse(result.isValid());

        List<ValidationError> errors = result.getErrors();
        assertEquals(9, errors.size(), errors.toString());

        List<String> errorMessages = errors.stream().map(ValidationError::toString).collect(Collectors.toList());
        assertTrue(errorMessages.contains("[/version] Expected tstr (text string) but got int (integer)"));
        assertTrue(errorMessages.contains("[/date] Expected tagged value (#6.0) but got untagged tstr (text string)"));
        assertTrue(errorMessages.contains("[/nextUpdate] Expected tagged value (#6.0) but got untagged tstr (text string)"));
        assertTrue(errorMessages.contains("[/certificateInfos/0/serialNumber] Expected tagged value (#6.2) but got untagged tstr (text string)"));
        assertTrue(errorMessages.contains("[/certificateInfos/0/ski] Expected bstr (byte string) but got tstr (text string)"));
        assertTrue(errorMessages.contains("[/certificateInfos/0/issuer] Expected bstr (byte string) but got tstr (text string)"));
        assertTrue(errorMessages.contains("[/certificateInfos/0/subject] Expected bstr (byte string) but got tstr (text string)"));
        assertTrue(errorMessages.contains("[/certificateInfos/0/notBefore] Expected tagged value (#6.0) but got untagged tstr (text string)"));
        assertTrue(errorMessages.contains("[/certificateInfos/0/notAfter] Expected tagged value (#6.0) but got untagged tstr (text string)"));
    }

    @Test
    void invalidVical2() throws IOException {
        CddlSchema schema = CddlValidator.loadSchemaFromFile(new File("src/test/resources/schema/vical.cddl").toPath());

        ValidationResult result = schema.validate(Files.readAllBytes(new File("src/test/resources/cbor/vical_invalid2.cbor").toPath()), "VICAL");
        assertFalse(result.isValid());

        List<ValidationError> errors = result.getErrors();
        assertEquals(4, errors.size());

        List<String> errorMessages = errors.stream().map(ValidationError::toString).collect(Collectors.toList());
        assertTrue(errorMessages.contains("[/] Required key 'version' is missing"));
        assertTrue(errorMessages.contains("[/] Required key 'vicalProvider' is missing"));
        assertTrue(errorMessages.contains("[/] Required key 'date' is missing"));
        assertTrue(errorMessages.contains("[/] Required key 'certificateInfos' is missing"));
    }

}
