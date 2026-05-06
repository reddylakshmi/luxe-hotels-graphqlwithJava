package com.luxe.property.datasource;

import com.luxe.property.schema.types.*;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PropertyMockDataSource implements PropertyDataSource {

    private final Map<String, Hotel> hotels = new LinkedHashMap<>();
    private final Map<String, Brand> brands = new LinkedHashMap<>();
    private final Map<String, RoomType> roomTypes = new LinkedHashMap<>();
    private final Map<String, Restaurant> restaurants = new LinkedHashMap<>();
    private final Map<String, Spa> spas = new LinkedHashMap<>();
    private final Map<String, Review> reviews = new LinkedHashMap<>();

    public PropertyMockDataSource() { initData(); }

    private static Amenity am(String id, String code, String name, String cat, String desc, boolean premium) {
        return new Amenity(id, code, name, cat, desc, null, premium, null, null);
    }

    private static MediaAsset media(String id, String url, String cat, boolean primary, int order) {
        return new MediaAsset(id, url, url.replace(".jpg","_thumb.jpg"), cat, null, null, primary, order);
    }

    private void initData() {
        OffsetDateTime now = OffsetDateTime.now();

        Brand lux = new Brand("brand-lux-001","LUX","Luxe Collection","luxe-collection","LUXURY",
                "Where Luxury Lives","The pinnacle of luxury hospitality.",
                null,null,"#C9A96E",3.0,4,
                "Carbon neutral by 2030.",now.minusYears(10),now);
        Brand prm = new Brand("brand-prm-001","PRM","Luxe Premium","luxe-premium","PREMIUM",
                "Premium Comfort, Every Stay","Elevated comfort for discerning travellers.",
                null,null,"#4A6FA5",2.0,1,
                "Green-certified properties.",now.minusYears(5),now);
        brands.put(lux.getId(), lux);
        brands.put(prm.getId(), prm);

        Amenity pool  = am("am-pool","POOL","Infinity Pool","POOL","Heated infinity pool.",false);
        Amenity spa   = am("am-spa","SPA","Luxury Spa","SPA","Full-service luxury spa.",true);
        Amenity gym   = am("am-gym","GYM","Fitness Centre","FITNESS","24h fitness centre.",false);
        Amenity wifi  = am("am-wifi","WIFI","Free WiFi","CONNECTIVITY","High-speed WiFi throughout.",false);
        Amenity valet = am("am-valet","VALET","Valet Parking","PARKING","24h valet parking.",true);
        Amenity conc  = am("am-conc","CONC","24h Concierge","BUSINESS","Around-the-clock concierge.",false);

        // ── Paris ─────────────────────────────────────────────────────────────
        Spa parisSpa = new Spa("spa-paris-001","prop-paris-001","Palais Spa & Wellness",
                LocalizedContent.of("Award-winning spa blending Parisian elegance with ancient wellness traditions."),
                "06:00–22:00",true);
        spas.put(parisSpa.id(), parisSpa);
        Restaurant parisRest = new Restaurant("rest-paris-001","prop-paris-001","Le Jardin d'Or",
                List.of("Modern French","Haute Cuisine"),
                LocalizedContent.of("Three-Michelin-starred restaurant helmed by Chef Julien Marchand."),
                "Smart Casual","12:00–14:30 | 19:00–22:30","€€€€",true,true);
        restaurants.put(parisRest.id(), parisRest);

        Hotel paris = new Hotel("prop-paris-001","brand-lux-001","LUX","PAR001",
                "The Grand Palais Paris","grand-palais-paris","ACTIVE",
                1910,2022,8,185,5,
                new GuestRating(9.4,2847,new RatingBreakdown(1980,612,201,42,12),
                        List.of(new TravelerTypeRating("Couples",9.6,1200),new TravelerTypeRating("Business",9.2,800))),
                new HotelLocation(
                        new Address("5 Avenue Montaigne",null,"Paris","Île-de-France","75008","FR","France"),
                        new Coordinates(48.8674,2.3065),"Europe/Paris",
                        new AirportInfo("CDG","Paris Charles de Gaulle",34.0,45),null),
                new HotelContact("+33-1-5567-8900","paris@luxehotels.com","https://paris.luxehotels.com","+33-1-5567-8901",null),
                LocalizedContent.of("An iconic palace hotel on Avenue Montaigne. Since 1910, The Grand Palais Paris has defined the art of luxury hospitality."),
                new HotelPolicies(
                        new HotelPolicies.CheckInPolicy("15:00",new HotelPolicies.EarlyLatePolicy(true,"EUR 50",true)),
                        new HotelPolicies.CheckOutPolicy("12:00",new HotelPolicies.EarlyLatePolicy(true,"EUR 50",true)),
                        new HotelPolicies.CancellationPolicy(48,"Free cancellation up to 48h before arrival."),
                        new HotelPolicies.PetPolicy(false,null,"No pets allowed."),"Non-smoking throughout.","Children of all ages welcome."),
                List.of(pool,spa,gym,wifi,valet,conc),new ArrayList<>(),
                List.of(parisRest),parisSpa,null,
                List.of(media("m-par-1","https://cdn.luxe.com/paris/exterior.jpg","EXTERIOR",true,1),
                        media("m-par-2","https://cdn.luxe.com/paris/lobby.jpg","INTERIOR",false,2)),
                List.of(new Attraction("a-par-1","Eiffel Tower","Landmark",1.8,4.9),new Attraction("a-par-2","Louvre Museum","Museum",2.1,4.8)),
                new ParkingInfo(true,"Underground",true,"EUR 65/day","EUR 45/day",true),
                new SustainabilityInfo(92,List.of(new SustainabilityInfo.SustainabilityCertification("Green Globe","Green Globe International",2023)),true,80,65),
                List.of(new Award("Best Luxury Hotel Paris 2024","Condé Nast Traveller",2024),new Award("Forbes Five-Star","Forbes Travel Guide",2024)),
                true,now);
        hotels.put(paris.getId(), paris);

        // ── Tokyo ─────────────────────────────────────────────────────────────
        Spa tokyoSpa = new Spa("spa-tokyo-001","prop-tokyo-001","Ikigai Wellness Sanctuary",
                LocalizedContent.of("Ancient Japanese healing meets modern wellness science."),"07:00–23:00",true);
        spas.put(tokyoSpa.id(), tokyoSpa);
        Restaurant tokyoRest = new Restaurant("rest-tokyo-001","prop-tokyo-001","Hana",
                List.of("Kaiseki","Modern Japanese"),LocalizedContent.of("Multi-course kaiseki with seasonal ingredients."),
                "Formal","18:00–22:00","¥¥¥¥",true,true);
        restaurants.put(tokyoRest.id(), tokyoRest);

        Hotel tokyo = new Hotel("prop-tokyo-001","brand-lux-001","LUX","TYO001",
                "Luxe Tokyo Imperial","luxe-tokyo-imperial","ACTIVE",
                2005,2021,35,247,5,
                new GuestRating(9.6,3102,new RatingBreakdown(2400,520,142,30,10),
                        List.of(new TravelerTypeRating("Couples",9.7,1400),new TravelerTypeRating("Business",9.5,900))),
                new HotelLocation(
                        new Address("1-1 Marunouchi",null,"Tokyo",null,"100-0005","JP","Japan"),
                        new Coordinates(35.6812,139.7671),"Asia/Tokyo",
                        new AirportInfo("HND","Tokyo Haneda",18.0,35),null),
                new HotelContact("+81-3-5224-1111","tokyo@luxehotels.com","https://tokyo.luxehotels.com","+81-3-5224-1112",null),
                LocalizedContent.of("Overlooking the Imperial Palace Gardens, Luxe Tokyo Imperial blends Japanese aesthetics with contemporary luxury."),
                null,List.of(pool,spa,gym,wifi,conc),new ArrayList<>(),
                List.of(tokyoRest),tokyoSpa,null,
                List.of(media("m-tok-1","https://cdn.luxe.com/tokyo/exterior.jpg","EXTERIOR",true,1)),
                List.of(new Attraction("a-tok-1","Imperial Palace","Landmark",0.3,4.8)),
                new ParkingInfo(true,"Underground",true,"JPY 5000/day",null,true),
                new SustainabilityInfo(88,List.of(),true,70,80),
                List.of(new Award("Best Hotel Japan 2024","Travel + Leisure",2024)),true,now);
        hotels.put(tokyo.getId(), tokyo);

        // ── Dubai ─────────────────────────────────────────────────────────────
        Spa dubaiSpa = new Spa("spa-dubai-001","prop-dubai-001","Zenith Spa",
                LocalizedContent.of("3,000 sqm sanctuary inspired by Arabian wellness traditions."),"06:00–23:00",true);
        spas.put(dubaiSpa.id(), dubaiSpa);
        Restaurant dubaiRest = new Restaurant("rest-dubai-001","prop-dubai-001","Saffron",
                List.of("Contemporary Middle Eastern"),LocalizedContent.of("Panoramic views of the Burj Khalifa."),
                "Smart Casual","07:00–23:00","AED AED AED",false,true);
        restaurants.put(dubaiRest.id(), dubaiRest);

        Hotel dubai = new Hotel("prop-dubai-001","brand-lux-001","LUX","DXB001",
                "Luxe Dubai Palace","luxe-dubai-palace","ACTIVE",
                2018,null,72,420,5,
                new GuestRating(9.3,2210,new RatingBreakdown(1650,410,110,30,10),
                        List.of(new TravelerTypeRating("Couples",9.4,1100))),
                new HotelLocation(
                        new Address("Sheikh Zayed Road",null,"Dubai",null,null,"AE","United Arab Emirates"),
                        new Coordinates(25.1972,55.2744),"Asia/Dubai",
                        new AirportInfo("DXB","Dubai International",15.0,20),null),
                new HotelContact("+971-4-555-8000","dubai@luxehotels.com","https://dubai.luxehotels.com","+971-4-555-8001","+971-50-555-8000"),
                LocalizedContent.of("A modern palace rising above Downtown Dubai with unrivalled views of the Burj Khalifa."),
                null,List.of(pool,spa,gym,wifi,valet,conc),new ArrayList<>(),
                List.of(dubaiRest),dubaiSpa,null,
                List.of(media("m-dub-1","https://cdn.luxe.com/dubai/exterior.jpg","EXTERIOR",true,1)),
                List.of(new Attraction("a-dub-1","Burj Khalifa","Landmark",0.5,4.9)),
                new ParkingInfo(true,"Multi-storey",true,"AED 120/day",null,true),
                new SustainabilityInfo(78,List.of(),false,30,40),
                List.of(new Award("Best Luxury Hotel UAE 2024","World Travel Awards",2024)),true,now);
        hotels.put(dubai.getId(), dubai);

        // ── NYC ───────────────────────────────────────────────────────────────
        Spa nycSpa = new Spa("spa-nyc-001","prop-nyc-001","The Luxe Spa New York",
                LocalizedContent.of("A serene urban escape featuring New York-exclusive treatments."),"06:30–22:00",true);
        spas.put(nycSpa.id(), nycSpa);
        Restaurant nycRest = new Restaurant("rest-nyc-001","prop-nyc-001","Park & Fifth",
                List.of("Contemporary American"),LocalizedContent.of("New York's celebrated steakhouse with Central Park views."),
                "Smart Casual","07:00–23:00","$$$$",false,true);
        restaurants.put(nycRest.id(), nycRest);

        Hotel nyc = new Hotel("prop-nyc-001","brand-prm-001","PRM","NYC001",
                "The Luxe New York","luxe-new-york","ACTIVE",
                1998,2020,42,320,5,
                new GuestRating(9.1,1875,new RatingBreakdown(1350,380,110,25,10),
                        List.of(new TravelerTypeRating("Business",9.3,900))),
                new HotelLocation(
                        new Address("768 Fifth Avenue",null,"New York","NY","10019","US","United States"),
                        new Coordinates(40.7648,-73.9734),"America/New_York",
                        new AirportInfo("JFK","John F. Kennedy International",24.0,45),null),
                new HotelContact("+1-212-555-9000","nyc@luxehotels.com","https://nyc.luxehotels.com","+1-212-555-9001",null),
                LocalizedContent.of("Commanding Fifth Avenue and Central Park South — the city's most coveted address since 1998."),
                null,List.of(gym,wifi,valet,conc),new ArrayList<>(),
                List.of(nycRest),nycSpa,null,
                List.of(media("m-nyc-1","https://cdn.luxe.com/nyc/exterior.jpg","EXTERIOR",true,1)),
                List.of(new Attraction("a-nyc-1","Central Park","Park",0.1,4.9)),
                new ParkingInfo(true,"Valet only",true,"USD 95/day",null,false),
                new SustainabilityInfo(82,List.of(new SustainabilityInfo.SustainabilityCertification("LEED Gold","USGBC",2022)),true,60,50),
                List.of(new Award("Best Hotel New York 2024","Condé Nast Traveller",2024)),false,now);
        hotels.put(nyc.getId(), nyc);

        // ── London ────────────────────────────────────────────────────────────
        Spa londonSpa = new Spa("spa-london-001","prop-london-001","The Mayfair Spa",
                LocalizedContent.of("A discreet Mayfair sanctuary offering world-class treatments."),"07:00–21:00",true);
        spas.put(londonSpa.id(), londonSpa);
        Restaurant londonRest = new Restaurant("rest-london-001","prop-london-001","The Garden Room",
                List.of("Modern British"),LocalizedContent.of("Seasonal British cuisine from our own kitchen garden."),
                "Smart Casual","07:00–22:30","££££",false,true);
        restaurants.put(londonRest.id(), londonRest);

        Hotel london = new Hotel("prop-london-001","brand-lux-001","LUX","LON001",
                "The Luxe London Mayfair","luxe-london-mayfair","ACTIVE",
                1924,2019,7,152,5,
                new GuestRating(9.5,2430,new RatingBreakdown(1900,380,120,20,10),
                        List.of(new TravelerTypeRating("Couples",9.6,1200))),
                new HotelLocation(
                        new Address("45 Park Lane",null,"London","England","W1K 1PN","GB","United Kingdom"),
                        new Coordinates(51.5045,-0.1522),"Europe/London",
                        new AirportInfo("LHR","London Heathrow",24.0,50),null),
                new HotelContact("+44-20-7629-8888","london@luxehotels.com","https://london.luxehotels.com","+44-20-7629-8889",null),
                LocalizedContent.of("A landmark of British elegance overlooking Hyde Park, preferred by royalty and statesmen since 1924."),
                null,List.of(spa,gym,wifi,conc),new ArrayList<>(),
                List.of(londonRest),londonSpa,null,
                List.of(media("m-lon-1","https://cdn.luxe.com/london/exterior.jpg","EXTERIOR",true,1)),
                List.of(new Attraction("a-lon-1","Hyde Park","Park",0.1,4.8)),
                new ParkingInfo(true,"Valet",true,"GBP 75/day",null,false),
                new SustainabilityInfo(86,List.of(new SustainabilityInfo.SustainabilityCertification("Green Tourism Gold","Green Tourism",2023)),true,75,60),
                List.of(new Award("Best Hotel London 2024","Travel + Leisure",2024),new Award("Forbes Five-Star","Forbes Travel Guide",2024)),
                true,now);
        hotels.put(london.getId(), london);

        // ── Room Types ────────────────────────────────────────────────────────
        addRoom("rt-paris-deluxe","prop-paris-001","DLX","Deluxe Room","DELUXE",
                "Elegant room with Haussmann views.",45.0,new OccupancyLimit(2,1,3),
                List.of(new BedConfiguration("KING",1)),"City View","3-5",
                List.of("Nespresso","Marble bathroom","Hermès amenities"),false,false);
        addRoom("rt-paris-suite","prop-paris-001","JRS","Junior Suite","JUNIOR_SUITE",
                "Spacious suite with Eiffel Tower views.",80.0,new OccupancyLimit(2,2,4),
                List.of(new BedConfiguration("KING",1)),"Eiffel Tower View","6-8",
                List.of("Separate living room","Butler service","Walk-in wardrobe"),false,true);
        addRoom("rt-tokyo-deluxe","prop-tokyo-001","DLX","Deluxe Room","DELUXE",
                "Contemporary Japanese-inspired room.",38.0,new OccupancyLimit(2,1,3),
                List.of(new BedConfiguration("KING",1)),"Garden View","10-20",
                List.of("Japanese soaking tub","Tokyo skyline view"),false,false);
        addRoom("rt-tokyo-suite","prop-tokyo-001","STE","Imperial Suite","SUITE",
                "Panoramic Tokyo views with private terrace.",120.0,new OccupancyLimit(2,2,4),
                List.of(new BedConfiguration("KING",1)),"Tokyo Panorama","30-35",
                List.of("Private terrace","Grand piano","Personal butler"),false,false);
        addRoom("rt-dubai-deluxe","prop-dubai-001","DLX","Deluxe Room","DELUXE",
                "Luxurious room with Burj Khalifa views.",52.0,new OccupancyLimit(2,1,3),
                List.of(new BedConfiguration("KING",1)),"Burj Khalifa View","20-40",
                List.of("Rain shower","Smart home system"),false,false);
        addRoom("rt-nyc-deluxe","prop-nyc-001","DLX","Central Park View Room","DELUXE",
                "Sophisticated room with Central Park views.",40.0,new OccupancyLimit(2,1,3),
                List.of(new BedConfiguration("KING",1)),"Central Park View","15-30",
                List.of("Park panorama","Marble bathroom"),false,false);
        addRoom("rt-london-deluxe","prop-london-001","DLX","Hyde Park Deluxe","DELUXE",
                "Elegant room with Hyde Park views.",42.0,new OccupancyLimit(2,1,3),
                List.of(new BedConfiguration("KING",1)),"Hyde Park View","2-7",
                List.of("Penhaligon's amenities","Roll-top bath"),false,false);

        // ── Reviews ───────────────────────────────────────────────────────────
        addReview("rev-001","prop-paris-001","Sophia C.","2025-03",9.8,
                new CategoryRatings(10.0,9.8,9.5,9.0,9.9,9.7),
                "Absolutely magnificent","The Grand Palais Paris exceeded every expectation. Service is unrivalled.","en",true,142);
        addReview("rev-002","prop-tokyo-001","Yuki T.","2025-04",9.9,
                new CategoryRatings(10.0,10.0,9.8,9.5,10.0,9.8),
                "Best hotel in Tokyo","Perfection in every detail. The kaiseki dinner was transcendent.","en",true,201);
        addReview("rev-003","prop-london-001","Diana O.","2025-01",9.6,
                new CategoryRatings(9.8,9.7,9.5,8.8,9.7,9.5),
                "London's finest","The Mayfair Spa and afternoon tea are worth the visit alone.","en",true,117);
        addReview("rev-004","prop-dubai-001","Fatima A.","2025-03",9.3,
                new CategoryRatings(9.5,9.4,9.0,8.5,9.5,9.2),
                "Stunning in every way","The views of the Burj Khalifa from our suite were breathtaking.","en",true,88);
    }

    private void addRoom(String id, String hotelId, String code, String name, String category,
                         String desc, double sqm, OccupancyLimit occ, List<BedConfiguration> beds,
                         String view, String floor, List<String> highlights, boolean smoking, boolean connecting) {
        RoomType rt = new RoomType(id, hotelId, code, name, category,
                LocalizedContent.of(desc), sqm, occ, beds, view, floor, List.of(),
                List.of(media(id+"-img","https://cdn.luxe.com/rooms/"+id+".jpg","ROOM",true,1)),
                List.of(), highlights, smoking, connecting);
        roomTypes.put(id, rt);
        Hotel h = hotels.get(hotelId);
        if (h != null) h.getRoomTypes().add(rt);
    }

    private void addReview(String id, String hotelId, String guestName, String stayDate,
                            double rating, CategoryRatings cats, String title, String body,
                            String lang, boolean verified, int helpful) {
        reviews.put(id, new Review(id, hotelId, guestName, stayDate, rating, cats, title, body, lang, verified, helpful, null));
    }

    @Override public Optional<Hotel> getHotelById(String id) { return Optional.ofNullable(hotels.get(id)); }
    @Override public Optional<Hotel> getHotelBySlug(String slug) {
        return hotels.values().stream().filter(h -> slug.equals(h.getSlug())).findFirst();
    }
    @Override public List<Hotel> searchHotels(Map<String, Object> filter, String sortBy) {
        List<Hotel> result = new ArrayList<>(hotels.values());
        if (filter != null) {
            String query = (String) filter.get("query");
            if (query != null) { String q = query.toLowerCase();
                result = result.stream().filter(h -> h.getName().toLowerCase().contains(q)
                        || h.getLocation().address().city().toLowerCase().contains(q)).collect(Collectors.toList()); }
            Integer minStar = (Integer) filter.get("minStarRating");
            if (minStar != null) result = result.stream().filter(h -> h.getStarRating() >= minStar).collect(Collectors.toList());
            if (Boolean.TRUE.equals(filter.get("hasSpa"))) result = result.stream().filter(Hotel::isHasSpa).collect(Collectors.toList());
            if (Boolean.TRUE.equals(filter.get("hasPool"))) result = result.stream().filter(Hotel::isHasPool).collect(Collectors.toList());
        }
        if ("GUEST_RATING".equals(sortBy)) result.sort((a, b) -> Double.compare(
                b.getGuestRating() != null ? b.getGuestRating().overall() : 0,
                a.getGuestRating() != null ? a.getGuestRating().overall() : 0));
        else if ("NAME".equals(sortBy)) result.sort(Comparator.comparing(Hotel::getName));
        return result;
    }
    @Override public List<Hotel> getFeaturedHotels(String brandTier, String countryCode, int limit) {
        return hotels.values().stream().filter(Hotel::getIsFeatured)
                .filter(h -> countryCode == null || countryCode.equals(h.getLocation().address().countryCode()))
                .limit(limit).collect(Collectors.toList());
    }
    @Override public Optional<Brand> getBrandById(String id) { return Optional.ofNullable(brands.get(id)); }
    @Override public Optional<Brand> getBrandByCode(String code) {
        return brands.values().stream().filter(b -> code.equals(b.getCode())).findFirst();
    }
    @Override public List<Brand> getAllBrands(String tier) {
        return brands.values().stream().filter(b -> tier == null || tier.equals(b.getTier())).collect(Collectors.toList());
    }
    @Override public Optional<RoomType> getRoomTypeById(String id) { return Optional.ofNullable(roomTypes.get(id)); }
    @Override public List<RoomType> getRoomTypesByHotelId(String hotelId) {
        return roomTypes.values().stream().filter(r -> hotelId.equals(r.getHotelId())).collect(Collectors.toList());
    }
    @Override public Optional<Restaurant> getRestaurantById(String id) { return Optional.ofNullable(restaurants.get(id)); }
    @Override public Optional<Spa> getSpaById(String id) { return Optional.ofNullable(spas.get(id)); }
    @Override public List<Review> getReviewsByHotelId(String hotelId, String sortBy) {
        List<Review> result = reviews.values().stream().filter(r -> hotelId.equals(r.getHotelId())).collect(Collectors.toList());
        if ("HIGHEST_RATED".equals(sortBy)) result.sort((a,b) -> Double.compare(b.getOverallRating(),a.getOverallRating()));
        else if ("MOST_HELPFUL".equals(sortBy)) result.sort((a,b) -> b.getHelpfulCount()-a.getHelpfulCount());
        return result;
    }
    @Override public Optional<Review> getReviewById(String id) { return Optional.ofNullable(reviews.get(id)); }
    @Override public Review submitReview(String hotelId, String guestName, double overallRating,
                                         Map<String,Object> cats, String title, String body, String stayDate) {
        CategoryRatings cr = null;
        if (cats != null) cr = new CategoryRatings(
                cats.get("cleanliness") instanceof Number n ? n.doubleValue() : null,
                cats.get("service") instanceof Number n ? n.doubleValue() : null,
                cats.get("location") instanceof Number n ? n.doubleValue() : null,
                cats.get("value") instanceof Number n ? n.doubleValue() : null,
                cats.get("comfort") instanceof Number n ? n.doubleValue() : null, null);
        String id = "rev-" + UUID.randomUUID().toString().substring(0,8);
        Review r = new Review(id, hotelId, guestName, stayDate, overallRating, cr, title, body, "en", false, 0, null);
        reviews.put(id, r);
        return r;
    }
    @Override public Optional<Review> markReviewHelpful(String reviewId) {
        Review r = reviews.get(reviewId);
        if (r != null) r.incrementHelpful();
        return Optional.ofNullable(r);
    }
}
