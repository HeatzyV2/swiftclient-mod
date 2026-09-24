package dev.swiftclient.mixin;

import dev.swiftclient.core.hud.Nametag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AvatarRenderer.class})
public abstract class AvatarNameSelfMixin {
   @Inject(
      method = {"shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$showOwnNameInThirdPerson(Avatar avatar, double distSq, CallbackInfoReturnable<Boolean> cir) {
      if (Nametag.showOwnName()) {
         Minecraft mc = Minecraft.getInstance();
         if (avatar == mc.player && mc.player != null && !mc.options.getCameraType().isFirstPerson()) {
            cir.setReturnValue(true);
         }
      }
   }
}
