package com.luxe.corporate.schema.types;

import com.luxe.common.pagination.HasId;
import java.util.Map;

public class CorporateTraveler implements HasId {
    private final String id, accountId, guestId, employeeId, department, costCenter,
            managerEmail, travelClass;
    private boolean active;

    public CorporateTraveler(String id, String accountId, String guestId,
                              String employeeId, String department, String costCenter,
                              String managerEmail, String travelClass, boolean active) {
        this.id = id; this.accountId = accountId; this.guestId = guestId;
        this.employeeId = employeeId; this.department = department; this.costCenter = costCenter;
        this.managerEmail = managerEmail; this.travelClass = travelClass; this.active = active;
    }

    @Override public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public String getGuestId() { return guestId; }
    public Map<String, Object> getGuest() {
        return guestId != null ? Map.of("id", guestId) : null;
    }
    public String getEmployeeId() { return employeeId; }
    public String getDepartment() { return department; }
    public String getCostCenter() { return costCenter; }
    public String getManagerEmail() { return managerEmail; }
    public String getTravelClass() { return travelClass; }
    public boolean isActive() { return active; }
    public void deactivate() { this.active = false; }
}
