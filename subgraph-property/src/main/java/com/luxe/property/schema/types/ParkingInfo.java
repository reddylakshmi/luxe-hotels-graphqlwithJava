package com.luxe.property.schema.types;

public record ParkingInfo(
        boolean available, String type, boolean valetAvailable,
        String valetFeePerDay, String selfParkFeePerDay, boolean evChargingAvailable
) {}
