package dev.swiftclient.mixin;

import dev.swiftclient.visuals.JumpCirclesModule;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Jump Circles: a ring where you take off. */
@Mixin({LivingEntity.class})
public abstract class JumpCirclesLivingEntityMixin {
   @Inject(
      method = {"jumpFromGround"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V"
      )}
   )
   private void swiftclient$jumpCircle(CallbackInfo ci) {
      if (JumpCirclesModule.active != null) {
         JumpCirclesModule.onJump((LivingEntity)(Object)this);
      }
   }
}
