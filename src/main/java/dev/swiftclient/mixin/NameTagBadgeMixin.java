package dev.swiftclient.mixin;

import dev.swiftclient.core.badges.BadgeState;
import dev.swiftclient.render.SwiftBadge;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderer.class})
public abstract class NameTagBadgeMixin {
   @Inject(
      method = {"getNameTag"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void swiftclient$badgeNameTag(Entity entity, CallbackInfoReturnable<Component> cir) {
      if (cir.getReturnValue() != null) {
         if (entity instanceof AbstractClientPlayer p) {
            String grade = BadgeState.gradeFor(p.getUUID(), p.getGameProfile().name());
            Component badge = SwiftBadge.prefix(grade);
            if (badge != null) {
               cir.setReturnValue(Component.empty().append(badge).append((Component)cir.getReturnValue()));
            }
         }
      }
   }
}
