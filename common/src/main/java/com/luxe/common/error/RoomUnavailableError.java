package com.luxe.common.error;

public record RoomUnavailableError(String code, String message) {
    public RoomUnavailableError(String message) {
        this("ROOM_UNAVAILABLE", message);
    }
}
