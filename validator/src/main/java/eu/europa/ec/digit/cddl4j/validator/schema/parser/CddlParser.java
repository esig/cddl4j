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
package eu.europa.ec.digit.cddl4j.validator.schema.parser;

import eu.europa.ec.digit.cddl4j.validator.CddlParseException;
import eu.europa.ec.digit.cddl4j.validator.schema.CddlRuleSet;
import eu.europa.ec.digit.cddl4j.validator.schema.CddlSchema;
import eu.europa.ec.digit.cddl4j.validator.schema.type.ArrayType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.CddlType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.ChoiceType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.ControlType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.GroupChoiceType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.GroupEntry;
import eu.europa.ec.digit.cddl4j.validator.schema.type.GroupEnumerationType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.GroupType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.LiteralType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.MapEntry;
import eu.europa.ec.digit.cddl4j.validator.schema.type.MapType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.OccurrenceType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.PrimitiveType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.RangeType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.ReferenceType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.TaggedType;
import eu.europa.ec.digit.cddl4j.validator.schema.type.UnwrapType;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Recursive-descent parser for CDDL (RFC 8610).
 *
 * <p>The parser is intentionally stateful and is not thread-safe; create one
 * parser per concurrent parse operation. The resulting {@link CddlSchema} is
 * immutable and safe to share.
 *
 * <p>Supported constructs:
 * <ul>
 *   <li>Rule definitions: {@code name = type}</li>
 *   <li>Additive rule updates: {@code name /= type} (union into existing rule)</li>
 *   <li>Primitive prelude types: {@code uint int nint float float16/32/64 bool tstr bstr nil null any}</li>
 *   <li>Boolean literals: {@code true false}</li>
 *   <li>Integer and float literals</li>
 *   <li>Text string literals: {@code "hello"}</li>
 *   <li>Byte string literals: {@code h'deadbeef'} and {@code b64'...'}</li>
 *   <li>Type choice: {@code a / b / c}</li>
 *   <li>Range operators: {@code 0..255} (inclusive) and {@code 0...256} (exclusive upper)</li>
 *   <li>Grouped types: {@code (type)}</li>
 *   <li>Array types: {@code [entry, entry]}</li>
 *   <li>Map types: {@code {key: value, key => value}}</li>
 *   <li>Occurrence: {@code ? * + n*m}</li>
 *   <li>CBOR tagged: {@code #6.n(type)} and {@code #6(type)}</li>
 *   <li>Major type selectors: {@code #0} – {@code #7}</li>
 *   <li>Named rule references (forward references resolved at validation time)</li>
 *   <li>Group choices: {@code //} and additive group rules {@code //=}</li>
 *   <li>Unwrap operator in arrays/maps: {@code ~}</li>
 *   <li>Control operators: {@code .size}, {@code .cbor}, and {@code .cborseq}</li>
 * </ul>
 */
public final class CddlParser {

    /** List of tokens, each token representing a character or symbol extracted from the source data */
    private List<Token> tokens;

    /** Current position of the parser */
    private int pos;

    /**
     * Creates a parser for CDDL source text.
     */
    public CddlParser() {
        // Source state is initialized for each parse operation.
    }

    /**
     * Parses and compiles a CDDL source string.
     *
     * @param source the CDDL source
     * @return the compiled schema
     * @throws CddlParseException if the source is invalid
     */
    public CddlSchema parse(String source) {
        this.tokens = new CddlTokenizer(source).tokenize();
        return compileCddlSchema();
    }

    /**
     * Parses and compiles UTF-8 encoded CDDL source.
     *
     * @param source the UTF-8 encoded CDDL source
     * @return the compiled schema
     * @throws CddlParseException if the source is invalid
     */
    public CddlSchema parse(byte[] source) {
        this.tokens = new CddlTokenizer(source).tokenize();
        return compileCddlSchema();
    }

    /**
     * Parses and compiles UTF-8 encoded CDDL source from an input stream.
     *
     * <p>The input stream is consumed by the tokenizer and is not closed by
     * this method. Ownership of the input stream remains with the caller.
     *
     * @param source the input stream containing UTF-8 encoded CDDL source
     * @return the compiled schema
     * @throws CddlParseException if the source is invalid or cannot be read
     */
    public CddlSchema parse(InputStream source) {
        this.tokens = new CddlTokenizer(source).tokenize();
        return compileCddlSchema();
    }

    /**
     * Compiles the tokenized CDDL source into a schema.
     *
     * @return the compiled schema
     * @throws CddlParseException if the tokenized source is invalid
     */
    private CddlSchema compileCddlSchema() {
        this.pos = 0;

        LinkedHashMap<String, CddlType> schemaRules = new LinkedHashMap<>();
        parseRules(schemaRules);
        if (schemaRules.isEmpty()) {
            throw new CddlParseException("CDDL schema must contain at least one rule");
        }

        String rootRuleName = schemaRules.keySet().iterator().next();

        CddlRuleSet ruleSet = new CddlRuleSet(schemaRules, rootRuleName);
        return new CddlSchema(ruleSet);
    }

    // ------------------------------------------------------------------
    // Rules
    // ------------------------------------------------------------------

    private void parseRules(LinkedHashMap<String, CddlType> rules) {
        while (!peek().is(TokenType.EOF)) {
            try {
                // Skip stray commas or newline artifacts
                if (peek().is(TokenType.COMMA)) {
                    advance(); continue;
                }

                Token nameTok = expect(TokenType.IDENT);
                String name = nameTok.getValue();

                // Assignment token: =  /=  //=
                boolean additive = false;
                boolean groupAdditive = false;
                if (peek().is(TokenType.ASSIGN)) {
                    advance();
                } else if (peek().is(TokenType.ASSIGN_ADDITIVE)) {
                    advance();
                    additive = true;
                } else if (peek().is(TokenType.ASSIGN_GROUP_ADDITIVE)) {
                    advance();
                    additive = true;
                    groupAdditive = true;
                } else {
                    throw new CddlParseException("Expected '=' after rule name '" + name + "'",
                            nameTok.getLine(), nameTok.getColumn());
                }

                CddlType type = parseType();

                if (additive) {
                    CddlType existing = rules.get(name);
                    rules.put(name, groupAdditive ? mergeGroupRule(existing, type) : mergeTypeRule(existing, type));
                } else {
                    if (rules.containsKey(name)) {
                        throw new CddlParseException("Rule '" + name + "' is already defined",
                                nameTok.getLine(), nameTok.getColumn());
                    }
                    rules.put(name, type);
                }

                // Optional trailing comma between rules (non-standard but accepted)
                if (peek().is(TokenType.COMMA)) {
                    advance();
                }

            } catch (CddlParseException e) {
                throw e;
            } catch (Exception e) {
                throw new CddlParseException("Parse error : " + e.getMessage(), e);
            }
        }
    }

    /** Merges alternatives of a type rule using the {@code /=} operator. */
    private CddlType mergeTypeRule(CddlType existing, CddlType additional) {
        List<CddlType> alternatives = new ArrayList<>();
        if (existing instanceof ChoiceType) {
            alternatives.addAll(((ChoiceType) existing).getAlternatives());
        } else if (existing != null) {
            alternatives.add(existing);
        }
        if (additional instanceof ChoiceType) {
            alternatives.addAll(((ChoiceType) additional).getAlternatives());
        } else {
            alternatives.add(additional);
        }
        return new ChoiceType(alternatives);
    }

    /** Merges alternatives of a group rule using the {@code //=} operator. */
    private CddlType mergeGroupRule(CddlType existing, CddlType additional) {
        List<GroupType> alternatives = new ArrayList<>();
        addGroupAlternatives(existing, alternatives);
        addGroupAlternatives(additional, alternatives);
        if (alternatives.isEmpty()) {
            throw new CddlParseException("Group additive rule must define at least one group alternative");
        }
        return alternatives.size() == 1 ? alternatives.get(0) : new GroupChoiceType(alternatives);
    }

    /** Adds the group alternatives represented by the supplied type. */
    private void addGroupAlternatives(CddlType type, List<GroupType> alternatives) {
        if (type instanceof GroupType) {
            alternatives.add((GroupType) type);
        } else if (type instanceof GroupChoiceType) {
            alternatives.addAll(((GroupChoiceType) type).getAlternatives());
        } else if (type != null) {
            throw new CddlParseException("Rule is not a CDDL group: " + type);
        }
    }

    // ------------------------------------------------------------------
    // Type grammar
    // ------------------------------------------------------------------

    /**
     * type = type1 ("/" type1)*
     */
    private CddlType parseType() {
        CddlType first = parseType1();
        if (!peek().is(TokenType.SLASH)) {
            return first;
        }

        List<CddlType> alts = new ArrayList<>();
        alts.add(first);
        while (peek().is(TokenType.SLASH)) {
            advance(); // consume "/"
            alts.add(parseType1());
        }
        return new ChoiceType(alts);
    }
    /**
     * type1 = type2 [rangeop type2] [ctlop type2]
     *
     * @return parsed type
     */
    private CddlType parseType1() {
        CddlType left = parseType2();

        if (peek().is(TokenType.RANGE)) {
            advance();
            CddlType right = parseType2();
            return new RangeType(left, right, false);
        }

        if (peek().is(TokenType.RANGE_EXCL)) {
            advance();
            CddlType right = parseType2();
            return new RangeType(left, right, true);
        }

        if (peek().is(TokenType.CTRL_OP)) {
            Token operator = advance();
            CddlType controller = parseType2();
            return new ControlType(left, operator.getValue(), controller);
        }

        return left;
    }

    /**
     * type2 = value / typename / "(" type ")" / "{" group "}" / "[" group "]"
     *       / "~" typename / "&" "(" group ")" / "&" groupname
     *       / "#6" ["." uint] "(" type ")" / "#" DIGIT
     */
    private CddlType parseType2() {
        Token tok = peek();

        switch (tok.getType()) {
            case NUMBER_INT:
                advance();
                return LiteralType.ofInt(parseInteger(tok));

            case NUMBER_FLOAT:
                advance();
                return LiteralType.ofFloat(Double.parseDouble(tok.getValue()));

            case TEXT:
                advance();
                return LiteralType.ofText(tok.getValue());

            case BYTES_HEX:
                advance();
                return LiteralType.ofBytes(hexToBytes(tok.getValue(), tok));

            case BYTES_B64:
                advance();
                return LiteralType.ofBytes(Base64.getDecoder().decode(tok.getValue()));

            case IDENT: {
                advance();
                String name = tok.getValue();
                // Boolean literals
                if ("true".equals(name)) {
                    return LiteralType.ofBool(true);
                }
                if ("false".equals(name)) {
                    return LiteralType.ofBool(false);
                }
                // Named rule reference
                return new ReferenceType(name);
            }

            case LPAREN:
                return parseParenthesized();

            case LBRACE:
                return parseMapType();

            case LBRACKET:
                return parseArrayType();

            case HASH:
                return parseHashType();

            case AMPERSAND:
                return parseGroupEnumerationType();

            case TILDE:
                return parseWrappedType();

            default:
                throw new CddlParseException("Unexpected token " + tok + " — expected a type",
                        tok.getLine(), tok.getColumn());
        }
    }

    // ------------------------------------------------------------------
    // Group grammar
    // ------------------------------------------------------------------

    /**
     * Parses a parenthesized type or group.
     *
     * <p>Parentheses are ambiguous in CDDL: they can group a type expression,
     * such as {@code (uint / tstr)}, or define a group, such as
     * {@code (foo: uint, bar: tstr)}. The contents are inspected at the current
     * parenthesis nesting level to distinguish the two forms.
     *
     * @return parsed type or group
     */
    private CddlType parseParenthesized() {
        if (isParenthesizedGroup()) {
            return parseGroup();
        }

        expect(TokenType.LPAREN);
        CddlType inner = parseType();
        expect(TokenType.RPAREN);
        return inner;
    }

    /**
     * Determines whether the current parenthesized construct represents a group.
     *
     * <p>A top-level comma, member-key separator, or occurrence indicator makes
     * the construct a group. Nested arrays, maps, and parentheses are ignored
     * while looking for these tokens.
     *
     * @return {@code true} when the current parenthesized construct is a group
     */
    private boolean isParenthesizedGroup() {
        if (!peek().is(TokenType.LPAREN)) {
            return false;
        }

        int parenDepth = 0;
        int braceDepth = 0;
        int bracketDepth = 0;

        for (int index = pos; index < tokens.size(); index++) {
            TokenType type = tokens.get(index).getType();

            switch (type) {
                case LPAREN:
                    parenDepth++;
                    break;

                case RPAREN:
                    parenDepth--;
                    if (parenDepth == 0) {
                        return false;
                    }
                    break;

                case LBRACE:
                    braceDepth++;
                    break;

                case RBRACE:
                    braceDepth--;
                    break;

                case LBRACKET:
                    bracketDepth++;
                    break;

                case RBRACKET:
                    bracketDepth--;
                    break;

                default:
                    if (parenDepth == 1 && braceDepth == 0 && bracketDepth == 0) {
                        if (type == TokenType.COMMA
                                || type == TokenType.COLON
                                || type == TokenType.FAT_ARROW
                                || type == TokenType.QUESTION
                                || type == TokenType.STAR
                                || type == TokenType.PLUS
                                || type == TokenType.DOUBLE_SLASH) {
                            return true;
                        }

                        if (type == TokenType.NUMBER_INT && index + 1 < tokens.size()
                                && tokens.get(index + 1).is(TokenType.STAR)) {
                            return true;
                        }
                    }
            }
        }

        return false;
    }

    /**
     * Parses a parenthesized CDDL group.
     *
     * @return parsed group
     */
    private CddlType parseGroup() {
        expect(TokenType.LPAREN);
        List<GroupType> alternatives = new ArrayList<>();
        alternatives.add(new GroupType(parseGroupEntries()));

        while (peek().is(TokenType.DOUBLE_SLASH)) {
            advance();
            alternatives.add(new GroupType(parseGroupEntries()));
        }

        expect(TokenType.RPAREN);
        return alternatives.size() == 1 ? alternatives.get(0) : new GroupChoiceType(alternatives);
    }

    /**
     * Parses one group-choice alternative until the closing parenthesis or a group choice separator.
     *
     * @return parsed group entries
     */
    private List<GroupEntry> parseGroupEntries() {
        List<GroupEntry> entries = new ArrayList<>();

        while (!peek().is(TokenType.RPAREN) && !peek().is(TokenType.EOF)
                && !peek().is(TokenType.DOUBLE_SLASH)) {
            if (peek().is(TokenType.COMMA)) {
                advance();
                continue;
            }

            entries.add(parseGroupEntry());

            if (peek().is(TokenType.COMMA)) {
                advance();
            }
        }

        return entries;
    }

    /**
     * Parses a single CDDL group entry.
     *
     * @return parsed group entry
     */
    private GroupEntry parseGroupEntry() {
        int[] occurrence = parseOccurrence();
        int min = occurrence[0];
        int max = occurrence[1];

        if (peek().is(TokenType.LPAREN)) {
            return GroupEntry.value(parseGroup(), min, max);
        }

        if (peek().is(TokenType.IDENT) && peekAt(1).is(TokenType.COLON)) {
            String key = advance().getValue();
            advance();
            return GroupEntry.bareWord(key, parseType(), min, max, true);
        }

        if (peek().is(TokenType.TEXT) && peekAt(1).is(TokenType.COLON)) {
            String key = advance().getValue();
            advance();
            return GroupEntry.bareWord(key, parseType(), min, max, true);
        }

        CddlType keyType = parseType1();

        boolean cut = false;
        if (peek().is(TokenType.CARET)) {
            advance();
            cut = true;
        }

        if (peek().is(TokenType.FAT_ARROW)) {
            advance();
            return GroupEntry.typed(keyType, parseType(), min, max, cut);
        }

        if (peek().is(TokenType.COLON)) {
            advance();
            return GroupEntry.typed(keyType, parseType(), min, max, true);
        }

        return GroupEntry.value(keyType, min, max);
    }

    // ------------------------------------------------------------------
    // Map grammar  {group}
    // ------------------------------------------------------------------

    private CddlType parseMapType() {
        expect(TokenType.LBRACE);
        // Support group choice (///) by building a list of entry lists
        List<List<MapEntry>> groups = parseMapGroupChoices();
        expect(TokenType.RBRACE);

        if (groups.size() == 1) {
            return new MapType(groups.get(0));
        }
        // Multiple group choices → union of map types
        List<CddlType> alts = new ArrayList<>();
        for (List<MapEntry> g : groups) {
            alts.add(new MapType(g));
        }
        return new ChoiceType(alts);
    }

    private List<List<MapEntry>> parseMapGroupChoices() {
        List<List<MapEntry>> groups = new ArrayList<>();
        groups.add(parseMapGroup());

        while (peek().is(TokenType.DOUBLE_SLASH)) {
            advance();
            groups.add(parseMapGroup());
        }
        return groups;
    }

    private List<MapEntry> parseMapGroup() {
        List<MapEntry> entries = new ArrayList<>();

        while (!peek().is(TokenType.RBRACE)
                && !peek().is(TokenType.RPAREN)
                && !peek().is(TokenType.EOF)
                && !peek().is(TokenType.DOUBLE_SLASH)) {

            if (peek().is(TokenType.COMMA)) {
                advance();
                continue;
            }

            if (peek().is(TokenType.LPAREN)) {
                advance();
                List<MapEntry> inner = parseMapGroup();
                entries.addAll(inner);
                expect(TokenType.RPAREN);
                continue;
            }

            if (peek().is(TokenType.TILDE)) {
                advance();
                String ruleName = expect(TokenType.IDENT).getValue();
                entries.add(MapEntry.unwrappedReference(ruleName));
                continue;
            }

            int[] occurrence = parseOccurrence();
            if (isGroupReference()) {
                String groupName = advance().getValue();
                entries.add(MapEntry.groupReference(groupName, occurrence[0], occurrence[1]));
                if (peek().is(TokenType.COMMA)) {
                    advance();
                }
                continue;
            }

            entries.add(parseMapEntry(occurrence[0], occurrence[1]));
            if (peek().is(TokenType.COMMA)) {
                advance();
            }
        }

        return entries;
    }

    /**
     * Determines whether the current token is a standalone named-group reference.
     *
     * <p>An identifier followed by a group boundary, rather than {@code :} or
     * {@code =>}, is interpreted as a reference to another CDDL group.
     *
     * @return {@code true} when the current token starts a group reference
     */
    private boolean isGroupReference() {
        if (!peek().is(TokenType.IDENT)) {
            return false;
        }

        TokenType next = peekAt(1).getType();
        return next == TokenType.COMMA
                || next == TokenType.RBRACE
                || next == TokenType.DOUBLE_SLASH;
    }

    /**
     * mapentry = [occur] (bareword ":" / type1 "=>" / value ":") type
     */
    private MapEntry parseMapEntry() {
        int[] occur = parseOccurrence();
        return parseMapEntry(occur[0], occur[1]);
    }

    /**
     * Parses a map entry after its occurrence indicator has already been consumed.
     *
     * @param min minimum occurrence
     * @param max maximum occurrence
     * @return parsed map entry
     */
    private MapEntry parseMapEntry(int min, int max) {

        // Check for bare-word key: IDENT ":"
        if (peek().is(TokenType.IDENT) && peekAt(1).is(TokenType.COLON)) {
            String key = advance().getValue();
            advance(); // ":"
            CddlType valueType = parseType();
            return MapEntry.bareWord(key, valueType, min, max);
        }

        // Check for text-literal key: "text" ":"
        if (peek().is(TokenType.TEXT) && peekAt(1).is(TokenType.COLON)) {
            String key = advance().getValue(); // the literal text
            advance(); // ":"
            CddlType valueType = parseType();
            return MapEntry.bareWord(key, valueType, min, max);
        }

        // Otherwise: type key => value  (or legacy bareword without ":")
        // Parse the key type
        CddlType keyType = parseType1();

        boolean cut = false;
        if (peek().is(TokenType.CARET)) {
            advance();
            cut = true;
        }

        if (peek().is(TokenType.FAT_ARROW)) {
            advance();
            CddlType valueType = parseType();
            return MapEntry.typed(keyType, valueType, min, max, cut);
        }

        if (peek().is(TokenType.COLON)) {
            advance();
            CddlType valueType = parseType();
            // Treat as bare-word if keyType is a reference
            if (keyType instanceof ReferenceType) {
                return MapEntry.bareWord(((ReferenceType) keyType).getRuleName(), valueType, min, max);
            }
            return MapEntry.typed(keyType, valueType, min, max, true);
        }

        throw new CddlParseException("Expected '=>' or ':' after map key type",
                peek().getLine(), peek().getColumn());
    }

    // ------------------------------------------------------------------
    // Array grammar  [group]
    // ------------------------------------------------------------------

    private CddlType parseArrayType() {
        expect(TokenType.LBRACKET);
        List<List<GroupEntry>> groups = parseArrayGroupChoices();
        expect(TokenType.RBRACKET);

        if (groups.size() == 1) {
            return new ArrayType(groups.get(0));
        }
        List<CddlType> alts = new ArrayList<>();
        for (List<GroupEntry> g : groups) {
            alts.add(new ArrayType(g));
        }
        return new ChoiceType(alts);
    }

    private List<List<GroupEntry>> parseArrayGroupChoices() {
        List<List<GroupEntry>> groups = new ArrayList<>();
        groups.add(parseArrayGroup());

        while (peek().is(TokenType.DOUBLE_SLASH)) {
            advance();
            groups.add(parseArrayGroup());
        }
        return groups;
    }

    private List<GroupEntry> parseArrayGroup() {
        List<GroupEntry> entries = new ArrayList<>();

        while (!peek().is(TokenType.RBRACKET)
                && !peek().is(TokenType.RPAREN)
                && !peek().is(TokenType.EOF)
                && !peek().is(TokenType.DOUBLE_SLASH)) {

            if (peek().is(TokenType.COMMA)) {
                advance();
                continue;
            }

            if (peek().is(TokenType.LPAREN)) {
                advance();
                List<GroupEntry> inner = parseArrayGroup();
                entries.addAll(inner);
                if (peek().is(TokenType.RPAREN)) advance();
                continue;
            }

            GroupEntry entry = parseArrayEntry();
            entries.add(entry);
            if (peek().is(TokenType.COMMA)) {
                advance();
            }
        }

        return entries;
    }

    /**
     * arrayentry = [occur] [label ":"] type
     */
    private GroupEntry parseArrayEntry() {
        int[] occur = parseOccurrence();
        int min = occur[0];
        int max = occur[1];

        if (peek().is(TokenType.TILDE)) {
            advance();
            String ruleName = expect(TokenType.IDENT).getValue();
            if (min != 1 || max != 1) {
                throw new CddlParseException(
                        "Occurrence modifiers are not supported for unwrapped rule '" + ruleName + "'");
            }
            return GroupEntry.unwrappedReference(ruleName);
        }

        // Labelled entry: IDENT ":" type  (label is a comment only, not validated)
        String label = null;
        if (peek().is(TokenType.IDENT) && peekAt(1).is(TokenType.COLON)) {
            label = advance().getValue();
            advance(); // ":"
        }

        CddlType entryType = parseType();
        return new GroupEntry(entryType, min, max, label);
    }

    // ------------------------------------------------------------------
    // Hash / tagged / major-type
    // ------------------------------------------------------------------

    private CddlType parseHashType() {
        expect(TokenType.HASH);

        // Bare "#" means any CBOR value.
        if (isHashTerminator(peek().getType())) {
            return PrimitiveType.ANY;
        }

        // #6.1(type) may be tokenized as NUMBER_FLOAT("6.1").
        if (peek().is(TokenType.NUMBER_FLOAT)) {
            Token number = advance();
            String value = number.getValue();

            int dotIndex = value.indexOf('.');
            if (dotIndex <= 0 || dotIndex == value.length() - 1) {
                return PrimitiveType.ANY;
            }

            int majorType;
            BigInteger argument;

            try {
                majorType = Integer.parseInt(value.substring(0, dotIndex));
            } catch (NumberFormatException exception) {
                throw new CddlParseException("Invalid CBOR major-type selector: " + value,
                        number.getLine(), number.getColumn());
            }

            String argumentValue = value.substring(dotIndex + 1);
            // The lexer reads #6.0xNN as NUMBER_FLOAT("6.0") followed by IDENT("xNN").
            if (majorType == 6 && "0".equals(argumentValue) && peek().is(TokenType.IDENT)
                    && areAdjacent(number, peek()) && isRadixPrefix(peek().getValue())) {
                argumentValue += advance().getValue();
            }
            argument = parseCborTagNumber(argumentValue, number);

            return parseTaggedContent(majorType, argument);
        }

        // #6(type), #6.32(type), or a major-type selector.
        if (peek().is(TokenType.NUMBER_INT)) {
            int majorType = Integer.parseInt(advance().getValue());

            if (majorType == 6) {
                BigInteger tagNumber = TaggedType.ANY_TAG;

                if (peek().is(TokenType.CTRL_OP)) {
                    Token suffixToken = advance();
                    String suffix = suffixToken.getValue();

                    if (!suffix.startsWith(".") || suffix.length() == 1) {
                        throw new CddlParseException("Invalid CBOR tag number: " + suffix,
                                suffixToken.getLine(), suffixToken.getColumn());
                    }

                    tagNumber = parseCborTagNumber(suffix.substring(1), suffixToken);
                }

                return parseTaggedContent(majorType, tagNumber);
            }

            // Major type selector #0–#5 and #7.
            return PrimitiveType.majorType(majorType);
        }

        return PrimitiveType.ANY;
    }

    private boolean isHashTerminator(TokenType type) {
        return type == TokenType.EOF
                || type == TokenType.COMMA
                || type == TokenType.RBRACE
                || type == TokenType.RBRACKET
                || type == TokenType.RPAREN
                || type == TokenType.SLASH
                || type == TokenType.ASSIGN;
    }

    /** Shared logic for building a TaggedType or PrimitiveType.majorType after the number is known. */
    private CddlType parseTaggedContent(int major, BigInteger tagNum) {
        if (major == 6) {
            if (peek().is(TokenType.LPAREN)) {
                advance();
                CddlType content = peek().is(TokenType.RPAREN) ? PrimitiveType.ANY : parseType();
                expect(TokenType.RPAREN);
                return new TaggedType(tagNum, content);
            }
            return new TaggedType(tagNum, PrimitiveType.ANY);
        }
        return PrimitiveType.majorType(major);
    }

    // ------------------------------------------------------------------
    // AND / group-to-choice
    // ------------------------------------------------------------------

    /**
     * type2 = ... / "&" "(" group ")" / "&" groupname
     */
    private CddlType parseGroupEnumerationType() {
        expect(TokenType.AMPERSAND);

        if (peek().is(TokenType.LPAREN)) {
            CddlType group = parseGroup();
            return new GroupEnumerationType(group);
        }

        if (peek().is(TokenType.IDENT)) {
            return new GroupEnumerationType(advance().getValue());
        }

        throw new CddlParseException("Expected a group or group name after '&'", peek().getLine(), peek().getColumn());
    }

    // ------------------------------------------------------------------
    // TILDE / wrapped groups
    // ------------------------------------------------------------------

    private CddlType parseWrappedType() {
        expect(TokenType.TILDE);
        return new UnwrapType(expect(TokenType.IDENT).getValue());
    }

    // ------------------------------------------------------------------
    // Occurrence  [occur] = ? | * | + | uint "*" [uint]
    // ------------------------------------------------------------------

    private int[] parseOccurrence() {
        if (peek().is(TokenType.QUESTION)) {
            advance();
            return new int[]{0, 1};
        }

        if (peek().is(TokenType.STAR)) {
            Token star = advance();
            return parseStarOccur(0, star);
        }

        if (peek().is(TokenType.PLUS)) {
            advance();
            return new int[]{1, OccurrenceType.UNBOUNDED};
        }

        if (peek().is(TokenType.NUMBER_INT) && peekAt(1).is(TokenType.STAR)
                && areAdjacent(peek(), peekAt(1))) {
            int lo = parseOccurrenceBound(advance(), false);
            Token star = advance();
            return parseStarOccur(lo, star);
        }

        return new int[]{ 1, 1 };
    }

    /**
     * Parses the optional upper bound following an occurrence star.
     *
     * <p>The upper bound is part of the occurrence indicator only when it is
     * directly adjacent to the {@code *}. A separated number starts the
     * controlled type instead. This distinction is required for expressions
     * such as {@code * 1..10}, where {@code 1..10} is the repeated type.
     *
     * @param min the minimum number of occurrences
     * @param star the occurrence star token
     * @return the minimum and maximum occurrence counts
     */
    private int[] parseStarOccur(int min, Token star) {
        if (peek().is(TokenType.NUMBER_INT) && areAdjacent(star, peek())) {
            int max = parseOccurrenceBound(advance(), true);
            return new int[]{ min, max };
        }

        return new int[]{ min, OccurrenceType.UNBOUNDED };
    }

    /**
     * Determines whether two tokens are directly adjacent in the source.
     *
     * <p>This is used for occurrence indicators, where whitespace separates
     * the optional numeric bound from the controlled type.
     *
     * @param left the preceding token
     * @param right the following token
     * @return {@code true} if no source characters separate the tokens
     */
    private boolean areAdjacent(Token left, Token right) {
        return left.getLine() == right.getLine()
                && left.getColumn() + left.getValue().length() == right.getColumn();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Token peek() {
        return tokens.get(pos);
    }

    private Token peekAt(int offset) {
        int idx = pos + offset;
        return idx < tokens.size() ? tokens.get(idx) : tokens.get(tokens.size() - 1);
    }

    private Token advance() {
        Token t = tokens.get(pos);
        if (!t.is(TokenType.EOF)) {
            pos++;
        }
        return t;
    }

    private Token expect(TokenType type) {
        Token t = peek();
        if (!t.is(type)) {
            throw new CddlParseException("Expected " + type + " but found " + t, t.getLine(), t.getColumn());
        }
        return advance();
    }

    /**
     * Parses a CDDL integer literal and preserves source location in diagnostics.
     *
     * @param token {@link Token}
     * @return {@link BigInteger}
     */
    private static BigInteger parseInteger(Token token) {
        try {
            return parseIntegerValue(token.getValue());
        } catch (NumberFormatException exception) {
            throw new CddlParseException(
                    "Invalid integer literal: " + token.getValue(), token.getLine(), token.getColumn());
        }
    }

    /**
     * Parses a non-negative CDDL uint, including decimal, hexadecimal, and binary notation.
     *
     * @param value {@link String}
     * @param token {@link Token}
     * @param description {@link String}
     * @return {@link BigInteger}
     */
    private static BigInteger parseUnsignedInteger(String value, Token token, String description) {
        BigInteger result;
        try {
            result = parseIntegerValue(value);
        } catch (NumberFormatException exception) {
            throw new CddlParseException("Invalid " + description + ": " + value, token.getLine(), token.getColumn());
        }
        if (result.signum() < 0) {
            throw new CddlParseException(description + " must not be negative: " + value,
                    token.getLine(), token.getColumn());
        }
        return result;
    }

    /**
     * Parses a CDDL tag number and enforces CBOR's unsigned 64-bit tag range.
     *
     * @param value {@link String}
     * @param token {@link Token}
     * @return {@link BigInteger}
     */
    private static BigInteger parseCborTagNumber(String value, Token token) {
        BigInteger tagNumber = parseUnsignedInteger(value, token, "CBOR tag number");
        if (tagNumber.bitLength() > 64) {
            throw new CddlParseException("CBOR tag number exceeds unsigned 64-bit range: " + value,
                    token.getLine(), token.getColumn());
        }
        return tagNumber;
    }

    /**
     * Parses a numeric literal while preserving its sign for prefixed forms.
     *
     * @param value {@link String}
     * @return {@link BigInteger}
     */
    private static BigInteger parseIntegerValue(String value) {
        boolean negative = value.startsWith("-");
        int prefixOffset = negative ? 1 : 0;
        int radix = 10;
        int digitsOffset = prefixOffset;

        if (value.startsWith("0x", prefixOffset) || value.startsWith("0X", prefixOffset)) {
            radix = 16;
            digitsOffset += 2;
        } else if (value.startsWith("0b", prefixOffset) || value.startsWith("0B", prefixOffset)) {
            radix = 2;
            digitsOffset += 2;
        }

        BigInteger parsed = new BigInteger(value.substring(digitsOffset), radix);
        return negative ? parsed.negate() : parsed;
    }

    /**
     * Parses an occurrence bound and rejects values that cannot be represented by the schema model.
     *
     * @param token {@link Token}
     * @param upperBound boolean
     * @return integer
     */
    private static int parseOccurrenceBound(Token token, boolean upperBound) {
        BigInteger value = parseUnsignedInteger(token.getValue(), token, "occurrence bound");
        BigInteger maximum = BigInteger.valueOf(Integer.MAX_VALUE);
        int comparison = value.compareTo(maximum);
        if (comparison > 0 || (upperBound && comparison == 0)) {
            throw new CddlParseException("Occurrence bound is too large: " + token.getValue(),
                    token.getLine(), token.getColumn());
        }
        return value.intValue();
    }

    private static boolean isRadixPrefix(String identifier) {
        return identifier.length() > 1
                && (identifier.charAt(0) == 'x' || identifier.charAt(0) == 'X'
                || identifier.charAt(0) == 'b' || identifier.charAt(0) == 'B');
    }

    private static byte[] hexToBytes(String hex, Token tok) {
        if (hex.length() % 2 != 0) {
            throw new CddlParseException("Hex byte-string has odd length: h'" + hex + "'",
                    tok.getLine(), tok.getColumn());
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
}
