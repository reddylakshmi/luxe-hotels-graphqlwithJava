package com.luxe.reservations.schema.types;

public record ExpressCheckoutSuccess(
        String reservationId,
        Folio folio,
        String emailedTo,
        String message
) {}
