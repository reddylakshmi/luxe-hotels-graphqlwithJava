package com.luxe.content.schema.types;

import java.util.List;

public record LocalizedContent(
        String locale, String text, boolean fallbackUsed, List<LocaleText> translations
) {
    public record LocaleText(String locale, String text) {}

    public static LocalizedContent ofEnglish(String text) {
        return new LocalizedContent("en", text, false, List.of(new LocaleText("en", text)));
    }

    public static LocalizedContent of(String locale, String text, List<LocaleText> all) {
        LocaleText found = all.stream()
                .filter(t -> t.locale().equals(locale))
                .findFirst().orElse(null);
        if (found != null) return new LocalizedContent(locale, found.text(), false, all);
        LocaleText english = all.stream()
                .filter(t -> "en".equals(t.locale()))
                .findFirst().orElseGet(() -> all.isEmpty() ? new LocaleText(locale, "") : all.get(0));
        return new LocalizedContent(locale, english.text(), true, all);
    }
}
