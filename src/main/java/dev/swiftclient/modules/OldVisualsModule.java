package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;

/** The 1.7 / 1.8 feel: no hand dip after a hit, armour that flashes red with its wearer. */
@ConsumedBy({"ItemInHandRendererMixin", "EquipmentLayerRendererMixin"})
public final class OldVisualsModule extends Module {
   public final ModuleSetting noDip;
   public final ModuleSetting redArmor;

   public OldVisualsModule() {
      super("old_visuals", "1.7 Visuals", "Brings back the 1.7 / 1.8 look: no hand dip after each hit, armour flashing red with its wearer.", "Visual", "retro", false);
      this.noDip = this.toggle("no_dip", "No hand dip", true).desc("Your item stays up after a hit instead of lowering while the attack recharges.");
      this.redArmor = this.toggle("red_armor", "Red armor on hit", true).desc("Armour turns red with the player or mob wearing it when they are hurt.");
   }

   public static boolean on(boolean dip) {
      if (!ModuleManager.isLoaded()) {
         return false;
      }

      OldVisualsModule m = ModuleManager.get(OldVisualsModule.class);
      return m.isEnabled() && (dip ? m.noDip.boolValue() : m.redArmor.boolValue());
   }
}
