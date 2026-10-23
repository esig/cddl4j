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
package eu.europa.ec.digit.cddl4j.cbor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Decodes CBOR data items.
 *
 * <p>The decoder accepts both definite- and indefinite-length arrays, maps,
 * byte strings, and text strings. It is variation tolerant with respect to
 * preferred serialization: for example, an integer may use a longer-than-
 * necessary argument encoding.
 *
 * <p>RFC 8949 distinguishes well-formed encodings from valid CBOR data: a
 * duplicate-key map may be well-formed but is not valid and has indeterminate
 * decoding. This decoder rejects duplicate keys rather than choosing an
 * application-specific interpretation.
 *
 * <p>Only one top-level data item is accepted by the decode methods.
 *
 * <p>For preventing the application on reaching the resource limits,
 * implementation provides the following  safeguards, configurable with System
 * properties:
 * <ul>
 * <li>{@code eu.europa.ec.digit.cddl4j.cbor.max_byte_string_length} -
 * prevents pre-allocation of a byte array from exceeding VM memory limit,
 * for untrusted byte string length inputs.
 * By default, the size of a byte string is limited to 10 MB.
 * Negative values are treated as no limit.</li>
 * <li>{@code eu.europa.ec.digit.cddl4j.cbor.max_nesting_depth} - prevents
 * deeply nested untrusted input from exhausting the Java call stack, decoding
 * is limited to a pre-defined maximum number of nested data items.
 * By default, the value is 32. Negative values are treated as no limit.</li>
 * </ul>
 *
 * <p>NOTE: This is implementations limits, not a restriction imposed by CBOR.
 *
 * <p>The decoder does not impose a total input-size or item-count limit.
 * Callers decoding untrusted streams should enforce an input budget.
 */
public final class CBORDecoder {

    // TODO: Expose caller-configurable item limits for untrusted input.

    /**
     * Default constructor.
     */
    private CBORDecoder() {
        // Utility class.
    }

    /**
     * Decodes exactly one CBOR data item from a byte array.
     *
     * @param data the encoded CBOR data
     * @return the decoded CBOR value
     * @throws NullPointerException if {@code data} is {@code null}
     * @throws CBORDecodeException if the input is truncated, malformed,
     *     contains invalid UTF-8, contains duplicate map keys, or contains
     *     trailing bytes, or exceeds the supported nesting depth
     */
    public static CBORValue decode(byte[] data) throws CBORDecodeException {
        Objects.requireNonNull(data, "Input is null");
        return decode(new ByteArrayInputStream(data));
    }

    /**
     * Decodes exactly one CBOR data item from an input stream.
     *
     * <p>The stream is consumed until the end of the top-level data item and
     * one additional byte is read to determine whether trailing data exists.
     * The supplied stream is not closed by this method.
     *
     * @param input the stream containing encoded CBOR data
     * @return the decoded CBOR value
     * @throws NullPointerException if {@code input} is {@code null}
     * @throws CBORDecodeException if the input is truncated, malformed,
     *     contains invalid UTF-8, contains duplicate map keys, or contains
     *     trailing bytes, or exceeds the supported nesting depth
     */
    public static CBORValue decode(InputStream input) throws CBORDecodeException {
        Reader reader = new Reader(input);
        CBORValue result = reader.readValue();

        if (reader.hasRemaining()) {
            reader.error("Trailing bytes after top-level value");
        }

        return result;
    }

    /**
     * Checks whether the supplied byte array contains a valid CBOR data item.
     *
     * <p>The byte array is decoded as a CBOR data item. The method returns
     * {@code true} if the input can be successfully parsed as a CBOR data item,
     * or {@code false} if decoding fails due to malformed, truncated, or
     * otherwise invalid CBOR data.
     *
     * <p>This method rejects trailing data after the first CBOR data item.
     *
     * @param input the byte array containing CBOR data
     * @return {@code true} if the input contains a valid CBOR data item;
     *     {@code false} otherwise
     * @throws NullPointerException if {@code input} is {@code null}
     */
    public static boolean isCbor(byte[] input) {
        Objects.requireNonNull(input, "Input is null");
        return isCbor(new ByteArrayInputStream(input));
    }

