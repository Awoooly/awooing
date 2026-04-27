package com.awoly.awooing.client.sprite;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.GlyphMetrics;
import net.minecraft.client.font.TextDrawable;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.Style;
import org.joml.Matrix4f;

public final class SpriteGlyph implements BakedGlyph {

    private final Sprite emoji;
    private final float width;
    private final float height;
    private final GlyphMetrics metrics;

    public SpriteGlyph(Sprite emoji, float height) {
        this.emoji = emoji;

        int imgW = emoji.getWidth();
        int imgH = emoji.getHeight();

        float scale = height / (float) imgH;

        this.height = height;
        this.width = imgW * scale;

        this.metrics = GlyphMetrics.empty(width);
    }

    public SpriteGlyph(Sprite emoji) {
        this(emoji, Sprite.DEFAULT_GLYPH_HEIGHT);
    }

    @Override
    public GlyphMetrics getMetrics() {
        return metrics;
    }

    @Override
    public TextDrawable.DrawnGlyphRect create(float x, float y, int color, int shadowColor, Style style, float boldOffset,
            float shadowOffset) {
        final float fx = x;
        final float fy = y;
        final Style fstyle = style.withParent(emoji.getStyle());
        return new TextDrawable.DrawnGlyphRect() {
            @Override
            public Style style() {
                return fstyle;
            }
            @Override
            public RenderPipeline getPipeline() {
                return RenderPipelines.GUI_TEXT;
            }
            @Override
            public RenderLayer getRenderLayer(TextLayerType type) {
                return RenderLayers.text(emoji.getTextureId());
            }
            @Override
            public GpuTextureView textureView() {
                if (emoji == null || emoji.getTexture() == null) {
                    return null;
                }
                return emoji.getTexture().getGlTextureView();
            }
            @Override
            public float getEffectiveMinX() {
                return fx;
            }
            @Override
            public float getEffectiveMaxX() {
                return fx + width;
            }
            @Override
            public float getEffectiveMinY() {
                return fy;
            }
            @Override
            public float getEffectiveMaxY() {
                return fy + height;
            }
            @Override
            public void render(Matrix4f matrix, VertexConsumer consumer, int light, boolean noDepth) {
                if (emoji == null) {
                    return;
                }
                float x1 = fx;
                float y1 = fy - 1f;
                float x2 = x1 + width;
                float y2 = y1 + height;
                float u1 = 0f;
                float v1 = 0f;
                float u2 = 1f;
                float v2 = 1f;
                int a = (color >> 24) & 255;
                int r = 255;
                int g = 255;
                int b = 255;
                consumer.vertex(matrix, x1, y1, 0f).texture(u1, v1).color(r, g, b, a).light(light);
                consumer.vertex(matrix, x1, y2, 0f).texture(u1, v2).color(r, g, b, a).light(light);
                consumer.vertex(matrix, x2, y2, 0f).texture(u2, v2).color(r, g, b, a).light(light);
                consumer.vertex(matrix, x2, y1, 0f).texture(u2, v1).color(r, g, b, a).light(light);
            }
        };
    }
}
