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

   @Inject(
      method = {"extractContent"},
      at = {@At("TAIL")}
   )
   private void lightclient$partnerStar(GuiGraphicsExtractor ctx, int a, int b, boolean hovered, float delta, CallbackInfo ci) {
      if (PartnerServers.isPinnedAddress(this.getServerData().ip)) {
         OnlineServerEntry self = (OnlineServerEntry)(Object)this;
         Font font = Minecraft.getInstance().font;
         int x = self.getContentX() - 13;
         int y = self.getContentYMiddle() - 9 / 2;
         ctx.text(font, "★", x, y, -10163, true);
      }
   }
}
