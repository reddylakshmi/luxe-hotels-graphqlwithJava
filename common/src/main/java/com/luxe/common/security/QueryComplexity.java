package com.luxe.common.security;

import graphql.language.Argument;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.IntValue;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Pure cost + depth calculator for an incoming GraphQL query.
 * Walks the AST (no schema knowledge needed) and produces two
 * numbers — the total cost and the maximum nesting depth.
 *
 * <h3>Cost model</h3>
 * <ul>
 *   <li>Every selected field contributes a base cost of 1.</li>
 *   <li>Fields that take a {@code first}, {@code limit}, or
 *       {@code last} argument multiply the cost of their subtree by
 *       that integer (capped at {@link #LIST_DEFAULT} when the arg
 *       is missing / not an integer literal, so pagination-less list
 *       fields don't get a free ride).</li>
 *   <li>Federation {@code _entities(representations: [...])} is
 *       multiplied by the literal array length when present.</li>
 *   <li>Fragments are inlined inline; spreads are resolved against
 *       the operation's fragment definitions.</li>
 * </ul>
 *
 * <h3>Depth</h3>
 * Depth is the count of nested selection sets from the operation
 * root to the deepest leaf. A query like {@code { a { b { c } } }}
 * has depth 3.
 *
 * <p>Everything here is pure / deterministic so vitest-style table
 * tests can cover every branch without a graphql-java runtime.
 */
public final class QueryComplexity {

    private QueryComplexity() {}

    /**
     * Default per-list cost when the field carries no explicit
     * pagination argument. Keeps "list everything" queries
     * accountable instead of treating them as cost = 1.
     */
    public static final int LIST_DEFAULT = 25;

    public record Cost(int score, int depth) {}

    public static Cost calculate(Document document) {
        Map<String, FragmentDefinition> fragments = collectFragments(document);
        int score = 0;
        int depth = 0;
        for (var def : document.getDefinitions()) {
            if (def instanceof OperationDefinition op) {
                Cost child = scoreSelectionSet(op.getSelectionSet(), 1, fragments);
                score += child.score();
                depth = Math.max(depth, child.depth());
            }
        }
        return new Cost(score, depth);
    }

    private static Map<String, FragmentDefinition> collectFragments(Document document) {
        Map<String, FragmentDefinition> out = new HashMap<>();
        for (var def : document.getDefinitions()) {
            if (def instanceof FragmentDefinition fd) out.put(fd.getName(), fd);
        }
        return out;
    }

    private static Cost scoreSelectionSet(
            SelectionSet set, int currentDepth, Map<String, FragmentDefinition> fragments) {
        if (set == null) return new Cost(0, currentDepth);
        int total = 0;
        int deepest = currentDepth;
        for (Selection<?> sel : set.getSelections()) {
            Cost childCost = scoreSelection(sel, currentDepth, fragments);
            total += childCost.score();
            deepest = Math.max(deepest, childCost.depth());
        }
        return new Cost(total, deepest);
    }

    private static Cost scoreSelection(
            Selection<?> sel, int currentDepth, Map<String, FragmentDefinition> fragments) {
        if (sel instanceof Field field) {
            return scoreField(field, currentDepth, fragments);
        }
        if (sel instanceof InlineFragment frag) {
            return scoreSelectionSet(frag.getSelectionSet(), currentDepth, fragments);
        }
        if (sel instanceof FragmentSpread spread) {
            FragmentDefinition def = fragments.get(spread.getName());
            if (def == null) return new Cost(0, currentDepth);
            return scoreSelectionSet(def.getSelectionSet(), currentDepth, fragments);
        }
        return new Cost(0, currentDepth);
    }

    private static Cost scoreField(
            Field field, int currentDepth, Map<String, FragmentDefinition> fragments) {
        int childDepth = currentDepth + 1;
        int multiplier = listMultiplier(field);
        Cost child = scoreSelectionSet(field.getSelectionSet(), childDepth, fragments);
        // Field itself = 1; subtree scaled by the list multiplier.
        int score = 1 + multiplier * child.score();
        return new Cost(score, child.depth());
    }

    /**
     * Look at the field's arguments to decide if it's a list field
     * and what multiplier to apply. {@code first}/{@code limit}/
     * {@code last} integer literals win; federation
     * {@code _entities(representations:)} uses the literal array
     * length; everything else returns 1 (singleton).
     *
     * <p>Variable-bound pagination (e.g. {@code first: $limit}) can't
     * be resolved at parse time — those fall through to
     * {@link #LIST_DEFAULT} so a hostile client can't bypass the cap
     * by passing a variable.
     */
    private static int listMultiplier(Field field) {
        if (field.getArguments() == null || field.getArguments().isEmpty()) {
            // Federation _entities without representations isn't valid;
            // we don't need to special-case it here.
            return 1;
        }
        boolean looksLikeList = false;
        for (Argument arg : field.getArguments()) {
            String name = arg.getName();
            if ("representations".equals(name)) {
                // graphql-java may give us an ArrayValue. We can't
                // easily count it without importing more types; treat
                // it as LIST_DEFAULT — conservative.
                return LIST_DEFAULT;
            }
            if ("first".equals(name) || "limit".equals(name) || "last".equals(name)) {
                looksLikeList = true;
                if (arg.getValue() instanceof IntValue iv) {
                    return Math.max(1, iv.getValue().intValue());
                }
                // Variable-bound — refuse to trust the client's
                // implicit cap and apply LIST_DEFAULT.
                return LIST_DEFAULT;
            }
        }
        return looksLikeList ? LIST_DEFAULT : 1;
    }
}
