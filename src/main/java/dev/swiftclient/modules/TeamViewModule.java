package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.social.Friends;
import dev.swiftclient.world.WorldOverlays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Keeps track of your teammates: outline through walls and a marker with their distance above them. */
@ConsumedBy({"WorldOverlays", "EntityGlowMixin"})
public final class TeamViewModule extends Module implements WorldOverlays.WorldOverlay {
   public final ModuleSetting glow;
   public final ModuleSetting markers;
   public final ModuleSetting friends;

   public TeamViewModule() {
      super("team_view", "Team View", "Find your teammates at a glance: outline through walls and a marker with their distance.", "PvP", "users", false);
      this.glow = this.toggle("glow", "Outline teammates", true).desc("Outline your teammates in their team colour, even behind blocks.");
      this.markers = this.toggle("markers", "Distance markers", true).desc("Write the name and distance of each teammate above them, through walls.");
      this.friends = this.toggle("friends", "Count friends as team", true).desc("Treat your Swift friends as teammates on servers without teams.");
   }

   /** Same scoreboard team as you, or a Swift friend when that option is on. */
   public boolean isTeammate(Entity e) {
      Minecraft mc = Minecraft.getInstance();
      if (!(e instanceof AbstractClientPlayer p) || mc.player == null || p == mc.player) {
         return false;
      } else if (mc.player.getTeam() != null && mc.player.isAlliedTo(p)) {
         return true;
      } else {
         return this.friends.boolValue() && Friends.isFriend(p.getGameProfile().name());
      }
   }

   /** Read by the glow mixin: should this entity get an outline? */
   public static boolean shouldGlow(Entity e) {
      if (!ModuleManager.isLoaded()) {
         return false;
      }

      TeamViewModule m = ModuleManager.get(TeamViewModule.class);
      if (m.isEnabled() && m.glow.boolValue() && m.isTeammate(e)) {
         return true;
      } else {
         return e instanceof AbstractClientPlayer p && Friends.glowInWorld() && Friends.isFriend(p.getGameProfile().name()) && p != Minecraft.getInstance().player;
      }
   }

   @Override
   public void emitGizmos(Minecraft mc, Vec3 camera, float partialTick) {
      if (!this.markers.boolValue()) {
         return;
      }

      for (AbstractClientPlayer p : mc.level.players()) {
         if (this.isTeammate(p)) {
            Vec3 pos = p.getPosition(partialTick).add(0.0, p.getBbHeight() + 0.9, 0.0);
            int dist = Math.round(p.distanceTo(mc.player));
            int color = p.getTeamColor() == 0xFFFFFF ? 0xFF6BD36B : 0xFF000000 | p.getTeamColor();
            Gizmos.billboardText(p.getGameProfile().name() + "  " + dist + "m", pos, TextGizmo.Style.forColorAndCentered(color).withScale(0.4F))
               .setAlwaysOnTop();
         }
      }
   }
}
