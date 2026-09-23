package dev.swiftclient.mixin;

import dev.swiftclient.core.partner.PartnerServers;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList.OnlineServerEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({JoinMultiplayerScreen.class})
public abstract class JoinMultiplayerScreenMixin {
   @Shadow
   protected ServerSelectionList serverSelectionList;
   @Shadow
   private Button editButton;

   @Inject(
      method = {"onSelectedChange"},
      at = {@At("TAIL")}
   )
   private void lightclient$lockPartnerEdit(CallbackInfo ci) {
      if (this.serverSelectionList.getSelected() instanceof OnlineServerEntry entry && PartnerServers.isPinnedAddress(entry.getServerData().ip)) {
         this.editButton.active = false;
      }
   }

   @Inject(
      method = {"deleteCallback"},
      at = {@At("HEAD")}
   )
   private void lightclient$hidePartnerOnDelete(boolean confirmed, CallbackInfo ci) {
      if (confirmed) {
         if (this.serverSelectionList.getSelected() instanceof OnlineServerEntry entry) {
            String ip = entry.getServerData().ip;
            if (PartnerServers.isPinnedAddress(ip)) {
               PartnerServers.hide(ip);
            }
         }
      }
   }
}
