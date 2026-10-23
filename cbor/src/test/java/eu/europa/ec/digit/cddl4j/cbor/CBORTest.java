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

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the project-owned CBOR model, decoder, and deterministic encoder.
 */
class CBORTest {

    @Test
    void roundTripsAllCoreValueCategories() throws CBORDecodeException {
        CBORValue map = CBOR.map()
                .put("uint", 42)
                .put("negative", -7)
                .put("bytes", new byte[] { 0, 1, (byte) 255 })
                .put("text", "héllo")
                .put("bool", true)
                .put("null", CBOR.nullValue())
                .put("array", CBOR.array()
                        .add(1.5)
                        .add(CBOR.undefined())
                        .value())
                .value();

        CBORValue decoded = CBORDecoder.decode(map.encodeToBytes());

        assertEquals(map, decoded);
    }

    @Test
    void supportsArbitraryNestedTags() throws CBORDecodeException {
        CBORValue tagged = CBOR.tag(42,
                CBOR.tag(1004, CBOR.text("value"))
        );

        assertTrue(tagged.isTag());
        assertEquals(BigInteger.valueOf(42), tagged.getTag());

        CBORValue innerTag = tagged.getTaggedValue();

        assertTrue(innerTag.isTag());
        assertEquals(BigInteger.valueOf(1004), innerTag.getTag());
        assertEquals("value", innerTag.getTaggedValue().asString());

        CBORValue decoded = CBOR.decodeFromBytes(tagged.encodeToBytes());
        assertEquals(tagged, decoded);
    }

    @Test
    void deterministicEncodingIsIndependentOfMapInsertionOrder() {
        CBORValue first = CBOR.map()
                .put("z", 1)
                .put("a", 2)
                .value();

        CBORValue second = CBOR.map()
                .put("a", 2)
                .put("z", 1)
                .value();

        assertArrayEquals(CBOREncoder.encodeDeterministic(second), CBOREncoder.encodeDeterministic(first));
    }

    @Test
    void supportsIndefiniteArrayMapAndStringForms() throws CBORDecodeException {
        // [_ 1, "x", {"a": h'0102'} _]
        byte[] bytes = new byte[] {
                (byte) 0x9f, 0x01, 0x61, 0x78, (byte) 0xbf, 0x61,
                0x61, 0x42,  0x01, 0x02, (byte) 0xff, (byte) 0xff
        };

        CBORValue result = CBOR.decodeFromBytes(bytes);

        assertEquals(CBORType.ARRAY, result.getType());
        assertEquals(3, result.size());
        assertArrayEquals(new byte[] {1, 2}, result.get(2).get(CBOR.text("a")).asBytes());
    }

    @Test
    void supportsIndefiniteByteAndTextStrings() throws CBORDecodeException {
        // 7f 63 hel 62 lo ff
        byte[] textBytes = new byte[] { (byte) 0x7f, 0x63, 0x68, 0x65, 0x6c, 0x62, 0x6c, 0x6f, (byte) 0xff };

        CBORValue text = CBOR.decodeFromBytes(textBytes);

        assertEquals("hello", text.asString());

        // 5f 41 01 42 02 03 ff
        byte[] binaryBytes = new byte[] { (byte) 0x5f, 0x41, 0x01, 0x42, 0x02, 0x03, (byte) 0xff };

        CBORValue binary = CBOR.decodeFromBytes(binaryBytes);
        assertArrayEquals(new byte[] {1, 2, 3}, binary.asBytes());
    }

    @Test
    void rejectsDuplicateMapKeysWithoutOverwriting() {
        // {"a": 1, "a": 2}
        byte[] duplicate = new byte[] { (byte) 0xa2, 0x61, 0x61, 0x01, 0x61, 0x61, 0x02 };

        CBORDecodeException exception = assertThrows(CBORDecodeException.class,  () -> CBOR.decodeFromBytes(duplicate));
        assertTrue(exception.getMessage().contains("Duplicate map key"));
    }

    @Test
    void rejectsDuplicateNonTextKeysToo() {
        // {1: 1, 1: 2}
        byte[] duplicate = new byte[] { (byte) 0xa2, 0x01, 0x01, 0x01, 0x02 };
        assertThrows(CBORDecodeException.class, () -> CBOR.decodeFromBytes(duplicate));
    }

