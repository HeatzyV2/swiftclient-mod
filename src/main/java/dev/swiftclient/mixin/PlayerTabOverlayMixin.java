package dev.swiftclient.mixin;

import dev.swiftclient.core.badges.BadgeState;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.TabAnimState;
import dev.swiftclient.core.mods.modules.WiderTabModule;
import dev.swiftclient.render.SwiftBadge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerTabOverlay.class})
public abstract class PlayerTabOverlayMixin {
   @Inject(
      method = {"getNameForDisplay"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void swiftclient$prefixBadge(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
      String grade = BadgeState.gradeFor(info.getProfile().id(), info.getProfile().name());
      Component badge = SwiftBadge.prefix(grade);
      if (badge != null) {
         cir.setReturnValue(Component.empty().append(badge).append((Component)cir.getReturnValue()));
      }
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At("HEAD")}
   )
   private void swiftclient$tabAnimStart(GuiGraphicsExtractor ctx, int scaledWindowWidth, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
      if (ModuleManager.active("tabanim")) {
         ctx.pose().pushMatrix();
         ctx.pose().translate(0.0F, TabAnimState.slideY());
      }
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At("TAIL")}
   )
   private void swiftclient$tabAnimEnd(GuiGraphicsExtractor ctx, int scaledWindowWidth, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
      if (ModuleManager.active("tabanim")) {
         ctx.pose().popMatrix();
      }
   }

   @ModifyArg(
      method = {"getPlayerInfos"},
      index = 0,
      at = @At(
         value = "INVOKE",
         target = "Ljava/util/stream/Stream;limit(J)Ljava/util/stream/Stream;"
      )
   )
   private long swiftclient$widerTab(long original) {
      return ModuleManager.active("widertab") ? WiderTabModule.limite() : original;
   }
}
