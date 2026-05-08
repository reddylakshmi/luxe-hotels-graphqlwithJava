package com.luxe.guest.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class GuestProfile implements HasId {
    private final String id, email, avatarUrl;
    private final String languagePreference, currencyPreference;
    private final GuestName name;
    private final GuestExternalIds externalIds;
    private final List<GuestAddress> addresses;
    private final List<PaymentMethod> paymentMethods;
    private final List<TravelCompanion> travelCompanions;
    private final List<SavedHotel> savedHotels;
    private final GuestCorporateProfile corporateProfile;
    private final OffsetDateTime memberSince;

    // Mutable via updateGuestProfile / updatePreferences. Other fields
    // stay final because the schema currently has no mutation for them.
    private String phone;
    private String nationality;
    private LocalDate dateOfBirth;
    private GuestPreferences preferences;
    private OffsetDateTime updatedAt;

    public GuestProfile(String id, String email, String phone, String nationality, String avatarUrl,
                        String languagePreference, String currencyPreference, LocalDate dateOfBirth,
                        GuestName name, GuestExternalIds externalIds, GuestPreferences preferences,
                        List<GuestAddress> addresses, List<PaymentMethod> paymentMethods,
                        List<TravelCompanion> travelCompanions, List<SavedHotel> savedHotels,
                        GuestCorporateProfile corporateProfile,
                        OffsetDateTime memberSince, OffsetDateTime updatedAt) {
        this.id = id; this.email = email; this.phone = phone; this.nationality = nationality;
        this.avatarUrl = avatarUrl; this.languagePreference = languagePreference;
        this.currencyPreference = currencyPreference; this.dateOfBirth = dateOfBirth;
        this.name = name; this.externalIds = externalIds; this.preferences = preferences;
        this.addresses = addresses; this.paymentMethods = paymentMethods;
        this.travelCompanions = travelCompanions; this.savedHotels = savedHotels;
        this.corporateProfile = corporateProfile;
        this.memberSince = memberSince; this.updatedAt = updatedAt;
    }

    @Override public String getId() { return id; }

    public void addAddressToList(GuestAddress a) { addresses.add(a); }
    public void removeAddressFromList(String id) { addresses.removeIf(a -> a.id().equals(id)); }
    public void replaceAddressInList(String id, GuestAddress next) {
        for (int i = 0; i < addresses.size(); i++) {
            if (addresses.get(i).id().equals(id)) { addresses.set(i, next); return; }
        }
    }
    public void clearPrimaryAddresses() {
        for (int i = 0; i < addresses.size(); i++) {
            GuestAddress a = addresses.get(i);
            if (a.isPrimary()) {
                addresses.set(i, new GuestAddress(a.id(), a.type(), a.line1(), a.line2(),
                        a.city(), a.stateCode(), a.postalCode(), a.countryCode(), false));
            }
        }
    }
    public void addPaymentMethodToList(PaymentMethod pm) { paymentMethods.add(pm); }
    public void removePaymentMethodFromList(String pmId) { paymentMethods.removeIf(pm -> pm.getId().equals(pmId)); }
    public void clearDefaultPaymentMethods() { paymentMethods.forEach(pm -> pm.setDefault(false)); }
    public void addSavedHotelToList(SavedHotel h) { savedHotels.add(h); }
    public void removeSavedHotelFromList(String hotelId) { savedHotels.removeIf(h -> h.hotelId().equals(hotelId)); }
    public void addTravelCompanionToList(TravelCompanion tc) { travelCompanions.add(tc); }
    public void removeTravelCompanionFromList(String tcId) { travelCompanions.removeIf(tc -> tc.id().equals(tcId)); }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getNationality() { return nationality; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getLanguagePreference() { return languagePreference; }
    public String getCurrencyPreference() { return currencyPreference; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public GuestName getName() { return name; }
    public GuestExternalIds getExternalIds() { return externalIds; }
    public GuestPreferences getPreferences() { return preferences; }
    public void setPreferences(GuestPreferences p) { this.preferences = p; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<GuestAddress> getAddresses() { return addresses; }
    public List<PaymentMethod> getPaymentMethods() { return paymentMethods; }
    public List<TravelCompanion> getTravelCompanions() { return travelCompanions; }
    public List<SavedHotel> getSavedHotels() { return savedHotels; }
    public GuestCorporateProfile getCorporateProfile() { return corporateProfile; }
    public OffsetDateTime getMemberSince() { return memberSince; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
