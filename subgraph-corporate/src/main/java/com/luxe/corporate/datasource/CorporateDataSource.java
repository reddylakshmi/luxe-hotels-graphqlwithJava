package com.luxe.corporate.datasource;

import com.luxe.corporate.schema.types.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CorporateDataSource {
    Optional<CorporateAccount> findById(String id);
    Optional<CorporateAccount> findByGuestId(String guestId);
    Optional<CorporateAccount> findByContractNumber(String contractNumber);

    Optional<TravelPolicy> findPolicy(String accountId);
    List<NegotiatedRate> findRates(String accountId, String hotelId);

    CorporateAccount enroll(Map<String, Object> input);
    TravelPolicy updatePolicy(String accountId, Map<String, Object> input);
    CorporateTraveler addTraveler(Map<String, Object> input);
    boolean removeTraveler(String travelerId);

    List<TravelApproval> pendingApprovals(String accountId);
    Optional<TravelApproval> findApproval(String id);
    TravelApproval reviewApproval(String id, String decision, String reviewer, String notes);

    TravelReport report(String accountId, String period);
}
