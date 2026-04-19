package com.awoly.awooing.client.event;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.isClientConnected;
import static com.awoly.awooing.client.Utils.renderMsg;

import com.awoly.awooing.client.Awooing;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public final class DisconnectListener {

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register(DisconnectListener::onDisconnect);
    }

    public static void onDisconnect(ClientPlayNetworkHandler handler, MinecraftClient client) {
        if (isClientConnected()) {
            try {
                Awooing.getInstance().chatClient.close();
            } catch (Exception e) {
                renderMsg(INFO_COLOR, "Error closing chat client");
                e.printStackTrace();
            }
            renderMsg(INFO_COLOR, "Disconnected from chat server because you left the world");
        }
    }
}
