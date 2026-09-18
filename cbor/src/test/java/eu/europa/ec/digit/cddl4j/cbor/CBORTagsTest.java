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

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CBORTagsTest {

    @Test
    void tdateUsesTagZero() {
        CBORValue value = CBORTags.tdate(OffsetDateTime.of(
                2024, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC));
        assertEquals(CBORTags.TDATE, value.getTag().longValue());
    }

    @Test
    void tdateFromOffsetDateTimePreservesOffset() {
        OffsetDateTime dateTime = OffsetDateTime.of(
                2024, 1, 2, 3, 4, 5, 123_000_000, ZoneOffset.ofHours(2));

        CBORValue value = CBORTags.tdate(dateTime);

        assertEquals(CBORTags.TDATE, value.getTag().longValue());
        assertEquals("2024-01-02T03:04:05.123+02:00", value.getTaggedValue().asString());
    }

    @Test
    void tdateFromInstantUsesUtc() {
        Instant instant = Instant.parse("2024-01-02T03:04:05.123Z");

        CBORValue value = CBORTags.tdate(instant);

        assertEquals(CBORTags.TDATE, value.getTag().longValue());
        assertEquals("2024-01-02T03:04:05.123Z", value.getTaggedValue().asString());
    }

    @Test
    void tdateFromDateUsesUtc() {
        Date date = Date.from(Instant.parse("2024-01-02T03:04:05.123Z"));

        CBORValue value = CBORTags.tdate(date);

        assertEquals(CBORTags.TDATE, value.getTag().longValue());
        assertEquals("2024-01-02T03:04:05.123Z", value.getTaggedValue().asString());
    }

    @Test
    void tdatePreservesFractionalSeconds() {
        OffsetDateTime dateTime = OffsetDateTime.of(2024, 1, 2, 3, 4, 5,
                123_456_789, ZoneOffset.ofHoursMinutes(5, 30));

        CBORValue value = CBORTags.tdate(dateTime);
        assertEquals("2024-01-02T03:04:05.123456789+05:30", value.getTaggedValue().asString());
    }

    @Test
    void tdateRejectsNull() {
        assertThrows(NullPointerException.class, () -> CBORTags.tdate((Instant) null));
        assertThrows(NullPointerException.class, () -> CBORTags.tdate((Date) null));
        assertThrows(NullPointerException.class, () -> CBORTags.tdate((OffsetDateTime) null));
    }

    @Test
    void timeUsesTagOne() {
        CBORValue value = CBORTags.time(Instant.ofEpochSecond(42));
        assertEquals(CBORTags.TIME, value.getTag().longValue());
    }

    @Test
    void timeWithIntegralInstantUsesInteger() {
        CBORValue value = CBORTags.time(Instant.ofEpochSecond(42));

        CBORValue taggedValue = value.getTaggedValue();

        assertTrue(taggedValue.isInteger());
        assertEquals(BigInteger.valueOf(42), taggedValue.asInteger());
    }

    @Test
    void timeWithFractionalInstantUsesFloatingPoint() {
        Instant instant = Instant.ofEpochSecond(42, 500_000_000);

        CBORValue value = CBORTags.time(instant);

        CBORValue taggedValue = value.getTaggedValue();

        assertTrue(taggedValue.isFloatingPoint());
        assertEquals(42.5, taggedValue.asDouble());
    }

    @Test
    void timeSupportsNegativeFractionalEpochValues() {
        Instant instant = Instant.ofEpochSecond(-1, 500_000_000);

        CBORValue value = CBORTags.time(instant);

        assertTrue(value.getTaggedValue().isFloatingPoint());
        assertEquals(-0.5, value.getTaggedValue().asDouble());
    }

    @Test
    void timeFromDateUsesInstantSemantics() {
        Date date = Date.from(Instant.ofEpochSecond(42, 500_000_000));

        CBORValue value = CBORTags.time(date);

        assertEquals(CBORTags.TIME, value.getTag().longValue());
        assertTrue(value.getTaggedValue().isFloatingPoint());
        assertEquals(42.5, value.getTaggedValue().asDouble());
    }

    @Test
    void timeRejectsNull() {
        assertThrows(NullPointerException.class, () -> CBORTags.time((Instant) null));
        assertThrows(NullPointerException.class, () -> CBORTags.time((Date) null));
    }

    @Test
    void uriUsesTag32() {
        CBORValue value = CBORTags.uri("https://example.com");

        assertEquals(CBORTags.URI, value.getTag().longValue());
        assertEquals("https://example.com", value.getTaggedValue().asString());
    }

    @Test
    void regexpUsesTag35() {
        CBORValue value = CBORTags.regexp("[a-z]+");

        assertEquals(CBORTags.REGEXP, value.getTag().longValue());
        assertEquals("[a-z]+", value.getTaggedValue().asString());
    }

    @Test
    void b64urlUsesTag33() {
        CBORValue value = CBORTags.b64url("SGVsbG8");

        assertEquals(CBORTags.B64URL, value.getTag().longValue());
        assertEquals("SGVsbG8", value.getTaggedValue().asString());
    }

    @Test
    void b64legacyUsesTag34() {
        CBORValue value = CBORTags.b64legacy("SGVsbG8=");

        assertEquals(CBORTags.B64LEGACY, value.getTag().longValue());
        assertEquals("SGVsbG8=", value.getTaggedValue().asString());
    }

    @Test
    void mimeMessageUsesTag36() {
        CBORValue value = CBORTags.mimeMessage("Content-Type: text/plain");

        assertEquals(CBORTags.MIME_MESSAGE, value.getTag().longValue());
        assertEquals("Content-Type: text/plain", value.getTaggedValue().asString());
    }

    @Test
    void biguintUsesTag2() {
        CBORValue value = CBORTags.biguint(BigInteger.valueOf(256));

        assertEquals(CBORTags.BIGUINT, value.getTag().longValue());
        assertTrue(value.getTaggedValue().isByteString());
        assertArrayEquals(new byte[] { 0x01, 0x00 }, value.getTaggedValue().asBytes());
    }

    @Test
    void biguintSupportsZero() {
        CBORValue value = CBORTags.biguint(BigInteger.ZERO);

        assertEquals(CBORTags.BIGUINT, value.getTag().longValue());
        assertArrayEquals(new byte[0], value.getTaggedValue().asBytes());
    }

    @Test
    void biguintRejectsNegativeValue() {
        assertThrows(IllegalArgumentException.class, () -> CBORTags.biguint(BigInteger.valueOf(-1)));
    }

    @Test
    void biguintRejectsNull() {
        assertThrows(NullPointerException.class, () -> CBORTags.biguint(null));
    }

    @Test
    void bignintUsesTag3() {
        CBORValue value = CBORTags.bignint(BigInteger.valueOf(-257));

        assertEquals(CBORTags.BIGNINT, value.getTag().longValue());
        assertArrayEquals(new byte[] {0x01, 0x00}, value.getTaggedValue().asBytes());
    }

    @Test
    void bignintUsesCorrectMagnitudeForMinusOne() {
        CBORValue value = CBORTags.bignint(BigInteger.valueOf(-1));
        assertArrayEquals(new byte[0], value.getTaggedValue().asBytes());
    }

    @Test
    void biguintZeroUsesEmptyByteString() {
        CBORValue value = CBORTags.biguint(BigInteger.ZERO);
        assertArrayEquals(new byte[] { (byte) 0xc2, (byte) 0x40 }, value.encodeToBytes());
    }

    @Test
    void bignintMinusOneUsesEmptyByteString() {
        CBORValue value = CBORTags.bignint(BigInteger.valueOf(-1));
        assertArrayEquals(new byte[] { (byte) 0xc3, (byte) 0x40 }, value.encodeToBytes());
    }

    @Test
    void bignintRejectsNonNegativeValue() {
        assertThrows(IllegalArgumentException.class, () -> CBORTags.bignint(BigInteger.ZERO));
        assertThrows(IllegalArgumentException.class, () -> CBORTags.bignint(BigInteger.ONE));
    }

    @Test
    void bignintRejectsNull() {
        assertThrows(NullPointerException.class, () -> CBORTags.bignint(null));
    }

    @Test
    void decfracUsesTag4() {
        CBORValue value = CBORTags.decfrac(-2, BigInteger.valueOf(12345));
        assertEquals(CBORTags.DECFRAC, value.getTag().longValue());

        CBORValue array = value.getTaggedValue();
        assertTrue(array.isArray());
        assertEquals(2, array.size());
        assertEquals(BigInteger.valueOf(-2), array.get(0).asInteger());
        assertEquals(BigInteger.valueOf(12345), array.get(1).asInteger());
    }

    @Test
    void decfracRejectsNullMantissa() {
        assertThrows(NullPointerException.class, () -> CBORTags.decfrac(-2, null));
    }

    @Test
    void bigfloatUsesTag5() {
        CBORValue value = CBORTags.bigfloat(-2, BigInteger.valueOf(12345));
        assertEquals(CBORTags.BIGFLOAT, value.getTag().longValue());

        CBORValue array = value.getTaggedValue();

        assertTrue(array.isArray());
        assertEquals(2, array.size());
        assertEquals(BigInteger.valueOf(-2), array.get(0).asInteger());
        assertEquals(BigInteger.valueOf(12345), array.get(1).asInteger());
    }

    @Test
    void bigfloatRejectsNullMantissa() {
        assertThrows(NullPointerException.class, () -> CBORTags.bigfloat(-2, null));
    }

    @Test
    void arbitraryLargeBiguintRoundTrips() throws CBORDecodeException {
        BigInteger value = new BigInteger("123456789012345678901234567890");

        CBORValue tagged = CBORTags.biguint(value);
        assertEquals(tagged, CBOR.decodeFromBytes(tagged.encodeToBytes()));
    }

    @Test
    void dateUsesRfc8943NumericDateTag() {
        LocalDate date = LocalDate.of(1940, 10, 9);

        CBORValue value = CBORTags.date(date);

        assertEquals(CBORTags.DATE_DAYS, value.getTag().longValue());
        assertEquals(BigInteger.valueOf(-10676), value.getTaggedValue().asInteger());
    }

    @Test
    void dateSupportsEpochAndDatesBeforeEpoch() {
        assertEquals(BigInteger.ZERO,
                CBORTags.date(LocalDate.of(1970, 1, 1))
                        .getTaggedValue()
                        .asInteger());

        assertEquals(BigInteger.valueOf(-1),
                CBORTags.date(LocalDate.of(1969, 12, 31))
                        .getTaggedValue()
                        .asInteger());

        assertEquals(BigInteger.ONE,
                CBORTags.date(LocalDate.of(1970, 1, 2))
                        .getTaggedValue()
                        .asInteger());
    }

    @Test
    void dateTextUsesRfc8943DateStringTag() {
        LocalDate date = LocalDate.of(1940, 10, 9);
        CBORValue value = CBORTags.fullDate(date);
        assertEquals(CBORTags.FULL_DATE, value.getTag().longValue());
        assertEquals("1940-10-09", value.getTaggedValue().asString());
    }

    @Test
    void rfc8943DatesRoundTrip() throws CBORDecodeException {
        LocalDate date = LocalDate.of(1940, 10, 9);

        CBORValue numeric = CBORTags.date(date);
        CBORValue textual = CBORTags.fullDate(date);

        assertEquals(numeric, CBOR.decodeFromBytes(numeric.encodeToBytes()));
        assertEquals(textual, CBOR.decodeFromBytes(textual.encodeToBytes()));
    }

    @Test
    void dateFromDateUsesUtc() {
        Date value = Date.from(Instant.parse("2026-09-03T00:30:00Z"));
        CBORValue result = CBORTags.date(value);
        assertEquals(LocalDate.of(2026, 9, 3).toEpochDay(), result.getTaggedValue().asInteger().longValue());
    }

    @Test
    void fullDateFromDateUsesUtc() {
        Date value = Date.from(Instant.parse("2026-09-03T00:30:00Z"));
        CBORValue result = CBORTags.fullDate(value);
        assertEquals("2026-09-03", result.getTaggedValue().asString());
    }

    @Test
    void semanticTagValuesRoundTrip() throws CBORDecodeException {
        CBORValue[] values = {
                CBORTags.tdate(OffsetDateTime.of(2024, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC)),
                CBORTags.time(Instant.ofEpochSecond(42, 500_000_000)),
                CBORTags.biguint(BigInteger.valueOf(256)),
                CBORTags.bignint(BigInteger.valueOf(-257)),
                CBORTags.decfrac(-2, BigInteger.valueOf(12345)),
                CBORTags.bigfloat(-2, BigInteger.valueOf(12345)),
                CBORTags.encodedCbor(new byte[] { 0x00, 0x01 }),
                CBORTags.uri("https://example.com"),
                CBORTags.b64url("SGVsbG8"),
                CBORTags.b64legacy("SGVsbG8="),
                CBORTags.regexp("[a-z]+"),
                CBORTags.mimeMessage("Content-Type: text/plain"),
                CBORTags.date(LocalDate.of(2001, 1, 1)),
                CBORTags.fullDate(LocalDate.of(2010, 1, 1))
        };

        for (CBORValue value : values) {
            assertEquals(value, CBOR.decodeFromBytes(value.encodeToBytes()));
        }
    }

    @Test
    void tagConstantsHaveExpectedValues() {
        assertEquals(0, CBORTags.TDATE);
        assertEquals(1, CBORTags.TIME);
        assertEquals(2, CBORTags.BIGUINT);
        assertEquals(3, CBORTags.BIGNINT);
        assertEquals(4, CBORTags.DECFRAC);
        assertEquals(5, CBORTags.BIGFLOAT);
        assertEquals(24, CBORTags.ENCODED_CBOR);
        assertEquals(32, CBORTags.URI);
        assertEquals(33, CBORTags.B64URL);
        assertEquals(34, CBORTags.B64LEGACY);
        assertEquals(35, CBORTags.REGEXP);
        assertEquals(36, CBORTags.MIME_MESSAGE);
        assertEquals(100, CBORTags.DATE_DAYS);
        assertEquals(1004, CBORTags.FULL_DATE);
    }

}