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

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
/**
 * Convenience constructors for standardized CBOR semantic tags.
 *
 * <p>This class provides convenience methods for commonly used standardized
 * CBOR semantic tags. It does not restrict applications to the tags exposed
 * here; application-specific or otherwise unsupported tags can be created
 * using {@link CBOR#tag(long, CBORValue)} or
 * {@link CBOR#tag(BigInteger, CBORValue)}.
 *
 * <p>Supported objects and corresponding methods:
 * <ul>
 *   <li>{@code tdate = #6.0(tstr)} — {@link #tdate(OffsetDateTime)}, {@link #tdate(Instant)}, {@link #tdate(Date)};
 *         date/time as an ISO-8601 text string.</li>
 *   <li>{@code time = #6.1(number)} — {@link #time(long)}, {@link #time(Instant)}, {@link #time(Date)};
 *         date/time as seconds since the Unix epoch.</li>
 *   <li>{@code biguint = #6.2(bstr)} — {@link #biguint(BigInteger)}; non-negative arbitrary-precision integer.</li>
 *   <li>{@code bignint = #6.3(bstr)} — {@link #bignint(BigInteger)}; negative arbitrary-precision integer.</li>
 *   <li>{@code bigint = biguint / bignint}; arbitrary-precision integer
 *         represented by either {@code biguint} or {@code bignint}.</li>
 *   <li>{@code decfrac = #6.4([e10: int, m: integer])} — {@link #decfrac(long, BigInteger)};
 *         decimal fraction represented by a base-10 exponent and an integer mantissa.</li>
 *   <li>{@code bigfloat = #6.5([e2: int, m: integer])} — {@link #bigfloat(long, BigInteger)};
 *         binary floating-point value represented by a base-2 exponent and an integer mantissa.</li>
 *   <li>{@code uri = #6.24(bstr)} — {@link #encodedCbor(byte[])}; encoded CBOR byte string.</li>
 *   <li>{@code uri = #6.32(tstr)} — {@link #uri(String)}; URI represented as a text string.</li>
 *   <li>{@code b64url = #6.33(tstr)} — {@link #b64url(String)}; base64url encoded data represented as a text string.</li>
 *   <li>{@code b64legacy = #6.34(tstr)} — {@link #b64legacy(String)}; legacy (or classic)
 *         base64 encoded data represented as a text string.</li>
 *   <li>{@code regexp = #6.35(tstr)} — {@link #regexp(String)}; regular expression represented as a text string.</li>
 *   <li>{@code mime-message = #6.36(tstr)} — {@link #mimeMessage(String)}; MIME message represented as a text string.</li>
 *   <li>{@code date = #6.100(int)} — {@link #date(LocalDate)}, {@link #date(Date)}; Gregorian calendar date.</li>
 *   <li>{@code full-date = #6.1004(tstr)} — {@link #fullDate(LocalDate)}, {@link #fullDate(Date)}; Gregorian calendar full-date.</li>
 * </ul>
 *
 * <p>Use {@link CBOR#tag(long, CBORValue)} for application-specific or
 * otherwise unsupported tags.
 */
public final class CBORTags {

    /** CBOR tag 0: date/time string ({@code tdate}). */
    public static final long TDATE = 0;

    /** CBOR tag 1: epoch-based date/time ({@code time}). */
    public static final long TIME = 1;

    /** CBOR tag 2: positive arbitrary-precision integer ({@code biguint}). */
    public static final long BIGUINT = 2;

    /** CBOR tag 3: negative arbitrary-precision integer ({@code bignint}). */
    public static final long BIGNINT = 3;

    /** CBOR tag 4: decimal fraction ({@code decfrac}). */
    public static final long DECFRAC = 4;

    /** CBOR tag 5: big float ({@code bigfloat}). */
    public static final long BIGFLOAT = 5;

    /** CBOR tag 24: encoded CBOR data item. */
    public static final long ENCODED_CBOR = 24;

