package dev.swiftclient.mixin;

import dev.swiftclient.renderer.RoundedButtons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractWidget.class})
public abstract class ButtonWidgetMixin {
   @Inject(
      method = {"extractWidgetRenderState"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void lightclient$replaceBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.gui.screen() instanceof TitleScreen) {
         if ((Object)this instanceof Button) {
            AbstractWidget self = (AbstractWidget)(Object)this;
            int x = self.getX();
            int y = self.getY();
            int w = self.getWidth();
            int h = self.getHeight();
            boolean hovered = self.isHovered() || mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
            boolean active = self.active;
            RoundedButtons.bind(context);
            RoundedButtons.drawButtonBackground(x, y, w, h, hovered, active);
            int textColor = active ? -1 : -6250336;
            Component message = self.getMessage();
            context.centeredText(mc.font, message, x + w / 2, y + (h - 8) / 2, textColor);
            ci.cancel();
         }
      }
   }
}
