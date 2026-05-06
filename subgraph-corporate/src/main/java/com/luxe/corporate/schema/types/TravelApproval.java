package com.luxe.corporate.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class TravelApproval implements HasId {
    private final String id, accountId, travelerId, reservationId, businessJustification;
    private String status;
    private final Money totalAmount;
    private final List<PolicyException> policyExceptions = new ArrayList<>();
    private final OffsetDateTime requestedAt;
    private OffsetDateTime decidedAt;
    private String decidedBy, notes;

    public TravelApproval(String id, String accountId, String travelerId, String reservationId,
                            String status, Money totalAmount,
                            List<PolicyException> exceptions, String businessJustification,
                            OffsetDateTime requestedAt) {
        this.id = id; this.accountId = accountId; this.travelerId = travelerId;
        this.reservationId = reservationId; this.status = status; this.totalAmount = totalAmount;
        if (exceptions != null) this.policyExceptions.addAll(exceptions);
        this.businessJustification = businessJustification;
        this.requestedAt = requestedAt;
    }

    @Override public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public String getTravelerId() { return travelerId; }
    public String getReservationId() { return reservationId; }
    public String getStatus() { return status; }
    public Money getTotalAmount() { return totalAmount; }
    public List<PolicyException> getPolicyExceptions() { return policyExceptions; }
    public String getBusinessJustification() { return businessJustification; }
    public OffsetDateTime getRequestedAt() { return requestedAt; }
    public OffsetDateTime getDecidedAt() { return decidedAt; }
    public String getDecidedBy() { return decidedBy; }
    public String getNotes() { return notes; }

    public void decide(String status, String decidedBy, String notes) {
        this.status = status;
        this.decidedAt = OffsetDateTime.now();
        this.decidedBy = decidedBy;
        this.notes = notes;
    }

    public record PolicyException(String rule, String detail, String severity) {}
}
