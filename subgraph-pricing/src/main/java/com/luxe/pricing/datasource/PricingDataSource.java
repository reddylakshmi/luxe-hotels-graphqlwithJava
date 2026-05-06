package com.luxe.pricing.datasource;

import com.luxe.pricing.schema.types.*;
import com.luxe.pricing.schema.types.Package;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PricingDataSource {
    AvailabilityResult searchRates(String hotelId, LocalDate checkIn, LocalDate checkOut,
                                    int adults, int children, String currency,
                                    List<String> ratePlanCodes, List<String> roomTypeIds,
                                    String promoCode, String corporateCode);

    Optional<Rate> validateRate(String rateToken);
    Optional<Rate> findRateById(String id);
    Optional<RatePlan> findRatePlanById(String id);

    List<Rate> findRatesByHotelId(String hotelId, LocalDate checkIn, LocalDate checkOut, int adults);
    List<Rate> findRatesByRoomTypeId(String roomTypeId, LocalDate checkIn, LocalDate checkOut, int adults);

    List<Promotion> findPromotions(String brandId, Boolean memberOnly);
    Optional<Promotion> findPromotionByCode(String code);

    Optional<Package> findPackageById(String id);

    List<DateRateSummary> getRateCalendar(String hotelId, LocalDate startDate, LocalDate endDate,
                                           int adults, String currency);

    Optional<GiftCardBalance> findGiftCardBalance(String code);

    List<RedemptionRate> findRedemptionRates(String hotelId, LocalDate checkIn,
                                              LocalDate checkOut, String roomTypeId);
}
