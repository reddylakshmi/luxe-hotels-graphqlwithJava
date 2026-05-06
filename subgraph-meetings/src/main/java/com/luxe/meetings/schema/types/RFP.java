package com.luxe.meetings.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RFP implements HasId {

    private final String id, rfpNumber;
    private String status;
    private String organizer, organization, contactEmail, contactPhone;
    private String eventName, eventType;
    private LocalDate startDate, endDate;
    private int attendees;
    private Integer guestRoomsPerNight;
    private final List<String> preferredHotelIds;
    private List<SpaceRequirement> spaceRequirements;
    private String cateringRequirements, additionalRequirements;
    private final List<RFPResponse> responses = new ArrayList<>();
    private final List<RFPStatusEvent> history = new ArrayList<>();
    private final OffsetDateTime submittedAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime expiresAt;

    public RFP(String id, String rfpNumber, String status,
                String organizer, String organization, String contactEmail, String contactPhone,
                String eventName, String eventType, LocalDate startDate, LocalDate endDate,
                int attendees, Integer guestRoomsPerNight, List<String> preferredHotelIds,
                List<SpaceRequirement> spaceRequirements,
                String cateringRequirements, String additionalRequirements,
                OffsetDateTime submittedAt) {
        this.id = id; this.rfpNumber = rfpNumber; this.status = status;
        this.organizer = organizer; this.organization = organization;
        this.contactEmail = contactEmail; this.contactPhone = contactPhone;
        this.eventName = eventName; this.eventType = eventType;
        this.startDate = startDate; this.endDate = endDate;
        this.attendees = attendees; this.guestRoomsPerNight = guestRoomsPerNight;
        this.preferredHotelIds = preferredHotelIds;
        this.spaceRequirements = spaceRequirements;
        this.cateringRequirements = cateringRequirements;
        this.additionalRequirements = additionalRequirements;
        this.submittedAt = submittedAt;
        this.updatedAt = submittedAt;
        this.expiresAt = submittedAt.plusDays(30);
        this.history.add(new RFPStatusEvent(status, "RFP submitted", submittedAt, organizer));
    }

    @Override public String getId() { return id; }
    public String getRfpNumber() { return rfpNumber; }
    public String getStatus() { return status; }
    public String getOrganizer() { return organizer; }
    public String getOrganization() { return organization; }
    public String getContactEmail() { return contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public String getEventName() { return eventName; }
    public String getEventType() { return eventType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getAttendees() { return attendees; }
    public Integer getGuestRoomsPerNight() { return guestRoomsPerNight; }
    public List<SpaceRequirement> getSpaceRequirements() { return spaceRequirements; }
    public String getCateringRequirements() { return cateringRequirements; }
    public String getAdditionalRequirements() { return additionalRequirements; }
    public List<RFPResponse> getResponses() { return responses; }
    public List<RFPStatusEvent> getHistory() { return history; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }

    public List<Map<String, Object>> getPreferredHotels() {
        return preferredHotelIds.stream()
                .<Map<String, Object>>map(id -> Map.of("id", id))
                .toList();
    }

    public void setStatus(String status, String notes, String actor) {
        this.status = status;
        this.updatedAt = OffsetDateTime.now();
        this.history.add(new RFPStatusEvent(status, notes, this.updatedAt, actor));
    }

    public void update(LocalDate startDate, LocalDate endDate, Integer attendees,
                        Integer guestRoomsPerNight, List<SpaceRequirement> spaceReqs,
                        String catering, String additional) {
        if (startDate != null) this.startDate = startDate;
        if (endDate != null)   this.endDate = endDate;
        if (attendees != null) this.attendees = attendees;
        if (guestRoomsPerNight != null) this.guestRoomsPerNight = guestRoomsPerNight;
        if (spaceReqs != null) this.spaceRequirements = spaceReqs;
        if (catering != null)  this.cateringRequirements = catering;
        if (additional != null) this.additionalRequirements = additional;
        this.updatedAt = OffsetDateTime.now();
    }

    public void addResponse(RFPResponse r) { this.responses.add(r); }

    public record SpaceRequirement(
            String name, String setup, int attendees, double durationHours, String startTime
    ) {}

    public record RFPResponse(
            String id, String hotelId,
            String status, List<String> proposedSpaceIds, Integer proposedRoomBlock,
            Money proposedRate, Money proposedFAndBMinimum, String notes,
            OffsetDateTime respondedAt, OffsetDateTime validUntil
    ) {
        public Map<String, Object> getHotel() { return Map.of("id", hotelId); }
    }

    public record RFPStatusEvent(String status, String notes, OffsetDateTime changedAt, String changedBy) {}
}
