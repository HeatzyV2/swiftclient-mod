package dev.swiftclient.modules;

import dev.swiftclient.compat.Mc;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.mods.ZoomState;
import dev.swiftclient.input.SwiftKeys;
import dev.swiftclient.mixin.SimpleOptionAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

public final class ZoomModule extends Module {
   private static final int MODE_TOGGLE = 1;
   public final ModuleSetting mode;
   public final ModuleSetting duration;
   public final ModuleSetting smooth;
   public final ModuleSetting fov;
   public final ModuleSetting sensitivity;
   /** The player's mouse sensitivity while it is scaled down for the zoom, null otherwise. */
   private Double savedSensitivity;
   /** The player's FOV while zoomed (or easing back), null otherwise. */
   private Integer savedFov;

   public ZoomModule() {
      super("zoom", "Zoom", "Hold the zoom key to zoom in (scroll to adjust).", "PvP", "zoom", false);
      this.action("controls", "Zoom key", () -> SwiftKeys.label(SwiftKeys.ZOOM), SwiftKeys::openControls)
         .desc("Change it in Options > Controls > Key Binds, under Swift Client.");
      this.mode = this.cycle("mode", "Mode", new String[]{"Hold", "Toggle"}, 0).desc("Hold the key, or press once to toggle the zoom on and off.");
      this.duration = this.slider("duration", "Smooth duration", 180.0, 50.0, 500.0, 10.0, "ms")
         .desc("How long the zoom takes to reach its target. Shorter feels snappier, longer feels cinematic.");
      this.smooth = this.toggle("smooth", "Smooth transition", true).desc("Ease in and out instead of snapping to the zoomed view.");
      this.fov = this.slider("fov", "Zoom FOV", 30.0, 10.0, 60.0, 5.0, "").desc("Field of view while zoomed. The lower the value, the closer it gets.");
      this.sensitivity = this.toggle("sensitivity", "Scale mouse sensitivity", true)
         .desc("Slow the mouse down in proportion to the zoom, so aiming stays precise when zoomed in.");
   }

   private static volatile boolean zooming;

   /** True while the zoom drives the FOV option (zoomed or easing back). */
   public static boolean zooming() {
      return zooming;
   }

   /** The FOV the zoom starts from and comes back to: the player's, or Custom FOV's. */
   private int baseFov() {
      return dev.swiftclient.visuals.CustomFovModule.base(this.savedFov);
   }

   private static OptionInstance<Integer> fovOption() {
      return Minecraft.getInstance().options.fov();
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      ZoomState.ensureTarget(this.fov.value());
      boolean keyDown = mc.player != null && Mc.screen() == null && SwiftKeys.ZOOM.isDown();
      ZoomState.update(keyDown, this.mode.cycleIndex() == MODE_TOGGLE);
      if (ZoomState.holding) {
         if (this.savedFov == null) {
            this.savedFov = fovOption().get();
            ZoomState.depuis(this.baseFov());
         }

         ZoomState.viser(ZoomState.target(), this.smooth.boolValue(), this.duration.value());
      } else if (this.savedFov != null) {
         ZoomState.viser(this.baseFov(), this.smooth.boolValue(), this.duration.value());
      }

      this.applyFrame();
      this.applySensitivity();
      zooming = this.savedFov != null;
   }

   /** Mouse sensitivity follows the zoom: the narrower the view, the slower the mouse. */
   private void applySensitivity() {
      Minecraft mc = Minecraft.getInstance();
      OptionInstance<Double> sens = mc.options.sensitivity();
      if (this.savedFov != null && this.sensitivity.boolValue() && mc.player != null) {
         if (this.savedSensitivity == null) {
            this.savedSensitivity = sens.get();
         }

         double ratio = Math.max(0.15, Math.min(1.0, ZoomState.courant() / Math.max(30.0, this.baseFov())));
         ((SimpleOptionAccessor)(Object)sens).swiftclient$setRaw(this.savedSensitivity * ratio);
      } else {
         this.restoreSensitivity();
      }
   }

   private void restoreSensitivity() {
      if (this.savedSensitivity != null) {
         Minecraft.getInstance().options.sensitivity().set(this.savedSensitivity);
         this.savedSensitivity = null;
      }
   }

   @Override
   protected void onRender(float partialTick) {
      this.applyFrame();
   }

   /** Per-frame FOV, so the ease runs at frame rate rather than tick rate. */
   private void applyFrame() {
      if (this.savedFov != null) {
         if (!ZoomState.holding && ZoomState.arrive()) {
            fovOption().set(this.savedFov);
            this.savedFov = null;
            zooming = false;
         } else {
            zooming = true;
            ((SimpleOptionAccessor)(Object)fovOption()).swiftclient$setRaw((int)Math.round(ZoomState.courant()));
         }
      }
   }

   @Override
   protected void onDisable() {
      this.restoreSensitivity();
      ZoomState.stop();
      zooming = false;
      if (this.savedFov != null) {
         fovOption().set(this.savedFov);
         this.savedFov = null;
      }
   }

   /** Called right before options.txt is written, so a zoomed FOV is never saved as the player's. */
   public void beforeOptionsSave() {
      if (this.savedSensitivity != null) {
         ((SimpleOptionAccessor)(Object)Minecraft.getInstance().options.sensitivity()).swiftclient$setRaw(this.savedSensitivity);
      }

      if (this.savedFov != null) {
         ((SimpleOptionAccessor)(Object)fovOption()).swiftclient$setRaw(this.savedFov);
      }
   }
}
