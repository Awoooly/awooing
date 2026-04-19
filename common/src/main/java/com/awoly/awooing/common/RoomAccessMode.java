package com.awoly.awooing.common;

public enum RoomAccessMode {
    PUBLIC("public"),
    PRIVATE("private");

    private final String value;

    RoomAccessMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RoomAccessMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return PUBLIC;
        }

        for (RoomAccessMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value)
                || (mode == PRIVATE && (value.equalsIgnoreCase("invite-only") || value.equalsIgnoreCase("password")))) {
                return mode;
            }
        }

        throw new IllegalArgumentException("Unknown room access mode: " + value);
    }
}