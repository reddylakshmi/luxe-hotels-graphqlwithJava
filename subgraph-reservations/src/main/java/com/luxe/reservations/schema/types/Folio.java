package com.luxe.reservations.schema.types;

import com.luxe.common.scalar.Money;
import java.time.LocalDate;
import java.util.List;

public class Folio {
    private final String id, reservationId, currency, status;
    private final List<FolioLineItem> lineItems;
    private final Money subtotal, taxes, total;

    public Folio(String id, String reservationId, String currency,
                  List<FolioLineItem> lineItems, Money subtotal, Money taxes, Money total,
                  String status) {
        this.id = id; this.reservationId = reservationId; this.currency = currency;
        this.lineItems = lineItems; this.subtotal = subtotal; this.taxes = taxes;
        this.total = total; this.status = status;
    }

    public String getId() { return id; }
    public String getReservationId() { return reservationId; }
    public String getCurrency() { return currency; }
    public List<FolioLineItem> getLineItems() { return lineItems; }
    public Money getSubtotal() { return subtotal; }
    public Money getTaxes() { return taxes; }
    public Money getTotal() { return total; }
    public String getStatus() { return status; }

    public record FolioLineItem(String id, LocalDate date, String description,
                                 Money amount, String category, int quantity) {}
}
