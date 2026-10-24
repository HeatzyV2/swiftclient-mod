package dev.swiftclient.mixin;

import dev.swiftclient.core.partner.PartnerServers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList.OnlineServerEntry;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({OnlineServerEntry.class})
public abstract class OnlineServerEntryMixin {
   @Shadow
   public abstract ServerData getServerData();

   //? if >=1.21.9 {
   @Inject(
      method = {"extractContent"},
      at = {@At("TAIL")}
   )
   private void swiftclient$partnerStar(GuiGraphicsExtractor ctx, int a, int b, boolean hovered, float delta, CallbackInfo ci) {
      OnlineServerEntry self = (OnlineServerEntry)(Object)this;
      int x = self.getContentX() - 13;
      int y = self.getContentYMiddle() - 9 / 2;
   //?} else {
   /*/^* Before 1.21.9 the entry is drawn by render(graphics, index, top, left, width, height, mouseX, mouseY, hovered, delta). ^/
   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void swiftclient$partnerStar(GuiGraphicsExtractor ctx, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float delta, CallbackInfo ci) {
      int x = left - 13;
      int y = top + height / 2 - 9 / 2;
   *///?}
      if (PartnerServers.isPinnedAddress(this.getServerData().ip)) {
         Font font = Minecraft.getInstance().font;
         ctx.text(font, "★", x, y, -10163, true);
      }
   }
}
