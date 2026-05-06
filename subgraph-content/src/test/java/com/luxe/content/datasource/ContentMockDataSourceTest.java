package com.luxe.content.datasource;

import com.luxe.content.schema.types.Article;
import com.luxe.content.schema.types.BrandStory;
import com.luxe.content.schema.types.DealSpotlight;
import com.luxe.content.schema.types.TravelInspiration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContentMockDataSourceTest {

    private ContentMockDataSource ds;

    @BeforeEach
    void setUp() {
        ds = new ContentMockDataSource();
    }

    @Test
    void featured_articles_returns_published_only_in_recency_order() {
        List<Article> all = ds.findFeaturedArticles(10, "en");
        assertThat(all).isNotEmpty();
        assertThat(all).allSatisfy(a -> assertThat(a.getStatus()).isEqualTo("PUBLISHED"));
        for (int i = 0; i < all.size() - 1; i++) {
            assertThat(all.get(i).getPublishedAt())
                    .isAfterOrEqualTo(all.get(i + 1).getPublishedAt());
        }
    }

    @Test
    void article_by_slug_returns_localized_content_when_locale_known() {
        var fr = ds.findArticleBySlug("paris-by-night", "fr");
        assertThat(fr).isPresent();
        assertThat(fr.get().getTitle().locale()).isEqualTo("fr");
        assertThat(fr.get().getTitle().fallbackUsed()).isFalse();
    }

    @Test
    void article_by_slug_falls_back_to_english_when_locale_missing() {
        // tokyo-omakase has only English translations seeded
        var de = ds.findArticleBySlug("tokyo-omakase-renaissance", "de");
        assertThat(de).isPresent();
        assertThat(de.get().getTitle().fallbackUsed()).isTrue();
    }

    @Test
    void article_by_unknown_slug_is_empty() {
        assertThat(ds.findArticleBySlug("not-a-slug", "en")).isEmpty();
    }

    @Test
    void inspirations_filter_by_destination_case_insensitive() {
        var paris = ds.findInspirations("Paris", null, 10, "en");
        assertThat(paris).isNotEmpty();
        assertThat(paris).allSatisfy(i ->
                assertThat(i.getDestination()).isEqualToIgnoringCase("Paris"));
    }

    @Test
    void inspirations_filter_by_season() {
        var spring = ds.findInspirations(null, "SPRING", 10, "en");
        assertThat(spring).allSatisfy(i ->
                assertThat(i.getBestSeason()).isEqualTo("SPRING"));
    }

    @Test
    void brand_story_returns_a_story_with_pillars() {
        BrandStory s = ds.getBrandStory("en");
        assertThat(s).isNotNull();
        assertThat(s.getPillars()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void deal_spotlights_with_active_true_only_returns_currently_valid() {
        List<DealSpotlight> deals = ds.findDealSpotlights(true, "en");
        assertThat(deals).allSatisfy(d ->
                assertThat(d.getStatus()).isEqualTo("PUBLISHED"));
    }

    @Test
    void content_collection_by_slug_returns_collection() {
        var col = ds.findCollectionBySlug("spring-2026", "en");
        assertThat(col).isPresent();
        assertThat(col.get().getArticleIds()).isNotEmpty();
    }

    @Test
    void articles_filter_by_category() {
        var dest = ds.findArticles(java.util.Map.of("category", "DESTINATION"), "en");
        assertThat(dest).isNotEmpty();
        assertThat(dest).allSatisfy(a -> assertThat(a.getCategory()).isEqualTo("DESTINATION"));
    }
}
