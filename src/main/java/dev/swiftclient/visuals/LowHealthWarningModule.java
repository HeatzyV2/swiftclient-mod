package dev.swiftclient.visuals;

import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.ARGB;

/** The screen edges glow and beat like a heart when your health is low. */
@ConsumedBy("HudClient")
public final class LowHealthWarningModule extends Module {
   public static volatile LowHealthWarningModule active;
   private static final String PULSE_HEARTBEAT = "Heartbeat";
   private static final String PULSE_BREATH = "Breath";
   private static final String PULSE_STEADY = "Steady";
   private static final String COLOR_THEME = "Theme";
   private static final String COLOR_MANUAL = "Manual";
   private static final int OPAQUE = -16777216;
   private static final int WHITE = -1;
   private static final float EDGE_LIFT = 0.5F;
   private static final float RISE_RATE = 5.0F;
   private static final float FALL_RATE = 2.6F;
   private static final float DEATH_RATE = 12.0F;
   private static final float CALM_BPM = 64.0F;
   private static final float PANIC_BPM = 150.0F;
   private static final float SECOND_BEAT = 0.2F;
   private static final float BEAT_WIDTH = 0.065F;
   private static final long GAP_NANOS = 250000000L;
   private static final float TAU = ((float)Math.PI * 2F);
   private static final float[] BAND_REACH = new float[]{1.0F, 0.55F, 0.22F};
   private static final float[] BAND_WEIGHT = new float[]{0.5F, 0.34F, 0.3F};
   private final ModuleSetting warnBelow = this.slider("warn_below", "Warn below", 7.0, 2.0, 20.0, 1.0, "").desc("Health (in half hearts) under which the warning starts.");
   private final ModuleSetting countAbsorption = this.toggle("count_absorption", "Count absorption", true).desc("Golden hearts count as health.");
   private final ModuleSetting pulse = this.cycle("pulse", "Pulse", new String[]{"Heartbeat", "Breath", "Steady"}, 0).desc("Heartbeat: a double beat that speeds up. Breath: slow waves. Steady: no pulse.");
   private final ModuleSetting intensity = this.slider("intensity", "Intensity", 0.7, 0.2, 1.0, 0.05, "").desc("How strong the glow is.");
   private final ModuleSetting reach = this.slider("reach", "Reach", 0.16, 0.06, 0.35, 0.01, "").desc("How far the glow comes in from the edges.");
   private final ModuleSetting edgeLine = this.toggle("edge_line", "Edge line", true).desc("A thin bright line along the screen edges.");
   private final ModuleSetting colorMode = this.cycle("color", "Color", new String[]{"Theme", "Manual"}, 0).desc("Theme uses the Swift accent colour, Manual the colours below.");
   private final ModuleSetting glowColor = this.color("glow_color", "Glow color", -13338378).desc("Colour of the glow (Manual).");
   private final ModuleSetting edgeColor = this.color("edge_color", "Edge color", -5650177).desc("Colour of the edge line (Manual).");
   private float level;
   private float danger;
   private float phase;
   private long lastFrame;

   public LowHealthWarningModule() {
      super("low_health_warning", "Low Health Warning", "The screen edges glow and beat like a heart when your health is low.", "Visual", "heart", false);
   }

   @Override

   protected void onEnable() {
      active = this;
      this.level = 0.0F;
      this.danger = 0.0F;
      this.phase = 0.0F;
      this.lastFrame = 0L;
   }

   @Override

   protected void onDisable() {
      active = null;
      this.level = 0.0F;
   }

   /** Drawn with the HUD (HudClient), under the Swift HUD elements. */
   public static void render(Canvas dl, int screenW, int screenH) {
      LowHealthWarningModule module = active;
      if (module != null) {
         module.renderHudOverlay(dl, (float)screenW, (float)screenH);
      }
   }

