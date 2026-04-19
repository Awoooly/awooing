package com.awoly.awooing.client.mixin;

import com.awoly.awooing.client.Utils;
import com.awoly.awooing.client.emoji.Emoji;
import com.awoly.awooing.client.emoji.EmojiRegistry;
import com.awoly.awooing.client.emoji.EmojiSubstitutingText;
import com.awoly.awooing.client.widget.EmojiTextFieldWidget;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {
    @Shadow private int textX;
    @Shadow private String text;
    @Shadow private int firstCharacterIndex;
    @Shadow private TextRenderer textRenderer;

    /** Injects the emoji font marker into every character so downstream rendering can identify this as emoji-capable text. */
    @Inject(method = "format", at = @At("RETURN"), cancellable = true)
    private void injectFontToOrderedText(String slice, int firstCharacterIndex,
                                         CallbackInfoReturnable<OrderedText> cir) {
        if (!((Object) this instanceof EmojiTextFieldWidget)) {
            return;
        }
        if (!Utils.isTypingAwooMessage(((TextFieldWidget) (Object) this).getText())) {
            return;
        }

        OrderedText original = cir.getReturnValue();
        cir.setReturnValue(visitor -> original.accept(
                (index, style, codePoint) -> visitor.accept(index, style.withFont(Utils.EMOJI_FONT), codePoint)));
    }

    /** Redirects the trimToWidth call inside renderWidget to the emoji-aware version. */
    @WrapOperation(
            method = "renderWidget",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/font/TextRenderer;trimToWidth(Ljava/lang/String;I)Ljava/lang/String;"))
    private String emojiAwareTrimToWidth(TextRenderer renderer, String str, int maxWidth, Operation<String> original) {
        if (!((Object) this instanceof EmojiTextFieldWidget emojiWidget) || !Utils.isTypingAwooMessage(this.text)) {
            return original.call(renderer, str, maxWidth);
        }
        return emojiWidget.emojiAwareTrimToWidth(str, maxWidth);
    }

    /** Redirects getWidth(OrderedText) to wrap with EmojiSubstitutingText for correct glyph width measurement. */
    @WrapOperation(
            method = "renderWidget",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Lnet/minecraft/text/OrderedText;)I"))
    private int emojiAwareGetWidth(TextRenderer renderer, OrderedText orderedText, Operation<Integer> original) {
        if (!((Object) this instanceof EmojiTextFieldWidget emojiWidget) || !Utils.isTypingAwooMessage(this.text)) {
            return original.call(renderer, orderedText);
        }
        return emojiWidget.getEmojiAwareWidth(new EmojiSubstitutingText(orderedText));
    }

    /**
     * Redirects getWidth(String) inside renderWidget.
     * This fixes the math for the cursor offset and the blue selection highlight.
     */
    @WrapOperation(
            method = "renderWidget",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Ljava/lang/String;)I"))
    private int emojiAwareStringWidth(TextRenderer renderer, String str, Operation<Integer> original) {
        if (!((Object) this instanceof EmojiTextFieldWidget emojiWidget) || !Utils.isTypingAwooMessage(this.text)) {
            return original.call(renderer, str);
        }
        return emojiWidget.getEmojiAwareWidth(str);
    }

    /** Keeps the horizontal scroll position aligned with emoji-aware width calculations. */
    @Inject(method = "updateFirstCharacterIndex(I)V", at = @At("HEAD"), cancellable = true)
    private void emojiAwareUpdateFirstCharacterIndex(int cursor, CallbackInfo ci) {
        if (!((Object) this instanceof EmojiTextFieldWidget) || !Utils.isTypingAwooMessage(text) || text == null) {
            return;
        }

        int innerWidth = ((TextFieldWidget) (Object) this).getInnerWidth();
        float width = 0f;
        int i = cursor;

        while (i > 0) {
            if (i > 1 && text.charAt(i - 1) == ':') {
                int closeColon = i - 1;
                int k = closeColon - 1;
                while (k >= 0 && Utils.isTokenChar(text.charAt(k))) {
                    k--;
                }
                if (k >= 0 && text.charAt(k) == ':' && closeColon > k + 1) {
                    String name = text.substring(k + 1, closeColon);
                    Emoji emoji = EmojiRegistry.get(name);
                    if (emoji != null) {
                        float emojiWidth = emoji.getOrCreateGlyph().getMetrics().getAdvance();
                        if (width + emojiWidth > innerWidth) {
                            break;
                        }
                        width += emojiWidth;
                        i = k;
                        continue;
                    }
                }
            }
            float charWidth = textRenderer.getTextHandler().getWidth(text.substring(i - 1, i));
            if (width + charWidth > innerWidth) {
                break;
            }
            width += charWidth;
            i--;
        }

        firstCharacterIndex = i;
        ci.cancel();
    }
}