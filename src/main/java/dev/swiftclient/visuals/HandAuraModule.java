package dev.swiftclient.visuals;

import dev.swiftclient.compat.Mc;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.ui.Px;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;

/** A glowing outline and halo around your first-person hand and held item. */
@ConsumedBy("HandAuraGameRendererMixin")
public final class HandAuraModule extends Module {
   public static volatile HandAuraModule active;
   private static final long BREATH_NANOS = 3_200_000_000L;
   private static final long SHEEN_NANOS = 4_400_000_000L;
   public final ModuleSetting colorMode;
   public final ModuleSetting glowColor;
   public final ModuleSetting aura;
   public final ModuleSetting reach;
   public final ModuleSetting rim;
   public final ModuleSetting sheen;
   public final ModuleSetting sheenColor;
   public final ModuleSetting breathe;

   public HandAuraModule() {
      super("hand_aura", "Hand Aura", "A glowing outline and halo around your hand and held item, in first person.", "Visual", "sparkles", false);
      this.colorMode = this.cycle("color", "Color", new String[]{"Theme", "Manual"}, 0).desc("Theme uses the Swift accent colour, Manual the colours below.");
      this.glowColor = this.color("glow_color", "Glow color", -13338378).desc("Colour of the aura (Manual).");
      this.aura = this.slider("aura", "Aura", 65.0, 0.0, 100.0, 5.0, "%").desc("Strength of the soft halo around the hand.");
      this.reach = this.slider("reach", "Reach", 8.0, 2.0, 20.0, 1.0, "px").desc("How far the halo spreads.");
      this.rim = this.slider("rim", "Rim", 40.0, 0.0, 100.0, 5.0, "%").desc("Strength of the thin bright outline.");
      this.sheen = this.slider("sheen", "Sheen", 25.0, 0.0, 100.0, 5.0, "%").desc("A shine that sweeps across the hand now and then.");
      this.sheenColor = this.color("sheen_color", "Sheen color", -3153665).desc("Colour of the shine (Manual).");
      this.breathe = this.toggle("breathe", "Breathe", true).desc("The halo slowly swells and fades.");
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
      if (RenderSystem.isOnRenderThread()) {
         HandAuraPass.release();
      } else {
         Minecraft.getInstance().execute(() -> {
            if (active == null) {
               HandAuraPass.release();
            }
         });
      }
   }

   /** The hands are about to be drawn: redirect them into the aura target (HandAuraGameRendererMixin). */
   public static boolean beginCapture() {
      HandAuraModule module = active;
      return module != null && module.shows() && HandAuraPass.begin();
   }

   public static void endCapture(boolean drawn) {
      if (HandAuraPass.end() && drawn) {
         HandAuraModule module = active;
         float scale = Math.max(1.0F, Mc.mainTarget().height / 1080.0F);
         if (module == null) {
            HandAuraPass.compose(-1, 0.0F, 2.0F, 0.0F, 2.0F * scale, -1, 0.0F, 0.0F);
         } else {
            long now = System.nanoTime();
            float phase = (float)((double)(now % SHEEN_NANOS) / SHEEN_NANOS);
            int glow = module.glowArgb();
            HandAuraPass.compose(
               glow, module.auraStrength(now), module.reach.floatValue() * scale, module.rim.floatValue() / 100.0F, 2.0F * scale, module.sheenArgb(glow), module.sheen.floatValue() / 100.0F, phase
            );
         }
      }
   }

   private boolean manual() {
      return this.colorMode.cycleIndex() == 1;
   }

   private int glowArgb() {
      return this.manual() ? this.glowColor.colorValue() : Px.ACCENT;
   }

   private int sheenArgb(int glow) {
      return this.manual() ? this.sheenColor.colorValue() : ARGB.srgbLerp(0.55F, glow, -1);
   }

   private boolean shows() {
      return this.aura.value() > 0.0 || this.rim.value() > 0.0;
   }

   private float auraStrength(long now) {
      float strength = this.aura.floatValue() / 100.0F;
      if (!this.breathe.boolValue()) {
         return strength;
      }

      double cycle = (double)(now % BREATH_NANOS) / BREATH_NANOS;
      float swell = (float)Math.sin(cycle * Math.PI);
      return strength * (0.6F + 0.4F * swell * swell);
   }
}
