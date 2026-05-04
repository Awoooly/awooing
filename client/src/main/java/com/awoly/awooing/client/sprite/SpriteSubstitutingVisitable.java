package com.awoly.awooing.client.sprite;

import com.awoly.awooing.client.Utils;
import java.util.Optional;
import java.util.function.BiFunction;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;

public final class SpriteSubstitutingVisitable implements StringVisitable {

    private final StringVisitable delegate;

    public SpriteSubstitutingVisitable(StringVisitable delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
        return delegate.visit(str -> {
            return visitString(str, Style.EMPTY, (style, s) -> visitor.accept(s));
        });
    }

    @Override
    public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style baseStyle) {
        return delegate.visit((style, str) -> visitString(str, style, visitor::accept), baseStyle);
    }

    private <T> Optional<T> visitString(String str, Style style, BiFunction<Style, String, Optional<T>> emit) {
        if (!Utils.isEmojiStyled(style) || !str.contains(":")) {
            return emit.apply(style, str);
        }

        int i = 0;
        while (i < str.length()) {
            char c = str.charAt(i);

            if (c == ':') {
                int j = i + 1;
                while (j < str.length() && isTokenChar(str.charAt(j))) {
                    j++;
                }

                // Look for the CLOSING colon in the SAME chunk
                if (j < str.length() && str.charAt(j) == ':' && j > i + 1) {
                    String name = str.substring(i + 1, j);
                    Sprite emoji = SpriteRegistry.getByName(name);

                    if (emoji != null) {
                        // Emit prefix
                        if (i > 0) {
                            Optional<T> r = emit.apply(style, str.substring(0, i));
                            if (r.isPresent()) {
                                return r;
                            }
                        }

                        // Emit Emoji
                        Optional<T> r = emit.apply(Style.EMPTY.withFont(Utils.SPRITE_GLYPH_FONT),
                                new String(Character.toChars(emoji.getInternalId())));
                        if (r.isPresent()) {
                            return r;
                        }

                        // IMPORTANT: Jump i to the end of this emoji and CONTINUE the loop
                        // instead of recursing, to avoid offset confusion.
                        str = str.substring(j + 1);
                        i = 0;
                        continue;
                    }
                }
            }
            i++;
        }

        return str.isEmpty() ? Optional.empty() : emit.apply(style, str);
    }

    private static boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
