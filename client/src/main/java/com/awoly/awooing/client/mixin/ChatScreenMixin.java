package com.awoly.awooing.client.mixin;

import com.awoly.awooing.client.Utils;
import com.awoly.awooing.client.emoji.EmojiRegistry;
import com.awoly.awooing.client.widget.EmojiTextFieldWidget;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends net.minecraft.client.gui.screen.Screen {
    @Shadow
    protected TextFieldWidget chatField;
    @Shadow
    protected String originalChatText;
    @Shadow
    ChatInputSuggestor chatInputSuggestor;

    @Unique
    private String lastEmojiPartial = null;

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    /** Replaces the vanilla chat field with EmojiTextFieldWidget so emoji-aware behavior can participate in chat input. */
    @WrapOperation(
        method = "init",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/screen/ChatScreen;chatField:Lnet/minecraft/client/gui/widget/TextFieldWidget;",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void replaceChatFieldAssignment(ChatScreen instance, TextFieldWidget originalWidget, Operation<Void> original) {
        EmojiTextFieldWidget custom = new EmojiTextFieldWidget(
            this.textRenderer, 
            originalWidget.getX(), 
            originalWidget.getY(), 
            originalWidget.getWidth(), 
            originalWidget.getHeight(), 
            null, 
            Text.translatable("chat.editBox")
        );
        
        custom.setMaxLength(256);
        custom.setDrawsBackground(false);
        custom.setText(this.originalChatText);
        
        // Pass our perfectly-sized custom widget into the field assignment
        original.call(instance, custom);
    }

    /** Adds the emoji suggestion window to the ChatScreen. */
    @Inject(method = "init()V", at = @At("TAIL"))
    private void attachEmojiSuggestionListener(CallbackInfo ci) {
        if (!(chatField instanceof EmojiTextFieldWidget)) {
            return;
        }

        Consumer<String> existingListener = ((TextFieldWidgetAccessor) chatField).getChangedListener();

        chatField.setChangedListener(text -> {
            if (existingListener != null) {
                existingListener.accept(text);
            }
            updateEmojiSuggestions(text, chatField.getCursor());
        });
    }

    /** Builds and opens the emoji suggestion window for the current partial token. */
    private void updateEmojiSuggestions(String text, int cursor) {
        if (chatInputSuggestor == null) {
            return;
        }

        ChatInputSuggestorAccessor suggestorAccessor = (ChatInputSuggestorAccessor) chatInputSuggestor;

        if (suggestorAccessor.isCompletingSuggestions()) {
            return;
        }

        if (!Utils.isTypingAwooMessage(text)) {
            lastEmojiPartial = null;
            return;
        }

        int colonPos = -1;
        for (int i = cursor - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == ':') {
                if (i > 0 && Utils.isTokenChar(text.charAt(i - 1))) {
                    int k = i - 1;
                    while (k >= 0 && Utils.isTokenChar(text.charAt(k))) {
                        k--;
                    }
                    if (k >= 0 && text.charAt(k) == ':') {
                        break;
                    }
                }
                colonPos = i;
                break;
            }
            if (!Character.isLetterOrDigit(c) && c != '_') {
                break;
            }
        }

        if (colonPos == -1 || cursor <= colonPos + 1) {
            chatInputSuggestor.clearWindow();
            lastEmojiPartial = null;
            return;
        }

        String partial = text.substring(colonPos + 1, cursor);
        if (partial.isEmpty() || !partial.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_')) {
            chatInputSuggestor.clearWindow();
            lastEmojiPartial = null;
            return;
        }

        if (partial.equals(lastEmojiPartial) && chatInputSuggestor.isOpen()) {
            return;
        }
        lastEmojiPartial = partial;

        final int finalColonPos = colonPos;

        List<Suggestion> suggestions = EmojiRegistry.getAll().keySet().stream().filter(name -> name.startsWith(partial))
                .limit(10)
                .map(name -> new Suggestion(StringRange.between(finalColonPos, finalColonPos + partial.length() + 1),
                        ":" + name + ":"))
                .collect(Collectors.toList());

        if (suggestions.isEmpty()) {
            chatInputSuggestor.clearWindow();
            return;
        }

        Suggestions brigadier = new Suggestions(
                StringRange.between(finalColonPos, finalColonPos + partial.length() + 1), suggestions);

        suggestorAccessor.setPendingSuggestions(CompletableFuture.completedFuture(brigadier));

        if (!chatInputSuggestor.isOpen()) {
            chatInputSuggestor.show(false);
        }
    }
}