    /**
     * Checks whether the supplied input stream contains a valid CBOR data item.
     *
     * <p>The stream is consumed while the CBOR data item is decoded. The method
     * returns {@code true} if the input can be successfully parsed as a CBOR
     * data item, or {@code false} if decoding fails due to malformed, truncated,
     * or otherwise invalid CBOR data.
     *
     * <p>This method rejects CBOR content containing trailing data.
     *
     * <p>The supplied input stream is not closed by this method.
     *
     * @param input the input stream containing CBOR data
     * @return if the input contains a valid CBOR data item
     * @throws NullPointerException if {@code input} is {@code null}
     */
    public static boolean isCbor(InputStream input) {
        try {
            Reader reader = new Reader(input);
            reader.readValue();
            return !reader.hasRemaining();
        } catch (CBORDecodeException e) {
            return false;
        }
    }

    /**
     * Performs reading of the CBOR content from an input stream.
     *
     * <p>The reader uses a {@link PushbackInputStream} internally to support
     * one-byte lookahead. This is required when processing CBOR constructs such
     * as indefinite-length arrays, maps, byte strings, and text strings, where
     * the next byte must be inspected to determine whether it is the CBOR break
     * code ({@code 0xff}) without consuming it.
     *
     * <p>The supplied input stream is wrapped in a
     * {@link PushbackInputStream} with a one-byte pushback buffer. The reader
     * does not close the underlying stream; stream ownership remains with the
     * caller.
     */
    static final class Reader {

        /**
         * Input stream used to read CBOR content.
         *
         * <p>The pushback capability allows the reader to inspect the next byte
         * without consuming it.
         */
        final PushbackInputStream input;

        /**
         * Number of bytes consumed from the input stream.
         *
         * <p>Bytes temporarily read during lookahead are not included in this
         * position until they are actually consumed by the decoder.
         */
        long position;

        /**
         * Creates a reader for the supplied input stream.
         *
         * <p>The supplied stream is wrapped in a
         * {@link PushbackInputStream} with a one-byte pushback buffer. The
         * underlying stream is not closed by this reader.
         *
         * @param input the input stream containing CBOR-encoded data
         * @throws NullPointerException if {@code input} is {@code null}
         */
        Reader(InputStream input) {
            Objects.requireNonNull(input, "Input is null");
            this.input = new PushbackInputStream(input, 1);
        }

        /**
         * Throws a decode exception at the current input position.
         *
         * @param message error message
         * @throws CBORDecodeException always
         */
        void error(String message) throws CBORDecodeException {
            /*
             * CBORDecodeException currently accepts an int position. Guard
             * against overflow for exceptionally large streams.
             */
            int errorPosition = position > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) position;

            throw new CBORDecodeException(message, errorPosition);
        }

        /**
         * Reads one byte from the input stream.
         *
         * <p>Unlike {@link InputStream#read()}, this method converts end-of-stream
         * into a {@link CBORDecodeException}, because reaching EOF while reading
         * a CBOR value means that the input is truncated.
         *
         * @return the next byte as an unsigned value in the range {@code 0..255}
         * @throws CBORDecodeException if the input is truncated or cannot be read
         */
        int read() throws CBORDecodeException {
            int value = readByte();

            if (value < 0) {
                error("Truncated CBOR input");
            }

            return value;
        }

        /**
         * Reads one byte without treating end-of-stream as an error.
         *
         * <p>This method is used internally  where EOF is a meaningful result
         * rather than necessarily a truncated CBOR value.
         *
         * @return the next byte as an unsigned value in the range {@code 0..255},
         *     or {@code -1} if the end of the stream has been reached
         * @throws CBORDecodeException if an I/O error occurs
         */
        int readByte() throws CBORDecodeException {
            try {
                int value = input.read();

                if (value < 0) {
                    return -1;
                }

                position++;
                return value & 0xff;
            } catch (IOException exception) {
                throw new CBORDecodeException("Unable to read CBOR input", currentErrorPosition(), exception);
            }
        }

        /**
         * Peeks at the next byte without consuming it.
         *
         * @return the next byte as an unsigned integer
         * @throws CBORDecodeException if EOF or an I/O error is encountered
         */
        int peek() throws CBORDecodeException {
            int value = readByte();

            if (value < 0) {
                error("Truncated CBOR input");
            }

            try {
                input.unread(value);
                position--;
                return value;
            } catch (IOException exception) {
                throw new CBORDecodeException("Unable to read CBOR input", currentErrorPosition(), exception);
            }
        }

