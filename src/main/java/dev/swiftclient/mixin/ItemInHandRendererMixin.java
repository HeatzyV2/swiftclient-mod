package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.swiftclient.modules.OldVisualsModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 1.7 Visuals: the held item no longer dips while the attack cooldown recharges. */
@Mixin({ItemInHandRenderer.class})
public abstract class ItemInHandRendererMixin {
   @WrapOperation(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"
      )
   )
   private float swiftclient$noDip(LocalPlayer player, float a, Operation<Float> original) {
      return OldVisualsModule.on(true) ? 1.0F : original.call(player, a);
   }
}
