package dev.swiftclient.mixin;

import net.minecraft.client.gui.components.PlayerSkinWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PlayerSkinWidget.class})
public interface PlayerSkinWidgetAccessor {
   @Accessor("rotationX")
   void lightclient$setXRotation(float var1);

   @Accessor("rotationY")
   void lightclient$setYRotation(float var1);

   @Accessor("rotationX")
   float lightclient$getXRotation();

   @Accessor("rotationY")
   float lightclient$getYRotation();
}
