package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

/** Motion blur: each frame keeps a trace of the previous ones, smoothing fast camera turns. */
@ConsumedBy("GameRendererBlurMixin")
public final class MotionBlurModule extends Module {
   public final ModuleSetting strength;

   public MotionBlurModule() {
      super("motion_blur", "Motion Blur", "Leaves a light trail when you turn, for a smoother, cinematic feel.", "Visual", "motionblur", false);
      this.strength = this.slider("strength", "Strength", 50.0, 10.0, 90.0, 5.0, "%").desc("How much of the previous frames stays visible. High values smear a lot.");
   }
}
