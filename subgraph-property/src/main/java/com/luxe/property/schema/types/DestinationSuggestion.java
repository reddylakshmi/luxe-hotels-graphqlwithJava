package com.luxe.property.schema.types;

/**
 * One autocomplete entry for the destination search box. Carries just enough
 * fields for the UI to render the row and act on a click — no media, no rates,
 * no nested objects. See schema.graphqls for the matching GraphQL type.
 */
public record DestinationSuggestion(
        String type,         // "HOTEL" | "CITY" | "COUNTRY"
        String label,
        String sublabel,
        String hotelId,
        String hotelSlug,
        String city,
        String country,
        String countryCode
) {

    public static DestinationSuggestion hotel(Hotel h) {
        Address addr = h.getLocation().address();
        String sub = (addr.line1() != null ? addr.line1() + ", " : "")
                + addr.city()
                + (addr.countryName() != null ? ", " + addr.countryName() : "");
        return new DestinationSuggestion(
                "HOTEL", h.getName(), sub,
                h.getId(), h.getSlug(),
                addr.city(), addr.countryName(), addr.countryCode());
    }

    public static DestinationSuggestion city(String city, String country, String countryCode, int hotelCount) {
        String sub = country + " · " + hotelCount + " hotel" + (hotelCount == 1 ? "" : "s");
        return new DestinationSuggestion(
                "CITY", city, sub, null, null, city, country, countryCode);
    }

    public static DestinationSuggestion country(String country, String countryCode, int hotelCount) {
        String sub = hotelCount + " hotel" + (hotelCount == 1 ? "" : "s");
        return new DestinationSuggestion(
                "COUNTRY", country, sub, null, null, null, country, countryCode);
    }
}
