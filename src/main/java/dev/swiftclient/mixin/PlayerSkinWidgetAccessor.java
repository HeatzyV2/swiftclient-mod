package dev.swiftclient.mixin;

import net.minecraft.client.gui.components.PlayerSkinWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PlayerSkinWidget.class})
public interface PlayerSkinWidgetAccessor {
   @Accessor("rotationX")
   void swiftclient$setXRotation(float rotation);

   @Accessor("rotationY")
   void swiftclient$setYRotation(float rotation);

   @Accessor("rotationX")
   float swiftclient$getXRotation();

   @Accessor("rotationY")
   float swiftclient$getYRotation();
}
