package com.luxe.guest.schema.types;

public record GuestName(
        String title, String firstName, String middleName,
        String lastName, String preferredName, String suffix
) {
    public String fullName() {
        return (firstName + " " + lastName).trim();
    }
}
