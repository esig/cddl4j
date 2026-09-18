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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Encodes CBOR values.
 *
 * <p>The default {@link #encode(CBORValue)} method uses preferred
 * serialization and preserves CBOR map insertion order.
 *
 * <p>{@link #encodeDeterministic(CBORValue)} uses deterministic CBOR
 * serialization and orders map keys lexicographically by their encoded bytes.
 *
 * <p>Arrays always preserve their element order.
 *
 * <p>The encoder can either return the encoded representation as a
 * {@code byte[]} or write it directly to a caller-provided
 * {@link OutputStream}.
 */
public final class CBOREncoder {

    /** Unsigned integer-8 maximum */
    private static final BigInteger UINT_8_MAX = BigInteger.valueOf(0xffL);

    /** Unsigned integer-16 maximum */
    private static final BigInteger UINT_16_MAX = BigInteger.valueOf(0xffffL);

    /** Unsigned integer-32 maximum */
    private static final BigInteger UINT_32_MAX = BigInteger.valueOf(0xffffffffL);

    /**
     * Default constructor
     */
    private CBOREncoder() {
        // Utility class.
    }

    /**
     * Encodes a CBOR value using preferred serialization.
     *
     * <p>Map entries are encoded in their insertion order.
     *
     * @param value the value to encode
     * @return the encoded CBOR bytes
     */
    public static byte[] encode(CBORValue value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            encode(value, output);
        } catch (IOException exception) {
            /*
             * OutputStream does not throw IOException. This catch
             * is required because the shared OutputStream-based encoder
             * exposes IOException for custom output streams.
             */
            throw new IllegalStateException("Unable to encode CBOR value", exception);
        }
        return output.toByteArray();
    }

    /**
     * Encodes a CBOR value using preferred serialization.
     *
     * <p>Map entries are encoded in their insertion order.
     *
     * <p>The supplied output stream is not closed or flushed by this method.
     * Stream ownership remains with the caller.
     *
     * @param value the value to encode
     * @param output the output stream receiving the encoded CBOR data
     * @throws IOException if an I/O error occurs while writing the encoded data
     */
    public static void encode(CBORValue value, OutputStream output) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(output, "output");

        new Encoder(MapOrdering.INSERTION).encode(value, output);
    }

    /**
     * Encodes a CBOR value using deterministic serialization.
     *
     * <p>Map keys are ordered lexicographically by their encoded bytes,
     * as required by the core deterministic encoding rules of RFC 8949.
     *
     * @param value the value to encode
     * @return the deterministic CBOR bytes
     */
    public static byte[] encodeDeterministic(CBORValue value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            encodeDeterministic(value, output);
        } catch (IOException exception) {
            /*
             * OutputStream does not throw IOException. This catch
             * is required because the shared OutputStream-based encoder
             * exposes IOException for custom output streams.
             */
            throw new IllegalStateException("Unable to encode CBOR value", exception);
        }

        return output.toByteArray();
    }

    /**
     * Encodes a CBOR value using deterministic serialization.
     *
     * <p>Map keys are ordered lexicographically by their encoded bytes,
     * as required by the core deterministic encoding rules of RFC 8949.
     *
     * <p>The supplied output stream is not closed or flushed by this method.
     * Stream ownership remains with the caller.
     *
     * @param value the value to encode
     * @param output the output stream receiving the encoded CBOR data
     * @throws IOException if an I/O error occurs while writing the encoded data
     */
    public static void encodeDeterministic(CBORValue value, OutputStream output) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(output, "output");

        new Encoder(MapOrdering.DETERMINISTIC).encode(value, output);
    }

    /**
     * Supported map ordering types:
     * - INSERTION : keeps the entries order based on the insertion order using a LinkedHashMap;
     * - DETERMINISTIC : orders in a deterministic way irrespectively of the insertion order using the RFC 8610 rules.
     */
    private enum MapOrdering {

        /** Keeps the entries order based on the insertion order using a LinkedHashMap */
        INSERTION,

        /** Orders in a deterministic way irrespectively of the insertion order using the RFC 8610 rules */
        DETERMINISTIC

    }

    /**
     * Performs the actual encoding.
     *
     * <p>The public API exposes two serialization modes, but the actual
     * encoding implementation is shared here. The only difference between
     * the modes is map-entry ordering.
     */
    private static final class Encoder {

        /**
         * Map ordering option
         */
        private final MapOrdering mapOrdering;

        /**
         * Default constructor
         *
         * @param mapOrdering {@link MapOrdering}
         */
        private Encoder(MapOrdering mapOrdering) {
            Objects.requireNonNull(mapOrdering, "mapOrdering");
            this.mapOrdering = mapOrdering;
        }

        /**
         * Encodes a CBOR value to an output stream.
         *
         * @param value the value to encode
         * @param output the output stream
         * @throws IOException if an I/O error occurs while writing
         */
        private void encode(CBORValue value, OutputStream output) throws IOException {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(output, "output");
            write(output, value);
        }

        private void write(OutputStream output, CBORValue value) throws IOException {
            switch (value.getType()) {
                case INTEGER:
                    writeInteger(output, value);
                    break;

                case BYTE_STRING:
                    writeByteString(output, value.asBytes());
                    break;

                case TEXT_STRING:
                    writeTextString(output, value.asString());
                    break;

                case ARRAY:
                    writeArray(output, value);
                    break;

                case MAP:
                    writeMap(output, value);
                    break;

                case TAG:
                    writeTag(output, value);
                    break;

                case BOOLEAN:
                    writeBoolean(output, value);
                    break;

                case NULL:
                    output.write(0xf6);
                    break;

                case UNDEFINED:
                    output.write(0xf7);
                    break;

                case SIMPLE_VALUE:
                    writeSimpleValue(output, value);
                    break;

                case FLOATING_POINT:
                    writeFloatingPoint(output, value.asDouble());
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported CBOR type: " + value.getType());
            }
        }

        private void writeInteger(OutputStream output, CBORValue value) throws IOException {
            BigInteger number = value.asInteger();
            if (value.isUnsignedInteger()) {
                writeArgument(output, 0, number);
            } else {
                writeArgument(output, 1, number.negate().subtract(BigInteger.ONE));
            }
        }

        private void writeByteString(OutputStream output, byte[] value) throws IOException {
            writeArgument(output, 2, BigInteger.valueOf(value.length));
            output.write(value, 0, value.length);
        }

        private void writeTextString(OutputStream output, String value) throws IOException {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            writeArgument(output, 3, BigInteger.valueOf(bytes.length));
            output.write(bytes, 0, bytes.length);
        }

        private void writeArray(OutputStream output, CBORValue value) throws IOException {
            List<CBORValue> elements = value.getElements();
            writeArgument(output, 4, BigInteger.valueOf(elements.size()));
            for (CBORValue element : elements) {
                write(output, element);
            }
        }

        private void writeMap(OutputStream output, CBORValue value) throws IOException {
            List<CBORMapEntry> entries = value.getEntries();
            writeArgument(output, 5, BigInteger.valueOf(entries.size()));

            if (mapOrdering == MapOrdering.DETERMINISTIC) {
                writeDeterministicEntries(output, entries);
            } else {
                for (CBORMapEntry entry : entries) {
                    write(output, entry.key());
                    write(output, entry.value());
                }
            }
        }

        /**
         * Orders map entries according to deterministic CBOR ordering and
         * writes those into the OutputStream.
         *
         * <p>The method does not create the map's header, but writes entries
         * directly in the stream.
         *
         * <p>The original map is never modified.
         */
        private void writeDeterministicEntries(OutputStream output, List<CBORMapEntry> entries) throws IOException {
            List<EncodedMapEntry> encodedEntries = new ArrayList<>(entries.size());
            for (CBORMapEntry entry : entries) {
                encodedEntries.add(new EncodedMapEntry(entry, encodeKey(entry.key())));
            }
            encodedEntries.sort(EncodedMapEntry.ORDERING);

            for (EncodedMapEntry encodedEntry : encodedEntries) {
                write(output, encodedEntry.entry.key());
                write(output, encodedEntry.entry.value());
            }
        }


        /**
         * Encodes a map key to a byte array for deterministic ordering.
         *
         * <p>Only map keys need to be materialized during deterministic
         * ordering.
         *
         * <p>A temporary {@link ByteArrayOutputStream} is used because deterministic
         * CBOR ordering requires the encoded representation of each map key to be
         * available before the map entries can be sorted. The resulting bytes are
         * used only for ordering and the actual CBOR result continues to be written
         * directly to the caller-provided output stream.
         */
        private byte[] encodeKey(CBORValue value) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                write(output, value);
            } catch (IOException exception) {
                /*
                 * OutputStream does not throw IOException.
                 */
                throw new IllegalStateException("Unable to encode CBOR map key", exception);
            }
            return output.toByteArray();
        }

        private void writeTag(OutputStream output, CBORValue value) throws IOException {

            writeArgument(output, 6, value.getTag());
            write(output, value.getTaggedValue());
        }

        private void writeBoolean(OutputStream output, CBORValue value) throws IOException {
            output.write(value.asBoolean() ? 0xf5 : 0xf4);
        }

        private void writeSimpleValue(OutputStream output, CBORValue value) throws IOException {
            int simpleValue = value.asSimpleValue();
            if (simpleValue < 0 || simpleValue > 255) {
                throw new IllegalArgumentException("Simple value must be between 0 and 255");
            }

            /*
             * Values 0..23 have compact representations.
             */
            if (simpleValue < 24) {
                output.write(0xe0 | simpleValue);
            } else {
                output.write(0xf8);
                output.write(simpleValue);
            }
        }

        /**
         * Encodes a floating-point value using the shortest representation
         * that preserves the represented value where possible.
         */
        private void writeFloatingPoint(OutputStream output, double value) throws IOException {
            /*
             * NaN is special because NaN != NaN. Use the shortest
             * binary16 NaN representation.
             */
            if (Double.isNaN(value)) {
                output.write(0xf9);
                output.write(0x7e);
                output.write(0x00);
                return;
            }

            float floatValue = (float) value;

            /*
             * If the value can be represented exactly as float,
             * try half precision first.
             */
            if ((double) floatValue == value) {
                int halfBits = doubleToHalfBits(value);
                if (halfBits >= 0 && halfToDouble(halfBits) == value) {
                    output.write(0xf9);
                    output.write((halfBits >>> 8) & 0xff);
                    output.write(halfBits & 0xff);
                    return;
                }

                /*
                 * Exact float representation.
                 */
                output.write(0xfa);

                int bits = Float.floatToRawIntBits(floatValue);
                
                output.write((bits >>> 24) & 0xff);
                output.write((bits >>> 16) & 0xff);
                output.write((bits >>> 8) & 0xff);
                output.write(bits & 0xff);
                
                return;
            }

            /*
             * Full double precision representation.
             */
            output.write(0xfb);

            long bits = Double.doubleToRawLongBits(value);

            for (int shift = 56; shift >= 0; shift -= 8) {
                output.write((int) (bits >>> shift) & 0xff);
            }
        }

        private void writeArgument(OutputStream output, int majorType, BigInteger argument) throws IOException {

            if (argument.signum() < 0) {
                throw new IllegalArgumentException("CBOR argument cannot be negative");
            }

            int additionalInformation = getAdditionalInformation(argument);

            output.write((majorType << 5) | additionalInformation);

            switch (additionalInformation) {
                case 24:
                    output.write(argument.intValue() & 0xff);
                    break;

                case 25:
                    writeBigEndian(output, argument, 2);
                    break;

                case 26:
                    writeBigEndian(output, argument, 4);
                    break;

                case 27:
                    writeBigEndian(output, argument, 8);
                    break;

                default:
                    break;
            }
        }

        private int getAdditionalInformation(BigInteger argument) {
            int additionalInformation;
            if (argument.compareTo(BigInteger.valueOf(24)) < 0) {
                additionalInformation = argument.intValue();

            } else if (argument.compareTo(UINT_8_MAX) <= 0) {
                additionalInformation = 24;

            } else if (argument.compareTo(UINT_16_MAX) <= 0) {
                additionalInformation = 25;

            } else if (argument.compareTo(UINT_32_MAX) <= 0) {
                additionalInformation = 26;

            } else if (argument.bitLength() <= 64) {
                additionalInformation = 27;

            } else {
                throw new IllegalArgumentException("CBOR argument exceeds uint64 range");
            }
            return additionalInformation;
        }

        private void writeBigEndian(OutputStream output, BigInteger value, int byteCount) throws IOException {
            for (int shift = (byteCount - 1) * 8; shift >= 0; shift -= 8) {
                output.write(value.shiftRight(shift).intValue() & 0xff);
            }
        }

        /**
         * Converts an IEEE 754 binary64 value to binary16 bits when the
         * value is representable in the binary16 format.
         *
         * @return the binary16 bits, or {@code -1} if the value cannot
         *     be represented as binary16
         */
        private int doubleToHalfBits(double value) {
            int bits = Float.floatToRawIntBits((float) value);
            int sign = (bits >>> 16) & 0x8000;
            int exponent = (bits >>> 23) & 0xff;
            int fraction = bits & 0x007fffff;

            if (exponent == 0xff) {
                return fraction == 0 ? sign | 0x7c00 : sign | 0x7e00;
            }

            int halfExponent = exponent - 127 + 15;
            if (halfExponent >= 31) {
                return -1;
            }

            if (halfExponent <= 0) {
                if (halfExponent < -10) {
                    return sign;
                }

                fraction |= 0x00800000;
                int shift = 14 - halfExponent;
                int halfFraction = fraction >>> shift;
                int remainderMask = (1 << shift) - 1;
                int remainder = fraction & remainderMask;
                int halfway = 1 << (shift - 1);
                if (remainder > halfway || (remainder == halfway && (halfFraction & 1) != 0)) {
                    halfFraction++;
                }
                return sign | halfFraction;
            }

            int halfFraction = fraction >>> 13;
            int remainder = fraction & 0x1fff;
            if (remainder > 0x1000 || (remainder == 0x1000 && (halfFraction & 1) != 0)) {
                halfFraction++;
                if (halfFraction == 0x400) {
                    halfFraction = 0;
                    halfExponent++;
                    if (halfExponent == 31) {
                        return sign | 0x7c00;
                    }
                }
            }

            return sign | (halfExponent << 10) | halfFraction;
        }

        /**
         * Converts an IEEE 754 binary16 value to its exact Java double
         * value.
         */
        private static double halfToDouble(int value) {
            int sign = (value >>> 15) & 1;
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

    /**
     * Holds a map entry together with the encoded bytes of its key.
     *
     * <p>The encoded key is calculated once before sorting.
     */
    private static final class EncodedMapEntry {

        /** Comparator used for deterministic map entries ordering */
        private static final Comparator<EncodedMapEntry> ORDERING = new Comparator<EncodedMapEntry>() {
            @Override
            public int compare(EncodedMapEntry left, EncodedMapEntry right) {
                return compareUnsignedBytes(left.encodedKey, right.encodedKey);
            }
        };

        /** Map entry */
        private final CBORMapEntry entry;

        /** Encoded entry key */
        private final byte[] encodedKey;

        /**
         * Default constructor
         *
         * @param entry {@link CBORMapEntry}
         * @param encodedKey byte array
         */
        private EncodedMapEntry(CBORMapEntry entry, byte[] encodedKey) {
            this.entry = entry;
            this.encodedKey = encodedKey;
        }
    }

    private static int compareUnsignedBytes(byte[] left, byte[] right) {
        int length = Math.min(left.length, right.length);

        for (int i = 0; i < length; i++) {
            int comparison = Integer.compare(left[i] & 0xff, right[i] & 0xff);

            if (comparison != 0) {
                return comparison;
            }
        }

        return Integer.compare(left.length, right.length);
    }

    /**
     * Produces a human-readable representation of a CBOR value.
     *
     * @param value the value to describe
     * @return a human-readable representation
     */
    static String describe(CBORValue value) {
        Objects.requireNonNull(value, "value");

        switch (value.getType()) {
            case INTEGER:
                return value.asInteger().toString();

            case FLOATING_POINT:
                return Double.toString(value.asDouble());

            case BYTE_STRING:
                return "h'" + hex(value.asBytes()) + "'";

            case TEXT_STRING:
                return quote(value.asString());

            case BOOLEAN:
                return Boolean.toString(
                        value.asBoolean());

            case NULL:
                return "null";

            case UNDEFINED:
                return "undefined";

            case SIMPLE_VALUE:
                return "simple(" + value.asSimpleValue() + ")";

            case ARRAY:
                return describeArray(value);

            case MAP:
                return describeMap(value);

            case TAG:
                return value.getTag() + "(" + describe(value.getTaggedValue()) + ")";

            default:
                return String.valueOf(CBORValue.raw(value));
        }
    }

    private static String describeArray(CBORValue value) {
        StringBuilder result = new StringBuilder("[");

        List<CBORValue> elements = value.getElements();

        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }

            result.append(describe(elements.get(i)));
        }

        return result.append(']').toString();
    }

    private static String describeMap(CBORValue value) {
        StringBuilder result = new StringBuilder("{");

        List<CBORMapEntry> entries = value.getEntries();

        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }

            CBORMapEntry entry =
                    entries.get(i);

            result.append(describe(entry.key()))
                    .append(": ")
                    .append(describe(entry.value()));
        }

        return result.append('}').toString();
    }

    private static String quote(String value) {
        return '"'
                + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + '"';
    }

    private static String hex(byte[] bytes) {
        char[] digits = "0123456789abcdef".toCharArray();
        char[] result = new char[bytes.length * 2];

        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            result[i * 2] = digits[value >>> 4];
            result[i * 2 + 1] = digits[value & 0x0f];
        }

        return new String(result);
    }

}
