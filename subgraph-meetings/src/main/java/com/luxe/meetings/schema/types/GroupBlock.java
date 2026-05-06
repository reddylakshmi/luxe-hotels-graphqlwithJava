package com.luxe.meetings.schema.types;

import com.luxe.common.pagination.HasId;
import com.luxe.common.scalar.Money;
import java.time.LocalDate;
import java.util.Map;

public class GroupBlock implements HasId {
    private final String id, rfpId, hotelId, blockCode;
    private final LocalDate startDate, endDate, cutoffDate;
    private final int totalRooms;
    private int pickedUpRooms;
    private final Money rate;
    private String status;

    public GroupBlock(String id, String rfpId, String hotelId, String blockCode,
                       LocalDate startDate, LocalDate endDate, int totalRooms,
                       int pickedUpRooms, Money rate, LocalDate cutoffDate, String status) {
        this.id = id; this.rfpId = rfpId; this.hotelId = hotelId; this.blockCode = blockCode;
        this.startDate = startDate; this.endDate = endDate;
        this.totalRooms = totalRooms; this.pickedUpRooms = pickedUpRooms;
        this.rate = rate; this.cutoffDate = cutoffDate; this.status = status;
    }

    @Override public String getId() { return id; }
    public String getRfpId() { return rfpId; }
    public String getHotelId() { return hotelId; }
    public Map<String, Object> getHotel() { return Map.of("id", hotelId); }
    public String getBlockCode() { return blockCode; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getTotalRooms() { return totalRooms; }
    public int getPickedUpRooms() { return pickedUpRooms; }
    public int getRemainingRooms() { return Math.max(0, totalRooms - pickedUpRooms); }
    public Money getRate() { return rate; }
    public LocalDate getCutoffDate() { return cutoffDate; }
    public String getStatus() { return status; }
    public void pickUp(int rooms) { this.pickedUpRooms += rooms; }
    public void setStatus(String s) { this.status = s; }
}
