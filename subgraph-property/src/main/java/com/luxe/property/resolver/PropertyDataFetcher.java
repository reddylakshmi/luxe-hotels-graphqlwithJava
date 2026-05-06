package com.luxe.property.resolver;

import com.luxe.common.auth.AuthContext;
import com.luxe.common.auth.AuthContextResolver;
import com.luxe.common.error.NotFoundError;
import com.luxe.property.datasource.PropertyDataSource;
import com.luxe.property.schema.types.*;
import com.netflix.graphql.dgs.*;
import graphql.schema.DataFetchingEnvironment;

import java.util.*;
import java.util.stream.Collectors;

@DgsComponent
public class PropertyDataFetcher {

    private final PropertyDataSource dataSource;
    private final AuthContextResolver authResolver;

    public PropertyDataFetcher(PropertyDataSource dataSource, AuthContextResolver authResolver) {
        this.dataSource = dataSource;
        this.authResolver = authResolver;
    }

    private AuthContext auth(DataFetchingEnvironment dfe) {
        return authResolver.resolve(dfe);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @DgsQuery
    public Hotel hotel(@InputArgument String id) {
        return dataSource.getHotelById(id).orElse(null);
    }

    @DgsQuery
    public Hotel hotelBySlug(@InputArgument String slug) {
        return dataSource.getHotelBySlug(slug).orElse(null);
    }

    @DgsQuery
    public Map<String, Object> hotels(@InputArgument Integer first,
                                       @InputArgument String after,
                                       @InputArgument Map<String, Object> filter,
                                       @InputArgument String sortBy) {
        List<Hotel> all = dataSource.searchHotels(filter, sortBy);
        return buildConnection(all, first, after);
    }

    @DgsQuery
    public Brand brand(@InputArgument String id) {
        return dataSource.getBrandById(id).orElse(null);
    }

    @DgsQuery
    public Brand brandByCode(@InputArgument String code) {
        return dataSource.getBrandByCode(code).orElse(null);
    }

    @DgsQuery
    public Map<String, Object> brands(@InputArgument Integer first,
                                       @InputArgument String after,
                                       @InputArgument String tier) {
        List<Brand> all = dataSource.getAllBrands(tier);
        return buildBrandConnection(all, first, after);
    }

    @DgsQuery
    public RoomType roomType(@InputArgument String id) {
        return dataSource.getRoomTypeById(id).orElse(null);
    }

    @DgsQuery
    public List<Hotel> featuredHotels(@InputArgument Integer first,
                                       @InputArgument String brandTier,
                                       @InputArgument String countryCode) {
        return dataSource.getFeaturedHotels(brandTier, countryCode, first != null ? first : 6);
    }

    @DgsQuery
    public Restaurant restaurant(@InputArgument String id) {
        return dataSource.getRestaurantById(id).orElse(null);
    }

    @DgsQuery
    public Spa spa(@InputArgument String id) {
        return dataSource.getSpaById(id).orElse(null);
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @DgsMutation
    public Object submitReview(@InputArgument Map<String, Object> input,
                                DataFetchingEnvironment dfe) {
        AuthContext authContext = auth(dfe);
        if (authContext != null) authContext.requireAuth();

        String hotelId = (String) input.get("hotelId");
        Number rating = (Number) input.get("overallRating");
        String title = (String) input.get("title");
        String body = (String) input.get("body");
        // Date scalar deserializes to java.time.LocalDate; coerce to ISO string for the data source.
        Object rawStayDate = input.get("stayDate");
        String stayDate = rawStayDate != null ? rawStayDate.toString() : null;

        if (dataSource.getHotelById(hotelId).isEmpty()) {
            return new NotFoundError("Hotel", hotelId);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> cats = (Map<String, Object>) input.get("categoryRatings");
        String guestName = authContext != null && authContext.guestId() != null
                ? authContext.guestId() : "Anonymous";

        return dataSource.submitReview(hotelId, guestName, rating.doubleValue(), cats, title, body, stayDate);
    }

    @DgsMutation
    public Object markReviewHelpful(@InputArgument String reviewId) {
        return dataSource.markReviewHelpful(reviewId)
                .<Object>map(r -> r)
                .orElseGet(() -> new NotFoundError("Review", reviewId));
    }

    // ── Parent-type field resolvers ────────────────────────────────────────────

    @DgsData(parentType = "Hotel", field = "brand")
    public Brand hotelBrand(DataFetchingEnvironment dfe) {
        Hotel hotel = dfe.getSource();
        return dataSource.getBrandById(hotel.getBrandId()).orElse(null);
    }

    @DgsData(parentType = "Hotel", field = "roomTypes")
    public List<RoomType> hotelRoomTypes(DataFetchingEnvironment dfe) {
        Hotel hotel = dfe.getSource();
        return dataSource.getRoomTypesByHotelId(hotel.getId());
    }

    @DgsData(parentType = "Hotel", field = "reviews")
    public Map<String, Object> hotelReviews(DataFetchingEnvironment dfe) {
        Hotel hotel = dfe.getSource();
        String sortBy = dfe.getArgument("sortBy");
        Integer first = dfe.getArgument("first");
        String after = dfe.getArgument("after");
        List<Review> all = dataSource.getReviewsByHotelId(hotel.getId(), sortBy);
        return buildReviewConnection(all, first, after);
    }

    @DgsData(parentType = "Hotel", field = "media")
    public Map<String, Object> hotelMedia(DataFetchingEnvironment dfe) {
        Hotel hotel = dfe.getSource();
        Integer first = dfe.getArgument("first");
        List<MediaAsset> all = hotel.getMedia();
        if (first != null) all = all.stream().limit(first).collect(Collectors.toList());
        return buildMediaConnection(all);
    }

    @DgsData(parentType = "Brand", field = "featuredHotels")
    public List<Hotel> brandFeaturedHotels(DataFetchingEnvironment dfe) {
        Brand brand = dfe.getSource();
        Integer first = dfe.getArgument("first");
        return dataSource.searchHotels(null, null).stream()
                .filter(h -> brand.getId().equals(h.getBrandId()) && h.getIsFeatured())
                .limit(first != null ? first : 6)
                .collect(Collectors.toList());
    }

    @DgsData(parentType = "Brand", field = "hotels")
    public Map<String, Object> brandHotels(DataFetchingEnvironment dfe) {
        Brand brand = dfe.getSource();
        Integer first = dfe.getArgument("first");
        String after = dfe.getArgument("after");
        List<Hotel> all = dataSource.searchHotels(null, null).stream()
                .filter(h -> brand.getId().equals(h.getBrandId()))
                .collect(Collectors.toList());
        return buildConnection(all, first, after);
    }

    @DgsData(parentType = "PropertyLocalizedContent", field = "text")
    public String localizedText(DataFetchingEnvironment dfe) {
        LocalizedContent content = dfe.getSource();
        String locale = dfe.getArgument("locale");
        return content.text(locale);
    }

    @DgsData(parentType = "PropertyLocalizedContent", field = "availableLocales")
    public List<String> availableLocales(DataFetchingEnvironment dfe) {
        LocalizedContent content = dfe.getSource();
        return content.getAvailableLocales();
    }

    // ── Entity Fetchers ───────────────────────────────────────────────────────

    @DgsEntityFetcher(name = "Hotel")
    public Hotel fetchHotelEntity(Map<String, Object> values) {
        return dataSource.getHotelById((String) values.get("id")).orElse(null);
    }

    @DgsEntityFetcher(name = "Brand")
    public Brand fetchBrandEntity(Map<String, Object> values) {
        return dataSource.getBrandById((String) values.get("id")).orElse(null);
    }

    @DgsEntityFetcher(name = "RoomType")
    public RoomType fetchRoomTypeEntity(Map<String, Object> values) {
        return dataSource.getRoomTypeById((String) values.get("id")).orElse(null);
    }

    // ── Connection helpers ────────────────────────────────────────────────────

    private Map<String, Object> buildConnection(List<Hotel> all, Integer first, String after) {
        int limit = first != null ? first : 10;
        int start = 0;
        if (after != null) {
            String decoded = new String(Base64.getDecoder().decode(after));
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getId().equals(decoded)) { start = i + 1; break; }
            }
        }
        List<Hotel> page = all.stream().skip(start).limit(limit).collect(Collectors.toList());
        List<Map<String, Object>> edges = page.stream().map(h -> Map.<String,Object>of(
                "node", h, "cursor", Base64.getEncoder().encodeToString(h.getId().getBytes())
        )).collect(Collectors.toList());
        return Map.of("edges", edges, "pageInfo", pageInfo(edges, start, all.size(), page.size()), "totalCount", all.size());
    }

    private Map<String, Object> buildBrandConnection(List<Brand> all, Integer first, String after) {
        int limit = first != null ? first : 20;
        List<Brand> page = all.stream().limit(limit).collect(Collectors.toList());
        List<Map<String, Object>> edges = page.stream().map(b -> Map.<String,Object>of(
                "node", b, "cursor", Base64.getEncoder().encodeToString(b.getId().getBytes())
        )).collect(Collectors.toList());
        return Map.of("edges", edges, "pageInfo", pageInfo(edges, 0, all.size(), page.size()), "totalCount", all.size());
    }

    private Map<String, Object> buildReviewConnection(List<Review> all, Integer first, String after) {
        int limit = first != null ? first : 10;
        List<Review> page = all.stream().limit(limit).collect(Collectors.toList());
        List<Map<String, Object>> edges = page.stream().map(r -> Map.<String,Object>of(
                "node", r, "cursor", Base64.getEncoder().encodeToString(r.getId().getBytes())
        )).collect(Collectors.toList());
        double avg = all.stream().mapToDouble(Review::getOverallRating).average().orElse(0.0);
        Map<String, Object> conn = new HashMap<>();
        conn.put("edges", edges);
        conn.put("pageInfo", pageInfo(edges, 0, all.size(), page.size()));
        conn.put("totalCount", all.size());
        conn.put("averageRating", avg > 0 ? avg : null);
        return conn;
    }

    private Map<String, Object> buildMediaConnection(List<MediaAsset> assets) {
        List<Map<String, Object>> edges = assets.stream().map(m -> Map.<String,Object>of(
                "node", m, "cursor", Base64.getEncoder().encodeToString(m.id().getBytes())
        )).collect(Collectors.toList());
        return Map.of("edges", edges, "pageInfo", pageInfo(edges, 0, assets.size(), assets.size()));
    }

    private Map<String, Object> pageInfo(List<Map<String,Object>> edges, int start, int total, int pageSize) {
        Map<String, Object> pi = new HashMap<>();
        pi.put("hasNextPage", start + pageSize < total);
        pi.put("hasPreviousPage", start > 0);
        pi.put("startCursor", edges.isEmpty() ? null : edges.get(0).get("cursor"));
        pi.put("endCursor", edges.isEmpty() ? null : edges.get(edges.size()-1).get("cursor"));
        return pi;
    }
}
