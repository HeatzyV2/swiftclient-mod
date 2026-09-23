package dev.swiftclient.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Intentionally empty — 3D title skins clash with Essential and clutter the Swift rail. */
@Mixin({TitleScreen.class})
public abstract class TitleScreenSkinMixin extends Screen {
   protected TitleScreenSkinMixin(Component title) {
      super(title);
   }

   @Inject(
      method = {"extractRenderState"},
      at = {@At("TAIL")}
   )
   private void lightclient$drawSkin(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      // no-op: skins removed for Essential compat + clean title DA
   }
}
