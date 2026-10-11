package dev.swiftclient.platform;

import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.ui.UiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

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
      HudManager.setGuiScale(Minecraft.getInstance().getWindow().getGuiScale());
      this.ui.draw(c, mouseX, mouseY, delta);
   }

   private static void mods() {
      long w = Minecraft.getInstance().getWindow().handle();
      boolean shift = GLFW.glfwGetKey(w, GLFW.GLFW_KEY_LEFT_SHIFT) == 1 || GLFW.glfwGetKey(w, GLFW.GLFW_KEY_RIGHT_SHIFT) == 1;
      boolean ctrl = GLFW.glfwGetKey(w, GLFW.GLFW_KEY_LEFT_CONTROL) == 1 || GLFW.glfwGetKey(w, GLFW.GLFW_KEY_RIGHT_CONTROL) == 1
         || GLFW.glfwGetKey(w, GLFW.GLFW_KEY_LEFT_SUPER) == 1 || GLFW.glfwGetKey(w, GLFW.GLFW_KEY_RIGHT_SUPER) == 1;
      UiScreen.setModifiers(shift, ctrl);
   }

   public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
      mods();
      return this.ui.click(e.x(), e.y(), e.button()) ? true : super.mouseClicked(e, doubled);
   }

   public boolean mouseReleased(MouseButtonEvent e) {
      mods();
      return this.ui.mouseReleased(e.x(), e.y(), e.button()) ? true : super.mouseReleased(e);
   }

   public boolean mouseDragged(MouseButtonEvent e, double dragX, double dragY) {
      mods();
      return this.ui.mouseDragged(e.x(), e.y(), e.button()) ? true : super.mouseDragged(e, dragX, dragY);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
      mods();
      return this.ui.scroll(mouseX, mouseY, dy) ? true : super.mouseScrolled(mouseX, mouseY, dx, dy);
   }

   public boolean charTyped(CharacterEvent e) {
      return this.ui.charTyped(e.codepointAsString()) ? true : super.charTyped(e);
   }

   public boolean keyPressed(KeyEvent e) {
      mods();
      return this.ui.keyPressed(e.key()) ? true : super.keyPressed(e);
   }

   public boolean shouldCloseOnEsc() {
      return this.ui.closeOnEscape();
   }

   public void onClose() {
      this.minecraft.setScreenAndShow(this.parent);
   }
}
