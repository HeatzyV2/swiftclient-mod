package dev.swiftclient.mixin;

import dev.swiftclient.ui.SwiftLoadingScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Swift loading screen in place of the world loading, saving and message screens; it then fades over the next one. */
@Mixin({Screen.class})
public abstract class LoadingScreensMixin {
   @Inject(method = {"extractRenderStateWithTooltipAndSubtitles"}, at = {@At("HEAD")}, cancellable = true)
   private void swiftclient$loadingScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
      Screen self = (Screen)(Object)this;
      if (self instanceof LevelLoadingScreen loading) {
         SwiftLoadingScreen.draw(graphics, 1.0F, ((LevelLoadingScreenAccessor)loading).swiftclient$smoothedProgress(), SwiftLoadingScreen.worldLabel());
         ci.cancel();
      } else if (self instanceof ProgressScreen progress) {
         ProgressScreenAccessor state = (ProgressScreenAccessor)progress;
         Component header = state.swiftclient$header();
         int percent = state.swiftclient$progress();
         SwiftLoadingScreen.draw(graphics, 1.0F, percent > 0 ? percent / 100.0F : -1.0F, swiftclient$status(header != null ? header : self.getTitle()));
         ci.cancel();
      } else if (self instanceof GenericMessageScreen) {
         SwiftLoadingScreen.draw(graphics, 1.0F, -1.0F, swiftclient$status(self.getTitle()));
         ci.cancel();
      }
   }

   @Inject(method = {"extractRenderStateWithTooltipAndSubtitles"}, at = {@At("TAIL")})
   private void swiftclient$fadeOut(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
      SwiftLoadingScreen.fadeOut(graphics);
   }

   @Unique
   private static String swiftclient$status(Component title) {
      String s = title == null ? "" : title.getString();
      return s.isBlank() ? SwiftLoadingScreen.loadingLabel() : s;
   }
}
