package com.luxe.pricing.datasource;

import com.luxe.pricing.schema.types.AvailabilityResult;
import com.luxe.pricing.schema.types.Promotion;
import com.luxe.pricing.schema.types.Rate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingMockDataSourceTest {

    private PricingMockDataSource ds;
    private final LocalDate ci = LocalDate.now().plusDays(30);
    private final LocalDate co = LocalDate.now().plusDays(33);

    @BeforeEach
    void setUp() {
        ds = new PricingMockDataSource();
    }

    @Test
    void search_rates_for_known_hotel_returns_room_availabilities() {
        AvailabilityResult result = ds.searchRates("prop-paris-001", ci, co,
                2, 0, "EUR", null, null, null, null);
        assertThat(result).isNotNull();
        assertThat(result.getRoomAvailabilities()).isNotEmpty();
    }

    @Test
    void search_rates_token_is_present() {
        AvailabilityResult result = ds.searchRates("prop-paris-001", ci, co,
                2, 0, "EUR", null, null, null, null);
        assertThat(result.getSearchToken()).isNotBlank();
    }

    @Test
    void find_rates_by_hotel_id_returns_rates() {
        List<Rate> rates = ds.findRatesByHotelId("prop-paris-001", ci, co, 2);
        assertThat(rates).isNotEmpty();
        assertThat(rates).allSatisfy(r -> assertThat(r.getRateToken()).isNotBlank());
    }

    @Test
    void invalid_rate_token_does_not_validate() {
        assertThat(ds.validateRate("bogus-token")).isEmpty();
    }

    @Test
    void find_promotions_returns_seeded_promotions() {
        List<Promotion> promos = ds.findPromotions(null, null);
        assertThat(promos).isNotEmpty();
        assertThat(promos).allSatisfy(p -> assertThat(p.getCode()).isNotBlank());
    }

    @Test
    void find_promotion_by_known_code_returns_it() {
        Promotion any = ds.findPromotions(null, null).get(0);
        assertThat(ds.findPromotionByCode(any.getCode()))
                .isPresent().get().extracting(Promotion::getCode).isEqualTo(any.getCode());
    }

    @Test
    void find_promotion_by_unknown_code_is_empty() {
        assertThat(ds.findPromotionByCode("NOT-REAL")).isEmpty();
    }
}
