package com.luxe.reservations.datasource;

import com.luxe.reservations.schema.types.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReservationDataSource {
    Optional<Reservation> findById(String id);
    Optional<Reservation> findByConfirmationNumber(String confirmationNumber, String guestLastName);
    List<Reservation> findByGuestId(String guestId, String status, String hotelId, String sortBy);
    List<Reservation> findByHotelId(String hotelId);
    List<Reservation> findUpcoming(String hotelId);

    Reservation create(Map<String, Object> input, String guestId);
    Reservation confirm(String id, String paymentMethodId, String guestProfileId);
    Reservation modify(String id, Map<String, Object> input);
    Reservation cancel(String id, String reason, boolean acceptPenalty);
    Reservation mobileCheckIn(String id, Map<String, Object> input);
    Reservation expressCheckout(String id);
    Reservation requestUpgrade(String id, Map<String, Object> input);
    Reservation applyGiftCard(String id, String giftCardCode);

    CheckInEligibility checkInEligibility(String reservationId);

    List<DiningReservation> findDiningByGuestId(String guestId, Boolean upcoming);
    DiningReservation createDining(Map<String, Object> input, String guestId);

    List<SpaAppointment> findSpaByGuestId(String guestId, Boolean upcoming);
    SpaAppointment bookSpa(Map<String, Object> input, String guestId);

    List<TransportationBooking> findTransportByGuestId(String guestId, Boolean upcoming);
    TransportationBooking bookTransport(Map<String, Object> input, String guestId);
}