    @Test
    void rejectsTruncationTrailingBytesAndReservedValues() {
        assertThrows(CBORDecodeException.class, () -> CBOR.decodeFromBytes(new byte[] {(byte) 0x18}));

        CBORDecodeException trailing = assertThrows(CBORDecodeException.class, () ->
                CBOR.decodeFromBytes(new byte[] {0x01, 0x02}));
        assertTrue(trailing.getMessage().contains("Trailing"));

        assertThrows(CBORDecodeException.class, () -> CBOR.decodeFromBytes(new byte[] {(byte) 0xff}));
        assertThrows(CBORDecodeException.class, () -> CBOR.decodeFromBytes(new byte[] {(byte) 0xf8, 0x01}));
    }

    @Test
    void rejectsAssignedAndReservedSimpleValueNumbers() {
        for (int value = 20; value <= 31; value++) {
            final int simpleValue = value;
            assertThrows(IllegalArgumentException.class, () -> CBOR.simpleValue(simpleValue));
        }
    }

    @Test
    void roundTripsPermittedSimpleValueNumbers() throws CBORDecodeException {
        for (int value : new int[] {0, 19, 32, 255}) {
            CBORValue simpleValue = CBOR.simpleValue(value);
            assertEquals(simpleValue, CBOR.decodeFromBytes(simpleValue.encodeToBytes()));
        }
    }

    @Test
    void rejectsExcessivelyNestedValuesWithCheckedDecodeFailure() {
        byte[] nestedArrays = new byte[258];
        for (int i = 0; i < 257; i++) {
            nestedArrays[i] = (byte) 0x81;
        }
        nestedArrays[257] = 0x00;

        CBORDecodeException exception = assertThrows(CBORDecodeException.class,
                () -> CBOR.decodeFromBytes(nestedArrays));

        assertTrue(exception.getMessage().contains("nesting depth"));
    }

    @Test
    void rejectsSimpleValuesOutsideByteRange() {
        assertThrows(IllegalArgumentException.class, () -> CBOR.simpleValue(-1));
        assertThrows(IllegalArgumentException.class, () -> CBOR.simpleValue(256));
    }

    @Test
    void truncatedLargeByteStringDoesNotPreallocateDeclaredLength() {
        byte[] declaredLargeString = {
                0x5a,
                0x7f, (byte) 0xff, (byte) 0xff, (byte) 0xff
        };

        assertThrows(CBORDecodeException.class, () -> CBOR.decodeFromBytes(declaredLargeString));
    }

    @Test
    void rejectsInvalidUtf8() {
        CBORDecodeException exception = assertThrows(CBORDecodeException.class, () ->
                CBOR.decodeFromBytes(new byte[] { 0x62, (byte) 0xc3, 0x28 }));
        assertTrue(exception.getMessage().contains("UTF-8"));
    }

    @Test
    void rejectsDuplicateKeysDuringProgrammaticConstruction() {
        CBORValue.CBORMapEditor map = CBOR.map();

        map.put(1, "first");

        assertThrows(IllegalArgumentException.class, () -> map.put(1, "second"));
    }

    @Test
    void editorsCanBeUsedForLongerEditingSessions() {
        CBORValue.CBORMapEditor map = CBOR.map();

        CBORValue.CBORMapEditor returned = map
                .put("a", 1)
                .put("b", 2)
                .set("a", 10)
                .remove("b");

        assertEquals(map, returned);
        assertEquals(1, map.value().size());
        assertEquals(BigInteger.valueOf(10), map.value().get("a").asInteger());
    }

    @Test
    void arrayEditorCanBeUsedForLongerEditingSessions() {
        CBORValue.CBORArrayEditor array = CBOR.array();

        CBORValue.CBORArrayEditor returned = array
                .add(1)
                .add(2)
                .set(0, 10)
                .remove(1);

        assertEquals(array, returned);
        assertEquals(1, array.value().size());
        assertEquals(BigInteger.TEN, array.value().get(0).asInteger());
    }

    @Test
    void editingExistingValueMutatesTheValueInPlace() {
        CBORValue array = CBOR.array()
                .add(1)
                .add(2)
                .add(3)
                .value();

        CBORValue.CBORArrayEditor editor = array.editArray()
                .set(0, 10)
                .remove(1)
                .add(20);

        assertEquals(3, array.size());
        assertEquals(BigInteger.valueOf(10), array.get(0).asInteger());
        assertEquals(BigInteger.valueOf(3), array.get(1).asInteger());
        assertEquals(BigInteger.valueOf(20), array.get(2).asInteger());

        assertEquals(array, editor.value());
    }

