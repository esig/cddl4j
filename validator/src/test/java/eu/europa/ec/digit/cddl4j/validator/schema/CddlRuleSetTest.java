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

import eu.europa.ec.digit.cddl4j.validator.schema.type.CddlType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.PrimitiveType;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CddlRuleSetTest {

    @Test
    void constructorDefensivelyCopiesRuleMaps() {
        Map<String, CddlType> customRules = new LinkedHashMap<>();
        Map<String, CddlType> schemaRules = new LinkedHashMap<>();
        customRules.put("custom", PrimitiveType.TSTR);
        schemaRules.put("root", PrimitiveType.UINT);

        CddlRuleSet ruleSet = new CddlRuleSet(customRules, schemaRules, "root");
        customRules.put("custom", PrimitiveType.UINT);
        schemaRules.put("root", PrimitiveType.TSTR);

        assertSame(PrimitiveType.TSTR, ruleSet.getRule("custom"));
        assertSame(PrimitiveType.UINT, ruleSet.getRule("root"));
        assertSame(PrimitiveType.TSTR, ruleSet.getRules().get("custom"));
        assertSame(PrimitiveType.UINT, ruleSet.getRules().get("root"));
    }

    @Test
    void withCustomRulesMergesNewRules() {
        CddlRuleSet original = new CddlRuleSet(
                singleton("first", PrimitiveType.TSTR), singleton("root", PrimitiveType.UINT), "root");

        CddlRuleSet derived = original.withCustomRules(singleton("second", PrimitiveType.BSTR));

        assertSame(PrimitiveType.TSTR, derived.getRule("first"));
        assertSame(PrimitiveType.BSTR, derived.getRule("second"));
        assertSame(PrimitiveType.TSTR, original.getRule("first"));
        assertNull(original.getRule("second"));
    }

    private static Map<String, CddlType> singleton(String name, CddlType type) {
        Map<String, CddlType> result = new LinkedHashMap<>();
        result.put(name, type);
        return result;
    }

}
