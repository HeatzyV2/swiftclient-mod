package dev.swiftclient.mixin;

import dev.swiftclient.freelook.CameraOverriddenEntity;
import dev.swiftclient.freelook.Freelook;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Camera.class})
public abstract class FreelookCameraMixin {
   @Unique
   private boolean lightclient$firstTime = true;
   @Shadow
   private Entity entity;

   @Shadow
   protected abstract void setRotation(float var1, float var2);

   @Inject(
      method = {"alignWithEntity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
         ordinal = 1,
         shift = Shift.AFTER
      )}
   )
   private void lightclient$freelookAlign(float partialTick, CallbackInfo ci) {
      if (Freelook.active && this.entity instanceof LocalPlayer) {
         CameraOverriddenEntity o = (CameraOverriddenEntity)this.entity;
         if (this.lightclient$firstTime && Minecraft.getInstance().player != null) {
            o.lightclient$setCamPitch(Minecraft.getInstance().player.getXRot());
            o.lightclient$setCamYaw(Minecraft.getInstance().player.getYRot());
            this.lightclient$firstTime = false;
         }

         this.setRotation(o.lightclient$getCamYaw(), o.lightclient$getCamPitch());
      } else if (this.entity instanceof LocalPlayer) {
         this.lightclient$firstTime = true;
      }
   }
}
