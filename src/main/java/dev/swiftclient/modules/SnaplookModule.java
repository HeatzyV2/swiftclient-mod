package dev.swiftclient.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.input.SwiftKeys;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/** Hold a key for a quick look behind you (front camera), release to come back. */
public final class SnaplookModule extends Module {
   public final ModuleSetting view;
   /** Camera before the key was pressed, null while not snapped. */
   private CameraType saved;

   public SnaplookModule() {
      super("snaplook", "Snaplook", "Hold a key to glance behind you in third person, release to come back.", "PvP", "snaplook", false);
      this.action("key", "Snaplook key", () -> SwiftKeys.label(SwiftKeys.SNAPLOOK), SwiftKeys::openControls)
         .desc("Change it in Options > Controls > Key Binds, under Swift Client.");
      this.view = this.cycle("view", "View", new String[]{"Front", "Back"}, 0)
         .desc("Front: the camera faces you, to see who follows. Back: classic third person.");
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      boolean down = mc.player != null && mc.gui.screen() == null && SwiftKeys.SNAPLOOK.isDown();
      if (down && this.saved == null) {
         this.saved = mc.options.getCameraType();
         mc.options.setCameraType(this.view.cycleIndex() == 0 ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK);
      } else if (!down && this.saved != null) {
         mc.options.setCameraType(this.saved);
         this.saved = null;
      }
   }

   @Override
   protected void onDisable() {
      if (this.saved != null) {
         Minecraft.getInstance().options.setCameraType(this.saved);
         this.saved = null;
      }
   }
}
