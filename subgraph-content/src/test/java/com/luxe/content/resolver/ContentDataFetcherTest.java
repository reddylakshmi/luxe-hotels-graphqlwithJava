package com.luxe.content.resolver;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ContentDataFetcherTest {

    @Autowired
    DgsQueryExecutor dgs;

    @Test
    void featured_articles_returns_localized_content() {
        List<Map<String, Object>> articles = dgs.executeAndExtractJsonPath(
                """
                { featuredArticles(first: 1, locale: "en") {
                    id slug title { text locale fallbackUsed }
                } }
                """,
                "data.featuredArticles");
        assertThat(articles).hasSize(1);
        Map<String, Object> title = (Map<String, Object>) articles.get(0).get("title");
        assertThat(title.get("locale")).isEqualTo("en");
        assertThat(title.get("fallbackUsed")).isEqualTo(false);
    }

    @Test
    void article_in_unknown_locale_falls_back_to_english_and_marks_fallback() {
        Boolean fallback = dgs.executeAndExtractJsonPath(
                """
                { article(slug: "tokyo-omakase-renaissance", locale: "de") {
                    title { text locale fallbackUsed }
                } }
                """,
                "data.article.title.fallbackUsed");
        assertThat(fallback).isTrue();
    }

    @Test
    void brand_story_includes_pillars() {
        List<Map<String, Object>> pillars = dgs.executeAndExtractJsonPath(
                """
                { brandStory(locale: "en") {
                    pillars { code title { text } }
                } }
                """,
                "data.brandStory.pillars");
        assertThat(pillars).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void travel_inspirations_filter_by_destination() {
        List<Map<String, Object>> paris = dgs.executeAndExtractJsonPath(
                """
                { travelInspirations(destination: "Paris", first: 5, locale: "en") {
                    destination region
                } }
                """,
                "data.travelInspirations");
        assertThat(paris).isNotEmpty();
        assertThat(paris).allSatisfy(i ->
                assertThat(((String) i.get("destination")).toLowerCase()).contains("paris"));
    }

    @Test
    void deal_spotlights_active_returns_published_deals() {
        List<Map<String, Object>> deals = dgs.executeAndExtractJsonPath(
                "{ dealSpotlights(active: true, locale: \"en\") { slug status } }",
                "data.dealSpotlights");
        assertThat(deals).isNotEmpty();
        assertThat(deals).allSatisfy(d -> assertThat(d.get("status")).isEqualTo("PUBLISHED"));
    }
}
