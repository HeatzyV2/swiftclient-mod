package dev.swiftclient.platform;

import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.theme.ThemeManager;
import dev.swiftclient.core.ui.UiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class CoreScreenHost extends Screen {
   private final UiScreen ui;
   private final Screen parent;

   public CoreScreenHost(UiScreen ui, Screen parent) {
      super(Component.literal(ui.title()));
      this.ui = ui;
      this.parent = parent;
      ui.bindHost(next -> Minecraft.getInstance().setScreenAndShow(new CoreScreenHost(next, this)), () -> Minecraft.getInstance().setScreenAndShow(parent));
   }

   protected void init() {
      this.ui.layout(this.width, this.height);
   }

   public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
      CanvasImpl c = new CanvasImpl(ctx);
      ThemeManager.tick();
      HudManager.setGuiScale(Minecraft.getInstance().getWindow().getGuiScale());
      this.ui.draw(c, mouseX, mouseY, delta);
      ThemeManager.drawCurtain(c, this.width, this.height);
   }

   public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
      return this.ui.click(e.x(), e.y(), e.button()) ? true : super.mouseClicked(e, doubled);
   }

   public boolean mouseReleased(MouseButtonEvent e) {
      return this.ui.mouseReleased(e.x(), e.y(), e.button()) ? true : super.mouseReleased(e);
   }

   public boolean mouseDragged(MouseButtonEvent e, double dragX, double dragY) {
      return this.ui.mouseDragged(e.x(), e.y(), e.button()) ? true : super.mouseDragged(e, dragX, dragY);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
      return this.ui.scroll(mouseX, mouseY, dy) ? true : super.mouseScrolled(mouseX, mouseY, dx, dy);
   }

   public boolean charTyped(CharacterEvent e) {
      return this.ui.charTyped(e.codepointAsString()) ? true : super.charTyped(e);
   }

   public boolean keyPressed(KeyEvent e) {
      return this.ui.keyPressed(e.key()) ? true : super.keyPressed(e);
   }

   public boolean shouldCloseOnEsc() {
      return this.ui.closeOnEscape();
   }

   public void onClose() {
      this.minecraft.setScreenAndShow(this.parent);
   }
}
