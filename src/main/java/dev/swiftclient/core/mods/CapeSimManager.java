package dev.swiftclient.core.mods;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CapeSimManager {
   public static final int PARTS = 16;
   private static final float GRAVITY = 25.0F;
   private static final float HEIGHT_MUL = 6.0F;
   private static final Map<UUID, CapeSimManager.Entry> ENTRIES = new ConcurrentHashMap<>();

   private CapeSimManager() {
   }

   public static CapeSim get(UUID id) {
      CapeSimManager.Entry e = ENTRIES.get(id);
      return e != null ? e.sim : null;
   }

   public static void tick(UUID id, double x, double y, double z, double yo, float yBodyRot, boolean crouching, boolean underWater, long nowMs) {
      CapeSimManager.Entry e = ENTRIES.computeIfAbsent(id, k -> new CapeSimManager.Entry());
      if (nowMs - e.lastTick >= 40L) {
         e.lastTick = nowMs;
         if (!e.init) {
            e.cx = x;
            e.cy = y;
            e.cz = z;
            e.init = true;
         }

         if (Math.abs(x - e.cx) > 10.0 || Math.abs(z - e.cz) > 10.0) {
            e.cx = x;
            e.cy = y;
            e.cz = z;
         }

         e.cx = e.cx + (x - e.cx) * 0.25;
         e.cy = e.cy + (y - e.cy) * 0.25;
         e.cz = e.cz + (z - e.cz) * 0.25;
         double d = e.cx - x;
         double m = e.cz - z;
         double o = Math.sin(yBodyRot * 0.017453292);
         double p = -Math.cos(yBodyRot * 0.017453292);
         double fallHack = clamp((yo - y) * 10.0, 0.0, 1.0);
         boolean sneakKick = crouching && !e.wasSneaking;
         float heightMul = underWater ? 12.0F : 6.0F;
         e.sim.gravity = underWater ? 2.5F : 25.0F;
         float changeX = (float)(d * o + m * p + fallHack + (sneakKick ? 3 : 0));
         float changeY = (float)((y - yo) * heightMul + (sneakKick ? 1 : 0));
         e.wasSneaking = crouching;
         e.sim.applyMovement(changeX, changeY);
         e.sim.simulate();
      }
   }

   private static double clamp(double v, double lo, double hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   public static void forget(UUID id) {
      ENTRIES.remove(id);
   }

   private static final class Entry {
      final CapeSim sim = new CapeSim(16);
      double cx;
      double cy;
      double cz;
      boolean init = false;
      boolean wasSneaking = false;
      long lastTick = 0L;
   }
}
