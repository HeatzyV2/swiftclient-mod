package dev.swiftclient.mixin;

import dev.swiftclient.visuals.DamageTiltModule;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Damage Tilt: scales the 14 degree hit tilt of the first person camera. */
@Mixin({GameRenderer.class})
public abstract class DamageTiltMixin {
   @ModifyConstant(method = {"bobHurt"}, constant = @Constant(doubleValue = 14.0))
   private double swiftclient$tilt(double vanilla) {
      return DamageTiltModule.scale(vanilla);
   }
}
