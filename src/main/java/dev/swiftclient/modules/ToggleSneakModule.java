package dev.swiftclient.modules;

import dev.swiftclient.core.mods.Module;
import net.minecraft.client.Minecraft;

/** Real toggle: while enabled, the vanilla Sneak key switches sneaking on and off (vanilla "Sneak: Toggle"). */
public final class ToggleSneakModule extends Module {
   public ToggleSneakModule() {
      super("togglesneak", "Toggle Sneak", "Press the Sneak key once to stay crouched, once more to stand up.", "Movement", "togglesneak", false);
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
