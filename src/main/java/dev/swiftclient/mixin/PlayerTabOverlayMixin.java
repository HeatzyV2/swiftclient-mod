package dev.swiftclient.mixin;

import dev.swiftclient.core.badges.BadgeState;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.TabAnimState;
import dev.swiftclient.core.mods.modules.TabEditorModule;
import dev.swiftclient.render.SwiftBadge;
import dev.swiftclient.core.social.Friends;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
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
      Component name = cir.getReturnValue();
      if (Friends.highlightInTab(info.getProfile().name())) {
         name = Component.empty().append(Component.literal("★ ").withColor(0xFFD45A)).append(name);
      }

      String grade = BadgeState.gradeFor(info.getProfile().id(), info.getProfile().name());
      Component badge = SwiftBadge.prefix(grade);
      if (badge != null) {
         name = Component.empty().append(badge).append(name);
      }

      cir.setReturnValue(name);
   }

   private static TabEditorModule swiftclient$editor() {
      TabEditorModule m = ModuleManager.get(TabEditorModule.class);
      return m.isEnabled() ? m : null;
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At("HEAD")}
   )
   private void swiftclient$tabAnimStart(GuiGraphicsExtractor ctx, int scaledWindowWidth, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
      TabEditorModule m = swiftclient$editor();
      if (m != null && m.slide.boolValue()) {
         ctx.pose().pushMatrix();
         ctx.pose().translate(0.0F, TabAnimState.slideY());
      }
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At("TAIL")}
   )
   private void swiftclient$tabAnimEnd(GuiGraphicsExtractor ctx, int scaledWindowWidth, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
      TabEditorModule m = swiftclient$editor();
      if (m != null && m.slide.boolValue()) {
         ctx.pose().popMatrix();
      }
   }

   /** Room for "123ms" instead of the 10 px signal icon. */
   @ModifyConstant(
      method = {"extractRenderState"},
      constant = {@Constant(intValue = 13)},
      require = 0
   )
   private int swiftclient$pingColumn(int original) {
      TabEditorModule m = swiftclient$editor();
      return m != null && m.pingNumbers.boolValue() ? original + 14 : original;
   }

   @Inject(
      method = {"extractPingIcon"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$pingNumber(GuiGraphicsExtractor graphics, int slotWidth, int xo, int yo, PlayerInfo info, CallbackInfo ci) {
      TabEditorModule m = swiftclient$editor();
      if (m != null && m.pingNumbers.boolValue()) {
         int ping = info.getLatency();
         String txt = ping < 0 ? "?" : ping + "ms";
         int color = ping < 0 ? 0xFF9AA3B2 : (ping < 80 ? 0xFF6BD36B : (ping < 160 ? 0xFFF2C94C : 0xFFFF6B6B));
         var font = Minecraft.getInstance().font;
         graphics.text(font, txt, xo + slotWidth - 1 - font.width(txt), yo, color, true);
         ci.cancel();
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
      TabEditorModule m = swiftclient$editor();
      return m != null ? m.limit() : original;
   }
}
