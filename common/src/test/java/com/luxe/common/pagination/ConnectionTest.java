package com.luxe.common.pagination;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionTest {

    private static record Item(String id) implements HasId {
        @Override public String getId() { return id; }
    }

    private static List<Item> items(String... ids) {
        return java.util.Arrays.stream(ids).map(Item::new).toList();
    }

    @Test
    void empty_input_returns_empty_connection() {
        Connection<Item> conn = Connection.of(List.of(), 10, null);
        assertThat(conn.edges()).isEmpty();
        assertThat(conn.totalCount()).isZero();
        assertThat(conn.pageInfo().hasNextPage()).isFalse();
        assertThat(conn.pageInfo().hasPreviousPage()).isFalse();
        assertThat(conn.pageInfo().startCursor()).isNull();
        assertThat(conn.pageInfo().endCursor()).isNull();
    }

    @Test
    void null_input_returns_empty_connection() {
        Connection<Item> conn = Connection.of(null, 10, null);
        assertThat(conn.edges()).isEmpty();
        assertThat(conn.totalCount()).isZero();
    }

    @Test
    void single_page_no_paging_needed() {
        Connection<Item> conn = Connection.of(items("a", "b", "c"), 10, null);
        assertThat(conn.edges()).hasSize(3);
        assertThat(conn.totalCount()).isEqualTo(3);
        assertThat(conn.pageInfo().hasNextPage()).isFalse();
        assertThat(conn.pageInfo().hasPreviousPage()).isFalse();
        assertThat(conn.edges().get(0).node().getId()).isEqualTo("a");
    }

    @Test
    void first_smaller_than_total_indicates_next_page() {
        Connection<Item> conn = Connection.of(items("a", "b", "c", "d"), 2, null);
        assertThat(conn.edges()).hasSize(2);
        assertThat(conn.edges()).extracting(e -> e.node().getId()).containsExactly("a", "b");
        assertThat(conn.pageInfo().hasNextPage()).isTrue();
        assertThat(conn.pageInfo().hasPreviousPage()).isFalse();
        assertThat(conn.totalCount()).isEqualTo(4);
    }

    @Test
    void after_cursor_decodes_id_and_skips_to_next_item() {
        String afterA = Base64.getEncoder().encodeToString("a".getBytes());
        Connection<Item> conn = Connection.of(items("a", "b", "c", "d"), 2, afterA);
        assertThat(conn.edges()).extracting(e -> e.node().getId()).containsExactly("b", "c");
        assertThat(conn.pageInfo().hasNextPage()).isTrue();
        assertThat(conn.pageInfo().hasPreviousPage()).isTrue();
    }

    @Test
    void after_cursor_at_end_returns_empty_page() {
        String afterD = Base64.getEncoder().encodeToString("d".getBytes());
        Connection<Item> conn = Connection.of(items("a", "b", "c", "d"), 2, afterD);
        assertThat(conn.edges()).isEmpty();
        assertThat(conn.pageInfo().hasNextPage()).isFalse();
    }

    @Test
    void cursor_is_base64_of_id() {
        Connection<Item> conn = Connection.of(items("hello"), 10, null);
        String expected = Base64.getEncoder().encodeToString("hello".getBytes());
        assertThat(conn.edges().get(0).cursor()).isEqualTo(expected);
        assertThat(conn.pageInfo().startCursor()).isEqualTo(expected);
        assertThat(conn.pageInfo().endCursor()).isEqualTo(expected);
    }

    @Test
    void start_and_end_cursor_bracket_the_page() {
        Connection<Item> conn = Connection.of(items("a", "b", "c"), 2, null);
        String aCursor = Base64.getEncoder().encodeToString("a".getBytes());
        String bCursor = Base64.getEncoder().encodeToString("b".getBytes());
        assertThat(conn.pageInfo().startCursor()).isEqualTo(aCursor);
        assertThat(conn.pageInfo().endCursor()).isEqualTo(bCursor);
    }

    @Test
    void of_all_returns_everything_without_paging_arguments() {
        Connection<Item> conn = Connection.ofAll(items("a", "b", "c"));
        assertThat(conn.edges()).hasSize(3);
        assertThat(conn.totalCount()).isEqualTo(3);
        assertThat(conn.pageInfo().hasNextPage()).isFalse();
    }

    @Test
    void total_count_reflects_full_input_not_page_size() {
        Connection<Item> conn = Connection.of(items("a", "b", "c", "d", "e"), 2, null);
        assertThat(conn.edges()).hasSize(2);
        assertThat(conn.totalCount()).isEqualTo(5);
    }
}
