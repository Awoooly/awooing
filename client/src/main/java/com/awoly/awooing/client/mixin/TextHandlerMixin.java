package com.awoly.awooing.client.mixin;

import net.minecraft.client.font.TextHandler;
import net.minecraft.text.StringVisitable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.awoly.awooing.client.sprite.SpriteSubstitutingVisitable;

@Mixin(TextHandler.class)
public abstract class TextHandlerMixin {
    private static final String WRAP_LINES_METHOD =
        "wrapLines(Lnet/minecraft/text/StringVisitable;"
            + "ILnet/minecraft/text/Style;Ljava/util/function/BiConsumer;)V";

    /** Makes sure an emoji isn't split into two lines. */
    @ModifyVariable(
        method = WRAP_LINES_METHOD,
        at = @At("HEAD"),
        index = 1,
        argsOnly = true)
    private StringVisitable injectEmojiLineBreaks(StringVisitable text) {
        if (!text.getString().contains(":")) {
            return text;
        }
        return new SpriteSubstitutingVisitable(text);
    }
}