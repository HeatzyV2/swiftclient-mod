package dev.swiftclient.visuals;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

/** A field of view of its own for the held item and hands, independent from the world FOV (CS viewmodel_fov). */
@ConsumedBy("ViewModelFovMixin")
public final class ViewModelModule extends Module {
   public static volatile ViewModelModule active;
   public final ModuleSetting fov;

   public ViewModelModule() {
      super("view_model", "View Model", "Gives your held item and hands their own field of view. A higher value pushes them away so the whole item floats in view, whatever your world FOV.", "Visual", "hand", false);
      this.fov = this.slider("fov", "View model FOV", 90.0, 30.0, 150.0, 1.0, "°").desc("Field of view used for your hands and held item only. Vanilla is about 70.");
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   public static float fov(float vanilla) {
      ViewModelModule module = active;
      return module != null ? (float)module.fov.value() : vanilla;
   }
}