    /** CBOR tag 32: URI ({@code uri}). */
    public static final long URI = 32;

    /** CBOR tag 33: base64url encoded data ({@code b64url}). */
    public static final long B64URL = 33;

    /** CBOR tag 34: legacy base64 encoded data ({@code b64legacy}). */
    public static final long B64LEGACY = 34;

    /** CBOR tag 35: regular expression ({@code regexp}). */
    public static final long REGEXP = 35;

    /** CBOR tag 36: MIME message ({@code mime-message}). */
    public static final long MIME_MESSAGE = 36;

    /** CBOR tag 100: Gregorian calendar date as days since 1970-01-01. */
    public static final long DATE_DAYS = 100;

    /** CBOR tag 1004: Gregorian calendar date as an RFC 3339 full-date string. */
    public static final long FULL_DATE = 1004;

    /** Default constructor. */
    private CBORTags() {
        // Utility class.
    }

    /**
     * Creates a {@code tdate} value using CBOR tag 0.
     *
     * <p>The instant is represented in UTC using the RFC 3339 {@code Z}
     * designator. Fractional seconds are preserved when present.
     *
     * <p>Example:
     * {@code tdate(Instant.parse("2026-08-31T12:34:56Z"))} produces
     * {@code #6.0("2026-08-31T12:34:56Z")}.
     *
     * @param value the instant
     * @return a tagged CBOR text string
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue tdate(Instant value) {
        Objects.requireNonNull(value, "value");

        return CBOR.tag(TDATE, CBOR.text(DateTimeFormatter.ISO_INSTANT.format(value)));
    }

    /**
     * Creates a {@code tdate} value using CBOR tag 0.
     *
     * <p>The {@link Date} is converted to an instant and represented in UTC
     * using the RFC 3339 {@code Z} designator. Fractional seconds are preserved
     * when present.
     *
     * <p>A {@link Date} does not contain timezone or offset information, so
     * UTC is used for its textual representation.
     *
     * @param value the date
     * @return a tagged CBOR text string
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue tdate(Date value) {
        Objects.requireNonNull(value, "value");

        return tdate(value.toInstant());
    }

    /**
     * Creates a {@code tdate} value using CBOR tag 0.
     *
     * <p>The supplied offset is preserved in the resulting RFC 3339
     * date/time string. Fractional seconds are preserved when present.
     *
     * <p>For example, an {@code OffsetDateTime} of
     * {@code 2026-08-31T14:34:56+02:00} is encoded as
     * {@code #6.0("2026-08-31T14:34:56+02:00")}.
     *
     * @param value the date/time
     * @return a tagged CBOR text string
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue tdate(OffsetDateTime value) {
        Objects.requireNonNull(value, "value");

        return CBOR.tag(TDATE, CBOR.text(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value)));
    }

    /**
     * Creates a {@code time} value using CBOR tag 1.
     *
     * <p>CBOR type: {@code time = #6.1(number)}.
     *
     * <p>The value is represented as the number of seconds since the Unix
     * epoch. An integral number of seconds is encoded as a CBOR integer.
     *
     * <p>Example: {@code time(1756643696)} produces the CBOR object
     * {@code #6.1(1756643696)}.
     *
     * @param epochSeconds the number of seconds since the Unix epoch
     * @return a tagged CBOR integer
     */
    public static CBORValue time(long epochSeconds) {
        return CBOR.tag(TIME, CBOR.integer(epochSeconds));
    }

