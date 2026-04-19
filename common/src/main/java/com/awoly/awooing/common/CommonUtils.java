package com.awoly.awooing.common;

import java.util.regex.Pattern;

public final class CommonUtils {
    private static final Pattern ROOM_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{1,10}$");

    private CommonUtils() {
    }

    public static boolean isValidRoomName(String roomName) {
        return roomName != null && ROOM_NAME_PATTERN.matcher(roomName).matches();
    }

    public static String getRoomNameErrorMessage() {
        return "Room names must be 1-10 characters long and contain only letters and numbers";
    }

    public static int versionToInt(String version) {
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return major * 1_000_000 + minor * 1_000 + patch;
        } catch (RuntimeException e) {
            return -1;
        }
    }
}
