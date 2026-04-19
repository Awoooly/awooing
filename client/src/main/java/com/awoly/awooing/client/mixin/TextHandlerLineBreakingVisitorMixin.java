package com.awoly.awooing.client.mixin;

import com.awoly.awooing.client.Utils;
import com.awoly.awooing.client.emoji.EmojiRegistry;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.font.TextHandler$LineBreakingVisitor")
public abstract class TextHandlerLineBreakingVisitorMixin {

    @Shadow
    private int lastSpaceBreak;
    @Shadow
    private Style lastSpaceStyle;
    @Shadow
    private int startOffset;

    /** Allows line breaking to split before an emoji glyph as a valid wrap point. */
    @Inject(method = "accept(ILnet/minecraft/text/Style;I)Z", at = @At("HEAD"))
    private void markEmojiBreakBefore(int i, Style style, int codePoint, CallbackInfoReturnable<Boolean> cir) {
        if (Utils.isEmojiGlyphStyled(style) && EmojiRegistry.getById(codePoint) != null) {
            lastSpaceBreak = i + startOffset;
            lastSpaceStyle = style;
        }
    }

    /** Allows line breaking to split after an emoji glyph as a valid wrap point. */
    @Inject(method = "accept(ILnet/minecraft/text/Style;I)Z", at = @At("RETURN"))
    private void markEmojiBreakAfter(int i, Style style, int codePoint, CallbackInfoReturnable<Boolean> cir) {
        if (Utils.isEmojiGlyphStyled(style) && EmojiRegistry.getById(codePoint) != null) {
            lastSpaceBreak = i + startOffset + Character.charCount(codePoint);
            lastSpaceStyle = style;
        }
    }
}
