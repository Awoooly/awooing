package com.awoly.awooing.client.mixin;

import com.awoly.awooing.client.Utils;
import com.awoly.awooing.client.sprite.Sprite;
import com.awoly.awooing.client.sprite.SpriteRegistry;
import com.awoly.awooing.client.sprite.SpriteSubstitutingText;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.suggestion.Suggestion;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screen.ChatInputSuggestor$SuggestionWindow")
public abstract class SuggestionWindowMixin {
    @Shadow
    private List<Suggestion> suggestions;
    @Shadow
    private String typedText;
    @Shadow
    private Rect2i area;

    /** Shifts the suggestion window so emoji-rendered text still lines up with the typed prefix. */
    @Inject(
            method = "<init>(Lnet/minecraft/client/gui/screen/ChatInputSuggestor;IIILjava/util/List;Z)V",
            at = @At("TAIL"))
    private void correctWindowPosition(ChatInputSuggestor suggestor, int x, int y, int width,
            List<Suggestion> suggestions, boolean narrateFirst, CallbackInfo ci) {
        if (suggestions.isEmpty()) {
            return;
        }

        int startIndex = suggestions.get(0).getRange().getStart();
        String prefix = this.typedText.substring(0, startIndex);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        OrderedText prefixOrdered = Language.getInstance()
                .reorder(Text.literal(prefix).setStyle(Style.EMPTY.withFont(Utils.SPRITE_FONT)));
        int correction = tr.getWidth(new SpriteSubstitutingText(prefixOrdered)) - tr.getWidth(prefix);

        this.area.setX(this.area.getX() + correction);
    }

    /** Renders emoji suggestions with the glyph substitution used elsewhere in chat. */
    @WrapOperation(
            method = "render(Lnet/minecraft/client/gui/DrawContext;II)V",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/gui/DrawContext;"
                    + "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;"
                    + "Ljava/lang/String;III)V"))
    private void drawEmojiSuggestion(
        DrawContext context,
        TextRenderer textRenderer,
        String text,
        int x,
        int y,
        int color,
        Operation<Void> original) {
        // Validation check
        if (!text.startsWith(":") || !text.endsWith(":") || text.length() < 3) {
            original.call(context, textRenderer, text, x, y, color);
            return;
        }

        String name = text.substring(1, text.length() - 1);
        Sprite emoji = SpriteRegistry.getByName(name);
        if (emoji == null) {
            original.call(context, textRenderer, text, x, y, color);
            return;
        }

        // 1. Draw the emoji icon
        OrderedText emojiOnly = Language.getInstance()
                .reorder(Text.literal(text).setStyle(Style.EMPTY.withFont(Utils.SPRITE_FONT)));
        context.drawTextWithShadow(textRenderer, new SpriteSubstitutingText(emojiOnly), x, y, color);

        // 2. Draw the text label next to it
        int emojiGlyphWidth = (int) emoji.getOrCreateGlyph().getMetrics().getAdvance();
        int spaceWidth = textRenderer.getWidth(" ");
        
        // We don't use 'original.call' for the second part because we need to offset 'x'. 
        // We just call the context directly.
        context.drawTextWithShadow(textRenderer, text, x + emojiGlyphWidth + spaceWidth, y, color);
    }

    /** Expands the background so emoji suggestions do not clip past the panel edge. */
    @WrapOperation(
        method = "render(Lnet/minecraft/client/gui/DrawContext;II)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    private void widenEmojiBackground(DrawContext context, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        // Calculate the extra width needed
        int maxExtra = suggestions.stream()
            .map(Suggestion::getText)
            .filter(t -> t.startsWith(":") && t.endsWith(":") && t.length() > 2)
            .map(t -> SpriteRegistry.getByName(t.substring(1, t.length() - 1)))
            .filter(e -> e != null)
            .mapToInt(e -> (int) e.getOrCreateGlyph().getMetrics().getAdvance() + tr.getWidth(" "))
            .max()
            .orElse(0);

        // Call the original fill but with the modified x2
        original.call(context, x1, y1, x2 + maxExtra, y2, color);
    }
}
