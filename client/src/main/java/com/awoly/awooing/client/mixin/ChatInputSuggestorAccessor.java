package com.awoly.awooing.client.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatInputSuggestor.class)
public interface ChatInputSuggestorAccessor {
    @Accessor("completingSuggestions")
    boolean isCompletingSuggestions();

    @Accessor("pendingSuggestions")
    void setPendingSuggestions(CompletableFuture<Suggestions> suggestions);
}
