package com.luxe.reservations.schema.types;

import com.luxe.common.scalar.Money;
import java.time.LocalDate;

public record ReservationAddOn(
        String id, String type, String name, String description,
        int quantity, Money pricePerUnit, Money totalPrice,
        LocalDate date, String status
) {}
