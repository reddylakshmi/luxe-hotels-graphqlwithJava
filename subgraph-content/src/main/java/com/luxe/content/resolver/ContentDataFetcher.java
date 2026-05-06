package com.luxe.content.resolver;

import com.luxe.common.pagination.Connection;
import com.luxe.content.datasource.ContentDataSource;
import com.luxe.content.schema.types.*;
import com.netflix.graphql.dgs.*;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DgsComponent
public class ContentDataFetcher {

    private final ContentDataSource dataSource;

    public ContentDataFetcher(ContentDataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @DgsQuery
    public Object articles(@InputArgument Integer first, @InputArgument String after,
                            @InputArgument Map<String, Object> filter,
                            @InputArgument String locale) {
        List<Article> all = dataSource.findArticles(filter, locale);
        Connection<Article> conn = Connection.of(all, first != null ? first : 10, after);
        return Map.of(
                "edges", conn.edges().stream()
                        .map(e -> Map.of("node", e.node(), "cursor", e.cursor())).toList(),
                "pageInfo", pageInfo(conn),
                "totalCount", conn.totalCount());
    }

    @DgsQuery
    public Article article(@InputArgument String slug, @InputArgument String locale) {
        return dataSource.findArticleBySlug(slug, locale).orElse(null);
    }

    @DgsQuery
    public List<Article> featuredArticles(@InputArgument Integer first,
                                            @InputArgument String locale) {
        return dataSource.findFeaturedArticles(first != null ? first : 6, locale);
    }

    @DgsQuery
    public List<TravelInspiration> travelInspirations(@InputArgument String destination,
                                                        @InputArgument String season,
                                                        @InputArgument Integer first,
                                                        @InputArgument String locale) {
        return dataSource.findInspirations(destination, season,
                first != null ? first : 10, locale);
    }

    @DgsQuery
    public BrandStory brandStory(@InputArgument String locale) {
        return dataSource.getBrandStory(locale);
    }

    @DgsQuery
    public List<DealSpotlight> dealSpotlights(@InputArgument Boolean active,
                                                @InputArgument String locale) {
        return dataSource.findDealSpotlights(active, locale);
    }

    @DgsQuery
    public ContentCollection contentCollection(@InputArgument String slug,
                                                 @InputArgument String locale) {
        return dataSource.findCollectionBySlug(slug, locale).orElse(null);
    }

    // ── ContentCollection composite resolvers ─────────────────────────────────

    @DgsData(parentType = "ContentCollection", field = "articles")
    public List<Article> collectionArticles(DataFetchingEnvironment dfe) {
        ContentCollection col = dfe.getSource();
        return dataSource.articlesByIds(col.getArticleIds(), col.getRequestedLocale());
    }

    @DgsData(parentType = "ContentCollection", field = "inspirations")
    public List<TravelInspiration> collectionInspirations(DataFetchingEnvironment dfe) {
        ContentCollection col = dfe.getSource();
        return dataSource.inspirationsByIds(col.getInspirationIds(), col.getRequestedLocale());
    }

    @DgsData(parentType = "ContentCollection", field = "spotlights")
    public List<DealSpotlight> collectionSpotlights(DataFetchingEnvironment dfe) {
        ContentCollection col = dfe.getSource();
        return dataSource.spotlightsByIds(col.getSpotlightIds(), col.getRequestedLocale());
    }

    // ── Federation entity fetcher ─────────────────────────────────────────────

    @DgsEntityFetcher(name = "Article")
    public Article fetchArticle(Map<String, Object> values) {
        return dataSource.findArticleById((String) values.get("id")).orElse(null);
    }

    private Map<String, Object> pageInfo(Connection<?> conn) {
        HashMap<String, Object> m = new HashMap<>();
        m.put("hasNextPage", conn.pageInfo().hasNextPage());
        m.put("hasPreviousPage", conn.pageInfo().hasPreviousPage());
        m.put("startCursor", conn.pageInfo().startCursor());
        m.put("endCursor", conn.pageInfo().endCursor());
        return m;
    }
}
