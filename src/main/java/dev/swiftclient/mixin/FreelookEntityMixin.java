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
   private float lightclient$camPitch;
   @Unique
   private float lightclient$camYaw;
   @Unique
   private float lightclient$anchorYaw;
   @Unique
   private boolean lightclient$hasAnchor = false;

   @Inject(
      method = {"turn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void lightclient$freelookTurn(double xDelta, double yDelta, CallbackInfo ci) {
      if (Freelook.active && (Object)this instanceof LocalPlayer) {
         double pitchDelta = yDelta * 0.15;
         double yawDelta = xDelta * 0.15;
         if (!this.lightclient$hasAnchor) {
            this.lightclient$anchorYaw = this.lightclient$camYaw;
            this.lightclient$hasAnchor = true;
         }

         this.lightclient$camPitch = Mth.clamp(this.lightclient$camPitch + (float)pitchDelta, -90.0F, 90.0F);
         float max = Freelook.maxYaw();
         if (max >= 360.0F) {
            this.lightclient$camYaw += (float)yawDelta;
         } else {
            this.lightclient$camYaw = Mth.clamp(this.lightclient$camYaw + (float)yawDelta, this.lightclient$anchorYaw - max, this.lightclient$anchorYaw + max);
         }

         ci.cancel();
      } else if (this.lightclient$hasAnchor) {
         this.lightclient$hasAnchor = false;
      }
   }

   @Unique
   @Override
   public float lightclient$getCamPitch() {
      return this.lightclient$camPitch;
   }

   @Unique
   @Override
   public float lightclient$getCamYaw() {
      return this.lightclient$camYaw;
   }

   @Unique
   @Override
   public void lightclient$setCamPitch(float pitch) {
      this.lightclient$camPitch = pitch;
   }

   @Unique
   @Override
   public void lightclient$setCamYaw(float yaw) {
      this.lightclient$camYaw = yaw;
      this.lightclient$anchorYaw = yaw;
      this.lightclient$hasAnchor = true;
   }
}
