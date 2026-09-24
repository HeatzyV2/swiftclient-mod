package dev.swiftclient.mixin;

import dev.swiftclient.core.mods.BlockOverlayState;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({LevelRenderer.class})
public abstract class BlockOutlineColorMixin {
   @ModifyVariable(
      method = {"submitHitOutline"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0,
      require = 0
   )
   private int swiftclient$outlineColor(int color) {
      return BlockOverlayState.enabled() ? BlockOverlayState.argb() : color;
   }
}
