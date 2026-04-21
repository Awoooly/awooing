package com.awoly.awooing.client.emoji;

import static com.awoly.awooing.client.Utils.EMOJI_GLYPH_FONT;
import static com.awoly.awooing.client.Utils.text;

import com.awoly.awooing.client.Utils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource.Font;
import net.minecraft.util.Formatting;
 

public final class EmojiSubstitutingText implements OrderedText {

    private final OrderedText delegate;

    public EmojiSubstitutingText(OrderedText delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean accept(CharacterVisitor visitor) {
        List<int[]> codePoints = new ArrayList<>();
        List<Style> styles = new ArrayList<>();

        delegate.accept((index, style, codePoint) -> {
            codePoints.add(new int[]{codePoint});
            styles.add(style);
            return true;
        });

        int i = 0;
        int outIndex = 0;

        while (i < codePoints.size()) {
            int cp = codePoints.get(i)[0];
            Style style = styles.get(i);

            if (Utils.isEmojiGlyphStyled(style)) {
                Emoji emoji = EmojiRegistry.getById(cp);
                if (emoji == null) {
                    if (!visitor.accept(outIndex++, style, cp)) {
                        return false;
                    }
                    i++;
                    continue;
                }

                Style emojiStyle = style
                    .withHoverEvent(new HoverEvent.ShowText(buildHoverText(emoji)))
                    .withColor(Utils.INFO_COLOR);
                if (!visitor.accept(outIndex++, emojiStyle, cp)) {
                    return false;
                }
                i++;
                continue;
            }

            Emoji existingEmoji = EmojiRegistry.getById(cp);
            if (existingEmoji != null) {
                Style emojiStyle = Style.EMPTY
                    .withFont(Utils.EMOJI_GLYPH_FONT)
                    .withHoverEvent(new HoverEvent.ShowText(buildHoverText(existingEmoji)))
                    .withColor(Utils.INFO_COLOR);
                if (!visitor.accept(outIndex++, emojiStyle, cp)) {
                    return false;
                }
                i++;
                continue;
            }

            if (cp != ':' || !Utils.isEmojiStyled(style)) {
                if (!visitor.accept(outIndex++, style.withFont(Font.DEFAULT), cp)) {
                    return false;
                }
                i++;
                continue;
            }

            int j = i + 1;
            boolean valid = true;
            while (j < codePoints.size()) {
                int c = codePoints.get(j)[0];
                if (c == ':') {
                    break;
                }
                if (Character.isLetterOrDigit(c) || c == '_') {
                    j++;
                    continue;
                }
                valid = false;
                break;
            }

            boolean closed = valid && j < codePoints.size() && j > i + 1;

            if (closed) {
                StringBuilder name = new StringBuilder();
                for (int k = i + 1; k < j; k++) {
                    name.appendCodePoint(codePoints.get(k)[0]);
                }

                Emoji emoji = EmojiRegistry.get(name.toString());
                if (emoji != null) {
                    Style emojiStyle = style.withFont(EMOJI_GLYPH_FONT)
                            .withHoverEvent(new HoverEvent.ShowText(buildHoverText(emoji)));
                    if (!visitor.accept(outIndex++, emojiStyle, emoji.getInternalId())) {
                        return false;
                    }
                    i = j + 1;
                    continue;
                }

                // Unrecognised token: emit literally including closing colon
                for (int k = i; k <= j; k++) {
                    if (!visitor.accept(outIndex++, style.withFont(Font.DEFAULT), codePoints.get(k)[0])) {
                        return false;
                    }
                }
                i = j + 1;
            } else {
                // Unclosed or invalid: emit what we have, reprocess the breaker char
                for (int k = i; k < j; k++) {
                    if (!visitor.accept(outIndex++, style.withFont(Font.DEFAULT), codePoints.get(k)[0])) {
                        return false;
                    }
                }
                i = j;
            }
        }

        return true;
    }

    private static MutableText buildHoverText(Emoji emoji) {
        return text(":" + emoji.getName() + ": ", Formatting.WHITE)
            .append(text(new String(Character.toChars(emoji.getInternalId())), Utils.INFO_COLOR)
                .styled(style -> style.withFont(EMOJI_GLYPH_FONT)));
    }
}
