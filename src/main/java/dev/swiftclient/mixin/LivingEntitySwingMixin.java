package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.swiftclient.visuals.SwingAnimationModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Swing Animation: swing speed (Tempo) and the combat-only check, for your own player only. */
@Mixin({LivingEntity.class})
public abstract class LivingEntitySwingMixin {
   @ModifyExpressionValue(
      method = {"updateSwingTime", "swing(Lnet/minecraft/world/InteractionHand;Z)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getCurrentSwingDuration()I"
      )
   )
   private int swiftclient$swingTempo(int ticks) {
      SwingAnimationModule module = SwingAnimationModule.active;
      return module != null && (Object)this == Minecraft.getInstance().player ? module.swingTicks(ticks) : ticks;
   }

   @Inject(
      method = {"swing(Lnet/minecraft/world/InteractionHand;Z)V"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/entity/LivingEntity;swinging:Z",
         opcode = 181
      )
   )
   private void swiftclient$latchSwing(InteractionHand hand, boolean broadcast, CallbackInfo ci) {
      SwingAnimationModule module = SwingAnimationModule.active;
      if (module != null && (Object)this == Minecraft.getInstance().player) {
         module.latch();
      }
   }
}
