package com.luxe.content.datasource;

import com.luxe.common.scalar.Money;
import com.luxe.content.schema.types.*;
import com.luxe.content.schema.types.LocalizedContent.LocaleText;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@ConditionalOnExpression("'${luxe.backend.base-url:}'.length() == 0")
public class ContentMockDataSource implements ContentDataSource {

    private final Map<String, Article> articles = new LinkedHashMap<>();
    private final Map<String, TravelInspiration> inspirations = new LinkedHashMap<>();
    private final Map<String, DealSpotlight> spotlights = new LinkedHashMap<>();
    private final Map<String, ContentCollection> collections = new LinkedHashMap<>();
    private final BrandStory brandStory;

    public ContentMockDataSource() {
        initArticles();
        initInspirations();
        initSpotlights();
        initCollections();
        this.brandStory = buildBrandStory();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private MediaAsset photo(String id, String url, String alt) {
        return new MediaAsset(id, url, url + "?w=480",
                LocalizedContent.ofEnglish(alt), null,
                "PHOTO", 1920, 1080, null);
    }

    private List<LocaleText> tr(String en, String fr, String es) {
        List<LocaleText> out = new ArrayList<>();
        out.add(new LocaleText("en", en));
        if (fr != null) out.add(new LocaleText("fr", fr));
        if (es != null) out.add(new LocaleText("es", es));
        return out;
    }

    private List<LocaleText> en(String text) {
        return List.of(new LocaleText("en", text));
    }

    // ── Articles ─────────────────────────────────────────────────────────────

    private void initArticles() {
        ContentAuthor camille = new ContentAuthor("auth-001", "Camille Laurent",
                "Senior Travel Editor",
                LocalizedContent.ofEnglish("Camille has covered luxury travel across 60+ countries."),
                "https://content.luxehotels.example/auth/camille.jpg");
        ContentAuthor marcus = new ContentAuthor("auth-002", "Marcus Chen",
                "Wellness Correspondent",
                LocalizedContent.ofEnglish("Marcus writes about wellness, mindfulness, and slow travel."),
                "https://content.luxehotels.example/auth/marcus.jpg");
        ContentAuthor sofia = new ContentAuthor("auth-003", "Sofía Reyes",
                "Food & Wine Editor",
                LocalizedContent.ofEnglish("Sofía explores Michelin-starred kitchens and hidden bistros alike."),
                "https://content.luxehotels.example/auth/sofia.jpg");

        OffsetDateTime now = OffsetDateTime.now();

        articles.put("art-001", new Article("art-001", "paris-by-night",
                "DESTINATION", "PUBLISHED",
                tr("Paris After Dark: A Curator's Guide",
                        "Paris la nuit : le guide d'un connaisseur",
                        "París de noche: la guía del experto"),
                tr("Twilight bistros, jazz cellars, and rooftop views",
                        "Bistros, caves de jazz et vues sur les toits",
                        "Bistrós, sótanos de jazz y vistas desde la azotea"),
                tr("Paris reveals its quietest secrets after the last metro. From a candle-lit terrace overlooking Notre-Dame to an unmarked jazz cave in the Marais, the night belongs to those who know where to look. ...",
                        "Paris révèle ses secrets les plus calmes après le dernier métro. ...",
                        "París revela sus secretos más íntimos tras el último metro. ..."),
                tr("A curator's guide to the city's twilight hours.",
                        "Le guide d'un curateur pour les heures crépusculaires.",
                        "La guía de un curador para las horas crepusculares."),
                photo("img-paris-night-01", "https://content.luxehotels.example/articles/paris-night.jpg",
                        "Lit Eiffel Tower over Seine"),
                List.of(photo("img-paris-night-02", "https://content.luxehotels.example/articles/paris-marais.jpg",
                        "Marais alley after rain")),
                camille, List.of("Paris", "Nightlife", "Romantic"),
                List.of("prop-paris-001"), 8,
                now.minusDays(20), now.minusDays(1)));

        articles.put("art-002", new Article("art-002", "tokyo-omakase-renaissance",
                "FOOD_AND_WINE", "PUBLISHED",
                tr("The Tokyo Omakase Renaissance",
                        "La renaissance de l'omakase à Tokyo",
                        "El renacimiento del omakase en Tokio"),
                tr("Twelve-seat counters and the chefs reshaping them", null, null),
                tr("In a city that has long defined sushi excellence, a quiet revolution is taking place at twelve-seat counters from Ginza to Nakameguro. ...",
                        null, null),
                tr("Twelve-seat counters reshape Tokyo's edible storytelling.",
                        null, null),
                photo("img-tokyo-sushi-01",
                        "https://content.luxehotels.example/articles/tokyo-omakase.jpg",
                        "Omakase counter, Ginza"),
                List.of(),
                sofia, List.of("Tokyo", "Sushi", "Food"),
                List.of("prop-tokyo-001"), 6,
                now.minusDays(45), now.minusDays(12)));

        articles.put("art-003", new Article("art-003", "wellness-without-the-noise",
                "WELLNESS", "PUBLISHED",
                tr("Wellness Without the Noise", null, null),
                tr("Why the next generation of luxury wellness is quiet, slow, and deeply personal", null, null),
                tr("Forget icy plunges and frantic biohacking. The next era of wellness is about doing less, with intention. ...", null, null),
                tr("A new era of luxury wellness — quieter, slower, more personal.", null, null),
                photo("img-wellness-01",
                        "https://content.luxehotels.example/articles/wellness.jpg",
                        "Spa pool at dawn"),
                List.of(),
                marcus, List.of("Wellness", "Spa", "Mindfulness"),
                List.of("prop-paris-001", "prop-tokyo-001"), 7,
                now.minusDays(30), now.minusDays(5)));

        articles.put("art-004", new Article("art-004", "designing-the-desert",
                "DESIGN", "PUBLISHED",
                tr("Designing the Desert: Inside Atlantis Royal", null, null),
                tr("How a flagship hotel translates dunes into living space", null, null),
                tr("Walk into Atlantis Royal at golden hour and you'll feel the desert before you see it. ...", null, null),
                tr("Inside the design language of Atlantis Royal Dubai.", null, null),
                photo("img-design-01",
                        "https://content.luxehotels.example/articles/dubai-design.jpg",
                        "Atlantis Royal at golden hour"),
                List.of(),
                camille, List.of("Dubai", "Design", "Architecture"),
                List.of("prop-dubai-001"), 9,
                now.minusDays(60), now.minusDays(20)));

        articles.put("art-005", new Article("art-005", "summer-in-the-cotswolds",
                "DESTINATION", "PUBLISHED",
                tr("A Summer in the Cotswolds", null, null),
                tr("Slow Sundays, ancient pubs, and an English garden you'll never want to leave", null, null),
                tr("There is a particular kind of summer that only the Cotswolds can produce. ...", null, null),
                tr("Slow Sundays and gardens you'll never want to leave.", null, null),
                photo("img-cotswolds-01",
                        "https://content.luxehotels.example/articles/cotswolds.jpg",
                        "Cotswolds village in summer"),
                List.of(),
                camille, List.of("England", "Countryside", "Family"),
                List.of("prop-london-001"), 5,
                now.minusDays(15), now.minusDays(2)));
    }

    // ── Inspirations ─────────────────────────────────────────────────────────

    private void initInspirations() {
        OffsetDateTime now = OffsetDateTime.now();
        inspirations.put("ins-001", new TravelInspiration("ins-001", "paris-spring",
                "Paris", "Western Europe", "SPRING",
                tr("Paris in Bloom", "Paris en fleurs", "París en flor"),
                tr("Markets, gardens, and the world's best terraces, set against the soft light of April.", null, null),
                photo("img-ins-paris", "https://content.luxehotels.example/inspirations/paris-spring.jpg",
                        "Paris cherry blossoms"),
                List.of(),
                List.of(en("Marché Bastille at sunrise"),
                        en("Walk the Promenade Plantée"),
                        en("Dinner at Septime")),
                List.of("prop-paris-001"),
                Money.of(4500, "USD"), 4, now.minusDays(40)));

        inspirations.put("ins-002", new TravelInspiration("ins-002", "tokyo-cherry-blossoms",
                "Tokyo", "East Asia", "SPRING",
                tr("Hanami in Tokyo", null, null),
                tr("Two weeks of cherry blossoms, and the rituals that surround them.", null, null),
                photo("img-ins-tokyo", "https://content.luxehotels.example/inspirations/tokyo-spring.jpg",
                        "Cherry blossoms in Ueno Park"),
                List.of(),
                List.of(en("Ueno Park hanami picnic"),
                        en("Yanaka tea ceremony"),
                        en("Sunset at Tokyo Tower")),
                List.of("prop-tokyo-001"),
                Money.of(6800, "USD"), 6, now.minusDays(30)));

        inspirations.put("ins-003", new TravelInspiration("ins-003", "dubai-desert-winter",
                "Dubai", "Middle East", "WINTER",
                tr("Dubai's Desert Winter", null, null),
                tr("Desert mornings and rooftop nights — Dubai shines brightest from November to February.", null, null),
                photo("img-ins-dubai", "https://content.luxehotels.example/inspirations/dubai-winter.jpg",
                        "Atlantis Royal across the bay"),
                List.of(),
                List.of(en("Sunrise dune drive"),
                        en("Dinner at Sky Terrace"),
                        en("Spa morning at Atlantis")),
                List.of("prop-dubai-001"),
                Money.of(8200, "USD"), 5, now.minusDays(20)));

        inspirations.put("ins-004", new TravelInspiration("ins-004", "manhattan-fall-weekend",
                "New York", "North America", "FALL",
                tr("A Manhattan Fall Weekend", null, null),
                tr("Three days of art, theater, and the best season the city offers.", null, null),
                photo("img-ins-nyc", "https://content.luxehotels.example/inspirations/nyc-fall.jpg",
                        "Central Park in autumn"),
                List.of(),
                List.of(en("MoMA Friday late hours"),
                        en("Broadway opening night"),
                        en("Brunch at Café Boulud")),
                List.of("prop-nyc-001"),
                Money.of(5400, "USD"), 3, now.minusDays(25)));

        inspirations.put("ins-005", new TravelInspiration("ins-005", "london-yearround",
                "London", "Western Europe", "YEAR_ROUND",
                tr("London, Beyond the Postcards", null, null),
                tr("Inside the markets, mews, and members' clubs that locals call home.", null, null),
                photo("img-ins-london", "https://content.luxehotels.example/inspirations/london.jpg",
                        "Foggy morning over the Thames"),
                List.of(),
                List.of(en("Borough Market breakfast"),
                        en("Soho gallery walk"),
                        en("West End ovation")),
                List.of("prop-london-001"),
                Money.of(4200, "GBP"), 4, now.minusDays(50)));
    }

    // ── Brand story ──────────────────────────────────────────────────────────

    private BrandStory buildBrandStory() {
        BrandStory.BrandPillar craft = new BrandStory.BrandPillar(
                "CRAFT", "hand",
                tr("Craft Without Compromise",
                        "Le métier sans compromis",
                        "Oficio sin concesiones"),
                tr("Every detail at every Luxe property reflects 1,000 small decisions made in service of the guest.", null, null));
        BrandStory.BrandPillar place = new BrandStory.BrandPillar(
                "PLACE", "compass",
                tr("Devotion to Place",
                        "Dévouement au lieu",
                        "Devoción al lugar"),
                tr("Each hotel speaks the language of its city — its history, its rituals, its people.", null, null));
        BrandStory.BrandPillar quiet = new BrandStory.BrandPillar(
                "QUIET", "leaf",
                tr("The Power of Quiet",
                        "La force du silence",
                        "El poder del silencio"),
                tr("True luxury is the absence of noise — visual, audible, and emotional.", null, null));

        return new BrandStory("brand-story-001",
                tr("Luxe International — Where Quiet Meets Craft", null, null),
                tr("Twelve cities. One philosophy. Anchored in place, devoted to craft.", null, null),
                tr("From a single hotel on the Île Saint-Louis in 1957, Luxe has grown to twelve cities across four continents — but the philosophy has never moved. ...", null, null),
                List.of(craft, place, quiet),
                photo("img-brand", "https://content.luxehotels.example/brand/hero.jpg",
                        "Luxe Paris facade at dusk"),
                "https://content.luxehotels.example/brand/film.mp4",
                OffsetDateTime.now().minusDays(120));
    }

    // ── Deal spotlights ──────────────────────────────────────────────────────

    private void initSpotlights() {
        LocalDate today = LocalDate.now();
        spotlights.put("dl-001", new DealSpotlight("dl-001", "spring-suites",
                "PUBLISHED", "SPRING25", 25.0,
                today.minusDays(10), today.plusDays(50),
                List.of("prop-paris-001", "prop-london-001"),
                "https://luxehotels.example/offers/spring-suites",
                tr("Spring Suite Awakening — 25% off",
                        "Réveil printanier en suite — 25% de réduction", null),
                tr("Step into spring with a 25% reduction on all suites at our European flagships.", null, null),
                en("Valid for stays through June 30. Subject to availability. Cannot be combined with other offers."),
                tr("Book Spring Suite", "Réserver la suite", null),
                photo("img-deal-spring", "https://content.luxehotels.example/deals/spring-suite.jpg",
                        "Suite at Le Grand Luxe Paris")));

        spotlights.put("dl-002", new DealSpotlight("dl-002", "fourth-night-free",
                "PUBLISHED", "STAY4", null,
                today.minusDays(30), today.plusDays(120),
                List.of("prop-tokyo-001", "prop-dubai-001", "prop-nyc-001"),
                "https://luxehotels.example/offers/fourth-night-free",
                tr("Stay 4, Pay 3 — A Long Weekend on Us", null, null),
                tr("Book any qualifying rate for four consecutive nights and the fourth is on us.", null, null),
                en("Promo applied at checkout. Lowest-rate night becomes complimentary."),
                tr("Plan a Long Weekend", null, null),
                photo("img-deal-stay4", "https://content.luxehotels.example/deals/stay4.jpg",
                        "Concierge at front desk")));

        spotlights.put("dl-003", new DealSpotlight("dl-003", "wellness-retreat",
                "DRAFT", null, 15.0,
                today.plusDays(20), today.plusDays(180),
                List.of("prop-paris-001", "prop-dubai-001"),
                "https://luxehotels.example/offers/wellness-retreat",
                tr("Wellness Retreat — 3 Nights, 15% Off", null, null),
                tr("Three nights of treatments, sound baths, and slow mornings at our flagship spas.", null, null),
                en("Includes daily breakfast and one signature treatment per night."),
                tr("Reserve Retreat", null, null),
                photo("img-deal-wellness", "https://content.luxehotels.example/deals/wellness.jpg",
                        "Spa courtyard")));
    }

    // ── Collections ──────────────────────────────────────────────────────────

    private void initCollections() {
        collections.put("col-spring", new ContentCollection("col-spring", "spring-2026",
                tr("Spring 2026 Collection", null, null),
                tr("A curated mix of articles, inspirations, and offers for the season ahead.", null, null),
                articles.get("art-005").getHeroImage(),
                List.of("art-001", "art-005"),
                List.of("ins-001", "ins-002"),
                List.of("dl-001"),
                OffsetDateTime.now().minusDays(15)));

        collections.put("col-wellness", new ContentCollection("col-wellness", "wellness-collection",
                tr("The Wellness Collection", null, null),
                tr("Stories, destinations, and offers for travelers in search of stillness.", null, null),
                articles.get("art-003").getHeroImage(),
                List.of("art-003"),
                List.of("ins-002", "ins-005"),
                List.of("dl-003"),
                OffsetDateTime.now().minusDays(7)));
    }

    // ── Lookups ──────────────────────────────────────────────────────────────

    private static String localeOf(String locale) { return locale != null ? locale : "en"; }

    @Override
    public List<Article> findArticles(Map<String, Object> filter, String locale) {
        String cat = filter != null ? (String) filter.get("category") : null;
        String tag = filter != null ? (String) filter.get("tag") : null;
        String hotelId = filter != null ? (String) filter.get("hotelId") : null;
        return articles.values().stream()
                .filter(a -> "PUBLISHED".equals(a.getStatus()))
                .filter(a -> cat == null || a.getCategory().equals(cat))
                .filter(a -> tag == null || a.getTags().contains(tag))
                .filter(a -> hotelId == null
                        || a.getRelatedHotels().stream().anyMatch(h -> hotelId.equals(h.get("id"))))
                .map(a -> a.forLocale(localeOf(locale)))
                .sorted(Comparator.comparing(Article::getPublishedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Article> findArticleBySlug(String slug, String locale) {
        return articles.values().stream()
                .filter(a -> a.getSlug().equals(slug))
                .findFirst()
                .map(a -> a.forLocale(localeOf(locale)));
    }

    @Override
    public List<Article> findFeaturedArticles(int limit, String locale) {
        return articles.values().stream()
                .filter(a -> "PUBLISHED".equals(a.getStatus()))
                .sorted(Comparator.comparing(Article::getPublishedAt).reversed())
                .limit(limit)
                .map(a -> a.forLocale(localeOf(locale)))
                .collect(Collectors.toList());
    }

    @Override
    public List<TravelInspiration> findInspirations(String destination, String season,
                                                      int limit, String locale) {
        return inspirations.values().stream()
                .filter(i -> destination == null || i.getDestination().equalsIgnoreCase(destination))
                .filter(i -> season == null || i.getBestSeason().equals(season))
                .map(i -> i.forLocale(localeOf(locale)))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public BrandStory getBrandStory(String locale) {
        return brandStory.forLocale(localeOf(locale));
    }

    @Override
    public List<DealSpotlight> findDealSpotlights(Boolean active, String locale) {
        LocalDate today = LocalDate.now();
        return spotlights.values().stream()
                .filter(d -> active == null || (active
                        ? "PUBLISHED".equals(d.getStatus())
                            && !today.isBefore(d.getValidFrom())
                            && !today.isAfter(d.getValidTo())
                        : !"PUBLISHED".equals(d.getStatus())
                            || today.isBefore(d.getValidFrom())
                            || today.isAfter(d.getValidTo())))
                .map(d -> d.forLocale(localeOf(locale)))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ContentCollection> findCollectionBySlug(String slug, String locale) {
        return collections.values().stream()
                .filter(c -> c.getSlug().equals(slug))
                .findFirst()
                .map(c -> c.forLocale(localeOf(locale)));
    }

    @Override public Optional<Article> findArticleById(String id) { return Optional.ofNullable(articles.get(id)); }

    @Override
    public List<Article> articlesByIds(List<String> ids, String locale) {
        return ids.stream().map(articles::get).filter(Objects::nonNull)
                .map(a -> a.forLocale(localeOf(locale))).toList();
    }

    @Override
    public List<TravelInspiration> inspirationsByIds(List<String> ids, String locale) {
        return ids.stream().map(inspirations::get).filter(Objects::nonNull)
                .map(i -> i.forLocale(localeOf(locale))).toList();
    }

    @Override
    public List<DealSpotlight> spotlightsByIds(List<String> ids, String locale) {
        return ids.stream().map(spotlights::get).filter(Objects::nonNull)
                .map(d -> d.forLocale(localeOf(locale))).toList();
    }
}
