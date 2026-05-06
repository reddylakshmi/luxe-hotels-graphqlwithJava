package com.luxe.content.datasource;

import com.luxe.content.schema.types.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ContentDataSource {
    List<Article> findArticles(Map<String, Object> filter, String locale);
    Optional<Article> findArticleBySlug(String slug, String locale);
    List<Article> findFeaturedArticles(int limit, String locale);

    List<TravelInspiration> findInspirations(String destination, String season, int limit, String locale);

    BrandStory getBrandStory(String locale);

    List<DealSpotlight> findDealSpotlights(Boolean active, String locale);

    Optional<ContentCollection> findCollectionBySlug(String slug, String locale);

    Optional<Article> findArticleById(String id);
    List<Article> articlesByIds(List<String> ids, String locale);
    List<TravelInspiration> inspirationsByIds(List<String> ids, String locale);
    List<DealSpotlight> spotlightsByIds(List<String> ids, String locale);
}
