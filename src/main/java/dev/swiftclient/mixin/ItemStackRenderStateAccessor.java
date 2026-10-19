package dev.swiftclient.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 3D Hand and CS Knife: read the resolved item model to place the hand on the grip. */
@Mixin({ItemStackRenderState.class})
public interface ItemStackRenderStateAccessor {
   @Accessor("layers")
   ItemStackRenderState.LayerRenderState[] swiftclient$layers();

   @Accessor("activeLayerCount")
   int swiftclient$activeLayerCount();
}
