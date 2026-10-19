package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.social.Friends;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Your hits on players from your Swift friends list are held back, so you never hit a teammate by mistake. */
@ConsumedBy("MultiPlayerGameModeMixin")
public final class FriendGuardModule extends Module {
   private static final long HINT_COOLDOWN_MS = 1500L;
   private long lastHintAt;

   public FriendGuardModule() {
      super("friend_guard", "Friend Guard", "Stops your hits on players from your friends list, so you never hit a teammate by mistake.", "PvP", "shield", false);
   }

   /** Should this attack be dropped? (MultiPlayerGameModeMixin, before it is sent.) */
   public static boolean blocks(Player attacker, Entity target) {
      if (!ModuleManager.isLoaded() || !(target instanceof Player friend) || friend == attacker || attacker != Minecraft.getInstance().player) {
         return false;
      }

      FriendGuardModule m = ModuleManager.get(FriendGuardModule.class);
      if (!m.isEnabled() || !Friends.isFriend(friend.getGameProfile().name())) {
         return false;
      }

      long now = System.currentTimeMillis();
      if (now - m.lastHintAt >= HINT_COOLDOWN_MS) {
         m.lastHintAt = now;
         Minecraft.getInstance().gui.hud.setOverlayMessage(Component.literal("Held back your hit on " + friend.getGameProfile().name()), false);
      }

      return true;
   }

   @Override
   protected void onDisable() {
      this.lastHintAt = 0L;
   }
}
