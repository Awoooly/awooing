package com.awoly.awooing.client.sprite;

import com.awoly.awooing.client.Utils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource.Font;

public final class SpriteSubstitutingText implements OrderedText {

    private final OrderedText delegate;

    public SpriteSubstitutingText(OrderedText delegate) {
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
                Sprite emoji = SpriteRegistry.getById(cp);
                if (emoji == null) {
                    if (!visitor.accept(outIndex++, style, cp)) {
                        return false;
                    }
                    i++;
                    continue;
                }

                if (!visitor.accept(outIndex++, emoji.getStyle(), emoji.getInternalId())) {
                    return false;
                }
                i++;
                continue;
            }

            Sprite existingEmoji = SpriteRegistry.getById(cp);
            if (existingEmoji != null) {
                if (!visitor.accept(outIndex++, existingEmoji.getStyle(), existingEmoji.getInternalId())) {
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

                Sprite emoji = SpriteRegistry.getByName(name.toString());
                if (emoji != null) {
                    if (!visitor.accept(outIndex++, emoji.getStyle(), emoji.getInternalId())) {
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
}
