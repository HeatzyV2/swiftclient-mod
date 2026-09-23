package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({Minecraft.class})
public abstract class MinecraftClientMixin {
   @ModifyReturnValue(
      method = {"createTitle"},
      at = {@At("RETURN")}
   )
   private String lightclient$rebrandWindowTitle(String original) {
      return original.startsWith("Minecraft ") ? "Swift Client " + original.substring("Minecraft ".length()) : original;
   }
}
