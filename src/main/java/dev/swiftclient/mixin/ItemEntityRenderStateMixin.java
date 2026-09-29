package dev.swiftclient.mixin;

import dev.swiftclient.render.DroppedItemState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({ItemEntityRenderState.class})
public abstract class ItemEntityRenderStateMixin implements DroppedItemState {
   @Unique
   private boolean swiftclient$onGround;
   @Unique
   private float swiftclient$scale = 1.0F;

   @Override
   public boolean swiftclient$onGround() {
      return this.swiftclient$onGround;
   }

   @Override
   public void swiftclient$setOnGround(boolean onGround) {
      this.swiftclient$onGround = onGround;
   }

   @Override
   public float swiftclient$scale() {
      return this.swiftclient$scale;
   }

   @Override
   public void swiftclient$setScale(float scale) {
      this.swiftclient$scale = scale;
   }
}
