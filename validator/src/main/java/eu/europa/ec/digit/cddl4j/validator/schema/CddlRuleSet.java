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
package eu.europa.ec.digit.cddl4j.validator.schema;


import eu.europa.ec.digit.cddl4j.validator.schema.type.CddlType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds all named CDDL rules (user-defined + RFC 8610 Appendix D prelude)
 * parsed from a schema source.
 *
 * <p>The <em>root</em> rule is the first <strong>user-defined</strong> rule in
 * the CDDL source, not the first prelude rule.  An explicit root name may be
 * provided at construction time (used by {@link CddlSchema#withRootRule}).
 */
public final class CddlRuleSet {

    /** RFC 8610 prelude rules. */
    private final Map<String, CddlType> preludeRules;

    /** Application-provided rules. */
    private final Map<String, CddlType> customRules;

    /** Rules declared by the parsed CDDL source. */
    private final Map<String, CddlType> schemaRules;

    /**
     * Name of the root rule.  When {@code null} the first rule in the map is
     * used (backward-compat fallback; never null after the parser fills this).
     */
    private final String rootRuleName;

    /**
     * Constructor used by the parser.
     *
     * @param rules        all rules (prelude merged with user rules)
     * @param rootRuleName name of the first user-defined rule (the default root)
     */
    public CddlRuleSet(LinkedHashMap<String, CddlType> rules, String rootRuleName) {
        this(Collections.emptyMap(), rules, rootRuleName);
    }

    /**
     * Full constructor.
     *
     * @param customRules application-provided rules
     * @param schemaRules rules declared in the CDDL source
     * @param rootRuleName default root rule name
     */
    public CddlRuleSet(Map<String, CddlType> customRules, Map<String, CddlType> schemaRules, String rootRuleName) {
        Objects.requireNonNull(customRules, "customRules must not be null");
        Objects.requireNonNull(schemaRules, "schemaRules must not be null");
        this.preludeRules = CddlPrelude.copy();
        this.customRules = Collections.unmodifiableMap(new LinkedHashMap<>(customRules));
        this.schemaRules = Collections.unmodifiableMap(new LinkedHashMap<>(schemaRules));
        this.rootRuleName = rootRuleName;
    }

    /**
     * Returns a new rule set with the supplied custom rules.
     *
     * <p>Existing custom rules are replaced by rules with the same names.
     * Schema rules always retain precedence over custom rules.
     *
     * @param rules custom rules
     * @return a new rule set
     */
    public CddlRuleSet withCustomRules(Map<String, CddlType> rules) {
        Objects.requireNonNull(rules, "rules must not be null");
        LinkedHashMap<String, CddlType> mergedRules = new LinkedHashMap<>(customRules);
        mergedRules.putAll(rules);
        return new CddlRuleSet(mergedRules, schemaRules, rootRuleName);
    }

    /**
     * Resolve a named rule.
     *
     * @param name rule identifier
     * @return the type for that rule, or {@code null} if not defined
     */
    public CddlType getRule(String name) {
        CddlType rule = schemaRules.get(name);
        if (rule != null) {
            return rule;
        }
        rule = customRules.get(name);
        if (rule != null) {
            return rule;
        }
        return preludeRules.get(name);
    }

    /**
     * The name of the root rule (first user-defined rule in the source).
     *
     * @return the effective root rule name
     * @throws IllegalStateException if no user rules were defined
     */
    public String getRootRuleName() {
        if (rootRuleName != null) {
            return rootRuleName;
        }
        // Fallback: first key in map (only reached if constructed without explicit root)
        if (schemaRules.isEmpty()) {
            throw new IllegalStateException("CDDL schema contains no rules");
        }
        return schemaRules.keySet().iterator().next();
    }

    /**
     * Returns an unmodifiable view of all rules (prelude + custom + user-defined).
     *
     * @return the named rules in insertion order
     */
    public Map<String, CddlType> getRules() {
        LinkedHashMap<String, CddlType> result = new LinkedHashMap<>(preludeRules);
        result.putAll(customRules);
        result.putAll(schemaRules);
        return Collections.unmodifiableMap(result);
    }

}