    /**
     * Creates a {@code time} value using CBOR tag 1.
     *
     * <p>CBOR type: {@code time = #6.1(number)}.
     *
     * <p>The value is represented as the number of seconds since the Unix
     * epoch. Integral seconds are encoded as a CBOR integer. Fractional
     * seconds are encoded as a CBOR floating-point value.
     *
     * @param value the instant
     * @return a tagged CBOR numeric value
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue time(Instant value) {
        Objects.requireNonNull(value, "value");

        if (value.getNano() == 0) {
            return time(value.getEpochSecond());
        }

        double seconds = value.getEpochSecond() + value.getNano() / 1_000_000_000.0;
        return CBOR.tag(TIME, CBOR.floatingPoint(seconds));
    }

    /**
     * Creates a {@code time} value using CBOR tag 1.
     *
     * <p>The {@link Date} is converted to an instant before encoding.
     *
     * @param value the date
     * @return a tagged CBOR numeric value
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue time(Date value) {
        Objects.requireNonNull(value, "value");

        return time(value.toInstant());
    }

    /**
     * Creates a {@code biguint} value using CBOR tag 2.
     *
     * <p>CBOR type: {@code biguint = #6.2(bstr)}.
     *
     * <p>The supplied value must be non-negative. Its magnitude is encoded as
     * an unsigned byte string as required by the CBOR bignum representation.
     *
     * <p>Example: {@code biguint(BigInteger.valueOf(256))} produces the CBOR
     * object {@code #6.2(h'0100')}.
     *
     * @param value the non-negative integer
     * @return a tagged CBOR byte string
     * @throws NullPointerException if {@code value} is null
     * @throws IllegalArgumentException if {@code value} is negative
     */
    public static CBORValue biguint(BigInteger value) {
        Objects.requireNonNull(value, "value");

        if (value.signum() < 0) {
            throw new IllegalArgumentException("Positive bignum cannot be negative");
        }

        return CBOR.tag(BIGUINT, CBOR.bytes(unsignedMagnitude(value)));
    }

    /**
     * Creates a {@code bignint} value using CBOR tag 3.
     *
     * <p>CBOR type: {@code bignint = #6.3(bstr)}.
     *
     * <p>For a negative integer {@code n}, the byte string contains the
     * unsigned magnitude of {@code -1 - n}.
     *
     * <p>Example: {@code bignint(BigInteger.valueOf(-257))} produces the CBOR
     * object {@code #6.3(h'0100')}.
     *
     * @param value the negative integer
     * @return a tagged CBOR byte string
     * @throws NullPointerException if {@code value} is null
     * @throws IllegalArgumentException if {@code value} is non-negative
     */
    public static CBORValue bignint(BigInteger value) {
        Objects.requireNonNull(value, "value");

        if (value.signum() >= 0) {
            throw new IllegalArgumentException("Negative bignum must be negative");
        }

        BigInteger magnitude = value.negate().subtract(BigInteger.ONE);

        return CBOR.tag(BIGNINT, CBOR.bytes(unsignedMagnitude(magnitude)));
    }

    /**
     * Creates a {@code decfrac} value using CBOR tag 4.
     *
     * <p>CBOR type: {@code decfrac = #6.4([e10: int, m: integer])}.
     *
     * <p>The value represents a decimal fraction as a two-element array.
     * The first element is the base-10 exponent and the second element is
     * the integer mantissa.
     *
     * <p>Example: {@code decfrac(-2, BigInteger.valueOf(12345))} produces
     * the CBOR object {@code #6.4([-2, 12345])}.
     *
     * @param exponent the base-10 exponent
     * @param mantissa the integer mantissa
     * @return a tagged CBOR array
     * @throws NullPointerException if {@code mantissa} is null
     */
    public static CBORValue decfrac(long exponent, BigInteger mantissa) {
        Objects.requireNonNull(mantissa, "mantissa");

        CBORValue value = CBOR.array()
                .add(CBOR.integer(exponent))
                .add(CBOR.integer(mantissa))
                .value();

        return CBOR.tag(DECFRAC, value);
    }

