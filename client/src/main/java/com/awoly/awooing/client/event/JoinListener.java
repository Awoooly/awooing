package com.awoly.awooing.client.event;

import com.awoly.awooing.client.Utils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class JoinListener {

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Utils.flushMessageBuffer();
        });
    }
}
