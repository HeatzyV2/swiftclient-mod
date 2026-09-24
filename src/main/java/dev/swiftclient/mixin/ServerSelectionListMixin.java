package dev.swiftclient.mixin;

import dev.swiftclient.core.partner.PartnerServer;
import dev.swiftclient.core.partner.PartnerServers;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList.OnlineServerEntry;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerData.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerSelectionList.class})
public abstract class ServerSelectionListMixin {
   @Shadow
   @Final
   private List<OnlineServerEntry> onlineServers;
   @Shadow
   @Final
   private JoinMultiplayerScreen screen;

   @Inject(
      method = {"updateOnlineServers"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screens/multiplayer/ServerSelectionList;refreshEntries()V",
         shift = Shift.BEFORE
      )}
   )
   private void swiftclient$injectPartners(ServerList list, CallbackInfo ci) {
      ServerSelectionList self = (ServerSelectionList)(Object)this;
      List<PartnerServer> partners = PartnerServers.all();
      Set<String> pinned = new HashSet<>();

      for (PartnerServer p : partners) {
         pinned.add(p.address().toLowerCase(Locale.ROOT));
      }

      this.onlineServers.removeIf(e -> e.getServerData().ip != null && pinned.contains(e.getServerData().ip.toLowerCase(Locale.ROOT)));

      for (int i = partners.size() - 1; i >= 0; i--) {
         PartnerServer p = partners.get(i);
         ServerData data = new ServerData(p.name(), p.address(), Type.OTHER);
         this.onlineServers.add(0, OnlineServerEntryInvoker.swiftclient$create(self, this.screen, data));
      }
   }
}