        /**
         * Determines whether another byte is available without consuming it.
         *
         * @return {@code true} if another byte is available
         * @throws CBORDecodeException if an I/O error is encountered
         */
        boolean hasRemaining() throws CBORDecodeException {
            int value = readByte();

            if (value < 0) {
                return false;
            }

            try {
                input.unread(value);
                position--;
                return true;
            } catch (IOException exception) {
                throw new CBORDecodeException("Unable to read CBOR input", currentErrorPosition(), exception);
            }
        }

        /**
         * Returns the current position converted to the exception's int
         * position representation.
         *
         * @return current error position
         */
        int currentErrorPosition() {
            return position > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) position;
        }

        /**
         * Reads one complete CBOR data item.
         *
         * @return the decoded data item
         * @throws CBORDecodeException if the item is malformed or exceeds the
         *     supported nesting depth
         */
        CBORValue readValue() throws CBORDecodeException {
            return readValue(0);
        }

        private CBORValue readValue(int depth) throws CBORDecodeException {
            int maxNestingDepth = getMaxNestingDepth();
            if (maxNestingDepth > -1 && depth > maxNestingDepth) {
                error("Maximum CBOR nesting depth of " + maxNestingDepth + " exceeded");
            }

            int initial = read();
            CBORMajorType majorType = CBORMajorType.fromInitialByte(initial);
            int additionalInformation = initial & 0x1f;

            if (additionalInformation == 31 && !isIndefiniteLengthAllowed(majorType)) {
                error("Indefinite length is not valid for major type " + majorType.getValue());
            }

            switch (majorType) {
                case UNSIGNED_INTEGER:
                    return CBORValue.scalar(CBORType.INTEGER, readUnsignedArgument(additionalInformation));

                case NEGATIVE_INTEGER:
                    return CBORValue.scalar(CBORType.INTEGER, readUnsignedArgument(additionalInformation).negate().subtract(BigInteger.ONE));

                case BYTE_STRING:
                    return CBORValue.scalar(CBORType.BYTE_STRING, readByteString(additionalInformation));

                case TEXT_STRING:
                    return CBORValue.scalar(CBORType.TEXT_STRING, readTextString(additionalInformation));

                case ARRAY:
                    return readArray(additionalInformation, depth);

                case MAP:
                    return readMap(additionalInformation, depth);

                case TAG:
                    return readTag(additionalInformation, depth);

                case SPECIAL:
                    return readSimpleValue(additionalInformation);

                default:
                    error("Unsupported CBOR major type " + majorType.getValue());
            }
            throw new IllegalStateException("Illegal state has been reached");
        }

        private int getMaxNestingDepth() {
            int maxNestingDepth = CBORProperties.DEFAULT_MAX_NESTING_DEPTH;
            String definedMaxNestingDepth = System.getProperty(CBORProperties.MAX_NESTING_DEPTH);
            if (definedMaxNestingDepth != null) {
                try {
                    maxNestingDepth = Integer.parseInt(definedMaxNestingDepth);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(String.format("Invalid %s: %s. Reason : %s",
                            CBORProperties.MAX_NESTING_DEPTH, definedMaxNestingDepth, e.getMessage()));
                }
            }
            return maxNestingDepth;
        }

        boolean isIndefiniteLengthAllowed(CBORMajorType majorType) {
            return CBORMajorType.BYTE_STRING == majorType || CBORMajorType.TEXT_STRING == majorType
                    || CBORMajorType.ARRAY == majorType || CBORMajorType.MAP == majorType;
        }

        CBORValue readTag(int additionalInformation, int depth) throws CBORDecodeException {
            BigInteger tag = readUnsignedArgument(additionalInformation);
            CBORValue content = readValue(depth + 1);

            return CBORValue.tag(tag, content);
        }

        BigInteger readUnsignedArgument(int additionalInformation) throws CBORDecodeException {
            switch (additionalInformation) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                    return BigInteger.valueOf(additionalInformation);

                case 24:
                    return BigInteger.valueOf(read());

                case 25:
                    return BigInteger.valueOf(((long) read() << 8) | read());

                case 26:
                    return BigInteger.valueOf(((long) read() << 24) | ((long) read() << 16)
                            | ((long) read() << 8) | read());

                case 27:
                    byte[] bytes = new byte[8];

                    for (int i = 0; i < bytes.length; i++) {
                        bytes[i] = (byte) read();
                    }

                    return new BigInteger(1, bytes);

                case 31:
                    error("Indefinite argument is not valid here");

                default:
                    error("Invalid additional information " + additionalInformation);
            }
            throw new IllegalStateException("Illegal state has been reached");
        }

        byte[] readByteString(int additionalInformation) throws CBORDecodeException {
            if (additionalInformation != 31) {
                return readDefiniteByteString(additionalInformation);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            while (peek() != 0xff) {
                int initial = read();
                int majorType = initial >>> 5;
                int chunkAdditionalInformation = initial & 0x1f;

                if (majorType != 2 || chunkAdditionalInformation == 31) {
                    error("Indefinite byte string contains an invalid chunk");
                }

                byte[] chunk = readDefiniteByteString(chunkAdditionalInformation);

                output.write(chunk, 0, chunk.length);
            }

            readBreak();
            return output.toByteArray();
        }

        /**
         * Reads a definite-length byte string with pre-allocating its declared size.
         * Verifies the byte string size against the pre-defined limits (default is 10 MB).
         *
         * @param additionalInformation the CBOR length encoding
         * @return the byte-string contents
         * @throws CBORDecodeException if unable to read the content
         */
        byte[] readDefiniteByteString(int additionalInformation) throws CBORDecodeException {
            BigInteger length = readUnsignedArgument(additionalInformation);

            if (length.bitLength() > 31) {
                error("Byte string length is too large");
            }

            int size = length.intValue();

            long maxByteStringLength = getMaxByteStringLength();
            if (maxByteStringLength > -1 && size > maxByteStringLength) {
                error("Maximum CBOR byte string length of " + maxByteStringLength + " exceeded : " + size);
            }

            byte[] result = new byte[size];
            readFully(result);
            return result;
        }

        private long getMaxByteStringLength() {
            long maxByteStringLength = CBORProperties.DEFAULT_MAX_BYTE_STRING_LENGTH;
            String definedMaxByteStringLength = System.getProperty(CBORProperties.MAX_BYTE_STRING_LENGTH);
            if (definedMaxByteStringLength != null) {
                try {
                    maxByteStringLength = Long.parseLong(definedMaxByteStringLength);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(String.format("Invalid %s: %s. Reason : %s",
                            CBORProperties.MAX_BYTE_STRING_LENGTH, definedMaxByteStringLength, e.getMessage()));
                }
            }
            return maxByteStringLength;
        }

        /**
         * Reads exactly {@code bytes.length} bytes from the input.
         *
         * @param bytes destination buffer
         * @throws CBORDecodeException if the input is truncated or an I/O
         *     error occurs
         */
        void readFully(byte[] bytes) throws CBORDecodeException {
            int offset = 0;

            while (offset < bytes.length) {
                try {
                    int count = input.read(bytes, offset, bytes.length - offset);

                    if (count < 0) {
                        error("Truncated byte string");
                    }

                    if (count == 0) {
                        /*
                         * InputStream implementations are discouraged from
                         * returning zero for a non-empty request, but a custom
                         * stream may do so. Fall back to a single-byte read to
                         * guarantee progress.
                         */
                        bytes[offset++] = (byte) read();
                    } else {
                        offset += count;
                        position += count;
                    }
                } catch (IOException exception) {
                    throw new CBORDecodeException("Unable to read CBOR input", currentErrorPosition(), exception);
                }
            }
        }

        String readTextString(int additionalInformation) throws CBORDecodeException {
            if (additionalInformation != 31) {
                return decodeUtf8(readDefiniteByteString(additionalInformation));
            }

            StringBuilder result = new StringBuilder();

            while (peek() != 0xff) {
                int initial = read();
                int majorType = initial >>> 5;
                int chunkAdditionalInformation = initial & 0x1f;

                if (majorType != 3 || chunkAdditionalInformation == 31) {
                    error("Indefinite text string contains an invalid chunk");
                }

                byte[] chunk = readDefiniteByteString(chunkAdditionalInformation);

                /*
                 * Each text-string chunk is itself a text string and therefore must
                 * contain valid UTF-8. Decoding each chunk separately also prevents
                 * malformed UTF-8 from being hidden across chunk boundaries.
                 */
                result.append(decodeUtf8(chunk));
            }

            readBreak();
            return result.toString();
        }

        String decodeUtf8(byte[] bytes) throws CBORDecodeException {
            try {
                return StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes))
                        .toString();
            } catch (CharacterCodingException exception) {
                throw new CBORDecodeException("Invalid UTF-8 text string", currentErrorPosition(), exception);
            }
        }

        CBORValue readArray(int additionalInformation, int depth) throws CBORDecodeException {
            CBORValue.CBORArrayEditor builder = CBOR.array();

            if (additionalInformation == 31) {
                while (peek() != 0xff) {
                    builder.add(readValue(depth + 1));
                }

                readBreak();
                return builder.value();
            }

            BigInteger length = readUnsignedArgument(additionalInformation);

            if (length.bitLength() > 31) {
                error("Array length is too large");
            }

            int count = length.intValue();

            for (int i = 0; i < count; i++) {
                builder.add(readValue(depth + 1));
            }

            return builder.value();
        }

        CBORValue readMap(int additionalInformation, int depth) throws CBORDecodeException {
            CBORValue.CBORMapEditor builder = CBOR.map();

            if (additionalInformation == 31) {
                while (peek() != 0xff) {
                    CBORValue key = readValue(depth + 1);
                    CBORValue value = readValue(depth + 1);

                    putMapEntry(builder, key, value);
                }

                readBreak();
                return builder.value();
            }

            BigInteger length = readUnsignedArgument(additionalInformation);

            if (length.bitLength() > 31) {
                error("Map length is too large");
            }

            int count = length.intValue();

            for (int i = 0; i < count; i++) {
                CBORValue key = readValue(depth + 1);
                CBORValue value = readValue(depth + 1);

                putMapEntry(builder, key, value);
            }

            return builder.value();
        }

        void putMapEntry(CBORValue.CBORMapEditor builder, CBORValue key, CBORValue value) throws CBORDecodeException {
            try {
                builder.put(key, value);
            } catch (IllegalArgumentException exception) {
                throw new CBORDecodeException("Duplicate map key", currentErrorPosition(), exception);
            }
        }

        CBORValue readSimpleValue(int additionalInformation) throws CBORDecodeException {
            switch (additionalInformation) {
                case 20:
                    return CBORValue.FALSE;

                case 21:
                    return CBORValue.TRUE;

                case 22:
                    return CBORValue.NULL;

                case 23:
                    return CBORValue.UNDEFINED;

                case 24:
                    int simpleValue = read();

                    /*
                     * Simple values 0..31 have dedicated/reserved encodings and cannot
                     * be represented using the additional-information-24 form.
                     */
                    if (simpleValue < 32) {
                        error("Reserved simple value encoding");
                    }

                    return CBORValue.scalar(CBORType.SIMPLE_VALUE, simpleValue);

                case 25:
                    return CBORValue.scalar(CBORType.FLOATING_POINT, halfToDouble((read() << 8) | read()));

                case 26:
                    int floatBits = (read() << 24) | (read() << 16) | (read() << 8) | read();
                    return CBORValue.scalar(CBORType.FLOATING_POINT, (double) Float.intBitsToFloat(floatBits));

                case 27:
                    long doubleBits = 0;

                    for (int i = 0; i < 8; i++) {
                        doubleBits = (doubleBits << 8) | read();
                    }

                    return CBORValue.scalar(CBORType.FLOATING_POINT, Double.longBitsToDouble(doubleBits));

                case 31:
                    error("Unexpected break code");

                default:
                    /*
                     * Additional-information values 0..19 represent simple values.
                     */
                    if (additionalInformation < 20) {
                        return CBORValue.scalar(CBORType.SIMPLE_VALUE, additionalInformation);
                    }
                    error("Reserved additional information for major type 7");
            }
            throw new IllegalStateException("Illegal state has been reached");
        }

        void readBreak() throws CBORDecodeException {
            if (read() != 0xff) {
                error("Expected break code");
            }
        }

        /**
         * Converts an IEEE 754 binary16 value to its exact Java double value.
         *
         * @param value the binary16 representation
         * @return the corresponding Java double
         */
        static double halfToDouble(int value) {
            int sign = (value >>> 15) & 0x01;
            int exponent = (value >>> 10) & 0x1f;
            int fraction = value & 0x03ff;

            if (exponent == 0) {
                if (fraction == 0) {
                    return sign == 0 ? 0.0 : -0.0;
                }

                double result = Math.scalb((double) fraction, -24);
                return sign == 0 ? result : -result;
            }

            if (exponent == 31) {
                if (fraction == 0) {
                    return sign == 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                }

                return Double.NaN;
            }

            double result = Math.scalb(1024.0 + fraction, exponent - 25);
            return sign == 0 ? result : -result;
        }
    }

}