   private void renderHudOverlay(Canvas dl, float screenW, float screenH) {
      Minecraft mc = Minecraft.getInstance();
      long now = System.nanoTime();
      float dt = this.lastFrame == 0L ? 0.0F : Math.min(Math.max((float)(now - this.lastFrame) / 1.0E9F, 0.0F), 0.1F);
      if (now - this.lastFrame > 250000000L) {
         this.level = 0.0F;
         this.phase = 0.0F;
      }

      this.lastFrame = now;
      LocalPlayer player = mc.player;
      boolean dead = player == null || player.isDeadOrDying();
      float want = 0.0F;
      if (!dead && !mc.gui.hud.isHidden() && !player.isSpectator() && !player.getAbilities().invulnerable) {
         float health = player.getHealth() + (this.countAbsorption.boolValue() ? player.getAbsorptionAmount() : 0.0F);
         float limit = this.warnBelow.floatValue();
         if (health < limit) {
            want = 1.0F;
            this.danger = Math.max(0.0F, Math.min(1.0F, (limit - health) / limit));
         }
      }

      float rate = dead ? 12.0F : (want > this.level ? 5.0F : 2.6F);
      this.level += (want - this.level) * (1.0F - (float)Math.exp((double)(-rate * dt)));
      if (this.level < 0.003F && want == 0.0F) {
         this.level = 0.0F;
         this.phase = 0.0F;
      } else {
         float bpm = 64.0F + 86.0F * this.danger;
         if (this.pulse.is("Breath")) {
            bpm *= 0.5F;
         }

         this.phase = (this.phase + dt * bpm / 60.0F) % 1.0F;
         float beat = this.beat();
         float shown = this.level * this.level * (3.0F - 2.0F * this.level);
         float strength = this.intensity.floatValue() * shown * (0.45F + 0.55F * this.danger);
         float depth = this.reach.floatValue() * Math.min(screenW, screenH) * (1.0F + 0.12F * beat);
         boolean manual = this.manual();
         int accent = manual ? 0 : Px.ACCENT;
         int base = manual ? this.glowColor.colorValue() | -16777216 : accent;

         for(int i = 0; i < BAND_REACH.length; ++i) {
            float alpha = strength * BAND_WEIGHT[i] * (0.6F + 0.4F * beat);
            frame(dl, screenW, screenH, depth * BAND_REACH[i], withAlpha(base, alpha));
         }

         if (this.edgeLine.boolValue()) {
            float line = Math.max(2.0F, depth * 0.07F);
            float alpha = this.intensity.floatValue() * shown * (0.2F + 0.55F * beat);
            int edge = manual ? this.edgeColor.colorValue() | -16777216 : ARGB.srgbLerp(0.5F, accent, -1);
            frame(dl, screenW, screenH, line, withAlpha(edge, alpha));
         }

      }
   }

   private float beat() {
      if (this.pulse.is("Steady")) {
         return 0.35F;
      } else {
         return this.pulse.is("Breath") ? 0.5F - 0.5F * (float)Math.cos((double)(((float)Math.PI * 2F) * this.phase)) : Math.min(1.0F, bump(this.phase, 0.0F) + 0.62F * bump(this.phase, 0.2F));
      }
   }

   private static float bump(float at, float center) {
      float d = Math.abs(at - center);
      d = Math.min(d, 1.0F - d) / 0.065F;
      return (float)Math.exp((double)(-d * d));
   }

   private boolean manual() {
      return this.colorMode.is("Manual");
   }

   private static int withAlpha(int abgr, float alpha) {
      int a = Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, alpha)));
      return abgr & 16777215 | a << 24;
   }

   private static void frame(Canvas dl, float w, float h, float depth, int edge) {
      if (!(depth < 1.0F) && edge >>> 24 != 0) {
         int clear = edge & 16777215;
         int iw = Math.round(w);
         int ih = Math.round(h);
         int d = Math.round(depth);
         dl.gradientV(0, 0, iw, d, edge, clear);
         dl.gradientV(0, ih - d, iw, d, clear, edge);
         int alpha = edge >>> 24;
         for (int i = 0; i < d; i++) {
            int a = Math.round(alpha * (1.0F - (i + 0.5F) / d));
            if (a <= 0) {
               break;
            }

            int c = a << 24 | clear;
            dl.fill(i, 0, i + 1, ih, c);
            dl.fill(iw - i - 1, 0, iw - i, ih, c);
         }
      }
   }
}
