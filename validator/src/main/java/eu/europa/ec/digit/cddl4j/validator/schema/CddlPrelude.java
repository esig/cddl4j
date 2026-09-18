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
package eu.europa.ec.digit.cddl4j.validator.schema;

import eu.europa.ec.digit.cddl4j.validator.schema.type.ArrayType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.CddlType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.ChoiceType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.GroupEntry;
import eu.europa.ec.digit.cddl4j.validator.schema.type.LiteralType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.PrimitiveType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.TaggedType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * RFC 8610 standard prelude (Appendix D).
 *
 * <p>Every compiled {@link CddlSchema} automatically includes these rules so
 * that schema authors can use any Appendix-D name without having to declare it
 * themselves.  User-defined rules with the same name shadow the prelude.
 *
 * <p>Prelude types are built once at class-load time and are shared
 * (all types in this library are immutable).
 */
public final class CddlPrelude {

    /** Singleton prelude, built once at class load. */
    private static final LinkedHashMap<String, CddlType> INSTANCE = build();

    /**
     * Default constructor
     *
     */
    private CddlPrelude() {
        // empty
    }

    /**
     * Returns a mutable copy of the prelude map.
     * Callers (the parser) add user rules on top of this copy.
     *
     * @return a mutable copy of the standard prelude rules
     */
    public static LinkedHashMap<String, CddlType> copy() {
        return new LinkedHashMap<>(INSTANCE);
    }

    // -----------------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------------

    private static LinkedHashMap<String, CddlType> build() {
        LinkedHashMap<String, CddlType> m = new LinkedHashMap<>();

        // ── Primitive types (also handled by PrimitiveType.forName at parse
        //    time; listed here so they are reachable as named rules at
        //    validation time, e.g. schema.validate(cbor, "uint")). ──────────
        m.put("any",        PrimitiveType.ANY);
        m.put("uint",       PrimitiveType.UINT);
        m.put("nint",       PrimitiveType.NINT);
        m.put("int",        PrimitiveType.INT);
        m.put("bstr",       PrimitiveType.BSTR);
        m.put("bytes",      PrimitiveType.BSTR);       // alias
        m.put("tstr",       PrimitiveType.TSTR);
        m.put("text",       PrimitiveType.TSTR);       // alias
        m.put("bool",       PrimitiveType.BOOL);
        m.put("nil",        PrimitiveType.NIL);
        m.put("null",       PrimitiveType.NIL);        // alias
        m.put("undefined",  PrimitiveType.UNDEFINED);
        m.put("float16",    PrimitiveType.FLOAT16);
        m.put("float32",    PrimitiveType.FLOAT32);
        m.put("float64",    PrimitiveType.FLOAT64);
        m.put("float",      PrimitiveType.FLOAT);

        // ── Boolean literals as singleton types (RFC 8610 §2.2) ──────────
        // false = #7.20 (CBOR simple value 20)
        // true  = #7.21 (CBOR simple value 21)
        m.put("false",      LiteralType.ofBool(false));
        m.put("true",       LiteralType.ofBool(true));

        // ── Float sub-ranges ─────────────────────────────────────────────
        // float16-32 = float16 / float32
        m.put("float16-32", choice(PrimitiveType.FLOAT16, PrimitiveType.FLOAT32));
        // float32-64 = float32 / float64
        m.put("float32-64", choice(PrimitiveType.FLOAT32, PrimitiveType.FLOAT64));

        // ── Number super-type ─────────────────────────────────────────────
        // number = uint / nint / float
        m.put("number", choice(PrimitiveType.UINT, PrimitiveType.NINT, PrimitiveType.FLOAT));

        // ── Big integer types (RFC 8949 §3.4.3) ──────────────────────────
        // biguint  = #6.2(bstr)
        CddlType biguint = new TaggedType(2, PrimitiveType.BSTR);
        // bignint  = #6.3(bstr)
        CddlType bignint = new TaggedType(3, PrimitiveType.BSTR);
        // bigint   = biguint / bignint
        CddlType bigint  = choice(biguint, bignint);
        m.put("biguint",  biguint);
        m.put("bignint",  bignint);
        m.put("bigint",   bigint);
        // integer  = int / bigint
        m.put("integer",  choice(PrimitiveType.INT, bigint));
        // unsigned = uint / biguint
        m.put("unsigned", choice(PrimitiveType.UINT, biguint));

        // ── Decimal fraction / bigfloat (RFC 8949 §3.4.4) ────────────────
        // decfrac  = #6.4([int, bigint])
        m.put("decfrac",  new TaggedType(4, twoElemArray(PrimitiveType.INT, bigint)));
        // bigfloat = #6.5([int, bigint])
        m.put("bigfloat", new TaggedType(5, twoElemArray(PrimitiveType.INT, bigint)));

        // ── Date / time tags (RFC 8949 §3.4.1) ───────────────────────────
        // tdate = #6.0(tstr)   — text date-time string (ISO 8601)
        m.put("tdate", new TaggedType(0, PrimitiveType.TSTR));
        // time  = #6.1(uint / float)   — numeric seconds since epoch
        m.put("time",  new TaggedType(1, choice(PrimitiveType.UINT, PrimitiveType.FLOAT)));

        // ── Base64/hex encoding tags (RFC 8949 §3.4.5) ───────────────────
        m.put("eb64url",      new TaggedType(21,    PrimitiveType.ANY));
        m.put("eb64legacy",   new TaggedType(22,    PrimitiveType.ANY));
        m.put("eb16",         new TaggedType(23,    PrimitiveType.ANY));
        // encoded-cbor = #6.24(bstr)
        m.put("encoded-cbor", new TaggedType(24,    PrimitiveType.BSTR));

        // ── URI / string encoding tags (RFC 8949 §3.4.5.3) ───────────────
        m.put("uri",          new TaggedType(32,    PrimitiveType.TSTR));
        m.put("b64url",       new TaggedType(33,    PrimitiveType.TSTR));
        m.put("b64legacy",    new TaggedType(34,    PrimitiveType.TSTR));
        m.put("regexp",       new TaggedType(35,    PrimitiveType.TSTR));
        m.put("mime-message", new TaggedType(36,    PrimitiveType.TSTR));

        // ── Self-described CBOR (RFC 8949 §3.4.6) ────────────────────────
        // cbor-any = #6.55799(any)
        m.put("cbor-any",     new TaggedType(55799, PrimitiveType.ANY));

        return m;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static ChoiceType choice(CddlType... alts) {
        return new ChoiceType(Arrays.asList(alts));
    }

    /**
     * Builds a fixed two-element array type.
     *
     * <p>The resulting CDDL type is {@code [first, second]}, where both elements
     * are required and must occur exactly once.
     *
     * @param first the CDDL type of the first array element
     * @param second the CDDL type of the second array element
     * @return a CDDL array type containing exactly the two specified elements
     */
    private static ArrayType twoElemArray(CddlType first, CddlType second) {
        List<GroupEntry> entries = new ArrayList<>(2);
        entries.add(new GroupEntry(first, 1, 1, null));
        entries.add(new GroupEntry(second, 1, 1, null));
        return new ArrayType(entries);
    }

}
