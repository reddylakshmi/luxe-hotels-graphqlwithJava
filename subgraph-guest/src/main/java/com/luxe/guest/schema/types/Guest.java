package com.luxe.guest.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class Guest implements HasId {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate dateOfBirth;
    private String nationality;
    private String passportNumber;
    private GuestPreferences preferences;
    private String loyaltyNumber;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Guest() {}

    public Guest(String id, String email, String firstName, String lastName, String phone,
                 LocalDate dateOfBirth, String nationality, String passportNumber,
                 GuestPreferences preferences, String loyaltyNumber, String status,
                 OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id; this.email = email; this.firstName = firstName; this.lastName = lastName;
        this.phone = phone; this.dateOfBirth = dateOfBirth; this.nationality = nationality;
        this.passportNumber = passportNumber; this.preferences = preferences;
        this.loyaltyNumber = loyaltyNumber; this.status = status;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    @Override public String getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    public String getPhone() { return phone; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getNationality() { return nationality; }
    public String getPassportNumber() { return passportNumber; }
    public GuestPreferences getPreferences() { return preferences; }
    public String getLoyaltyNumber() { return loyaltyNumber; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setEmail(String email) { this.email = email; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }
    public void setPreferences(GuestPreferences preferences) { this.preferences = preferences; }
    public void setLoyaltyNumber(String loyaltyNumber) { this.loyaltyNumber = loyaltyNumber; }
    public void setStatus(String status) { this.status = status; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
