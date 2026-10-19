package dev.swiftclient.visuals;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

/** A world field of view of your choice, past the 30-110 range of the vanilla slider. Zoom still works on top. */
@ConsumedBy("CustomFovMixin")
public final class CustomFovModule extends Module {
   public static volatile CustomFovModule active;
   public final ModuleSetting fov;

   public CustomFovModule() {
      super("custom_fov", "Custom FOV", "Sets your field of view freely, up to 160, past the limit of the vanilla slider.", "Visual", "eye", false);
      this.fov = this.slider("fov", "FOV", 110.0, 30.0, 160.0, 1.0, "°").desc("Your field of view. Vanilla goes from 30 to 110.");
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   /** The FOV the world is drawn with, from the player's vanilla FOV. */
   public static int base(int vanilla) {
      CustomFovModule module = active;
      return module != null ? (int)Math.round(module.fov.value()) : vanilla;
   }
}
