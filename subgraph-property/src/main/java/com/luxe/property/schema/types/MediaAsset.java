package com.luxe.property.schema.types;

public record MediaAsset(
        String id, String url, String thumbnailUrl,
        String category, String caption, String altText,
        boolean isPrimary, int sortOrder
) {}
