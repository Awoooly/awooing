package com.awoly.awooing.client.mixin;

import com.awoly.awooing.client.Awooing;
import com.awoly.awooing.client.ChatRoom;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.Locale;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket.NodeFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandTreeS2CPacket.class)
public class CommandTreeMixin {

    @Inject(method = "getCommandTree", at = @At("RETURN"), cancellable = true)
    private <S> void injectChatAwoo(CommandRegistryAccess registry, NodeFactory<S> factory,
            CallbackInfoReturnable<RootCommandNode<S>> cir) {

        RootCommandNode<S> root = cir.getReturnValue();

        CommandNode<S> chatNode = root.getChild("chat");
        if (chatNode == null) {
            chatNode = factory.literal("chat").build();
            root.addChild(chatNode);
        }

        CommandNode<S> awooNode = chatNode.getChild("awoo");
        if (awooNode == null) {
            awooNode = LiteralArgumentBuilder.<S>literal("awoo")
                    .build();
            chatNode.addChild(awooNode);
        }

        if (awooNode.getChild("room") != null) {
            cir.setReturnValue(root);
            return;
        }

        SuggestionProvider<S> roomSuggestions = (ctx, builder) -> {
            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

            for (ChatRoom room : Awooing.getInstance().chatClient.getJoinedRooms().values()) {
                String name = room.getName();
                if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(name);
                }
            }

            return builder.buildFuture();
        };

        CommandNode<S> roomArg =
                RequiredArgumentBuilder.<S, String>argument("room", StringArgumentType.word())
                        .suggests(roomSuggestions)
                        .build();

        awooNode.addChild(roomArg);
        cir.setReturnValue(root);
    }
}