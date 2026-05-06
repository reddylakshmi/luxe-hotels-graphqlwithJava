package com.luxe.loyalty.schema.types;

import java.time.OffsetDateTime;

public record CertificateRedemption(
        Certificate certificate,
        String reservationId,
        OffsetDateTime appliedAt,
        String message
) {}
