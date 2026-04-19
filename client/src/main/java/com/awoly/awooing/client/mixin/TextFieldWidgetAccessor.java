package com.awoly.awooing.client.mixin;

import java.util.function.Consumer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextFieldWidget.class)
public interface TextFieldWidgetAccessor {
    @Accessor("changedListener")
    Consumer<String> getChangedListener();
}
