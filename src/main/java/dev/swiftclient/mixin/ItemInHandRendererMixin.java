package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.swiftclient.modules.OldVisualsModule;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 1.7 Visuals: the held item no longer dips while the attack cooldown recharges. */
//? if >=26.3 {
/*@Mixin({net.minecraft.client.player.FirstPersonHandsAndItems.class})
*///?} else
@Mixin({net.minecraft.client.renderer.ItemInHandRenderer.class})
public abstract class ItemInHandRendererMixin {
   @WrapOperation(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         //? if >=1.21.11 {
         target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"
         //?} else
         /*target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"*/
      )
   )
   private float swiftclient$noDip(LocalPlayer player, float a, Operation<Float> original) {
      return OldVisualsModule.on(true) ? 1.0F : original.call(player, a);
   }
}
