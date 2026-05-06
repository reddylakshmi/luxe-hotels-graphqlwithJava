package com.luxe.content.schema.types;

public record ContentAuthor(
        String id, String name, String title, LocalizedContent bio, String photoUrl
) {}
