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

import java.io.InputStream;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Provides lightweight inspection of CBOR data without decoding the complete
 * data item.
 *
 * <p>The inspector consumes only the bytes required by the requested
 * inspection operation and leaves the input stream positioned immediately
 * after the consumed bytes.
 *
 * <p>The inspector is intended for use cases where the structure of a CBOR
 * data item needs to be inspected without decoding the complete item.
 *
 * <p>For complete CBOR decoding, use {@link CBORDecoder} instead.
 */
public final class CBORInspector {

    /**
     * Reader used to consume CBOR bytes from the input stream.
     */
    private final CBORDecoder.Reader reader;

    /**
     * Creates a CBOR inspector for the supplied input stream.
     *
     * <p>The input stream is not closed by this inspector. The caller retains
     * ownership of the stream.
     *
     * @param input the input stream containing CBOR data
     * @throws NullPointerException if {@code input} is {@code null}
     */
    public CBORInspector(InputStream input) {
        Objects.requireNonNull(input, "Input is null");
        this.reader = new CBORDecoder.Reader(input);
    }

    /**
     * Reads and consumes the initial byte of the next CBOR data item.
     *
     * <p>The returned value contains both the CBOR major type and the
     * additional information encoded in the initial byte. No bytes following
     * the initial byte are consumed by this method.
     *
     * <p>The returned byte can be passed to {@link CBORMajorType#fromInitialByte(int)}
     * to determine the major type and subsequently to {@link #readTag(int)} or
     * {@link #readLength(int)} when further inspection of the item is required.
     *
     * @return the consumed initial byte as an unsigned value in the range
     *     {@code 0..255}
     * @throws CBORDecodeException if the input is truncated or cannot be read
     */
    public int read() throws CBORDecodeException {
        return reader.read();
    }

    /**
     * Reads the tag number encoded by a previously consumed CBOR initial byte.
     *
     * <p>The supplied initial byte must represent the CBOR tag major type.
     * The initial byte itself has already been consumed by {@link #read()}.
     * This method consumes any additional bytes required to encode the tag
     * number.
     *
     * @param initialByte the previously consumed CBOR initial byte
     * @return the tag number
     * @throws CBORDecodeException if the initial byte does not represent a
     *     CBOR tag or if the tag number is malformed
     */
    public long readTag(int initialByte) throws CBORDecodeException {
        if (CBORMajorType.fromInitialByte(initialByte) != CBORMajorType.TAG) {
            throw new CBORDecodeException("Expected CBOR tag");
        }

        BigInteger tag = reader.readUnsignedArgument(initialByte & 0x1f);
        if (tag.bitLength() > 63) {
            throw new CBORDecodeException("CBOR tag is too large");
        }
        return tag.longValue();
    }

    /**
     * Reads a text string encoded by a previously consumed CBOR initial byte.
     *
     * <p>The supplied initial byte must represent the CBOR text string major type.
     * The initial byte itself must already have been consumed by {@link #read()}.
     *
     * <p>Both definite- and indefinite-length text strings are supported. For an
     * indefinite-length text string, all text-string chunks and the terminating
     * break code are consumed.
     *
     * @param initialByte the previously consumed CBOR initial byte
     * @return the decoded UTF-8 text string
     * @throws CBORDecodeException if the initial byte does not represent a text
     *     string, the string is malformed, the UTF-8 encoding is invalid, or the
     *     input is truncated
     */
    public String readTextString(int initialByte) throws CBORDecodeException {
        if (CBORMajorType.fromInitialByte(initialByte) != CBORMajorType.TEXT_STRING) {
            reader.error("Expected CBOR text string");
        }

        return reader.readTextString(initialByte & 0x1f);
    }

    /**
     * Reads a byte string encoded by a previously consumed CBOR initial byte.
     *
     * <p>The supplied initial byte must represent the CBOR byte string major type.
     * The initial byte itself must already have been consumed by {@link #read()}.
     *
     * <p>Both definite- and indefinite-length byte strings are supported. For an
     * indefinite-length byte string, all byte-string chunks and the terminating
     * break code are consumed.
     *
     * @param initialByte the previously consumed CBOR initial byte
     * @return the decoded byte string
     * @throws CBORDecodeException if the initial byte does not represent a byte
     *     string, the string is malformed, or the input is truncated
     */
    public byte[] readByteString(int initialByte) throws CBORDecodeException {
        if (CBORMajorType.fromInitialByte(initialByte) != CBORMajorType.BYTE_STRING) {
            reader.error("Expected CBOR byte string");
        }

        return reader.readByteString(initialByte & 0x1f);
    }

    /**
     * Reads the length or argument encoded by a previously consumed CBOR
     * initial byte.
     *
     * <p>The supplied initial byte must already have been consumed by
     * {@link #read()}. The additional information encoded in the initial byte
     * determines whether the value is contained directly in the initial byte
     * or whether additional bytes must be consumed from the input stream.
     *
     * <p>The method returns {@code -1} when the initial byte specifies an
     * indefinite-length item.
     *
     * <p>This method does not validate the major type encoded in the initial
     * byte. The caller is responsible for determining whether the value
     * represented by the initial byte is a length or another CBOR argument.
     *
     * @param initialByte the previously consumed CBOR initial byte
     * @return the encoded length or argument, or {@code -1} for an
     *     indefinite-length item
     * @throws CBORDecodeException if the argument is malformed or too large
     */
    public long readLength(int initialByte) throws CBORDecodeException {
        int additionalInformation = initialByte & 0x1f;

        if (additionalInformation == 31) {
            return -1;
        }

        BigInteger length = reader.readUnsignedArgument(additionalInformation);

        if (length.bitLength() > 63) {
            throw new CBORDecodeException("CBOR length is too large");
        }

        return length.longValue();
    }

}
