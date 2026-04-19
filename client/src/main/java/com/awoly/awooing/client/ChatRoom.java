package com.awoly.awooing.client;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom {
    private final String name;
    private volatile boolean leader;
    private final Map<String, Integer> userColors = new ConcurrentHashMap<>();

    public ChatRoom(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean leader) {
        this.leader = leader;
    }

    public Set<String> getUsers() {
        return userColors.keySet();
    }

    public boolean hasUser(String username) {
        return userColors.containsKey(username);
    }

    public Integer getUserColor(String username) {
        return userColors.get(username);
    }

    public void setUserColor(String username, int color) {
        userColors.put(username, color);
    }

    public void removeUser(String username) {
        userColors.remove(username);
    }

    public boolean hasNoUsers() {
        return userColors.isEmpty();
    }

    public void replaceUsers(Map<String, Integer> users) {
        userColors.clear();
        if (users != null) {
            userColors.putAll(users);
        }
    }
}