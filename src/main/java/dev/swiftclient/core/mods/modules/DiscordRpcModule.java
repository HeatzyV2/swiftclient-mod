package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

@ConsumedBy("RpcManager")
public final class DiscordRpcModule extends Module {
   public final ModuleSetting hideIp;

   public DiscordRpcModule() {
      super("discord_rpc", "Discord RPC", "Show Swift Client and what you are doing in your Discord status.", "Utility", "globe", true);
      this.hideIp = this.toggle("hide_ip", "Hide server address", true)
         .desc("Show \"On a server\" instead of the server address, so your status never gives away where you play.");
   }
}
