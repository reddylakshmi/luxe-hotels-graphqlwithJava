package com.luxe.guest.schema.types;

public record GuestCorporateProfile(
        String companyName, String corporateAccountId,
        String employeeId, String costCenter
) {}
