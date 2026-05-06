package com.luxe.guest.datasource;

import com.luxe.guest.schema.types.AuthPayload;
import com.luxe.guest.schema.types.GuestPreferences;
import com.luxe.guest.schema.types.GuestProfile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GuestDataSource {
    Optional<GuestProfile> findById(String id);
    Optional<GuestProfile> findByEmail(String email);
    List<GuestProfile> findAll();
    List<GuestProfile> findByFilter(String email, String status, String loyaltyNumber);

    AuthPayload signIn(String email, String password);
    AuthPayload signUp(String email, String password, String firstName, String lastName, String phone);

    GuestProfile update(String id, Map<String, Object> fields);
    GuestProfile updatePreferences(String id, GuestPreferences preferences);

    GuestProfile addPaymentMethod(String guestId, Map<String, Object> input);
    GuestProfile removePaymentMethod(String guestId, String paymentMethodId);
    GuestProfile setDefaultPaymentMethod(String guestId, String paymentMethodId);

    GuestProfile saveHotel(String guestId, String hotelId);
    GuestProfile unsaveHotel(String guestId, String hotelId);

    GuestProfile addTravelCompanion(String guestId, Map<String, Object> input);
    GuestProfile removeTravelCompanion(String guestId, String companionId);

    boolean existsByEmail(String email);
}
