package dev.swiftclient.visuals;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Numbers rise around the crosshair when you hit something and when you get hurt. */
public final class DamageIndicatorModule extends Module {
   public static volatile DamageIndicatorModule active;
   private static final int DEALT = 0xFFFFD34D;
   private static final int CRIT = 0xFFFF8A3D;
   private static final int TAKEN = 0xFFFF5C5C;
   private final ModuleSetting dealt = this.toggle("dealt", "Damage dealt", true).desc("Shows the damage you deal to the mob or player you hit.");
   private final ModuleSetting taken = this.toggle("taken", "Damage taken", true).desc("Shows the damage you take.");
   private final ModuleSetting unit = this.cycle("unit", "Unit", new String[]{"Health points", "Hearts"}, 0).desc("Health points: 1 per half heart. Hearts: 1 per full heart.");
   private final ModuleSetting duration = this.slider("duration", "Duration", 1.2, 0.5, 3.0, 0.1, "s").desc("How long a number stays on screen.");
   private final ModuleSetting size = this.slider("size", "Size", 1.4, 0.8, 3.0, 0.1, "x").desc("Size of the numbers.");
   private final Map<Integer, Float> health = new HashMap<>();
   private final List<Popup> popups = new ArrayList<>();
   private ClientLevel lastLevel;
   private int spread;

   public DamageIndicatorModule() {
      super("damage_indicator", "Damage Indicator", "Numbers rise around the crosshair when you hit something and when you get hurt.", "Visual", "bomb", false);
   }

   @Override
   protected void onEnable() {
      active = this;
      this.health.clear();
   }

   @Override
   protected void onDisable() {
      active = null;
      this.health.clear();
      this.popups.clear();
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      ClientLevel level = mc.level;
      if (player == null || level == null) {
         this.health.clear();
         return;
      }

      if (level != this.lastLevel) {
         this.lastLevel = level;
         this.health.clear();
      }

      if (this.health.size() > 512) {
         this.health.clear();
      }

      for (Entity entity : level.entitiesForRendering()) {
         if (!(entity instanceof LivingEntity living) || living.distanceToSqr(player) > 576.0) {
            continue;
         }

         float hp = living.getHealth() + living.getAbsorptionAmount();
         Float before = this.health.put(living.getId(), hp);
         if (before == null || hp >= before - 0.05F) {
            continue;
         }

         float amount = before - hp;
         if (living == player) {
            if (this.taken.boolValue()) {
               this.add(amount, TAKEN, -34);
            }
         } else if (this.dealt.boolValue() && living == player.getLastHurtMob()) {
            this.add(amount, amount >= 6.0F ? CRIT : DEALT, 30);
         }
      }
   }

   private void add(float amount, int color, int side) {
      float shown = this.unit.is("Hearts") ? amount / 2.0F : amount;
      String text = (this.unit.is("Hearts") ? "-" + format(shown) + " ❤" : "-" + format(shown));
      this.spread = (this.spread + 1) % 5;
      this.popups.add(new Popup(text, color, System.nanoTime(), side + (this.spread - 2) * 7));
   }

   private static String format(float v) {
      return Math.abs(v - Math.round(v)) < 0.05F ? Integer.toString(Math.round(v)) : String.format("%.1f", v);
   }

   /** Drawn with the HUD (ExtraHud). */
   public static void render(Canvas dl, int w, int h) {
      DamageIndicatorModule module = active;
      if (module != null) {
         module.draw(dl, w, h);
      }
   }

   private void draw(Canvas dl, int w, int h) {
      long now = System.nanoTime();
      long life = (long)(this.duration.floatValue() * 1.0E9F);
      float scale = this.size.floatValue();
      Iterator<Popup> it = this.popups.iterator();
      while (it.hasNext()) {
         Popup p = it.next();
         float f = (float)(now - p.born) / (float)life;
         if (f >= 1.0F) {
            it.remove();
            continue;
         }

         float fade = f < 0.65F ? 1.0F : (1.0F - f) / 0.35F;
         int alpha = Math.max(8, Math.round(255.0F * fade));
         int x = w / 2 + Math.round(p.dx * scale * 0.7F);
         int y = h / 2 - 12 - Math.round(f * 30.0F * scale);
         dl.pushScale(x, y, scale);
         dl.centeredText(p.text, x, y, alpha << 24 | p.color & 0xFFFFFF, true);
         dl.popScale();
      }
   }

   private record Popup(String text, int color, long born, int dx) {
   }
}
