package dev.swiftclient.modules;

import dev.swiftclient.core.mods.Module;
import net.minecraft.client.Minecraft;

/** Real toggle: while enabled, the vanilla Sneak key switches sneaking on and off (vanilla "Sneak: Toggle"). */
public final class ToggleSneakModule extends Module {
   public ToggleSneakModule() {
      super("togglesneak", "Toggle Sneak", "Appuie une fois sur la touche S'accroupir pour rester accroupi, une seconde fois pour te relever.", "Movement", "togglesneak", false);
   }

   @Override
   protected void onEnable() {
      VanillaOption.force(this, Minecraft.getInstance().options.toggleCrouch(), true);
   }

   @Override
   protected void onDisable() {
      VanillaOption.restore(this, Minecraft.getInstance().options.toggleCrouch());
   }
}
