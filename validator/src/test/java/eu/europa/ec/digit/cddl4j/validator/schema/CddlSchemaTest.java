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

import eu.europa.ec.digit.cddl4j.cbor.CBOR;
import eu.europa.ec.digit.cddl4j.cbor.CBORValue;
import eu.europa.ec.digit.cddl4j.validator.CddlParseException;
import eu.europa.ec.digit.cddl4j.validator.CddlValidator;
import eu.europa.ec.digit.cddl4j.validator.schema.operator.ControlOperator;
import eu.europa.ec.digit.cddl4j.validator.schema.type.CddlType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.PrimitiveType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.ReferenceType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.TaggedType;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationContext;
import eu.europa.ec.digit.cddl4j.validator.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CddlSchemaTest {

    /*
     * CDDL Prelude tests
     */

    @Test
    void customRuleCanBeReferencedBySchemaRule() {
        Map<String, CddlType> customRules = Collections.singletonMap("my-type", PrimitiveType.TSTR);
        CddlSchema schema = CddlValidator.loadSchema("root = my-type")
                .withRules(customRules);

        assertSame(PrimitiveType.TSTR, schema.getRuleSet().getRule("my-type"));
    }


    @Test
    void withRulesPreservesPreviouslyConfiguredCustomRules() {
        CddlSchema schema = CddlValidator.loadSchema("root = first")
                .withRules(Collections.singletonMap("first", PrimitiveType.TSTR))
                .withRules(Collections.singletonMap("second", PrimitiveType.UINT));

        assertSame(PrimitiveType.TSTR, schema.getRuleSet().getRule("first"));
        assertSame(PrimitiveType.UINT, schema.getRuleSet().getRule("second"));
    }

    @Test
    void schemaCustomizationPreservesControlOperators() {
        CddlSchema schema = CddlValidator.loadSchema("root = tstr .custom tstr");

        ControlOperator accept = new ControlOperator() {
            @Override
            public String getName() {
                return ".custom";
            }

            @Override
            public ValidationResult validate(CBORValue value, CddlType controller, ValidationContext ctx) {
                return ValidationResult.success();
            }
        };

        CddlSchema customized = schema.withControlOperators(Collections.singletonList(accept));

        assertTrue(customized.withRootRule("root").validate(CBOR.text("hello")).isValid());
        assertTrue(customized.withRules(Collections.singletonMap("helper", PrimitiveType.TSTR))
                .validate(CBOR.text("hello"))
                .isValid());
    }

    @Test
    void customRuleIsUsedDuringValidation() {
        Map<String, CddlType> customRules = Collections.singletonMap("my-type", PrimitiveType.TSTR);

        CddlSchema schema = CddlValidator.loadSchema("root = my-type")
                .withRules(customRules);

        ValidationResult textResult = schema.validate(CBOR.text("abc"));
        ValidationResult uintResult = schema.validate(CBOR.integer(42));

        assertTrue(textResult.isValid());
        assertFalse(uintResult.isValid());
    }

    @Test
    void schemaRuleOverridesCustomRule() {
        Map<String, CddlType> customRules = Collections.singletonMap("my-type", PrimitiveType.TSTR);
        CddlSchema schema = CddlValidator.loadSchema("my-type = uint\nroot = my-type")
                .withRules(customRules);

        assertFalse(schema.validate(CBOR.text("abc")).isValid());
        assertTrue(schema.validate(CBOR.integer(42)).isValid());

        assertNotSame(PrimitiveType.UINT, schema.getRuleSet().getRule("my-type"));
        assertEquals("uint", ((ReferenceType) schema.getRuleSet().getRule("my-type")).getRuleName());
    }

    @Test
    void customRuleOverridesPreludeRule() {
        Map<String, CddlType> customRules = Collections.singletonMap("uint", PrimitiveType.TSTR);
        CddlSchema schema = CddlValidator.loadSchema("root = uint")
                .withRules(customRules);
        assertSame(PrimitiveType.TSTR, schema.getRuleSet().getRule("uint"));
    }

    @Test
    void withRulesIsImmutable() {
        Map<String, CddlType> customRules = Collections.singletonMap("my-type", PrimitiveType.TSTR);

        CddlSchema original = CddlValidator.loadSchema("root = uint");
        CddlSchema derived = original
                .withRules(customRules);

        assertSame(PrimitiveType.UINT, original.getRuleSet().getRule("uint"));
        assertSame(PrimitiveType.TSTR, derived.getRuleSet().getRule("my-type"));
    }

    @Test
    void customTaggedRuleValidatesTaggedCBORValue() {
        Map<String, CddlType> customRules = Collections.singletonMap("my-uri", new TaggedType(32, PrimitiveType.TSTR));

        CddlSchema schema = CddlValidator.loadSchema("root = my-uri")
                .withRules(customRules);

        CBORValue valid = CBOR.tag(32, CBOR.text("example"));
        CBORValue invalid = CBOR.text("example");

        assertTrue(schema.validate(valid).isValid());
        assertFalse(schema.validate(invalid).isValid());
    }

    @Test
    void customRuleOverridesNonPrimitivePreludeRule() {
        Map<String, CddlType> customRules = Collections.singletonMap("uri", PrimitiveType.TSTR);

        CddlSchema schema = CddlValidator.loadSchema("root = uri")
                .withRules(customRules);

        CBORValue plainText = CBOR.text("example");
        CBORValue taggedText = CBOR.tag(32, CBOR.text("example"));

        assertTrue(schema.validate(plainText).isValid());
        assertFalse(schema.validate(taggedText).isValid());
    }

    @Test
    void customTaggedRuleOverridesTaggedPreludeRule() {
        Map<String, CddlType> customRules = Collections.singletonMap("biguint", new TaggedType(1000, PrimitiveType.BSTR));

        CddlSchema schema = CddlValidator.loadSchema("root = biguint")
                .withRules(customRules);

        CBORValue customValue = CBOR.tag(1000, CBOR.bytes(new byte[] { 0x01, 0x02 }));
        CBORValue preludeValue = CBOR.tag(2, CBOR.bytes(new byte[] { 0x01, 0x02 }));

        assertTrue(schema.validate(customValue).isValid());
        assertFalse(schema.validate(preludeValue).isValid());
    }

    @Test
    void customRuleOverrideDoesNotMutateOriginalSchema() {
        CddlSchema original = CddlValidator.loadSchema("root = uri");
        CddlSchema customized = original.withRules(Collections.singletonMap("uri", PrimitiveType.TSTR));

        CBORValue plainText = CBOR.text("example");
        CBORValue taggedText = CBOR.tag(32, CBOR.text("example"));

        assertFalse(original.validate(plainText).isValid());
        assertTrue(original.validate(taggedText).isValid());

        assertTrue(customized.validate(plainText).isValid());
        assertFalse(customized.validate(taggedText).isValid());
    }

    /*
     * Control operators tests
     */

    @Test
    void controlOperatorCanBeConfiguredAfterParsing() {
        CddlSchema schema = CddlValidator.loadSchema("root = tstr .custom tstr");

        ControlOperator accept = new ControlOperator() {
            @Override
            public String getName() {
                return ".custom";
            }
            @Override
            public ValidationResult validate(CBORValue value, CddlType controller, ValidationContext ctx) {
                return ValidationResult.success();
            }
        };

        CddlSchema configured = schema.withControlOperators(Collections.singletonList(accept));
        assertTrue(configured.validate(CBOR.text("hello")).isValid());
    }

    @Test
    void unknownControlOperatorFailsDuringValidation() {
        CddlSchema schema = CddlValidator.loadSchema("root = tstr .custom tstr");

        Exception exception = assertThrows(IllegalStateException.class, () -> schema.validate(CBOR.text("hello")));
        assertEquals("No control operator registered for '.custom'", exception.getMessage());
    }

    @Test
    void controlOperatorConfigurationIsImmutable() {
        CddlSchema schema = CddlValidator.loadSchema("root = tstr .custom tstr");

        ControlOperator accept = new ControlOperator() {
            @Override
            public String getName() {
                return ".custom";
            }
            @Override
            public ValidationResult validate(CBORValue value, CddlType controller, ValidationContext ctx) {
                return ValidationResult.success();
            }
        };

        CddlSchema configured = schema.withControlOperators(Collections.singletonList(accept));

        Exception exception = assertThrows(IllegalStateException.class, () -> schema.validate(CBOR.text("hello")));
        assertEquals("No control operator registered for '.custom'", exception.getMessage());
        assertTrue(configured.validate(CBOR.text("hello")).isValid());
    }

    /*
     * .size control operator tests
     */

    @Test
    void sizeControlValidatesByteStringLength() {
        CddlSchema schema = CddlValidator.loadSchema("root = bstr .size 4");

        assertTrue(schema.validate(CBOR.bytes(new byte[4])).isValid());
        assertFalse(schema.validate(CBOR.bytes(new byte[3])).isValid());
        assertFalse(schema.validate(CBOR.bytes(new byte[5])).isValid());
    }

    @Test
    void sizeControlValidatesTextStringByteLength() {
        CddlSchema schema = CddlValidator.loadSchema("root = tstr .size 4");

        assertTrue(schema.validate(CBOR.text("test")).isValid());
        assertTrue(schema.validate(CBOR.text("éé")).isValid());
        assertFalse(schema.validate(CBOR.text("é")).isValid());
        assertFalse(schema.validate(CBOR.text("tests")).isValid());
    }

    @Test
    void sizeControlSupportsRangeController() {
        CddlSchema schema = CddlValidator.loadSchema("root = bstr .size (1..63)");

        assertTrue(schema.validate(CBOR.bytes(new byte[1])).isValid());
        assertTrue(schema.validate(CBOR.bytes(new byte[63])).isValid());
        assertFalse(schema.validate(CBOR.bytes(new byte[0])).isValid());
        assertFalse(schema.validate(CBOR.bytes(new byte[64])).isValid());
    }

    @Test
    void sizeControlValidatesUnsignedIntegerMaximumSize() {
        CddlSchema schema = CddlValidator.loadSchema("root = uint .size 3");

        assertTrue(schema.validate(CBOR.integer(0)).isValid());
        assertTrue(schema.validate(CBOR.integer(255)).isValid());
        assertTrue(schema.validate(CBOR.integer(256)).isValid());
        assertTrue(schema.validate(CBOR.integer(16777215)).isValid());
        assertFalse(schema.validate(CBOR.integer(16777216)).isValid());
    }

    @Test
    void sizeControlWithZeroBytesOnlyAcceptsZero() {
        CddlSchema schema = CddlValidator.loadSchema("root = uint .size 0");

        assertTrue(schema.validate(CBOR.integer(0)).isValid());
        assertFalse(schema.validate(CBOR.integer(1)).isValid());
    }

    @Test
    void sizeControlSupportsReferencedIntegerController() {
        CddlSchema schema = CddlValidator.loadSchema(
                "root = uint .size max-size\n"
                        + "max-size = 3");

        assertTrue(schema.validate(CBOR.integer(16777215)).isValid());
        assertFalse(schema.validate(CBOR.integer(16777216)).isValid());
    }

    @Test
    void rfc8610LabelSizeRangeIsSupported() {
        CddlSchema schema = CddlValidator.loadSchema(
                "label = bstr .size (1..63)");

        assertTrue(schema.validate(CBOR.bytes(new byte[1])).isValid());
        assertTrue(schema.validate(CBOR.bytes(new byte[63])).isValid());

        assertFalse(schema.validate(CBOR.bytes(new byte[0])).isValid());
        assertFalse(schema.validate(CBOR.bytes(new byte[64])).isValid());
    }

    @Test
    void rfc8610FixedByteStringSizeIsSupported() {
        CddlSchema ip4 = CddlValidator.loadSchema(
                "ip4 = bstr .size 4");

        assertTrue(ip4.validate(CBOR.bytes(new byte[4])).isValid());
        assertFalse(ip4.validate(CBOR.bytes(new byte[3])).isValid());
        assertFalse(ip4.validate(CBOR.bytes(new byte[5])).isValid());

        CddlSchema ip6 = CddlValidator.loadSchema(
                "ip6 = bstr .size 16");

        assertTrue(ip6.validate(CBOR.bytes(new byte[16])).isValid());
        assertFalse(ip6.validate(CBOR.bytes(new byte[15])).isValid());
        assertFalse(ip6.validate(CBOR.bytes(new byte[17])).isValid());
    }

    @Test
    void rfc8610UnsignedIntegerSizeIsSupported() {
        CddlSchema schema = CddlValidator.loadSchema(
                "audio_sample = uint .size 3");

        assertTrue(schema.validate(CBOR.integer(0)).isValid());
        assertTrue(schema.validate(CBOR.integer(16_777_215)).isValid());

        assertFalse(schema.validate(CBOR.integer(16_777_216)).isValid());
        assertFalse(schema.validate(CBOR.integer(-1)).isValid());
    }

    @Test
    void rfc8610SizeFullAddressExample() {
        CddlSchema schema = CddlValidator.loadSchema(
                "full-address = [[+ label], ip4, ip6]\n"
                        + "ip4 = bstr .size 4\n"
                        + "ip6 = bstr .size 16\n"
                        + "label = bstr .size (1..63)");

        CBORValue noLabels = CBOR.array()
                .add(CBOR.array().value())
                .add(CBOR.bytes(new byte[4]))
                .add(CBOR.bytes(new byte[16]))
                .value();

        CBORValue withLabel = CBOR.array()
                .add(CBOR.array()
                        .add(CBOR.bytes(new byte[] {1}))
                        .value())
                .add(CBOR.bytes(new byte[4]))
                .add(CBOR.bytes(new byte[16]))
                .value();

        assertFalse(schema.validate(noLabels).isValid());
        assertTrue(schema.validate(withLabel).isValid());
    }

    @Test
    void repeatedValidationDoesNotReusePreviousResult() {
        CddlSchema schema = CddlValidator.loadSchema(
                "person = { id: id }\n"
                        + "id = uint");

        CBORValue valid = CBOR.map()
                .put(CBOR.value("id"), CBOR.integer(1))
                .value();

        CBORValue invalid = CBOR.map()
                .put(CBOR.value("id"), CBOR.text("bad"))
                .value();

        assertTrue(schema.validate(valid).isValid());
        assertFalse(schema.validate(invalid).isValid());
        assertTrue(schema.validate(valid).isValid());
    }

    @Test
    void schemaRemainsUsableAfterComplexValidationFailure() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = item / tstr\n"
                        + "item = { id: uint }");

        CBORValue invalidItem = CBOR.map()
                .put(CBOR.value("id"), CBOR.text("bad"))
                .value();

        CBORValue validText = CBOR.text("fallback");

        ValidationResult first = schema.validate(invalidItem);
        ValidationResult second = schema.validate(validText);
        assertFalse(first.isValid());
        assertTrue(second.isValid());
    }

    @Test
    void separateSchemasDoNotShareState() {
        CddlSchema uintSchema = CddlValidator.loadSchema(
                "value = uint");

        CddlSchema textSchema = CddlValidator.loadSchema(
                "value = tstr");

        assertTrue(uintSchema.validate(CBOR.integer(1)).isValid());
        assertTrue(textSchema.validate(CBOR.text("one")).isValid());

        assertFalse(uintSchema.validate(CBOR.text("one")).isValid());
        assertFalse(textSchema.validate(CBOR.integer(1)).isValid());
    }

    @Test
    void validatingSameValueTwiceProducesEquivalentResults() {
        CddlSchema schema = CddlValidator.loadSchema(
                "value = uint");

        CBORValue value = CBOR.text("bad");

        ValidationResult first = schema.validate(value);
        ValidationResult second = schema.validate(value);
        assertEquals(first.toString(), second.toString());
    }

    @Test
    void emptySchemaIsRejected() {
        assertThrows(CddlParseException.class, () -> CddlValidator.loadSchema("  \n "));
    }

}
