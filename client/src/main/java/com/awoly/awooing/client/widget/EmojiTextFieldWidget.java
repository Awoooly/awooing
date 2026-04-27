package com.awoly.awooing.client.widget;

import com.awoly.awooing.client.Utils;
import com.awoly.awooing.client.sprite.Sprite;
import com.awoly.awooing.client.sprite.SpriteRegistry;

import java.util.regex.Matcher;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class EmojiTextFieldWidget extends TextFieldWidget {

    private final TextRenderer emojiTextRenderer;

    public EmojiTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text message) {
        super(textRenderer, x, y, width, height, message);
        this.emojiTextRenderer = textRenderer;
    }

    public EmojiTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height,
            TextFieldWidget copyFrom, Text message) {
        super(textRenderer, x, y, width, height, copyFrom, message);
        this.emojiTextRenderer = textRenderer;
    }

    /*
     * Makes the cursor treat emojis like a single letter
     */
    @Override
    public void setCursor(int cursor, boolean selecting) {
        String text = getText();
        if (text != null && cursor > 0 && cursor < text.length() && Utils.isTypingAwooMessage(text)) {
            int current = getCursor();
            int delta = Math.abs(cursor - current);
            if (delta == 1) {
                boolean movingRight = cursor > current;
                Matcher matcher = Utils.EMOJI_PATTERN.matcher(text);
                while (matcher.find()) {
                    String name = matcher.group(1);
                    if (SpriteRegistry.getByName(name) != null) {
                        int tokenStart = matcher.start();
                        int tokenEnd = matcher.end();
                        if (cursor > tokenStart && cursor < tokenEnd) {
                            cursor = movingRight ? tokenEnd : tokenStart;
                            break;
                        }
                    }
                }
            }
        }
        super.setCursor(cursor, selecting);
    }

    /*
     * Makes sure that one keypress of delete or backspace deletes an entire emoji. 
     */
    @Override
    public boolean keyPressed(KeyInput input) {
        String text = getText();
        if (text != null && Utils.isTypingAwooMessage(text)) {
            int cursor = getCursor();
            boolean isBackspace = input.getKeycode() == GLFW.GLFW_KEY_BACKSPACE;
            boolean isDelete = input.getKeycode() == GLFW.GLFW_KEY_DELETE;
            if (isBackspace || isDelete) {
                Matcher matcher = Utils.EMOJI_PATTERN.matcher(text);
                while (matcher.find()) {
                    if (SpriteRegistry.getByName(matcher.group(1)) != null) {
                        if (isBackspace && matcher.end() == cursor) {
                            setText(text.substring(0, matcher.start()) + text.substring(matcher.end()));
                            super.setCursor(matcher.start(), false);
                            return true;
                        }
                        if (isDelete && matcher.start() == cursor) {
                            setText(text.substring(0, matcher.start()) + text.substring(matcher.end()));
                            return true;
                        }
                    }
                }
            }
        }
        return super.keyPressed(input);
    }

    /**
     * Measures the emoji-aware pixel width of an OrderedText. Already-substituted
     * emoji codepoints (glyph-styled) are measured via their advance width;
     * everything else falls back to the width retriever.
     */
    public int getEmojiAwareWidth(OrderedText text) {
        float[] width = {0f};
        text.accept((index, style, codePoint) -> {
            if (Utils.isEmojiGlyphStyled(style)) {
                Sprite emoji = SpriteRegistry.getById(codePoint);
                if (emoji != null) {
                    width[0] += emoji.getOrCreateGlyph().getMetrics().getAdvance();
                    return true;
                }
            }
            width[0] += emojiTextRenderer.getTextHandler().getWidth(Character.toString(codePoint));
            return true;
        });
        return Math.round(width[0]);
    }

    /**
     * Measures the emoji-aware pixel width of a raw String. Critical for
     * calculating the highlight box and cursor coordinates.
     */
    public int getEmojiAwareWidth(String str) {
        float width = 0f;
        int i = 0;
        while (i < str.length()) {
            char c = str.charAt(i);
            if (c == ':') {
                int j = i + 1;
                while (j < str.length() && isTokenChar(str.charAt(j))) {
                    j++;
                }
                if (j < str.length() && str.charAt(j) == ':' && j > i + 1) {
                    String name = str.substring(i + 1, j);
                    Sprite emoji = SpriteRegistry.getByName(name);
                    if (emoji != null) {
                        width += emoji.getOrCreateGlyph().getMetrics().getAdvance();
                        i = j + 1;
                        continue;
                    }
                }
            }
            width += emojiTextRenderer.getTextHandler().getWidth(String.valueOf(c));
            i++;
        }
        return Math.round(width);
    }

    /** Trims str to fit within maxWidth pixels, emoji-aware. */
    public String emojiAwareTrimToWidth(String str, int maxWidth) {
        float width = 0f;
        int i = 0;
        while (i < str.length()) {
            char c = str.charAt(i);
            if (c == ':') {
                int j = i + 1;
                while (j < str.length() && isTokenChar(str.charAt(j))) {
                    j++;
                }
                if (j < str.length() && str.charAt(j) == ':' && j > i + 1) {
                    String name = str.substring(i + 1, j);
                    Sprite emoji = SpriteRegistry.getByName(name);
                    if (emoji != null) {
                        float emojiWidth = emoji.getOrCreateGlyph().getMetrics().getAdvance();
                        if (width + emojiWidth > maxWidth) {
                            return str.substring(0, i);
                        }
                        width += emojiWidth;
                        i = j + 1;
                        continue;
                    }
                }
            }
            float charWidth = emojiTextRenderer.getTextHandler().getWidth(String.valueOf(c));
            if (width + charWidth > maxWidth) {
                return str.substring(0, i);
            }
            width += charWidth;
            i++;
        }
        return str;
    }

    private static boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
