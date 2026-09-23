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
