package com.luxe.meetings.datasource;

import com.luxe.meetings.schema.types.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MeetingsDataSource {

    List<EventSpace> findSpacesByHotel(String hotelId, Map<String, Object> filter);
    Optional<EventSpace> findSpaceById(String id);
    List<EventSpace> searchSpaces(Map<String, Object> input);

    List<CateringMenu> findCateringMenus(String hotelId);

    Optional<RFP> findRFPById(String id);
    List<RFP> findRFPsByOrganizer(String email, String status);
    RFP submitRFP(Map<String, Object> input, String organizerEmail);
    RFP updateRFP(String rfpId, Map<String, Object> input);
    RFP cancelRFP(String rfpId, String reason, String actor);

    Optional<GroupBlock> findGroupBlockById(String id);
    GroupBlock createGroupBlock(Map<String, Object> input);
}
