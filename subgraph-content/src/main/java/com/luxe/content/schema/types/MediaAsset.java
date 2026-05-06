package com.luxe.content.schema.types;

import com.luxe.common.pagination.HasId;

public record MediaAsset(
        String id, String url, String thumbnailUrl,
        LocalizedContent altText, LocalizedContent caption,
        String type, Integer width, Integer height, Integer durationSeconds
) implements HasId {
    @Override public String getId() { return id; }
}
