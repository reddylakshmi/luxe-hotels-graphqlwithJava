package com.luxe.property.datasource;

/**
 * Base URLs for media assets emitted by the property subgraph's mock data.
 * Centralised so the CDN host can be swapped in one place if/when the demo
 * gets pointed at a different CDN. Kept package-private — only the data
 * sources should be assembling these URLs.
 *
 * No magic numbers / strings beyond the base host; everything else (slug,
 * filename) is derived from the hotel/room identifier the asset belongs to.
 */
final class MediaUrls {

    /** Base of every emitted media URL. Swap here to retarget the demo's CDN. */
    static final String CDN_BASE = "https://cdn.luxe.com/";

    private MediaUrls() {}

    static String brandLogoUrl(String brandSlug) {
        return CDN_BASE + "brands/" + brandSlug + "-logo.svg";
    }

    static String brandHeroUrl(String brandSlug) {
        return CDN_BASE + "brands/" + brandSlug + "-hero.jpg";
    }

    /** Synthetic hotel exterior URL — used by the property generator. */
    static String hotelExteriorUrl(String brandSlug, String citySlug) {
        return CDN_BASE + brandSlug + "/" + citySlug + "/exterior.jpg";
    }

    static String hotelExteriorThumbUrl(String brandSlug, String citySlug) {
        return CDN_BASE + brandSlug + "/" + citySlug + "/exterior_thumb.jpg";
    }

    static String hotelLobbyUrl(String brandSlug, String citySlug) {
        return CDN_BASE + brandSlug + "/" + citySlug + "/lobby.jpg";
    }

    static String hotelLobbyThumbUrl(String brandSlug, String citySlug) {
        return CDN_BASE + brandSlug + "/" + citySlug + "/lobby_thumb.jpg";
    }

    static String roomImageUrl(String roomId) {
        return CDN_BASE + "rooms/" + roomId + ".jpg";
    }

    static String roomImageThumbUrl(String roomId) {
        return CDN_BASE + "rooms/" + roomId + "_thumb.jpg";
    }

    /** Used by hand-curated India hotels — paths under /india/{slug}/. */
    static String indiaHotelImageUrl(String slug, String filename) {
        return CDN_BASE + "india/" + slug + "/" + filename;
    }

    /** Used by hand-curated Paris/Tokyo/Dubai/NYC/London hotels. */
    static String legacyHotelImageUrl(String city, String filename) {
        return CDN_BASE + city + "/" + filename;
    }
}
