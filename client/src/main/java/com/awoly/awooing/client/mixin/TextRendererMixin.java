package com.awoly.awooing.client.mixin;

import com.awoly.awooing.client.Utils;
import com.awoly.awooing.client.emoji.Emoji;
import com.awoly.awooing.client.emoji.EmojiRegistry;
import com.awoly.awooing.client.emoji.EmojiSubstitutingText;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextRenderer.class)
public abstract class TextRendererMixin {
    /** Swaps in emoji substitution before text is prepared for rendering. */
    @Inject(
            method = "prepare(Lnet/minecraft/text/OrderedText;FFIZZI)"
                    + "Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;",
            at = @At("HEAD"),
            cancellable = true)
    private void wrapPrepare(OrderedText text, float x, float y, int color, boolean shadow, boolean trackEmpty,
            int backgroundColor, CallbackInfoReturnable<TextRenderer.GlyphDrawable> cir) {
        if (text instanceof EmojiSubstitutingText) {
            return;
        }
        if (EmojiRegistry.getAll().isEmpty()) {
            return;
        }
        if (!hasEmojiFont(text)) {
            return;
        }
        cir.setReturnValue(
                ((TextRenderer) (Object) this)
                        .prepare(new EmojiSubstitutingText(text), x, y, color, shadow, trackEmpty, backgroundColor));
    }

    private static boolean hasEmojiFont(OrderedText text) {
        boolean[] found = {false};
        text.accept((index, style, codePoint) -> {
            if (Utils.isEmojiStyled(style) || Utils.isEmojiGlyphStyled(style)) {
                found[0] = true;
                return false;
            }
            return true;
        });
        return found[0];
    }

    /** Uses the emoji glyph when Minecraft asks for the glyph of an emoji codepoint. */
    @Inject(
            method = "getGlyph(ILnet/minecraft/text/Style;)Lnet/minecraft/client/font/BakedGlyph;",
            at = @At("HEAD"),
            cancellable = true)
    private void onGetGlyph(int codePoint, Style style, CallbackInfoReturnable<BakedGlyph> cir) {
        if (!Utils.isEmojiGlyphStyled(style)) {
            return;
        }

        Emoji emoji = EmojiRegistry.getById(codePoint);
        if (emoji != null) {
            cir.setReturnValue(emoji.getOrCreateGlyph());
        }
    }

    /** Makes TextHandler measure emoji glyphs with their actual advance width. */
    @ModifyArg(
            method = "<init>(Lnet/minecraft/client/font/TextRenderer$GlyphsProvider;)V",
            at = @At(
                    value = "INVOKE",
                target = "Lnet/minecraft/client/font/TextHandler;<init>("
                    + "Lnet/minecraft/client/font/TextHandler$WidthRetriever;)V"))
    private TextHandler.WidthRetriever wrapWidthRetriever(TextHandler.WidthRetriever original) {
        return (codePoint, style) -> {
            if (Utils.isEmojiStyled(style)) {
                return original.getWidth(codePoint, Style.EMPTY);
            }

            if (!Utils.isEmojiGlyphStyled(style)) {
                return original.getWidth(codePoint, style);
            }

            Emoji emoji = EmojiRegistry.getById(codePoint);
            if (emoji != null) {
                return emoji.getOrCreateGlyph().getMetrics().getAdvance();
            }
            return original.getWidth(codePoint, Style.EMPTY);
        };
    }
}
