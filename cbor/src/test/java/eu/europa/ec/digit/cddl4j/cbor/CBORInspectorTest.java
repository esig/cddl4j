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

import java.io.ByteArrayInputStream;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CBORInspectorTest {

    @Test
    void readConsumesOnlyInitialByte() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { 0x01, 0x02 });

        CBORInspector inspector = new CBORInspector(input);

        assertEquals(0x01, inspector.read());
        assertEquals(0x02, input.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readReturnsUnsignedInitialByte() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { (byte) 0xff });

        CBORInspector inspector = new CBORInspector(input);

        assertEquals(0xff, inspector.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readTagReadsSmallTagWithoutConsumingFollowingItem() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { (byte) 0xc1, 0x01 });

        CBORInspector inspector = new CBORInspector(input);

        int initialByte = inspector.read();

        assertEquals(CBORMajorType.TAG, CBORMajorType.fromInitialByte(initialByte));
        assertEquals(1, inspector.readTag(initialByte));
        assertEquals(0x01, input.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readTagReadsOneByteTagArgument() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { (byte) 0xd8, 0x20, 0x01 });

        CBORInspector inspector = new CBORInspector(input);

        int initialByte = inspector.read();

        assertEquals(CBORMajorType.TAG, CBORMajorType.fromInitialByte(initialByte));
        assertEquals(32, inspector.readTag(initialByte));
        assertEquals(0x01, input.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readTagReadsTwoByteTagArgument() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { (byte) 0xd9, 0x01, 0x00, 0x01 });

        CBORInspector inspector = new CBORInspector(input);

        int initialByte = inspector.read();

        assertEquals(256, inspector.readTag(initialByte));
        assertEquals(0x01, input.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readTagReadsFourByteTagArgument() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { (byte) 0xda, 0x00, 0x01, 0x00, 0x00, 0x01 });

        CBORInspector inspector = new CBORInspector(input);

        int initialByte = inspector.read();

        assertEquals(65536, inspector.readTag(initialByte));
        assertEquals(0x01, input.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readTagReadsEightByteTagArgument() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {
                (byte) 0xdb, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01
        });

        CBORInspector inspector = new CBORInspector(input);

        int initialByte = inspector.read();

        assertEquals(256, inspector.readTag(initialByte));
        assertEquals(0x01, input.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readTagBigIntegerSupportsFullUnsigned64BitRange() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {
                (byte) 0xdb,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                0x01
        });

        CBORInspector inspector = new CBORInspector(input);

        assertEquals(
                new BigInteger("18446744073709551615"),
                inspector.readTagBigInteger(inspector.read()));
        assertEquals(0x01, input.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readTagLongRejectsTagsOutsideSignedRange() {
        CBORInspector inspector = new CBORInspector(new ByteArrayInputStream(new byte[] {
                (byte) 0xdb,
                (byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        }));

        assertThrows(
                CBORDecodeException.class,
                () -> inspector.readTag(inspector.read()));
    }

    @Test
    void readTagRejectsNonTagMajorType() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { 0x01 });

        CBORInspector inspector = new CBORInspector(input);

        int initialByte = inspector.read();

        assertThrows(CBORDecodeException.class, () -> inspector.readTag(initialByte));
        assertEquals(-1, input.read());
    }

    @Test
    void readLengthReadsSmallLength() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { 0x03, 0x01 });

        CBORInspector inspector = new CBORInspector(input);

        int initialByte = inspector.read();

        assertEquals(CBORMajorType.UNSIGNED_INTEGER, CBORMajorType.fromInitialByte(initialByte));
        assertEquals(3, inspector.readLength(initialByte));
        assertEquals(0x01, input.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readLengthReadsOneByteLength() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { 0x18, 0x18, 0x01 });

        CBORInspector inspector = new CBORInspector(input);

        int initialByte = inspector.read();

        assertEquals(24, inspector.readLength(initialByte));
        assertEquals(0x01, input.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readLengthReadsTwoByteLength() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { 0x19, 0x01, 0x00, 0x01 });

        CBORInspector inspector = new CBORInspector(input);

        int initialByte = inspector.read();

        assertEquals(256, inspector.readLength(initialByte));
        assertEquals(0x01, input.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readLengthReadsFourByteLength() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { 0x1a, 0x00, 0x01, 0x00, 0x00, 0x01 });

        CBORInspector inspector = new CBORInspector(input);

        int initialByte = inspector.read();

        assertEquals(65536, inspector.readLength(initialByte));
        assertEquals(0x01, input.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readLengthReadsEightByteLength() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] {
                0x1b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01
        });

        CBORInspector inspector = new CBORInspector(input);

        int initialByte = inspector.read();

        assertEquals(256, inspector.readLength(initialByte));
        assertEquals(0x01, input.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readLengthReturnsMinusOneForIndefiniteLength() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { (byte) 0x9f, 0x01, 0x02 });

        CBORInspector inspector = new CBORInspector(input);

        int initialByte = inspector.read();

        assertEquals(-1, inspector.readLength(initialByte));
        assertEquals(0x01, input.read());
        assertEquals(0x02, input.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readLengthDoesNotConsumeFollowingItemForDirectLength() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { 0x03, 0x01 });

        CBORInspector inspector = new CBORInspector(input);

        int initialByte = inspector.read();

        assertEquals(3, inspector.readLength(initialByte));
        assertEquals(0x01, input.read());
    }

    @Test
    void readLengthConsumesOnlyArgumentBytesForExtendedLength() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { 0x18, 0x20, 0x01 });

        CBORInspector inspector = new CBORInspector(input);

        int initialByte = inspector.read();

        assertEquals(32, inspector.readLength(initialByte));
        assertEquals(0x01, input.read());
        assertEquals(-1, input.read());
    }

    @Test
    void readFailsOnEmptyInput() {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

        CBORInspector inspector = new CBORInspector(input);

        assertThrows(CBORDecodeException.class, inspector::read);
    }

}
