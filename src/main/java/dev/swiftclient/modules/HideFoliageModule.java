package dev.swiftclient.modules;

/** Hides grass, ferns, bushes and flowers: a clear view of the ground (and more FPS in plains and jungles). */
public final class HideFoliageModule extends BuiltinPackModule {
   public HideFoliageModule() {
      super("hide_foliage", "Hide Foliage", "Hides tall grass, ferns, bushes and flowers for a clear view of the ground and more FPS.", "Visual", "foliage", "hide_foliage");
   }
}
