package dev.swiftclient.visuals;

import dev.swiftclient.core.gfx.Canvas;

/** The on-screen parts of the small visual modules, drawn together with the HUD (HudClient). */
public final class ExtraHud {
   private ExtraHud() {
   }

   public static void render(Canvas dl, int w, int h) {
      DamageIndicatorModule.render(dl, w, h);
      ArmorWarningModule.render(dl, w, h);
      TotemCounterModule.render(dl, w, h);
      KillEffectModule.render(dl, w, h);
   }
}
