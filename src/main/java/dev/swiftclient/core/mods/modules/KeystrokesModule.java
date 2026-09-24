package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

@ConsumedBy({"HudManager"})
public final class KeystrokesModule extends Module {
   public KeystrokesModule() {
      super("hud_keystrokes", "Keystrokes", "Movement keys and mouse buttons. Move it in the HUD Editor.", "HUD", "keyboard", false);
      this.settings
         .add(ModuleSetting.toggle("hud_keystrokes", "mouse", "Mouse buttons", true).desc("Show the two mouse buttons under the movement keys.").group("Keys"));
      this.settings
         .add(ModuleSetting.toggle("hud_keystrokes", "cps", "CPS on buttons", true).desc("Write the clicks per second inside each mouse button.").group("Keys"));
      this.settings.add(ModuleSetting.toggle("hud_keystrokes", "space", "Space bar", true).desc("Show the space bar under the other keys.").group("Keys"));
      this.settings
         .add(
            ModuleSetting.toggle("hud_keystrokes", "sneak", "Sneak key", false)
               .desc("Show the sneak key. Off by default: it is rarely the one you watch.")
               .group("Keys")
         );
      this.settings
         .add(ModuleSetting.slider("hud_keystrokes", "size", "Key size", 16.0, 12.0, 24.0, 1.0, "px").desc("Side of one key square, in pixels.").group("Style"));
      this.settings
         .add(ModuleSetting.color("hud_keystrokes", "pressedColor", "Pressed color", -1).desc("Colour of a key while it is held down.").group("Style"));
      this.settings
         .add(
            ModuleSetting.color("hud_keystrokes", "idleColor", "Idle color", 1996488704)
               .desc("Colour of a key at rest. Lower the alpha to make it discreet.")
               .group("Style")
         );
   }
}
