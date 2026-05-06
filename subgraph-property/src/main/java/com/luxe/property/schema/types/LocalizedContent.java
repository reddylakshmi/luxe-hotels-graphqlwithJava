package com.luxe.property.schema.types;

import java.util.List;
import java.util.Map;

public class LocalizedContent {
    private final Map<String, String> texts;

    public LocalizedContent(Map<String, String> texts) {
        this.texts = texts;
    }

    public static LocalizedContent of(String englishText) {
        return new LocalizedContent(Map.of("en", englishText));
    }

    public String text(String locale) {
        if (locale == null) return texts.getOrDefault("en", "");
        return texts.getOrDefault(locale, texts.getOrDefault("en", ""));
    }

    public List<String> getAvailableLocales() {
        return List.copyOf(texts.keySet());
    }

    public Map<String, String> getTexts() { return texts; }
}
