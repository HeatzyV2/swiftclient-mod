package dev.swiftclient.core.hud;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.hud.elements.ArmorElement;
import dev.swiftclient.core.hud.elements.BiomeElement;
import dev.swiftclient.core.hud.elements.ClockElement;
import dev.swiftclient.core.hud.elements.CoordsElement;
import dev.swiftclient.core.hud.elements.CpsElement;
import dev.swiftclient.core.hud.elements.DateElement;
import dev.swiftclient.core.hud.elements.DayCounterElement;
import dev.swiftclient.core.hud.elements.EffectsElement;
import dev.swiftclient.core.hud.elements.FpsElement;
import dev.swiftclient.core.hud.elements.KeystrokesElement;
import dev.swiftclient.core.hud.elements.MemoryElement;
import dev.swiftclient.core.hud.elements.MusicElement;
import dev.swiftclient.core.hud.elements.ScoreboardElement;
import dev.swiftclient.core.hud.elements.SpeedElement;
import dev.swiftclient.core.hud.elements.TntTimerElement;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.platform.Platform;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.ToIntFunction;

public final class HudManager {
   public static final int MARGIN = 2;
   public static final float SCALE_MIN = 0.5F;
   public static final float SCALE_MAX = 3.0F;
   public static final int A_START = 0;
   public static final int A_CENTER = 1;
   public static final int A_END = 2;
   private static final List<HudElement> ELEMENTS = List.of(
      new DateElement(),
      new ClockElement(),
      new DayCounterElement(),
      new CoordsElement(),
      new EffectsElement(),
      new ArmorElement(),
      new TntTimerElement(),
      new MusicElement(),
      new FpsElement(),
      new MemoryElement(),
      new BiomeElement(),
      new SpeedElement(),
      new CpsElement(),
      new KeystrokesElement(),
      new ScoreboardElement()
   );
   private static final Map<String, HudManager.Def> DEFAULTS = new HashMap<>();
   private static final HudManager.Def FALLBACK = new HudManager.Def(0.02F, 0.02F, 0, 0);
   private static final List<String> LEFT_STACK = List.of("date", "clock", "days", "coords", "fps", "memory", "biome", "speed");
   private static final Set<String> UNSET = new HashSet<>();
   private static int stackW;
   private static int stackH;
   private static float stackF;
   private static final Map<String, float[]> POS = new HashMap<>();
   private static final Map<String, Float> SCALE = new HashMap<>();
   public static final float HUD_SCALE_MIN = 1.0F;
   public static final float HUD_SCALE_MAX = 4.0F;
   private static float hudScale = 2.0F;
   private static float guiScale = 2.0F;
   private static final Map<String, int[]> ANCHOR = new HashMap<>();
   private static final Set<String> ANCHOR_LOCKED = new HashSet<>();
   private static volatile HudData lastData;
   private static boolean loaded;

   public static float hudScale() {
      ensureLoaded();
      return hudScale;
   }

   public static void setHudScale(float v) {
      ensureLoaded();
      hudScale = clampHudScale(v);
      Platform.game().setConfig("hud_scale_global", Float.toString(hudScale));
   }

   public static float clampHudScale(float v) {
      return Math.max(1.0F, Math.min(4.0F, v));
   }

   public static float facteur(float voulu, float gui) {
      return gui > 0.0F ? voulu / gui : 1.0F;
   }

   public static void addHudScale(float delta) {
      setHudScale(hudScale() + delta);
   }

   public static void setGuiScale(float v) {
      if (v > 0.0F) {
         guiScale = v;
      }
   }

   public static float facteurEcran() {
      ensureLoaded();
      return facteur(hudScale, guiScale);
   }

   public static List<HudElement> elements() {
      return ELEMENTS;
   }

   public static HudData lastData() {
      return lastData;
   }

   private static HudManager.Def def(String id) {
      return DEFAULTS.getOrDefault(id, FALLBACK);
   }

   private static void ensureLoaded() {
      if (!loaded) {
         loaded = true;

         for (HudElement e : ELEMENTS) {
            HudManager.Def d = def(e.id);
            String rawPos = Platform.game().getConfig("hud_pos_" + e.id, "");
            String rawAnchor = Platform.game().getConfig("hud_anchor_" + e.id, "");
            boolean legacy = !rawPos.isEmpty() && rawAnchor.isEmpty();
            if (rawPos.isEmpty()) {
               UNSET.add(e.id);
            }

            POS.put(e.id, rawPos.isEmpty() ? new float[]{d.fx(), d.fy()} : parsePos(rawPos, d));
            ANCHOR.put(e.id, legacy ? new int[]{0, 0} : parseAnchor(rawAnchor, d));
            if (!rawAnchor.isEmpty()) {
               ANCHOR_LOCKED.add(e.id);
            }

            SCALE.put(e.id, parseScale(Platform.game().getConfig("hud_scale_" + e.id, "1.0")));
         }

         try {
            hudScale = clampHudScale(Float.parseFloat(Platform.game().getConfig("hud_scale_global", "2.0")));
         } catch (Exception ignored) {
            hudScale = 2.0F;
         }
      }
   }

