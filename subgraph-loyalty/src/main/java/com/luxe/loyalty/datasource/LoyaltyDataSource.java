package com.luxe.loyalty.datasource;

import com.luxe.loyalty.schema.types.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoyaltyDataSource {

    Optional<LoyaltyAccount> findByGuestId(String guestId);
    Optional<LoyaltyAccount> findByLoyaltyNumber(String loyaltyNumber);
    Optional<LoyaltyAccount> findById(String id);

    LoyaltyAccount enroll(String guestId, String referralCode, Boolean marketingOptIn);

    List<PointsTransaction> findTransactions(String accountId, Map<String, Object> filter, String sortBy);
    List<Certificate> findCertificates(String accountId, String status);

    PointsTransaction transferPoints(String fromLoyaltyNumber, String toLoyaltyNumber,
                                      int points, String message);
    PointsTransaction transferToAirline(String accountId, String partnerId,
                                         String partnerAccountNumber, int points);

    LinkedPartnerAccount linkPartnerAccount(String accountId, String partnerId,
                                             String partnerAccountNumber, String lastName);

    Challenge registerForChallenge(String accountId, String challengeId);

    Certificate redeemCertificate(String accountId, String certificateId, String reservationId);

    PointsTransaction buyPoints(String accountId, int points, String paymentMethodId);
    PointsTransaction giftPoints(String fromLoyaltyNumber, String toLoyaltyNumber,
                                  int points, String message);
    LoyaltyAccount extendPointsExpiry(String accountId);

    List<LoyaltyPartner> findPartners(List<String> categories);
    Optional<LoyaltyPartner> findPartnerById(String id);

    List<Challenge> findAvailableChallenges();
    Optional<Challenge> findChallengeById(String id);

    List<LoyaltyBenefit> benefitsForTier(String tier);

    PointsValuation valuePoints(int points, String currency);
}
