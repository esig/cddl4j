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
package eu.europa.ec.digit.cddl4j.validator.schema.operator;

import eu.europa.ec.digit.cddl4j.cbor.CBORDecodeException;
import eu.europa.ec.digit.cddl4j.cbor.CBORDecoder;
import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.validator.schema.type.CddlType;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationError;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Standard implementation of {@code .cborseq}.
 */
public final class CborSequenceControlOperator implements ControlOperator {

    /** Singleton instance. */
    public static final CborSequenceControlOperator INSTANCE = new CborSequenceControlOperator();

    /**
     * Default constructor
     */
    private CborSequenceControlOperator() {
        // Singleton.
    }

    @Override
    public String getName() {
        return ".cborseq";
    }

    @Override
    public ValidationResult validate(CBORValue value, CddlType controller, ValidationContext ctx) {
        if (!value.isByteString()) {
            return ValidationResult.failure(new ValidationError(ctx.getInstancePath(),
                    "Control .cborseq requires a byte string target"));
        }
        try {
            return controller.validate(CBORDecoder.decode(wrapAsIndefiniteArray(value.asBytes())), ctx);

        } catch (CBORDecodeException exception) {
            return ValidationResult.failure(new ValidationError(ctx.getInstancePath(),
                    "Invalid CBOR for .cborseq: " + exception.getMessage()));
        }
    }

    private byte[] wrapAsIndefiniteArray(byte[] sequence) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(sequence.length + 2);
        output.write(0x9f);
        try {
            output.write(sequence);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to construct CBOR sequence wrapper", exception);
        }
        output.write(0xff);
        return output.toByteArray();
    }

}
