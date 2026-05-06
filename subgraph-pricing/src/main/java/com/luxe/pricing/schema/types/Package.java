package com.luxe.pricing.schema.types;

import com.luxe.common.pagination.HasId;
import java.time.LocalDate;
import java.util.List;

public class Package implements HasId {
    private final String id, code, name, description, hotelId;
    private final List<String> includes;
    private final double priceModifier;
    private final LocalDate validFrom, validTo;
    private final boolean active;

    public Package(String id, String code, String name, String description, String hotelId,
                   List<String> includes, double priceModifier,
                   LocalDate validFrom, LocalDate validTo, boolean active) {
        this.id = id; this.code = code; this.name = name; this.description = description;
        this.hotelId = hotelId; this.includes = includes; this.priceModifier = priceModifier;
        this.validFrom = validFrom; this.validTo = validTo; this.active = active;
    }

    @Override public String getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getHotelId() { return hotelId; }
    public List<String> getIncludes() { return includes; }
    public double getPriceModifier() { return priceModifier; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public boolean isActive() { return active; }
}
