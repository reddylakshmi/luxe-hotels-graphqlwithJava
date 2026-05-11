package com.luxe.common.security;

import graphql.parser.Parser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryComplexityTest {

    @Test
    void leaf_only_query_costs_one_per_field() {
        var cost = QueryComplexity.calculate(parse("{ a b c }"));
        assertThat(cost.score()).isEqualTo(3);
        assertThat(cost.depth()).isEqualTo(2); // root + leaves
    }

    @Test
    void nested_object_field_increments_depth() {
        var cost = QueryComplexity.calculate(parse("{ a { b { c d } } }"));
        // a (1) + b (1) + c (1) + d (1) = 4
        assertThat(cost.score()).isEqualTo(4);
        assertThat(cost.depth()).isEqualTo(4);
    }

    @Test
    void first_argument_multiplies_subtree_cost() {
        // hotels(first: 10) { id name } => 1 (root) + 10 * (1 + 1) = 21
        var cost = QueryComplexity.calculate(parse("{ hotels(first: 10) { id name } }"));
        assertThat(cost.score()).isEqualTo(21);
    }

    @Test
    void limit_and_last_arguments_behave_like_first() {
        var withLimit = QueryComplexity.calculate(parse("{ items(limit: 5) { id } }"));
        var withLast = QueryComplexity.calculate(parse("{ items(last: 5) { id } }"));
        assertThat(withLimit.score()).isEqualTo(withLast.score());
        assertThat(withLimit.score()).isEqualTo(6); // 1 + 5 * 1
    }

    @Test
    void variable_bound_pagination_falls_back_to_LIST_DEFAULT() {
        // The client could otherwise pass `first: 9999` after the
        // guardrail accepted the operation. Refuse to trust unresolved
        // variables and apply the conservative LIST_DEFAULT (25).
        var cost = QueryComplexity.calculate(
                parse("query($n: Int!) { items(first: $n) { id } }"));
        assertThat(cost.score()).isEqualTo(1 + QueryComplexity.LIST_DEFAULT);
    }

    @Test
    void federation_entities_uses_LIST_DEFAULT_for_representations() {
        // We can't easily count the literal array length without
        // pulling in more AST types — LIST_DEFAULT is the safe call.
        var cost = QueryComplexity.calculate(parse("""
                query($reps: [_Any!]!) {
                  _entities(representations: $reps) {
                    ... on Hotel { id name }
                  }
                }
                """));
        // 1 (entities) + 25 * 2 (id + name) = 51
        assertThat(cost.score()).isEqualTo(51);
    }

    @Test
    void fragment_spread_is_inlined_in_cost() {
        var query = """
                {
                  user { ...userInfo }
                }
                fragment userInfo on User { id name email }
                """;
        var cost = QueryComplexity.calculate(parse(query));
        // user (1) + id (1) + name (1) + email (1) = 4
        assertThat(cost.score()).isEqualTo(4);
    }

    @Test
    void inline_fragment_is_traversed() {
        var query = """
                {
                  result {
                    ... on Hit { id score }
                    ... on Miss { id reason }
                  }
                }
                """;
        var cost = QueryComplexity.calculate(parse(query));
        // result + 4 leaves
        assertThat(cost.score()).isEqualTo(5);
    }

    @Test
    void deeply_nested_query_reports_correct_depth() {
        var cost = QueryComplexity.calculate(parse("{ a { b { c { d { e { f } } } } } }"));
        assertThat(cost.depth()).isEqualTo(7); // root + 6 levels
    }

    @Test
    void empty_document_costs_zero() {
        // A query with only fragment defs and no operation contributes
        // nothing to score — we only sum OperationDefinitions.
        var cost = QueryComplexity.calculate(parse(
                "fragment x on Hotel { id }"));
        assertThat(cost.score()).isZero();
    }

    private static graphql.language.Document parse(String src) {
        return new Parser().parseDocument(src);
    }
}
