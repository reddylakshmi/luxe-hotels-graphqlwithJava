package com.luxe.guest.resolver;

import com.luxe.common.auth.AuthContext;
import com.luxe.common.auth.AuthRole;
import com.luxe.common.error.NotFoundError;
import com.luxe.common.pagination.Connection;
import com.luxe.guest.datasource.GuestDataSource;
import com.luxe.guest.schema.types.*;
import com.netflix.graphql.dgs.*;
import graphql.schema.DataFetchingEnvironment;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

@DgsComponent
public class GuestDataFetcher {

    private final GuestDataSource dataSource;

    public GuestDataFetcher(GuestDataSource dataSource) {
        this.dataSource = dataSource;
    }

    private AuthContext getAuthContext(DataFetchingEnvironment dfe) {
        try {
            HttpServletRequest request = dfe.getGraphQlContext().get(HttpServletRequest.class);
            if (request == null) return AuthContext.anonymous();
            AuthContext ctx = (AuthContext) request.getAttribute("authContext");
            return ctx != null ? ctx : AuthContext.anonymous();
        } catch (Exception e) {
            return AuthContext.anonymous();
        }
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    @DgsQuery
    public Object me(DataFetchingEnvironment dfe) {
        AuthContext auth = getAuthContext(dfe);
        auth.requireAuth();
        return dataSource.findById(auth.guestId())
                .<Object>map(g -> g)
                .orElse(new NotFoundError("GuestProfile", auth.guestId()));
    }

    @DgsQuery
    public Object guest(@InputArgument String id, DataFetchingEnvironment dfe) {
        getAuthContext(dfe).requireRole(AuthRole.PROPERTY_STAFF);
        return dataSource.findById(id)
                .<Object>map(g -> g)
                .orElse(new NotFoundError("GuestProfile", id));
    }

    @DgsQuery
    public Object guests(@InputArgument Integer first, @InputArgument String after,
                          DataFetchingEnvironment dfe) {
        getAuthContext(dfe).requireRole(AuthRole.ADMIN);
        List<GuestProfile> all = dataSource.findAll();
        Connection<GuestProfile> conn = Connection.of(all, first != null ? first : 10, after);
        return buildConnection(conn);
    }

    // ── Auth Mutations ────────────────────────────────────────────────────────

    @DgsMutation
    public Object signIn(@InputArgument Map<String, Object> input) {
        try {
            return dataSource.signIn(
                    (String) input.get("email"),
                    (String) input.get("password"));
        } catch (RuntimeException e) {
            return new com.luxe.common.error.AuthenticationError(e.getMessage());
        }
    }

    @DgsMutation
    public Object signUp(@InputArgument Map<String, Object> input) {
        try {
            return dataSource.signUp(
                    (String) input.get("email"),
                    (String) input.get("password"),
                    (String) input.get("firstName"),
                    (String) input.get("lastName"),
                    (String) input.get("phone"));
        } catch (RuntimeException e) {
            return new com.luxe.common.error.ValidationError("SIGNUP_ERROR", e.getMessage(),
                    List.of(new com.luxe.common.error.FieldError("email", e.getMessage())));
        }
    }

    @DgsMutation
    public Map<String, Object> signOut(DataFetchingEnvironment dfe) {
        return Map.of("success", true, "message", "Signed out successfully");
    }

    // ── Profile Mutations ─────────────────────────────────────────────────────

    @DgsMutation
    public Object updateGuestProfile(@InputArgument Map<String, Object> input,
                                      DataFetchingEnvironment dfe) {
        AuthContext auth = getAuthContext(dfe);
        auth.requireAuth();
        GuestProfile updated = dataSource.update(auth.guestId(), input);
        return updated != null ? updated : new NotFoundError("GuestProfile", auth.guestId());
    }

    @DgsMutation
    public Object updatePreferences(@InputArgument Map<String, Object> input,
                                     DataFetchingEnvironment dfe) {
        AuthContext auth = getAuthContext(dfe);
        auth.requireAuth();
        GuestPreferences prefs = buildPreferences(input);
        GuestProfile updated = dataSource.updatePreferences(auth.guestId(), prefs);
        return updated != null ? updated : new NotFoundError("GuestProfile", auth.guestId());
    }

    // ── Payment Method Mutations ──────────────────────────────────────────────

    @DgsMutation
    public Object addPaymentMethod(@InputArgument Map<String, Object> input,
                                    DataFetchingEnvironment dfe) {
        AuthContext auth = getAuthContext(dfe);
        auth.requireAuth();
        GuestProfile updated = dataSource.addPaymentMethod(auth.guestId(), input);
        return updated != null ? updated : new NotFoundError("GuestProfile", auth.guestId());
    }

    @DgsMutation
    public Object removePaymentMethod(@InputArgument(name = "id") String paymentMethodId,
                                       DataFetchingEnvironment dfe) {
        AuthContext auth = getAuthContext(dfe);
        auth.requireAuth();
        GuestProfile updated = dataSource.removePaymentMethod(auth.guestId(), paymentMethodId);
        return updated != null ? updated : new NotFoundError("GuestProfile", auth.guestId());
    }

    @DgsMutation
    public Object setDefaultPaymentMethod(@InputArgument(name = "id") String paymentMethodId,
                                           DataFetchingEnvironment dfe) {
        AuthContext auth = getAuthContext(dfe);
        auth.requireAuth();
        GuestProfile updated = dataSource.setDefaultPaymentMethod(auth.guestId(), paymentMethodId);
        return updated != null ? updated : new NotFoundError("GuestProfile", auth.guestId());
    }

    // ── Saved Hotel Mutations ─────────────────────────────────────────────────

    @DgsMutation
    public Object saveHotel(@InputArgument String hotelId, DataFetchingEnvironment dfe) {
        AuthContext auth = getAuthContext(dfe);
        auth.requireAuth();
        GuestProfile updated = dataSource.saveHotel(auth.guestId(), hotelId);
        return updated != null ? updated : new NotFoundError("GuestProfile", auth.guestId());
    }

    @DgsMutation
    public Object unsaveHotel(@InputArgument String hotelId, DataFetchingEnvironment dfe) {
        AuthContext auth = getAuthContext(dfe);
        auth.requireAuth();
        GuestProfile updated = dataSource.unsaveHotel(auth.guestId(), hotelId);
        return updated != null ? updated : new NotFoundError("GuestProfile", auth.guestId());
    }

    // ── Travel Companion Mutations ────────────────────────────────────────────

    @DgsMutation
    public Object addTravelCompanion(@InputArgument Map<String, Object> input,
                                      DataFetchingEnvironment dfe) {
        AuthContext auth = getAuthContext(dfe);
        auth.requireAuth();
        GuestProfile updated = dataSource.addTravelCompanion(auth.guestId(), input);
        return updated != null ? updated : new NotFoundError("GuestProfile", auth.guestId());
    }

    @DgsMutation
    public Object removeTravelCompanion(@InputArgument(name = "id") String companionId,
                                         DataFetchingEnvironment dfe) {
        AuthContext auth = getAuthContext(dfe);
        auth.requireAuth();
        GuestProfile updated = dataSource.removeTravelCompanion(auth.guestId(), companionId);
        return updated != null ? updated : new NotFoundError("GuestProfile", auth.guestId());
    }

    // ── Field Resolvers ───────────────────────────────────────────────────────

    @DgsData(parentType = "GuestProfile", field = "paymentMethods")
    public Map<String, Object> paymentMethodsConnection(DataFetchingEnvironment dfe) {
        GuestProfile guest = dfe.getSource();
        List<PaymentMethod> all = guest.getPaymentMethods();
        Integer first = dfe.getArgument("first");
        String after = dfe.getArgument("after");
        int limit = first != null ? first : 10;
        int start = 0;
        if (after != null) {
            String decoded = new String(Base64.getDecoder().decode(after));
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getId().equals(decoded)) { start = i + 1; break; }
            }
        }
        List<PaymentMethod> page = all.stream().skip(start).limit(limit).toList();
        List<Map<String, Object>> edges = page.stream().map(pm -> Map.<String, Object>of(
                "node", pm,
                "cursor", Base64.getEncoder().encodeToString(pm.getId().getBytes())
        )).toList();
        return Map.of(
                "edges", edges,
                "pageInfo", Map.of("hasNextPage", start + page.size() < all.size(),
                        "hasPreviousPage", start > 0,
                        "startCursor", edges.isEmpty() ? null : edges.get(0).get("cursor"),
                        "endCursor", edges.isEmpty() ? null : edges.get(edges.size() - 1).get("cursor")),
                "totalCount", all.size()
        );
    }

    @DgsData(parentType = "GuestProfile", field = "savedHotels")
    public Map<String, Object> savedHotelsConnection(DataFetchingEnvironment dfe) {
        GuestProfile guest = dfe.getSource();
        List<SavedHotel> all = guest.getSavedHotels();
        Integer first = dfe.getArgument("first");
        int limit = first != null ? first : 10;
        List<SavedHotel> page = all.stream().limit(limit).toList();
        List<Map<String, Object>> edges = page.stream().map(h -> Map.<String, Object>of(
                "node", h,
                "cursor", Base64.getEncoder().encodeToString(h.id().getBytes())
        )).toList();
        return Map.of(
                "edges", edges,
                "pageInfo", Map.of("hasNextPage", page.size() < all.size(),
                        "hasPreviousPage", false,
                        "startCursor", edges.isEmpty() ? null : edges.get(0).get("cursor"),
                        "endCursor", edges.isEmpty() ? null : edges.get(edges.size() - 1).get("cursor")),
                "totalCount", all.size()
        );
    }

    // ── Entity Fetcher ────────────────────────────────────────────────────────

    @DgsEntityFetcher(name = "GuestProfile")
    public GuestProfile fetchGuestProfile(Map<String, Object> values) {
        return dataSource.findById((String) values.get("id")).orElse(null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> buildConnection(Connection<GuestProfile> conn) {
        return Map.of(
                "edges", conn.edges().stream()
                        .map(e -> Map.of("node", e.node(), "cursor", e.cursor()))
                        .toList(),
                "pageInfo", Map.of(
                        "hasNextPage", conn.pageInfo().hasNextPage(),
                        "hasPreviousPage", conn.pageInfo().hasPreviousPage(),
                        "startCursor", conn.pageInfo().startCursor(),
                        "endCursor", conn.pageInfo().endCursor()),
                "totalCount", conn.totalCount()
        );
    }

    @SuppressWarnings("unchecked")
    private GuestPreferences buildPreferences(Map<String, Object> input) {
        Map<String, Object> roomInput = (Map<String, Object>) input.get("room");
        Map<String, Object> bedInput = (Map<String, Object>) input.get("bed");
        Map<String, Object> dietaryInput = (Map<String, Object>) input.get("dietary");
        Map<String, Object> commInput = (Map<String, Object>) input.get("communication");
        Map<String, Object> transportInput = (Map<String, Object>) input.get("transport");
        Map<String, Object> accessInput = (Map<String, Object>) input.get("accessibility");

        GuestPreferences.RoomPreferences room = roomInput == null ? null
                : new GuestPreferences.RoomPreferences(
                        (List<String>) roomInput.getOrDefault("preferredTypes", List.of()),
                        (String) roomInput.get("viewPreference"),
                        (String) roomInput.get("floorPreference"),
                        Boolean.TRUE.equals(roomInput.get("quietRoom")),
                        Boolean.TRUE.equals(roomInput.get("highFloor")),
                        Boolean.TRUE.equals(roomInput.get("awayFromElevator")),
                        Boolean.TRUE.equals(roomInput.get("adjacentRooms"))
                );
        GuestPreferences.BedPreferences bed = bedInput == null ? null
                : new GuestPreferences.BedPreferences(
                        (String) bedInput.get("type"),
                        (String) bedInput.get("preferredConfiguration")
                );
        GuestPreferences.DietaryPreferences dietary = dietaryInput == null ? null
                : new GuestPreferences.DietaryPreferences(
                        (List<String>) dietaryInput.getOrDefault("restrictions", List.of()),
                        (List<String>) dietaryInput.getOrDefault("allergies", List.of()),
                        Boolean.TRUE.equals(dietaryInput.get("kosher")),
                        Boolean.TRUE.equals(dietaryInput.get("halal")),
                        Boolean.TRUE.equals(dietaryInput.get("vegan"))
                );
        GuestPreferences.CommunicationPreferences comm = commInput == null ? null
                : new GuestPreferences.CommunicationPreferences(
                        (String) commInput.getOrDefault("preferredLanguage", "en"),
                        (String) commInput.getOrDefault("preferredChannel", "EMAIL"),
                        !Boolean.FALSE.equals(commInput.get("marketingOptIn")),
                        Boolean.TRUE.equals(commInput.get("doNotDisturb"))
                );
        GuestPreferences.TransportPreferences transport = transportInput == null ? null
                : new GuestPreferences.TransportPreferences(
                        (String) transportInput.get("preferredMode"),
                        List.of()
                );
        GuestPreferences.AccessibilityNeeds access = accessInput == null ? null
                : new GuestPreferences.AccessibilityNeeds(
                        Boolean.TRUE.equals(accessInput.get("wheelchairAccessible")),
                        Boolean.TRUE.equals(accessInput.get("rollInShower")),
                        Boolean.TRUE.equals(accessInput.get("hearingAccessible"))
                );

        return new GuestPreferences(room, bed, null, dietary, access, transport, comm);
    }
}
