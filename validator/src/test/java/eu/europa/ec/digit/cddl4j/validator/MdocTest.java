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

class MdocTest {

    @Test
    void validMdoc() throws IOException {
        CddlSchema schema = CddlValidator.loadSchemaFromFile(new File("src/test/resources/schema/deviceResponse.cddl").toPath());

        ValidationResult result = schema.validate(Files.readAllBytes(new File("src/test/resources/cbor/mdoc_valid.cbor").toPath()), "DeviceResponse");
        assertTrue(result.isValid(), result.getErrors().toString());
    }

    @Test
    void invalidMdoc() throws IOException {
        CddlSchema schema = CddlValidator.loadSchemaFromFile(new File("src/test/resources/schema/deviceResponse.cddl").toPath());

        ValidationResult result = schema.validate(Files.readAllBytes(new File("src/test/resources/cbor/mdoc_invalid.cbor").toPath()), "DeviceResponse");
        assertFalse(result.isValid(), result.getErrors().toString());

        List<ValidationError> errors = result.getErrors();
        assertEquals(2, errors.size(), errors.toString());

        List<String> errorMessages = errors.stream().map(ValidationError::getMessage).collect(Collectors.toList());
        assertTrue(errorMessages.contains("Required key 'version' is missing"));
        assertTrue(errorMessages.contains("Value did not match any alternative in choice ({deviceSignature: DeviceSignature} / {deviceMac: DeviceMac})"));

        assertEquals(0, errors.get(0).getCauses().size());
        assertEquals(2, errors.get(1).getCauses().size(), errors.toString());

        List<ValidationError> causes = errors.get(1).getCauses();
        errorMessages = causes.stream().map(ValidationError::getMessage).collect(Collectors.toList());
        assertTrue(errorMessages.contains("Choice alternative ({deviceSignature: DeviceSignature}) validation failed"));
        assertTrue(errorMessages.contains("Choice alternative ({deviceMac: DeviceMac}) validation failed"));

        assertEquals(1, causes.get(0).getCauses().size(), causes.toString());
        errorMessages = causes.get(0).getCauses().stream().map(ValidationError::getMessage).collect(Collectors.toList());
        assertTrue(errorMessages.contains("Value did not match any alternative in choice (bstr .cbor header_map / bstr .size 0)"));

        assertEquals(2, causes.get(1).getCauses().size(), causes.toString());
        errorMessages = causes.get(1).getCauses().stream().map(ValidationError::getMessage).collect(Collectors.toList());
        assertTrue(errorMessages.contains("Required key 'deviceMac' is missing"));
        assertTrue(errorMessages.contains("Unexpected key 'deviceSignature' not defined in schema"));
    }

}
