package com.awoly.awooing.client.emoji;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class Emoji {

    private final String name;
    private final int internalId;
    private final NativeImageBackedTexture texture;
    private final Identifier textureId;
    private volatile EmojiGlyph cachedGlyph;

    public Emoji(String name, int internalId, NativeImageBackedTexture texture) {
        this.name = name;
        this.internalId = internalId;
        this.texture = texture;
        this.textureId = Identifier.of("awooing", "emoji/" + name);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            mc.execute(() -> mc.getTextureManager().registerTexture(textureId, texture));
        }
    }

    public String getName() {
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

    public int getWidth() {
        NativeImage image = texture != null ? texture.getImage() : null;
        return image != null ? image.getWidth() : 8;
    }

    public int getHeight() {
        NativeImage image = texture != null ? texture.getImage() : null;
        return image != null ? image.getHeight() : 8;
    }

    public EmojiGlyph getOrCreateGlyph() {
        if (cachedGlyph == null) {
            cachedGlyph = new EmojiGlyph(this);
        }
        return cachedGlyph;
    }

    @Override
    public String toString() {
        return ":" + name + ":";
    }
}
