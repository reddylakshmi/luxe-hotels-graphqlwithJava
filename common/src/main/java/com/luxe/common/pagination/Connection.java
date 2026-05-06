package com.luxe.common.pagination;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public record Connection<T>(
        List<Edge<T>> edges,
        PageInfo pageInfo,
        int totalCount
) {
    public static <T extends HasId> Connection<T> of(List<T> items, Integer first, String after) {
        if (items == null || items.isEmpty()) {
            return new Connection<>(List.of(), PageInfo.empty(), 0);
        }

        int totalCount = items.size();
        int startIndex = 0;

        if (after != null) {
            String afterId = new String(Base64.getDecoder().decode(after));
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId().equals(afterId)) {
                    startIndex = i + 1;
                    break;
                }
            }
        }

        List<T> sliced = items.subList(startIndex, items.size());
        boolean hasNextPage = false;

        if (first != null && sliced.size() > first) {
            sliced = sliced.subList(0, first);
            hasNextPage = true;
        }

        List<Edge<T>> edges = sliced.stream()
                .map(item -> new Edge<>(item, Base64.getEncoder().encodeToString(item.getId().getBytes())))
                .collect(Collectors.toList());

        String startCursor = edges.isEmpty() ? null : edges.get(0).cursor();
        String endCursor = edges.isEmpty() ? null : edges.get(edges.size() - 1).cursor();

        PageInfo pageInfo = new PageInfo(hasNextPage, after != null, startCursor, endCursor);
        return new Connection<>(edges, pageInfo, totalCount);
    }

    public static <T extends HasId> Connection<T> ofAll(List<T> items) {
        return of(items, null, null);
    }
}
