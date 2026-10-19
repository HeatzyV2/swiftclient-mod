package dev.swiftclient.visuals;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.world.Waypoints;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Drops a waypoint where you die, and removes it when you get back to your stuff. */
public final class DeathMarkerModule extends Module {
   public static volatile DeathMarkerModule active;
   private static final String PREFIX = "Death ";
   private static final int RED = 0xFFFF5C5C;
   private final ModuleSetting onlyLast = this.toggle("only_last", "Keep only the last", true).desc("A new death replaces the previous death marker.");
   private final ModuleSetting autoRemove = this.toggle("auto_remove", "Remove when reached", true).desc("The marker disappears when you walk back to it.");
   private final ModuleSetting chat = this.toggle("chat", "Coordinates in chat", true).desc("Prints where you died in the chat.");
   private boolean wasDead;

   public DeathMarkerModule() {
      super("death_marker", "Death Marker", "Drops a waypoint where you die so you can find your stuff again.", "Utility", "pin", false);
   }

   @Override
   protected void onEnable() {
      active = this;
      this.wasDead = false;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   @Override
   protected void onTick() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         this.wasDead = false;
         return;
      }

      boolean dead = player.isDeadOrDying();
      if (dead && !this.wasDead) {
         BlockPos at = player.blockPosition();
         if (this.onlyLast.boolValue()) {
            Waypoints.removeIf(w -> w.name().startsWith(PREFIX));
         }

         String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
         Waypoints.addAt(PREFIX + time, at.getX(), at.getY(), at.getZ(), RED);
         if (this.chat.boolValue()) {
            Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.literal("You died at " + at.getX() + " " + at.getY() + " " + at.getZ()));
         }
      } else if (!dead && this.autoRemove.boolValue() && player.tickCount % 10 == 0) {
         Waypoints.removeIf(w -> w.name().startsWith(PREFIX) && player.distanceToSqr(w.x() + 0.5, w.y(), w.z() + 0.5) < 9.0);
      }

      this.wasDead = dead;
   }
}
