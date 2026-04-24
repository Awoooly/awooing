package com.awoly.awooing.client;

import com.awoly.awooing.client.command.AwooChatCommand;
import com.awoly.awooing.client.command.AwooCommand;
import com.awoly.awooing.client.command.AwooMessageCommand;
import com.awoly.awooing.client.command.AwooReplyCommand;
import com.awoly.awooing.client.command.ForwardChatCommand;
import com.awoly.awooing.client.config.ConfigManager;
import com.awoly.awooing.client.emoji.EmojiRegistry;
import com.awoly.awooing.client.event.ChatListener;
import com.awoly.awooing.client.event.CommandListener;
import com.awoly.awooing.common.PermissionType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Awooing implements ClientModInitializer {

    private static Awooing INSTANCE;
    public ChatClient chatClient;
    public boolean isAwooing;
    public PermissionType permissionType = PermissionType.USER;
    public String forwardAs;
    public String lastPrivateSender;
    public String currentRoomId;
    public static final Logger LOGGER = LoggerFactory.getLogger("awooing.client");
    public static final int DEFAULT_PORT = 29125;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        ConfigManager.load();
        EmojiRegistry.initialize();

        ClientCommandRegistrationCallback.EVENT.register(AwooCommand::register);
        ClientCommandRegistrationCallback.EVENT.register(AwooChatCommand::register);
        ClientCommandRegistrationCallback.EVENT.register(ForwardChatCommand::register);
        ClientCommandRegistrationCallback.EVENT.register(AwooMessageCommand::register);
        ClientCommandRegistrationCallback.EVENT.register(AwooReplyCommand::register);

        ChatListener.register();
        CommandListener.register();
        // DisconnectListener.register();

        if (ConfigManager.config.autoConnect && !ConfigManager.config.lastIp.isBlank()) {
            AwooCommand.connectToHost(ConfigManager.config.lastIp, ConfigManager.config.lastPort);
        }
    }

    public static Awooing getInstance() {
        return INSTANCE;
    }
}
