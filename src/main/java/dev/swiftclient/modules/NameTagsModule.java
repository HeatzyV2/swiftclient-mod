package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;

/** Name tags above players and mobs: your own in third person, size and background. */
@ConsumedBy({"AvatarNameSelfMixin", "NameTagStyleMixin"})
public final class NameTagsModule extends Module {
   public final ModuleSetting own;
   public final ModuleSetting scale;
   public final ModuleSetting background;

   public NameTagsModule() {
      super("nametags", "NameTags", "Name tags above players: yours in third person, bigger or smaller, with or without background.", "Visual", "nametag", true);
      this.own = this.toggle("own", "Show your own", true).desc("See your own name tag in third person view.");
      this.scale = this.slider("scale", "Size", 100.0, 50.0, 200.0, 10.0, "%").desc("Size of the name tags. 100% is vanilla.");
      this.background = this.slider("background", "Background", 25.0, 0.0, 100.0, 5.0, "%").desc("Opacity of the dark box behind names. 0% removes it.");
   }

   private static NameTagsModule active() {
      if (!ModuleManager.isLoaded()) {
         return null;
      }

      NameTagsModule m = ModuleManager.get(NameTagsModule.class);
      return m.isEnabled() ? m : null;
   }

   public static boolean showOwn() {
      NameTagsModule m = active();
      return m != null && m.own.boolValue();
   }

   public static float scale() {
      NameTagsModule m = active();
      return m == null ? 1.0F : (float)(m.scale.value() / 100.0);
   }

   /** Background opacity, or -1 to keep vanilla. */
   public static float background() {
      NameTagsModule m = active();
      return m == null ? -1.0F : (float)(m.background.value() / 100.0);
   }
}
