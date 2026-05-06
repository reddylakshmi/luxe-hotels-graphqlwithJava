package com.luxe.common.pagination;

public record PageInfo(
        boolean hasNextPage,
        boolean hasPreviousPage,
        String startCursor,
        String endCursor
) {
    public static PageInfo empty() {
        return new PageInfo(false, false, null, null);
    }
}