    /**
     * Creates a {@code bigfloat} value using CBOR tag 5.
     *
     * <p>CBOR type: {@code bigfloat = #6.5([e2: int, m: integer])}.
     *
     * <p>The value represents a binary floating-point number as a two-element
     * array. The first element is the base-2 exponent and the second element
     * is the integer mantissa.
     *
     * <p>Example: {@code bigfloat(-2, BigInteger.valueOf(12345))} produces
     * the CBOR object {@code #6.5([-2, 12345])}.
     *
     * @param exponent the base-2 exponent
     * @param mantissa the integer mantissa
     * @return a tagged CBOR array
     * @throws NullPointerException if {@code mantissa} is null
     */
    public static CBORValue bigfloat(long exponent, BigInteger mantissa) {
        Objects.requireNonNull(mantissa, "mantissa");

        CBORValue value = CBOR.array()
                .add(CBOR.integer(exponent))
                .add(CBOR.integer(mantissa))
                .value();

        return CBOR.tag(BIGFLOAT, value);
    }

    /**
     * Creates an encoded CBOR value using CBOR tag 24.
     *
     * <p>CBOR type: {@code encoded-cbor = #6.24(bstr)}.
     *
     * <p>The supplied bytes are treated as an encoded CBOR data item and are
     * wrapped in a CBOR byte string.
     *
     * @param value the encoded CBOR data
     * @return a tagged CBOR byte string
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue encodedCbor(byte[] value) {
        Objects.requireNonNull(value, "value");

        return CBOR.tag(ENCODED_CBOR, CBOR.bytes(value));
    }

    /**
     * Creates a {@code uri} value using CBOR tag 32.
     *
     * <p>CBOR type: {@code uri = #6.32(tstr)}.
     *
     * <p>The URI is represented as a CBOR text string.
     *
     * <p>Example: {@code uri("https://example.com/")} produces the CBOR
     * object {@code #6.32("https://example.com/")}.
     *
     * @param value the URI
     * @return a tagged CBOR text string
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue uri(String value) {
        return CBOR.tag(URI, CBOR.text(value));
    }

    /**
     * Creates a {@code regexp} value using CBOR tag 35.
     *
     * <p>CBOR type: {@code regexp = #6.35(tstr)}.
     *
     * <p>The regular expression is represented as a CBOR text string.
     *
     * <p>Example: {@code regexp("[a-z]+")} produces the CBOR object
     * {@code #6.35("[a-z]+")}.
     *
     * @param value the regular expression
     * @return a tagged CBOR text string
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue regexp(String value) {
        return CBOR.tag(REGEXP, CBOR.text(value));
    }

    /**
     * Creates a {@code b64url} value using CBOR tag 33.
     *
     * <p>CBOR type: {@code b64url = #6.33(tstr)}.
     *
     * <p>The value is a base64url-encoded string represented as a CBOR text
     * string.
     *
     * <p>Example: {@code b64url("SGVsbG8")} produces the CBOR object
     * {@code #6.33("SGVsbG8")}.
     *
     * @param value the base64url-encoded string
     * @return a tagged CBOR text string
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue b64url(String value) {
        return CBOR.tag(B64URL, CBOR.text(value));
    }

    /**
     * Creates a {@code b64legacy} value using CBOR tag 34.
     *
     * <p>CBOR type: {@code b64legacy = #6.34(tstr)}.
     *
     * <p>The value is a legacy base64-encoded string represented as a CBOR
     * text string.
     *
     * <p>Example: {@code b64legacy("SGVsbG8=")} produces the CBOR object
     * {@code #6.34("SGVsbG8=")}.
     *
     * @param value the legacy base64-encoded string
     * @return a tagged CBOR text string
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue b64legacy(String value) {
        return CBOR.tag(B64LEGACY, CBOR.text(value));
    }

    /**
     * Creates a {@code mime-message} value using CBOR tag 36.
     *
     * <p>CBOR type: {@code mime-message = #6.36(tstr)}.
     *
     * <p>The MIME message is represented as a CBOR text string.
     *
     * <p>Example: {@code mimeMessage("Content-Type: text/plain")} produces
     * the CBOR object {@code #6.36("Content-Type: text/plain")}.
     *
     * @param value the MIME message
     * @return a tagged CBOR text string
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue mimeMessage(String value) {
        return CBOR.tag(MIME_MESSAGE, CBOR.text(value));
    }

    /**
     * Creates a Gregorian calendar date using CBOR tag 100.
     *
     * <p>CBOR type: {@code date = #6.100(int)}.
     *
     * <p>The date is represented as the number of days since
     * {@code 1970-01-01}. Dates before the Unix epoch are represented by
     * negative values.
     *
     * <p>For example, {@code date(LocalDate.of(1970, 1, 1))} produces the
     * CBOR object {@code #6.100(0)}, while
     * {@code date(LocalDate.of(1969, 12, 31))} produces
     * {@code #6.100(-1)}.
     *
     * <p>This corresponds to the {@code date} tag defined by RFC 8943.
     *
     * @param value the Gregorian calendar date
     * @return a tagged CBOR integer
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue date(LocalDate value) {
        Objects.requireNonNull(value, "value");

        return CBOR.tag(DATE_DAYS, CBOR.integer(value.toEpochDay()));
    }

    /**
     * Creates a Gregorian calendar date using CBOR tag 100.
     *
     * <p>The supplied {@link Date} is converted to an {@link Instant} and then
     * to a {@link LocalDate} using UTC. Consequently, the resulting date is the
     * calendar date corresponding to the instant at UTC.
     *
     * <p>CBOR type: {@code date = #6.100(int)}.
     *
     * <p>The date is represented as the number of days since
     * {@code 1970-01-01}. Dates before the Unix epoch are represented by
     * negative values.
     *
     * <p>This corresponds to the {@code date} tag defined by RFC 8943.
     *
     * @param value the date
     * @return a tagged CBOR integer
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue date(Date value) {
        Objects.requireNonNull(value, "value");

        return date(value.toInstant().atZone(ZoneOffset.UTC).toLocalDate());
    }

    /**
     * Creates a Gregorian calendar full-date using CBOR tag 1004.
     *
     * <p>CBOR type: {@code full-date = #6.1004(tstr)}.
     *
     * <p>The date is represented as an RFC 3339 {@code full-date} string
     * in {@code YYYY-MM-DD} form. Unlike CBOR tag 0, this value represents
     * a calendar date only and does not contain a time of day or timezone.
     *
     * <p>For example, {@code fullDate(LocalDate.of(2026, 8, 31))} produces
     * the CBOR object {@code #6.1004("2026-08-31")}.
     *
     * <p>This corresponds to the {@code full-date} tag defined by RFC 8943.
     *
     * @param value the Gregorian calendar date
     * @return a tagged CBOR text string
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue fullDate(LocalDate value) {
        Objects.requireNonNull(value, "value");

        return CBOR.tag(FULL_DATE, CBOR.text(value.toString()));
    }

    /**
     * Creates a Gregorian calendar full-date using CBOR tag 1004.
     *
     * <p>The supplied {@link Date} is converted to an {@link Instant} and then
     * to a {@link LocalDate} using UTC. Consequently, the resulting full-date
     * is the calendar date corresponding to the instant at UTC.
     *
     * <p>CBOR type: {@code full-date = #6.1004(tstr)}.
     *
     * <p>The date is represented as an RFC 3339 {@code full-date} string
     * in {@code YYYY-MM-DD} form.
     *
     * <p>This corresponds to the {@code full-date} tag defined by RFC 8943.
     *
     * @param value the date
     * @return a tagged CBOR text string
     * @throws NullPointerException if {@code value} is null
     */
    public static CBORValue fullDate(Date value) {
        Objects.requireNonNull(value, "value");

        return fullDate(value.toInstant().atZone(ZoneOffset.UTC).toLocalDate());
    }

    private static byte[] unsignedMagnitude(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length == 1 && bytes[0] == 0) {
            return new byte[0];
        }
        if (bytes[0] == 0) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
    }


}
