package dev.swiftclient.mixin;

import dev.swiftclient.core.partner.PartnerServers;
import dev.swiftclient.ui.SessionReloadButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList.OnlineServerEntry;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({JoinMultiplayerScreen.class})
public abstract class JoinMultiplayerScreenMixin extends Screen {
   @Shadow
   protected ServerSelectionList serverSelectionList;
   @Shadow
   private Button editButton;

   protected JoinMultiplayerScreenMixin(Component title) {
      super(title);
   }

   /** Reload-session button, mirroring the vanilla icon button on the right of the bottom rows. */
   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void swiftclient$reloadSession(CallbackInfo ci) {
      this.addRenderableWidget(SessionReloadButton.create(this.editButton.getX() - 4 - SessionReloadButton.SIZE, this.editButton.getY()));
   }

   @Inject(
      method = {"onSelectedChange"},
      at = {@At("TAIL")}
   )
   private void swiftclient$lockPartnerEdit(CallbackInfo ci) {
      if (this.serverSelectionList.getSelected() instanceof OnlineServerEntry entry && PartnerServers.isPinnedAddress(entry.getServerData().ip)) {
         this.editButton.active = false;
      }
   }

   @Inject(
      method = {"deleteCallback"},
      at = {@At("HEAD")}
   )
   private void swiftclient$hidePartnerOnDelete(boolean confirmed, CallbackInfo ci) {
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
