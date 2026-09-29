package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

@ConsumedBy({"HudManager"})
public final class MinimapModule extends Module {
   public MinimapModule() {
      super("hud_minimap", "Minimap", "Map of the terrain around you, with players and waypoints. Move it in the HUD Editor.", "World", "map", false);
      this.slider("size", "Size", 90.0, 60.0, 160.0, 5.0, "px").desc("Side of the map on screen.").group("Display");
      this.slider("radius", "Range", 48.0, 24.0, 96.0, 8.0, "").desc("How many blocks are shown from you to the edge.").group("Display");
      this.toggle("rotate", "Rotate with you", true).desc("Keep where you look at the top. Off: north stays at the top.").group("Display");
      this.toggle("coords", "Show coordinates", true).desc("Write your position under the map.").group("Display");
      this.toggle("players", "Show players", true).desc("Mark the other players around you.").group("Markers");
      this.toggle("waypoints", "Show waypoints", true).desc("Mark your waypoints.").group("Markers");
      this.color("color", "Your marker", -1).desc("Colour of the arrow that stands for you.").group("Markers");
   }
}
