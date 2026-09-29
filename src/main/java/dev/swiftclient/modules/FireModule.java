package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;

/** The flames on your screen while you burn: lower, more transparent, or hidden. */
@ConsumedBy("ScreenEffectRendererMixin")
public final class FireModule extends Module {
   public final ModuleSetting hide;
   public final ModuleSetting height;
   public final ModuleSetting opacity;

   public FireModule() {
      super("fire", "Fire", "The flames on your screen when you burn: lower, more transparent, or gone.", "Visual", "fire", false);
      this.hide = this.toggle("hide", "Hide completely", false).desc("No flames at all on your screen. You still see yourself burning in third person.");
      this.height = this.slider("height", "Lower by", 30.0, 0.0, 60.0, 5.0, "%").desc("How far the flames are pushed down the screen.");
      this.opacity = this.slider("opacity", "Opacity", 60.0, 10.0, 100.0, 5.0, "%").desc("100% is vanilla.");
   }

   private static FireModule active() {
      if (!ModuleManager.isLoaded()) {
         return null;
      }

      FireModule m = ModuleManager.get(FireModule.class);
      return m.isEnabled() ? m : null;
   }

   public static boolean hidden() {
      FireModule m = active();
      return m != null && m.hide.boolValue();
   }

   /** Downward shift in screen units (the overlay spans -1..1). */
   public static float offset() {
      FireModule m = active();
      return m == null ? 0.0F : (float)(m.height.value() / 100.0);
   }

   public static int tint(int vanilla) {
      FireModule m = active();
      if (m == null) {
         return vanilla;
      } else {
         int a = (int)Math.round((vanilla >>> 24) * m.opacity.value() / 100.0);
         return a << 24 | vanilla & 0x00FFFFFF;
      }
   }
}
