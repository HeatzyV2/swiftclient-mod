package dev.swiftclient.platform;

import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.ui.UiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.network.chat.Component;
import dev.swiftclient.compat.Input;

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
      HudManager.setGuiScale((float)Minecraft.getInstance().getWindow().getGuiScale());
      this.ui.draw(c, mouseX, mouseY, delta);
   }

   private static void mods() {
      UiScreen.setModifiers(Input.shiftDown(), Input.ctrlDown());
   }

   //? if >=1.21.9 {
   public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
      mods();
      return this.ui.click(e.x(), e.y(), Input.toCoreMouse(e.button())) ? true : super.mouseClicked(e, doubled);
   }

   public boolean mouseReleased(MouseButtonEvent e) {
      mods();
      return this.ui.mouseReleased(e.x(), e.y(), Input.toCoreMouse(e.button())) ? true : super.mouseReleased(e);
   }

   public boolean mouseDragged(MouseButtonEvent e, double dragX, double dragY) {
      mods();
      return this.ui.mouseDragged(e.x(), e.y(), Input.toCoreMouse(e.button())) ? true : super.mouseDragged(e, dragX, dragY);
   }
   //?} else {
   /*public boolean mouseClicked(double x, double y, int button) {
      mods();
      return this.ui.click(x, y, button) ? true : super.mouseClicked(x, y, button);
   }

   public boolean mouseReleased(double x, double y, int button) {
      mods();
      return this.ui.mouseReleased(x, y, button) ? true : super.mouseReleased(x, y, button);
   }

   public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
      mods();
      return this.ui.mouseDragged(x, y, button) ? true : super.mouseDragged(x, y, button, dragX, dragY);
   }
   *///?}

   public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
      mods();
      return this.ui.scroll(mouseX, mouseY, dy) ? true : super.mouseScrolled(mouseX, mouseY, dx, dy);
   }

   //? if >=1.21.9 {
   public boolean charTyped(CharacterEvent e) {
      return this.ui.charTyped(e.codepointAsString()) ? true : super.charTyped(e);
   }

   public boolean keyPressed(KeyEvent e) {
      mods();
      return this.ui.keyPressed(Input.toCoreKey(e.key())) ? true : super.keyPressed(e);
   }
   //?} else {
   /*public boolean charTyped(char c, int modifiers) {
      return this.ui.charTyped(String.valueOf(c)) ? true : super.charTyped(c, modifiers);
   }

   public boolean keyPressed(int key, int scancode, int modifiers) {
      mods();
      return this.ui.keyPressed(key) ? true : super.keyPressed(key, scancode, modifiers);
   }
   *///?}

   public boolean shouldCloseOnEsc() {
      return this.ui.closeOnEscape();
   }

   public void onClose() {
      this.minecraft.setScreenAndShow(this.parent);
   }
}