    @Test
    void editingExistingMapMutatesTheValueInPlace() {
        CBORValue map = CBOR.map()
                .put("a", 1)
                .put("b", 2)
                .value();

        CBORValue.CBORMapEditor editor = map.editMap()
                .set("a", 10)
                .remove("b")
                .put("c", 3);

        assertEquals(2, map.size());
        assertEquals(BigInteger.valueOf(10), map.get("a").asInteger());
        assertEquals(BigInteger.valueOf(3), map.get("c").asInteger());

        assertEquals(map, editor.value());
    }

    @Test
    void nestedValuesCanBeEditedDirectly() {
        CBORValue value = CBOR.map()
                .put("headers", CBOR.map()
                        .put("alg", "RS256")
                        .put("kid", "old")
                        .value())
                .value();

        value.get("headers")
                .editMap()
                .set("kid", "new")
                .put("typ", "JWT");

        assertEquals("RS256", value.get("headers").get("alg").asString());
        assertEquals("new", value.get("headers").get("kid").asString());
        assertEquals("JWT", value.get("headers").get("typ").asString());
    }

    @Test
    void editorsReturnTheirUnderlyingValue() {
        CBORValue.CBORArrayEditor array = CBOR.array()
                .add(1)
                .add(2);

        CBORValue arrayValue = array.value();

        assertEquals(CBORType.ARRAY, arrayValue.getType());
        assertEquals(2, arrayValue.size());

        CBORValue.CBORMapEditor map = CBOR.map().put("a", 1);

        CBORValue mapValue = map.value();

        assertEquals(CBORType.MAP, mapValue.getType());
        assertEquals(1, mapValue.size());
    }

    @Test
    void byteStringsAreIsolatedFromInputAndOutput() {
        byte[] input = {1, 2};

        CBORValue bytes = CBOR.bytes(input);

        input[0] = 9;

        byte[] output = bytes.asBytes();
        output[1] = 8;

        assertArrayEquals(new byte[] {1, 2}, bytes.asBytes());
    }

    @Test
    void supportsMaximumUnsigned64BitTag() throws CBORDecodeException {
        BigInteger maximum = new BigInteger("18446744073709551615");

        CBORValue value = CBOR.tag(maximum, CBOR.text("value"));

        assertEquals(maximum, value.getTag());
        assertEquals(value, CBOR.decodeFromBytes(value.encodeToBytes()));

        assertArrayEquals(new byte[] {
                            (byte) 0xdb,
                            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                            0x65, 0x76, 0x61, 0x6c, 0x75, 0x65
                          },
                value.encodeToBytes()
        );
    }

    @Test
    void rejectsInvalidTagNumbers() {
        assertThrows(IllegalArgumentException.class, () -> CBOR.tag(BigInteger.valueOf(-1), CBOR.nullValue()));

        assertThrows(IllegalArgumentException.class,
                () -> CBOR.tag(new BigInteger("18446744073709551616"), CBOR.nullValue()));

        assertThrows(IllegalArgumentException.class,
                () -> CBOR.integer(new BigInteger("18446744073709551616")).encodeToBytes());

        assertThrows(IllegalArgumentException.class,
                () -> CBOR.integer(new BigInteger("-18446744073709551617")).encodeToBytes());
    }

    @Test
    void deterministicEncodingOrdersMapKeysLexicographicallyByEncoding() {
        CBORValue value = CBOR.map()
                .put(-1, 1)
                .put(100, 2)
                .value();

        assertArrayEquals(new byte[] {
                (byte) 0xa2, 0x18, 0x64, 0x02, 0x20, 0x01
        }, CBOREncoder.encodeDeterministic(value));
    }

    @Test
    void deterministicEncodingUsesEncodedKeyOrdering() {
        CBORValue value = CBOR.map()
                .put("z", 1)
                .put("a", 2)
                .value();

        assertArrayEquals(new byte[] { (byte) 0xa2, 0x61, 0x61, 0x02, 0x61, 0x7a, 0x01 },
                CBOREncoder.encodeDeterministic(value));
    }

    @Test
    void encodesExactlyRepresentableHalfPrecisionValuesUsingBinary16() {
        assertArrayEquals(new byte[] {(byte) 0xf9, 0x3e, 0x00}, CBOREncoder.encode(CBOR.floatingPoint(1.5)));
        assertArrayEquals(new byte[] {(byte) 0xf9, 0x7b, (byte) 0xff}, CBOREncoder.encode(CBOR.floatingPoint(65504.0)));
    }

