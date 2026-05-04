package com.awoly.awooing.client.sprite;

import static com.awoly.awooing.client.Utils.SPRITE_GLYPH_FONT;
import static com.awoly.awooing.client.Utils.text;

import com.awoly.awooing.client.Awooing;
import com.awoly.awooing.client.Utils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public final class Sprite {

    public static final float DEFAULT_GLYPH_HEIGHT = 9f;
    private static final Map<String, TextureRef> ASSET_TEXTURES_BY_PATH = new ConcurrentHashMap<>();

    private final String name;
    private final int internalId;
    private final float glyphHeight;
    private final NativeImageBackedTexture texture;
    private final Identifier textureId;
    private final SpriteSource source;
    private final Style style;
    private volatile SpriteGlyph cachedGlyph;

    public static Sprite emoji(String name, int internalId, NativeImageBackedTexture texture) {
        return new Sprite(name, internalId, DEFAULT_GLYPH_HEIGHT, texture);
    }

    public static Sprite asset(int internalId, float glyphHeight, String texturePath) {
        TextureRef textureRef = ASSET_TEXTURES_BY_PATH.computeIfAbsent(texturePath, Sprite::loadAssetTexture);
        return new Sprite(internalId, glyphHeight, textureRef.texture, textureRef.textureId);
    }

    public static Sprite assetFromTexture(int internalId, float glyphHeight, NativeImageBackedTexture texture) {
        Identifier textureId = Identifier.of("awooing", "sprite/" + Integer.toHexString(internalId));
        return new Sprite(internalId, glyphHeight, texture, textureId);
    }

    private Sprite(String name, int internalId, float glyphHeight, NativeImageBackedTexture texture) {
        this.name = name;
        this.internalId = internalId;
        this.glyphHeight = glyphHeight;
        this.texture = texture;
        this.textureId = Identifier.of("awooing", "emoji/" + name);
        this.source = SpriteSource.EMOJI;
        this.style = Style.EMPTY
                .withFont(SPRITE_GLYPH_FONT)
                .withHoverEvent(new HoverEvent.ShowText(
                        text(":" + name + ": ")
                                .append(text(new String(Character.toChars(internalId)), Utils.INFO_COLOR)
                                        .styled(s -> s.withFont(SPRITE_GLYPH_FONT)))));

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            mc.execute(() -> mc.getTextureManager().registerTexture(textureId, texture));
        }
    }

    private Sprite(int internalId, float glyphHeight, NativeImageBackedTexture texture, Identifier textureId) {
        this.name = null;
        this.internalId = internalId;
        this.glyphHeight = glyphHeight > 0f ? glyphHeight : DEFAULT_GLYPH_HEIGHT;
        this.texture = texture;
        this.textureId = textureId;
        this.source = SpriteSource.ASSET;
        this.style = Style.EMPTY.withFont(SPRITE_GLYPH_FONT);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            mc.execute(() -> mc.getTextureManager().registerTexture(textureId, texture));
        }
    }

    private static TextureRef loadAssetTexture(String texturePath) {
        try (InputStream stream = Sprite.class.getResourceAsStream(texturePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Asset sprite resource not found: " + texturePath);
            }

            NativeImageBackedTexture texture = loadTexture(stream, "asset:" + texturePath);
            String textureKey = "asset/" + Integer.toHexString(texturePath.hashCode());
            return new TextureRef(texture, Identifier.of("awooing", textureKey));
        } catch (Exception e) {
            Awooing.LOGGER.warn("Failed to load asset sprite at {}", texturePath, e);
            NativeImageBackedTexture fallback = new NativeImageBackedTexture(
                    () -> "asset:fallback",
                    new NativeImage(8, 8, true));
            return new TextureRef(fallback, Identifier.of("awooing", "asset/fallback"));
        }
    }

    static NativeImageBackedTexture loadTexture(InputStream stream, String textureName) throws Exception {
        byte[] data = stream.readAllBytes();
        try (NativeImage image = NativeImage.read(new ByteArrayInputStream(data))) {
            if (image == null) {
                throw new IllegalArgumentException("Texture image was empty: " + textureName);
            }

            return new NativeImageBackedTexture(() -> textureName, image);
        }
    }

    private record TextureRef(NativeImageBackedTexture texture, Identifier textureId) {}

    public @Nullable String getName() {
        return name;
    }

    public int getInternalId() {
        return internalId;
    }

    public NativeImageBackedTexture getTexture() {
        return texture;
    }

    public Identifier getTextureId() {
        return textureId;
    }

    public SpriteSource getSource() {
        return source;
    }

    public Style getStyle() {
        return style;
    }

    public int getWidth() {
        NativeImage image = texture != null ? texture.getImage() : null;
        return image != null ? image.getWidth() : 8;
    }

    public int getHeight() {
        NativeImage image = texture != null ? texture.getImage() : null;
        return image != null ? image.getHeight() : 8;
    }

    public SpriteGlyph getOrCreateGlyph() {
        if (cachedGlyph == null) {
            cachedGlyph = new SpriteGlyph(this, glyphHeight);
        }
        return cachedGlyph;
    }

    @Override
    public String toString() {
        return name == null ? "<sprite:" + internalId + ">" : ":" + name + ":";
    }
}