   private static float[] parsePos(String raw, HudManager.Def d) {
      try {
         String[] p = raw.split(",");
         return new float[]{clamp01(Float.parseFloat(p[0])), clamp01(Float.parseFloat(p[1]))};
      } catch (Exception ignored) {
         return new float[]{d.fx(), d.fy()};
      }
   }

   private static int[] parseAnchor(String raw, HudManager.Def d) {
      try {
         String[] p = raw.split(",");
         return new int[]{clampAnchor(Integer.parseInt(p[0].trim())), clampAnchor(Integer.parseInt(p[1].trim()))};
      } catch (Exception ignored) {
         return new int[]{d.ax(), d.ay()};
      }
   }

   private static float parseScale(String raw) {
      try {
         return clampScale(Float.parseFloat(raw.trim()));
      } catch (Exception ignored) {
         return 1.0F;
      }
   }

   public static float clampScale(float v) {
      return Math.max(0.5F, Math.min(3.0F, v));
   }

   private static int clampAnchor(int v) {
      return Math.max(0, Math.min(2, v));
   }

   private static float clamp01(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }

   public static float[] pos(String id) {
      ensureLoaded();
      return POS.getOrDefault(id, new float[]{0.02F, 0.02F});
   }

   public static void setPos(String id, float fx, float fy) {
      ensureLoaded();
      POS.put(id, new float[]{clamp01(fx), clamp01(fy)});
   }

   public static int[] anchor(String id) {
      ensureLoaded();
      HudManager.Def d = def(id);
      return ANCHOR.getOrDefault(id, new int[]{d.ax(), d.ay()});
   }

   public static boolean anchorLocked(String id) {
      ensureLoaded();
      return ANCHOR_LOCKED.contains(id);
   }

   public static void setAnchor(String id, int ax, int ay, int w, int h, int screenW, int screenH) {
      ensureLoaded();
      int[] xy = resolve(id, w, h, screenW, screenH);
      ANCHOR.put(id, new int[]{clampAnchor(ax), clampAnchor(ay)});
      ANCHOR_LOCKED.add(id);
      setTopLeft(id, xy[0], xy[1], w, h, screenW, screenH);
   }

   public static void autoAnchor(String id, int w, int h, int screenW, int screenH) {
      ensureLoaded();
      if (!ANCHOR_LOCKED.contains(id)) {
         int[] xy = resolve(id, w, h, screenW, screenH);
         int cx = xy[0] + w / 2;
         int cy = xy[1] + h / 2;
         int ax = cx < screenW / 3 ? 0 : (cx > screenW * 2 / 3 ? 2 : 1);
         int ay = cy < screenH / 3 ? 0 : (cy > screenH * 2 / 3 ? 2 : 1);
         ANCHOR.put(id, new int[]{ax, ay});
         setTopLeft(id, xy[0], xy[1], w, h, screenW, screenH);
      }
   }

   public static void layoutDefaults(int screenW, int screenH, ToIntFunction<String> heightOf) {
      ensureLoaded();
      if (!UNSET.isEmpty()) {
         float g = facteurEcran();
         if (screenW != stackW || screenH != stackH || g != stackF) {
            stackW = screenW;
            stackH = screenH;
            stackF = g;
            int bord = Math.max(1, Math.round(6.0F * g));
            int ecart = Math.max(1, Math.round(2.0F * g));
            int minH = Math.max(1, Math.round(8.0F * g));
            int y = bord;

            for (String id : LEFT_STACK) {
               if (UNSET.contains(id)) {
                  ANCHOR.put(id, new int[]{0, 0});
                  POS.put(id, new float[]{clamp01((float)bord / screenW), clamp01((float)y / screenH)});
                  y += Math.max(minH, heightOf.applyAsInt(id)) + ecart;
               }
            }
         }
      }
   }

   private static void markPlaced(String id) {
      UNSET.remove(id);
   }

   public static int[] resolve(String id, int w, int h, int screenW, int screenH) {
      float[] p = pos(id);
      int[] a = anchor(id);
      int x = Math.round(p[0] * screenW) - offset(a[0], w);
      int y = Math.round(p[1] * screenH) - offset(a[1], h);
      return new int[]{clampAxis(x, w, screenW), clampAxis(y, h, screenH)};
   }

   private static int offset(int anchor, int size) {
      return anchor == 1 ? size / 2 : (anchor == 2 ? size : 0);
   }

   private static int clampAxis(int v, int size, int screen) {
      int m = Math.max(1, Math.round(2.0F * facteurEcran()));
      int max = screen - size - m;
      return max < m ? m : Math.max(m, Math.min(v, max));
   }

   public static void setTopLeft(String id, int x, int y, int w, int h, int screenW, int screenH) {
      markPlaced(id);
      int[] a = anchor(id);
      setPos(id, (float)(x + offset(a[0], w)) / screenW, (float)(y + offset(a[1], h)) / screenH);
   }

