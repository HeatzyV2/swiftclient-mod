package dev.swiftclient.core.hud;

import java.util.List;

public interface HudData {
   int IN_FORWARD = 1;
   int IN_LEFT = 2;
   int IN_BACK = 4;
   int IN_RIGHT = 8;
   int IN_JUMP = 16;
   int IN_SNEAK = 32;
   int IN_SPRINT = 64;
   int IN_ATTACK = 128;
   int IN_USE = 256;

   boolean inWorld();

   double x();

   double y();

   double z();

   String facing();

   long worldTime();

   List<HudData.Effect> effects();

   List<HudData.Armor> armor();

   default HudData.Armor heldItem() {
      return null;
   }

   default HudData.Sidebar sidebar() {
      return null;
   }

   int tntFuseTicks();

   default String biome() {
      return "";
   }

   default int inputMask() {
      return 0;
   }

   /** Round trip to the server in ms, -1 when unknown (singleplayer, not in the tab list yet). */
   default int ping() {
      return -1;
   }

   /** Camera yaw in degrees, 0 = south, 90 = west (Minecraft convention). */
   default float yaw() {
      return 0.0F;
   }

   /** Address of the server being played on, empty in singleplayer. */
   default String serverAddress() {
      return "";
   }

   default boolean sprinting() {
      return false;
   }

   default boolean sneaking() {
      return false;
   }

   default List<HudData.BossBar> bossBars() {
      return List.of();
   }

   /** Inventory totals, keyed by {@link HudData#COUNTED_ITEMS}. */
   default List<HudData.ItemCount> itemCounts() {
      return List.of();
   }

   /** Items that recently entered or left the inventory, newest first. */
   default List<HudData.Pickup> pickups() {
      return List.of();
   }

   default List<HudData.Cooldown> cooldowns() {
      return List.of();
   }

   default HudData.Combat combat() {
      return HudData.Combat.NONE;
   }

   /** Enabled resource packs, top of the list first (the one that wins). */
   default List<String> resourcePacks() {
      return List.of();
   }

   /** Minimap texture handle (drawn with {@code Canvas.textureRegion}), refreshed by the call. Null if unavailable. */
   default HudData.Minimap minimap(int radius, boolean rotate) {
      return null;
   }

   /** Keys of {@link #itemCounts()}, in display order. */
   List<String> COUNTED_ITEMS = List.of("arrows", "pearls", "gapples", "potions", "totems", "blocks");

   public record BossBar(String name, Object nameComponent, float progress, int color) {
   }

   public record ItemCount(String key, Object stack, int count) {
   }

   public record Pickup(Object stack, String name, int delta, long timeMs) {
   }

   public record Cooldown(Object stack, float fraction) {
   }

   /**
    * Your last fights: consecutive hits without being hit back ({@code combo}), distance of the last hit
    * ({@code reach}, -1 before any hit) and who you are fighting ({@code target}, null when nobody).
    */
   public record Combat(int combo, double reach, long lastHitMs, HudData.Target target) {
      public static final HudData.Combat NONE = new HudData.Combat(0, -1.0, 0L, null);
   }

   public record Target(String name, String uuid, float health, float maxHealth, int armor, double distance, boolean player) {
   }

   /**
    * {@code texture} is an image of {@code size} x {@code size} pixels centred on the player (north up, or
    * facing up when rotated). {@code markers} are other players / waypoints, in pixels from the centre.
    */
   public record Minimap(Object texture, int size, List<HudData.MapMarker> markers) {
   }

   public record MapMarker(float dx, float dy, int color, boolean waypoint) {
   }

   public record Armor(String name, int damage, int maxDamage, Object stack) {
      public Armor(String name, int damage, int maxDamage) {
         this(name, damage, maxDamage, null);
      }

      public float durability() {
         return this.maxDamage <= 0 ? 1.0F : Math.max(0.0F, Math.min(1.0F, (float)(this.maxDamage - this.damage) / this.maxDamage));
      }
   }

   public record Effect(String name, int amplifier, int durationTicks) {
   }

   public record Sidebar(String title, Object titleText, List<HudData.SidebarLine> lines) {
      public Sidebar(String title, List<HudData.SidebarLine> lines) {
         this(title, null, lines);
      }
   }

   public record SidebarLine(String text, String score, Object textComponent, Object scoreComponent) {
      public SidebarLine(String text, String score) {
         this(text, score, null, null);
      }
   }
}
