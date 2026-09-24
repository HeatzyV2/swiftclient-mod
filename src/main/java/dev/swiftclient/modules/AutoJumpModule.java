package dev.swiftclient.modules;

import dev.swiftclient.core.mods.Module;
import net.minecraft.client.Minecraft;

public final class AutoJumpModule extends Module {
   public AutoJumpModule() {
      super("autojump", "Auto Jump", "Saute automatiquement les blocs d'un cran.", "Movement", "autojump", false);
   }

   @Override
   protected void onEnable() {
      VanillaOption.force(this, Minecraft.getInstance().options.autoJump(), true);
   }

   @Override
   protected void onDisable() {
      VanillaOption.restore(this, Minecraft.getInstance().options.autoJump());
   }
}
