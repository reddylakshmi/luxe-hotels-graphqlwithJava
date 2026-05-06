package com.luxe.guest.schema.types;

public record GuestExternalIds(
        String loyaltyNumber, String pmsGuestId,
        String ssoSubject, String crmContactId
) {}
