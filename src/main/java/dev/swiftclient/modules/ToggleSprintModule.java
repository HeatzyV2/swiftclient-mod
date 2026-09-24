package dev.swiftclient.modules;

import dev.swiftclient.core.mods.Module;
import net.minecraft.client.Minecraft;

/** Real toggle: while enabled, the vanilla Sprint key switches sprinting on and off (vanilla "Sprint: Toggle"). */
public final class ToggleSprintModule extends Module {
   public ToggleSprintModule() {
      super("togglesprint", "Toggle Sprint", "Press the Sprint key once to sprint, once more to stop.", "Movement", "togglesprint", false);
   }

   @Override
   protected void onEnable() {
      VanillaOption.force(this, Minecraft.getInstance().options.toggleSprint(), true);
   }

   @Override
   protected void onDisable() {
      VanillaOption.restore(this, Minecraft.getInstance().options.toggleSprint());
   }
}
