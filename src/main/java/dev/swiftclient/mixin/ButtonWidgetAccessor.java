package dev.swiftclient.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Button.class})
public interface ButtonWidgetAccessor {
   @Accessor("onPress")
   OnPress swiftclient$getOnPress();
}