    @Test
    void roundTripsFloatingPointEdgeValues() throws CBORDecodeException {
        double[] values = { -0.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN,
                Double.MIN_VALUE, Double.MAX_VALUE };

        for (double value : values) {
            CBORValue decoded = CBOR.decodeFromBytes(CBOR.floatingPoint(value).encodeToBytes());
            assertEquals(Double.doubleToLongBits(value), Double.doubleToLongBits(decoded.asDouble()));
        }
    }

    /** Verifies that the configured byte limit is cumulative across indefinite string chunks. */
    @Test
    void configuredStringLengthLimitAppliesAcrossIndefiniteChunks() {
        String previousLimit = System.getProperty(CBORProperties.MAX_BYTE_STRING_LENGTH);
        System.setProperty(CBORProperties.MAX_BYTE_STRING_LENGTH, "1");

        try {
            byte[] byteString = {
                    (byte) 0x5f, (byte) 0x41, 0x61, (byte) 0x41, 0x62, (byte) 0xff
            };
            byte[] textString = {
                    (byte) 0x7f, (byte) 0x61, 0x61, (byte) 0x61, 0x62, (byte) 0xff
            };

            assertThrows(CBORDecodeException.class, () -> CBORDecoder.decode(byteString));
            assertThrows(CBORDecodeException.class, () -> CBORDecoder.decode(textString));
        } finally {
            if (previousLimit == null) {
                System.clearProperty(CBORProperties.MAX_BYTE_STRING_LENGTH);
            } else {
                System.setProperty(CBORProperties.MAX_BYTE_STRING_LENGTH, previousLimit);
            }
        }
    }

    /** Verifies that malformed indefinite-string chunks are rejected by the decoder. */
    @Test
    void rejectsInvalidIndefiniteStringChunks() {
        assertThrows(CBORDecodeException.class,
                () -> CBOR.decodeFromBytes(new byte[] { (byte) 0x5f, 0x61, 0x61, (byte) 0xff}));
        assertThrows(CBORDecodeException.class,
                () -> CBOR.decodeFromBytes(new byte[] { (byte) 0x7f, 0x62, (byte) 0xc3, (byte) 0xff, (byte) 0xff }));
    }
    
    @Test
    void integersUseDifferentMajorTypesButSameType() throws CBORDecodeException {
        CBORValue unsigned = CBOR.integer(1);
        CBORValue negative = CBOR.integer(-1);

        assertEquals(CBORType.INTEGER, unsigned.getType());
        assertEquals(CBORType.INTEGER, negative.getType());

        assertEquals(BigInteger.ONE, unsigned.asInteger());
        assertEquals(BigInteger.valueOf(-1), negative.asInteger());

        assertEquals(CBORMajorType.UNSIGNED_INTEGER, CBORMajorType.fromInitialByte(0x01));
        assertEquals(CBORMajorType.NEGATIVE_INTEGER, CBORMajorType.fromInitialByte(0x20));
    }

    @Test
    void specialMajorTypeMapsToDifferentTypes() throws CBORDecodeException {
        CBORValue falseValue = CBORDecoder.decode(new byte[] {(byte) 0xf4});
        CBORValue nullValue = CBORDecoder.decode(new byte[] {(byte) 0xf6});
        CBORValue floatValue = CBORDecoder.decode(new byte[] {(byte) 0xfb, 0, 0, 0, 0, 0, 0, 0, 0});

        assertEquals(CBORType.BOOLEAN, falseValue.getType());
        assertEquals(CBORType.NULL, nullValue.getType());
        assertEquals(CBORType.FLOATING_POINT, floatValue.getType());

        assertEquals(CBORMajorType.SPECIAL, CBORMajorType.fromInitialByte(0xf4));
        assertEquals(CBORMajorType.SPECIAL, CBORMajorType.fromInitialByte(0xf6));
        assertEquals(CBORMajorType.SPECIAL, CBORMajorType.fromInitialByte(0xfb));
    }

    @Test
    void booleanAccessorReturnsBooleanForBooleanValue() {
        assertTrue(CBOR.trueValue().isBoolean());
        assertFalse(CBOR.falseValue().asBoolean());
        assertTrue(CBOR.trueValue().asBoolean());
    }

    @Test
    void majorTypesAreDecodedFromInitialByte() {
        for (CBORMajorType type : CBORMajorType.values()) {
            int initialByte = type.getValue() << 5;
            assertEquals(type, CBORMajorType.fromInitialByte(initialByte));
        }
    }
    
}
