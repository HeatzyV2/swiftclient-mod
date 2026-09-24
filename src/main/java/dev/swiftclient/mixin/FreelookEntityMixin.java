package dev.swiftclient.mixin;

import dev.swiftclient.freelook.CameraOverriddenEntity;
import dev.swiftclient.freelook.Freelook;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public class FreelookEntityMixin implements CameraOverriddenEntity {
   @Unique
   private float swiftclient$camPitch;
   @Unique
   private float swiftclient$camYaw;
   @Unique
   private float swiftclient$anchorYaw;
   @Unique
   private boolean swiftclient$hasAnchor = false;

   @Inject(
      method = {"turn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$freelookTurn(double xDelta, double yDelta, CallbackInfo ci) {
      if (Freelook.active && (Object)this instanceof LocalPlayer) {
         double pitchDelta = yDelta * 0.15;
         double yawDelta = xDelta * 0.15;
         if (!this.swiftclient$hasAnchor) {
            this.swiftclient$anchorYaw = this.swiftclient$camYaw;
            this.swiftclient$hasAnchor = true;
         }

         this.swiftclient$camPitch = Mth.clamp(this.swiftclient$camPitch + (float)pitchDelta, -90.0F, 90.0F);
         float max = Freelook.maxYaw();
         if (max >= 360.0F) {
            this.swiftclient$camYaw += (float)yawDelta;
         } else {
            this.swiftclient$camYaw = Mth.clamp(this.swiftclient$camYaw + (float)yawDelta, this.swiftclient$anchorYaw - max, this.swiftclient$anchorYaw + max);
         }

         ci.cancel();
      } else if (this.swiftclient$hasAnchor) {
         this.swiftclient$hasAnchor = false;
      }
   }

   @Unique
   @Override
   public float swiftclient$getCamPitch() {
      return this.swiftclient$camPitch;
   }

   @Unique
   @Override
   public float swiftclient$getCamYaw() {
      return this.swiftclient$camYaw;
   }

   @Unique
   @Override
   public void swiftclient$setCamPitch(float pitch) {
      this.swiftclient$camPitch = pitch;
   }

   @Unique
   @Override
   public void swiftclient$setCamYaw(float yaw) {
      this.swiftclient$camYaw = yaw;
      this.swiftclient$anchorYaw = yaw;
      this.swiftclient$hasAnchor = true;
   }
}
