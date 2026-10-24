package dev.swiftclient.modules;

import dev.swiftclient.compat.Draw3D;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.ui.Notifications;
import dev.swiftclient.input.SwiftKeys;
import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.world.WaypointCodec;
import dev.swiftclient.world.Waypoints;
import dev.swiftclient.world.WorldOverlays;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** Mark places and find them again: a beam and a label with the distance, per server and dimension. */
public final class WaypointsModule extends Module implements WorldOverlays.WorldOverlay {
   public final ModuleSetting nextName;
   public final ModuleSetting beam;
   public final ModuleSetting maxDistance;
   public final ModuleSetting death;
   /** Marker for a death point, so it can be told apart and removed once you are back. */
   private static final String DEATH_PREFIX = "† ";
   private static final int DEATH_COLOR = 0xFFFF4D4D;
   private int lastX;
   private int lastY;
   private int lastZ;
   private boolean haveLast;
   private boolean wasDead;

   public WaypointsModule() {
      super("waypoints", "Waypoints", "Mark places and find them again: a beam and a label with the distance.", "World", "pin", false);
      this.action("key", "Add key", () -> SwiftKeys.label(SwiftKeys.WAYPOINT), SwiftKeys::openControls)
         .desc("Press it to drop a waypoint at your feet. Change it in Options > Controls > Key Binds.");
      this.nextName = this.text("name", "Next waypoint name", "", 24).desc("Name given to the next waypoint. Empty: \"Waypoint 1\", \"Waypoint 2\"...");
      this.action("add", "Add here", () -> Tr.of("swift.waypoints.add"), this::addHere).desc("Drop a waypoint where you stand.");
      this.action("remove", "Remove nearest", () -> Tr.of("swift.waypoints.remove"), () -> {
         Waypoints.Waypoint w = Waypoints.removeNearest();
         if (w != null) {
            say(Tr.of("swift.waypoints.removed", w.name()));
         }
      }).desc("Delete the waypoint closest to you.");
      this.action("clear", "Remove all", () -> Tr.of("swift.waypoints.clear"), () -> say(Tr.of("swift.waypoints.cleared", Waypoints.clearHere())))
         .desc("Delete every waypoint of this server or world.");
      this.beam = this.toggle("beam", "Beam", true).desc("Draw a vertical beam on each waypoint so you see it from afar.");
      this.maxDistance = this.slider("distance", "Max distance", 1000.0, 100.0, 5000.0, 100.0, "").desc("Waypoints further than this are hidden.");
      this.death = this.toggle("death", "Death point", true).desc("Drop a red waypoint where you die, and remove it when you are back there.");
      this.action("share", "Copy share code", () -> Tr.of("swift.waypoints.share"), this::copyCode)
         .desc("Copy all waypoints of this server as a code you can paste to a friend.").group("Sharing");
      this.action("import", "Import from clipboard", () -> Tr.of("swift.waypoints.import"), this::importCode)
         .desc("Add the waypoints from a share code you copied.").group("Sharing");
   }

   private void copyCode() {
      var all = Waypoints.allHere();
      if (all.isEmpty()) {
         say(Tr.of("swift.waypoints.share_none"));
      } else {
         Platform.game().copyToClipboard(WaypointCodec.encode(all));
         say(Tr.of("swift.waypoints.shared", all.size()));
      }
   }

   private void importCode() {
      try {
         int added = Waypoints.importAll(WaypointCodec.decode(Platform.game().readClipboard()));
         say(Tr.of("swift.waypoints.imported", added));
      } catch (IllegalArgumentException e) {
         say(Tr.of("swift.waypoints.import_bad"));
      }
   }

   /** Remembers where you stand while alive, drops a point when you die, and clears it when you come back. */
   private void trackDeath() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         this.haveLast = false;
         this.wasDead = false;
         return;
      }

      boolean dead = mc.player.isDeadOrDying();
      if (dead && !this.wasDead && this.haveLast && this.death.boolValue()) {
         Waypoints.removeIf(w -> w.name().startsWith(DEATH_PREFIX));
         Waypoints.addAt(DEATH_PREFIX + Tr.of("swift.waypoints.death"), this.lastX, this.lastY, this.lastZ, DEATH_COLOR);
         say(Tr.of("swift.waypoints.death_added", this.lastX, this.lastY, this.lastZ));
      }

      this.wasDead = dead;
      if (!dead) {
         this.lastX = mc.player.getBlockX();
         this.lastY = mc.player.getBlockY();
         this.lastZ = mc.player.getBlockZ();
         this.haveLast = true;
         if (this.death.boolValue()) {
            Waypoints.removeIf(w -> w.name().startsWith(DEATH_PREFIX) && mc.player.distanceToSqr(w.x() + 0.5, w.y(), w.z() + 0.5) < 16.0);
         }
      }
   }

   private void addHere() {
      Waypoints.Waypoint w = Waypoints.addHere(this.nextName.textValue());
      if (w != null) {
         this.nextName.setText("");
         say(Tr.of("swift.waypoints.added", w.name(), w.x(), w.y(), w.z()));
      }
   }

   private static void say(String msg) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         //? if >=26.1 {
         mc.player.sendOverlayMessage(Component.literal(msg));
         //?} else
         /*mc.player.displayClientMessage(Component.literal(msg), true);*/
      }

      Notifications.push(Tr.of("swift.waypoints.title"), msg, "pin");
   }

   @Override
   protected void onTick() {
      this.trackDeath();
      while (SwiftKeys.WAYPOINT.consumeClick()) {
         this.addHere();
      }
   }

   @Override
   public void emitGizmos(Minecraft mc, Vec3 camera, float partialTick) {
      Vec3 eye = mc.player.getEyePosition(partialTick);
      double max = this.maxDistance.value();

      for (Waypoints.Waypoint w : Waypoints.visibleHere()) {
         Vec3 base = new Vec3(w.x() + 0.5, w.y(), w.z() + 0.5);
         double dist = base.distanceTo(eye);
         if (dist > max) {
            continue;
         }

         if (this.beam.boolValue()) {
            Draw3D.line(base, base.add(0.0, 256.0, 0.0), w.color() & 0x00FFFFFF | 0xAA000000, 4.0F);
         }

         // Far labels are pulled towards you so they stay readable.
         Vec3 label = base.add(0.0, 2.2, 0.0);
         if (dist > 48.0) {
            label = eye.add(label.subtract(eye).normalize().scale(48.0));
         }

         // Grows with distance (up to where the label is pulled in) so it stays readable.
         float scale = 0.45F + (float)Math.min(dist, 48.0) * 0.025F;
         Draw3D.text(w.name() + "  " + Math.round(dist) + "m", label, w.color() | 0xFF000000, scale);
      }
   }
}
