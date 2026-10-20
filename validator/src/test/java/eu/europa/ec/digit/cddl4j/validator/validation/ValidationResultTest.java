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
package eu.europa.ec.digit.cddl4j.validator.validation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationResultTest {

    @Test
    void failureDefensivelyCopiesErrors() {
        ValidationError error = new ValidationError("/value", "invalid");
        List<ValidationError> errors = new ArrayList<>();
        errors.add(error);

        ValidationResult result = ValidationResult.failure(errors);
        errors.clear();

        assertEquals(1, result.getErrors().size());
        assertSame(error, result.getErrors().get(0));
        assertThrows(UnsupportedOperationException.class, () -> result.getErrors().clear());
    }

    @Test
    void errorDefensivelyCopiesCauses() {
        ValidationError cause = new ValidationError("/value", "invalid");
        List<ValidationError> causes = new ArrayList<>();
        causes.add(cause);

        ValidationError error = new ValidationError("/", "root failure", causes);
        causes.clear();

        assertEquals(1, error.getCauses().size());
        assertSame(cause, error.getCauses().get(0));
        assertThrows(UnsupportedOperationException.class, () -> error.getCauses().clear());
    }

}
