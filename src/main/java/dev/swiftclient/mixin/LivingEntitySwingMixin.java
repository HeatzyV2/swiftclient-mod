package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.swiftclient.visuals.SlowSwingModule;
import dev.swiftclient.visuals.SwingAnimationModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if >=26.3 {
/*import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?} else
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Swing Animation and Slow Swing: swing speed (Tempo, Slow Swing) and the combat-only check, for your own player only. */
@Mixin({LivingEntity.class})
public abstract class LivingEntitySwingMixin {
   //? if >=26.3 {
   /*/^* 26.3: the duration is fixed when a swing starts (SwingState). ^/
   @ModifyExpressionValue(
      method = {"swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getModifiedSwingDuration(Lnet/minecraft/world/item/component/SwingAnimation;)I"
      )
   )
   *///?} else {
   @ModifyExpressionValue(
      method = {"updateSwingTime", "swing(Lnet/minecraft/world/InteractionHand;Z)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getCurrentSwingDuration()I"
      )
   )
   //?}
   private int swiftclient$swingTempo(int ticks) {
      if ((Object)this != Minecraft.getInstance().player) {
         return ticks;
      }

      SwingAnimationModule module = SwingAnimationModule.active;
      SlowSwingModule slow = SlowSwingModule.active;
      int tempo = module != null ? module.swingTicks(ticks) : ticks;
      return slow != null ? slow.swingTicks(tempo) : tempo;
   }

   //? if >=26.3 {
   /*@Inject(method = {"swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z"}, at = @At("RETURN"))
   private void swiftclient$latchSwing(CallbackInfoReturnable<Boolean> cir) {
      if (!cir.getReturnValueZ()) {
         return;
      }
   *///?} else {
   @Inject(
      method = {"swing(Lnet/minecraft/world/InteractionHand;Z)V"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/entity/LivingEntity;swinging:Z",
         opcode = 181
      )
   )
   private void swiftclient$latchSwing(CallbackInfo ci) {
   //?}
      SwingAnimationModule module = SwingAnimationModule.active;
      if (module != null && (Object)this == Minecraft.getInstance().player) {
         module.latch();
      }
   }
}
