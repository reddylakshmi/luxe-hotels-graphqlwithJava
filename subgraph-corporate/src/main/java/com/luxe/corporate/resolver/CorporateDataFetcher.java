package com.luxe.corporate.resolver;

import com.luxe.corporate.schema.types.GuestProfile;

import com.luxe.common.auth.AuthContext;
import com.luxe.common.auth.AuthContextResolver;
import com.luxe.common.auth.AuthRole;
import com.luxe.common.error.AlreadyEnrolledError;
import com.luxe.common.error.NotFoundError;
import com.luxe.common.error.ValidationError;
import com.luxe.common.error.FieldError;
import com.luxe.corporate.datasource.CorporateDataSource;
import com.luxe.corporate.schema.types.*;
import com.netflix.graphql.dgs.*;
import graphql.schema.DataFetchingEnvironment;

import java.util.List;
import java.util.Map;

@DgsComponent
public class CorporateDataFetcher {

    private final CorporateDataSource dataSource;
    private final AuthContextResolver authResolver;

    public CorporateDataFetcher(CorporateDataSource dataSource, AuthContextResolver authResolver) {
        this.dataSource = dataSource;
        this.authResolver = authResolver;
    }

    private AuthContext getAuth(DataFetchingEnvironment dfe) {
        return authResolver.resolve(dfe);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @DgsQuery
    public CorporateAccount myCorporateAccount(DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        return dataSource.findByGuestId(auth.guestId()).orElse(null);
    }

    @DgsQuery
    public CorporateAccount corporateAccount(@InputArgument String id,
                                               DataFetchingEnvironment dfe) {
        getAuth(dfe).requireRole(AuthRole.PROPERTY_STAFF);
        return dataSource.findById(id).orElse(null);
    }

    @DgsQuery
    public TravelReport travelReport(@InputArgument String accountId,
                                       @InputArgument String period,
                                       DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        return dataSource.report(accountId, period);
    }

    @DgsQuery
    public List<TravelApproval> pendingApprovals(@InputArgument String accountId,
                                                   DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        return dataSource.pendingApprovals(accountId);
    }

    @DgsQuery
    public TravelPolicy travelPolicy(@InputArgument String accountId,
                                       DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        return dataSource.findPolicy(accountId).orElse(null);
    }

    @DgsQuery
    public List<NegotiatedRate> negotiatedRates(@InputArgument String accountId,
                                                  @InputArgument String propertyId,
                                                  DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        return dataSource.findRates(accountId, propertyId);
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @DgsMutation
    public Object enrollCorporate(@InputArgument Map<String, Object> input,
                                    DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        if (input.get("companyName") == null || input.get("primaryContact") == null) {
            return new ValidationError("INVALID_INPUT",
                    "companyName and primaryContact are required",
                    List.of(new FieldError("companyName", "Required")));
        }
        return dataSource.enroll(input);
    }

    @DgsMutation
    public Object updateTravelPolicy(@InputArgument String accountId,
                                       @InputArgument Map<String, Object> input,
                                       DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        if (dataSource.findById(accountId).isEmpty())
            return new NotFoundError("CorporateAccount", accountId);
        return dataSource.updatePolicy(accountId, input);
    }

    @DgsMutation
    public Object addCorporateTraveler(@InputArgument Map<String, Object> input,
                                         DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        if (input.get("accountId") == null || input.get("guestId") == null) {
            return new ValidationError("INVALID_INPUT",
                    "accountId and guestId are required",
                    List.of(new FieldError("accountId", "Required")));
        }
        var t = dataSource.addTraveler(input);
        return t != null ? t : new NotFoundError("CorporateAccount",
                String.valueOf(input.get("accountId")));
    }

    @DgsMutation
    public boolean removeCorporateTraveler(@InputArgument String travelerId,
                                             DataFetchingEnvironment dfe) {
        getAuth(dfe).requireAuth();
        return dataSource.removeTraveler(travelerId);
    }

    @DgsMutation
    public Object reviewTravelApproval(@InputArgument String approvalId,
                                         @InputArgument String decision,
                                         @InputArgument String notes,
                                         DataFetchingEnvironment dfe) {
        AuthContext auth = getAuth(dfe);
        auth.requireAuth();
        if (dataSource.findApproval(approvalId).isEmpty())
            return new NotFoundError("TravelApproval", approvalId);
        return dataSource.reviewApproval(approvalId, decision, auth.guestId(), notes);
    }

    // ── GuestProfile.corporateAccount federation ──────────────────────────────

    @DgsData(parentType = "GuestProfile", field = "corporateAccount")
    public CorporateAccount guestCorporateAccount(DataFetchingEnvironment dfe) {
        GuestProfile guest = dfe.getSource();
        return dataSource.findByGuestId(guest.getId()).orElse(null);
    }

    @DgsEntityFetcher(name = "CorporateAccount")
    public CorporateAccount fetchAccount(Map<String, Object> values) {
        return dataSource.findById((String) values.get("id")).orElse(null);
    }
    @DgsEntityFetcher(name = "GuestProfile")
    public GuestProfile fetchGuestProfileReference(Map<String, Object> values) {
        return new GuestProfile((String) values.get("id"));
    }

}
