package dev.swiftclient.pvp;

import dev.swiftclient.core.hud.HudData;
import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Follows your fights from the client side: every attack you send (see MultiPlayerGameModeMixin) and the
 * hurt animations that answer it. Feeds the Combo Counter, Reach Display and PvP Info elements.
 */
public final class CombatTracker {
   /** A hit only counts if the target flashes red within this many ticks of the attack. */
   private static final int CONFIRM_TICKS = 4;
   private static final long COMBO_TIMEOUT_MS = 3000L;
   private static final long TARGET_TIMEOUT_MS = 6000L;
   private static int combo;
   private static double reach = -1.0;
   private static long lastHitMs;
   private static long lastAttackMs;
   private static WeakReference<LivingEntity> pending = new WeakReference<>(null);
   private static int pendingTicks;
   private static WeakReference<LivingEntity> target = new WeakReference<>(null);
   private static int lastSelfHurt;

   private CombatTracker() {
   }

   /** You swung at {@code entity} (called before the packet goes out). */
   public static void onAttack(Entity entity) {
      LocalPlayer self = Minecraft.getInstance().player;
      if (self != null && entity instanceof LivingEntity living) {
         lastAttackMs = System.currentTimeMillis();
         target = new WeakReference<>(living);
         pending = new WeakReference<>(living);
         pendingTicks = CONFIRM_TICKS;
         reach = distance(self.getEyePosition(), entity.getBoundingBox());
      }
   }

   /** Closest distance from {@code eye} to the box. */
   static double distance(Vec3 eye, AABB box) {
      double dx = Math.max(Math.max(box.minX - eye.x, 0.0), eye.x - box.maxX);
      double dy = Math.max(Math.max(box.minY - eye.y, 0.0), eye.y - box.maxY);
      double dz = Math.max(Math.max(box.minZ - eye.z, 0.0), eye.z - box.maxZ);
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer self = mc.player;
      if (self == null) {
         reset();
         return;
      }

      // Being hit breaks the combo.
      if (self.hurtTime > 0 && self.hurtTime > lastSelfHurt) {
         combo = 0;
      }

      lastSelfHurt = self.hurtTime;
      LivingEntity p = pending.get();
      if (p != null) {
         if (p.hurtTime > 0 && p.hurtTime >= p.hurtDuration - CONFIRM_TICKS) {
            combo++;
            lastHitMs = System.currentTimeMillis();
            pending = new WeakReference<>(null);
         } else if (--pendingTicks <= 0) {
            pending = new WeakReference<>(null);
         }
      }

      long now = System.currentTimeMillis();
      if (combo > 0 && now - Math.max(lastHitMs, lastAttackMs) > COMBO_TIMEOUT_MS) {
         combo = 0;
      }

      LivingEntity t = target.get();
      if (t != null && (!t.isAlive() || t.isRemoved() || now - lastAttackMs > TARGET_TIMEOUT_MS || t.distanceTo(self) > 32.0F)) {
         target = new WeakReference<>(null);
      }
   }

   public static void reset() {
      combo = 0;
      reach = -1.0;
      lastHitMs = 0L;
      target = new WeakReference<>(null);
      pending = new WeakReference<>(null);
   }

   public static HudData.Combat snapshot() {
      LocalPlayer self = Minecraft.getInstance().player;
      LivingEntity t = target.get();
      HudData.Target info = null;
      if (self != null && t != null) {
         boolean isPlayer = t instanceof Player;
         String uuid = isPlayer ? t.getUUID().toString() : null;
         info = new HudData.Target(
            t.getDisplayName().getString(), uuid, t.getHealth() + t.getAbsorptionAmount(), t.getMaxHealth(), t.getArmorValue(), t.distanceTo(self), isPlayer
         );
      }

      return new HudData.Combat(combo, reach, Math.max(lastHitMs, lastAttackMs), info);
   }
}
