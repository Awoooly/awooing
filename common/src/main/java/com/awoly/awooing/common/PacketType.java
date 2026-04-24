package com.awoly.awooing.common;

public enum PacketType {
    MSG("msg"),
    INFO("info"),
    PERMISSION("permission"),
    SESSION_CHALLENGE("session_challenge"),
    AUTH_RESPONSE("auth_response"),
    ROOM_CREATE("room_create"),
    ROOM_JOIN("room_join"),
    ROOM_LEAVE("room_leave"),
    ROOM_LIST("room_list"),
    HOST_CHANGE("host_change"),
    USER_LIST("user_list"),
    FORWARD_MSG("forward_msg"),
    FORWARD_ALLOW("forward_allow"),
    FORWARD_REVOKE("forward_revoke"),
    FORWARD_LIST("forward_list"),
    FORWARD_IS_ALLOWED("forward_is_allowed"),
    FORWARD_REQUEST("forward_request"),
    CHANGE_COLOR("change_color"),
    CHANGE_PRIVACY("change_privacy"),
    CHANGE_PASSWORD("change_password"),
    ROOM_INVITE("room_invite"),
    KICK("kick"),
    PRIVATE_MSG("private_msg");

    private final String key;

    PacketType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return key;
    }

    public static PacketType fromString(String s) {
        if (s == null) {
            return null;
        }
        for (PacketType t : values()) {
            if (t.key.equalsIgnoreCase(s)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown packet type: " + s);
    }
}
