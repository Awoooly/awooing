package com.awoly.awooing.client.mixin;

import com.awoly.awooing.client.Utils;
import com.awoly.awooing.client.emoji.EmojiSubstitutingText;
import com.awoly.awooing.client.emoji.EmojiSubstitutingVisitable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
        return new EmojiSubstitutingVisitable(text);
    }

    /** Uses emoji substitution when resolving the style under the chat hover cursor. */
    @WrapOperation(
        method = "getStyleAt(Lnet/minecraft/text/OrderedText;I)Lnet/minecraft/text/Style;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/text/OrderedText;accept(Lnet/minecraft/text/CharacterVisitor;)Z")
    )
    private boolean substituteEmojisDuringStyleLookup(
        OrderedText text,
        net.minecraft.text.CharacterVisitor visitor,
        Operation<Boolean> original) {
        final boolean[] hasCandidate = {false};
        
        // 1. Check for emoji candidates
        text.accept((index, style, codePoint) -> {
            if (Utils.isEmojiGlyphStyled(style) || codePoint == ':') {
                hasCandidate[0] = true;
                return false;
            }
            return true;
        });

        if (!hasCandidate[0]) {
            return original.call(text, visitor);
        }

        OrderedText marked = v -> text.accept((index, style, codePoint) -> {
            Style s = Utils.isEmojiGlyphStyled(style) ? style.withFont(Utils.EMOJI_FONT) : style;
            return v.accept(index, s, codePoint);
        });

        return original.call(new EmojiSubstitutingText(marked), visitor);
    }
}