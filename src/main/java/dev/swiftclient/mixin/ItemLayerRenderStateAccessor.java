package dev.swiftclient.mixin;

import java.util.function.Supplier;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ItemStackRenderState.LayerRenderState.class})
public interface ItemLayerRenderStateAccessor {
   @Accessor(/*? if >=26.1 {*/"itemTransform"/*?} else {*//*"transform"*//*?}*/)
   ItemTransform swiftclient$itemTransform();

   @Accessor("extents")
   Supplier<Vector3fc[]> swiftclient$extents();
}