   public static float scale(String id) {
      ensureLoaded();
      return SCALE.getOrDefault(id, 1.0F);
   }

   public static void setScale(String id, float s) {
      ensureLoaded();
      SCALE.put(id, clampScale(s));
   }

   public static void addScale(String id, float delta) {
      setScale(id, scale(id) + delta);
   }

   public static void reset(String id) {
      ensureLoaded();
      UNSET.add(id);
      stackH = 0;
      stackW = 0;
      stackF = 0.0F;
      HudManager.Def d = def(id);
      POS.put(id, new float[]{d.fx(), d.fy()});
      ANCHOR.put(id, new int[]{d.ax(), d.ay()});
      ANCHOR_LOCKED.remove(id);
      SCALE.put(id, 1.0F);
   }

   public static void resetAll() {
      for (HudElement e : ELEMENTS) {
         reset(e.id);
      }
   }

   public static Map<String, String> exporter() {
      save();
      Map<String, String> out = new LinkedHashMap<>();

      for (HudElement e : ELEMENTS) {
         for (String k : new String[]{"hud_pos_", "hud_anchor_", "hud_scale_"}) {
            out.put(k + e.id, Platform.game().getConfig(k + e.id, ""));
         }
      }

      out.put("hud_scale_global", Platform.game().getConfig("hud_scale_global", "2.0"));
      return out;
   }

   public static void importer(Map<String, String> etat) {
      if (etat != null && !etat.isEmpty()) {
         for (Entry<String, String> en : etat.entrySet()) {
            Platform.game().setConfig(en.getKey(), en.getValue());
         }

         loaded = false;
         POS.clear();
         ANCHOR.clear();
         ANCHOR_LOCKED.clear();
         UNSET.clear();
         SCALE.clear();
      }
   }

   public static void save() {
      ensureLoaded();

      for (HudElement e : ELEMENTS) {
         float[] p = POS.get(e.id);
         if (p != null) {
            Platform.game().setConfig("hud_pos_" + e.id, p[0] + "," + p[1]);
         }

         int[] a = ANCHOR.get(e.id);
         if (a != null) {
            Platform.game().setConfig("hud_anchor_" + e.id, a[0] + "," + a[1]);
         }

         Platform.game().setConfig("hud_scale_" + e.id, Float.toString(scale(e.id)));
      }

      Platform.game().setConfig("hud_scale_global", Float.toString(hudScale));
   }

   public static void render(Canvas c, int screenW, int screenH, HudData d) {
      ensureLoaded();
      lastData = d;
      HudStats.update(d);
      if (d != null && d.inWorld()) {
         float global = facteurEcran();
         layoutDefaults(screenW, screenH, id -> {
            for (HudElement el : ELEMENTS) {
               if (el.id.equals(id)) {
                  return Math.round(el.height(c, d) * scale(id) * global);
               }
            }

            return 0;
         });

         for (HudElement e : ELEMENTS) {
            if (ModuleManager.active(e.moduleId())) {
               float s = scale(e.id) * global;
               int w = Math.round(e.width(c, d) * s);
               int h = Math.round(e.height(c, d) * s);
               int[] xy = resolve(e.id, w, h, screenW, screenH);
               if (s == 1.0F) {
                  e.draw(c, d, xy[0], xy[1]);
               } else {
                  c.pushScale(xy[0], xy[1], s);
                  e.draw(c, d, 0, 0);
                  c.popScale();
               }
            }
         }
      }
   }

   static {
      DEFAULTS.put("date", new HudManager.Def(0.01F, 0.02F, 0, 0));
      DEFAULTS.put("clock", new HudManager.Def(0.01F, 0.055F, 0, 0));
      DEFAULTS.put("days", new HudManager.Def(0.01F, 0.09F, 0, 0));
      DEFAULTS.put("coords", new HudManager.Def(0.01F, 0.125F, 0, 0));
      DEFAULTS.put("effects", new HudManager.Def(0.99F, 0.05F, 2, 0));
      DEFAULTS.put("armor", new HudManager.Def(0.99F, 0.3F, 2, 0));
      DEFAULTS.put("tnt", new HudManager.Def(0.5F, 0.06F, 1, 0));
      DEFAULTS.put("music", new HudManager.Def(0.01F, 0.76F, 0, 0));
      DEFAULTS.put("fps", new HudManager.Def(0.01F, 0.215F, 0, 0));
      DEFAULTS.put("memory", new HudManager.Def(0.01F, 0.25F, 0, 0));
      DEFAULTS.put("biome", new HudManager.Def(0.01F, 0.285F, 0, 0));
      DEFAULTS.put("speed", new HudManager.Def(0.01F, 0.32F, 0, 0));
      DEFAULTS.put("cps", new HudManager.Def(0.99F, 0.62F, 2, 0));
      DEFAULTS.put("keystrokes", new HudManager.Def(0.99F, 0.68F, 2, 0));
      DEFAULTS.put("scoreboard", new HudManager.Def(0.995F, 0.5F, 2, 1));
   }

   private record Def(float fx, float fy, int ax, int ay) {
   }
}
