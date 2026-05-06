package com.luxe.experiences.datasource;

import com.luxe.experiences.schema.types.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExperiencesDataSource {

    List<Experience> findExperiences(String hotelId, String category, LocalDate date);
    Optional<Experience> findExperienceById(String id);

    ExperienceAvailability availability(String experienceId, LocalDate date, int partySize);
    boolean isSlotValid(String slotToken);

    List<SpaTreatment> spaTreatments(String hotelId);

    RestaurantAvailability restaurantAvailability(String hotelId, LocalDate date, int partySize);
    GolfTeeTimeAvailability golfAvailability(String hotelId, LocalDate date, int players);

    ExperienceBooking bookExperience(String guestId, String experienceId,
                                       String slotToken, int participants, String specialRequests);
    ExperienceBooking bookDining(String guestId, String restaurantId,
                                  String slotToken, int partySize, String specialRequests);
    ExperienceBooking bookGolf(String guestId, String courseId, String slotToken,
                                int players, Boolean cartRequested);
    ExperienceBooking cancel(String bookingId, String reason);

    Optional<ExperienceBooking> findBookingById(String id);
    List<ExperienceBooking> findBookingsByGuestId(String guestId, Boolean upcoming);
}
