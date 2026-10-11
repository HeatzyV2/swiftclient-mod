package dev.swiftclient.core.ui;

import dev.swiftclient.core.gfx.Canvas;
import java.util.function.Consumer;

public abstract class UiScreen {
   protected int width;
   protected int height;
   protected static final int KEY_ESCAPE = 256;
   protected static final int KEY_ENTER = 257;
   protected static final int KEY_BACKSPACE = 259;
   private static volatile boolean shiftDown;
   private static volatile boolean ctrlDown;
   private Consumer<UiScreen> opener;
   private Runnable closer;

   /** Set by the host before each input event, so screens can react to Shift / Ctrl (Cmd on macOS). */
   public static void setModifiers(boolean shift, boolean ctrl) {
      shiftDown = shift;
      ctrlDown = ctrl;
   }

   protected static boolean shift() {
      return shiftDown;
   }

   protected static boolean ctrl() {
      return ctrlDown;
   }

   public abstract String title();

   public void layout(int width, int height) {
      this.width = width;
      this.height = height;
   }

   public abstract void draw(Canvas c, int mouseX, int mouseY, float delta);

   public boolean click(double mouseX, double mouseY, int button) {
      return false;
   }

   public boolean scroll(double mouseX, double mouseY, double amount) {
      return false;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return false;
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button) {
      return false;
   }

   public boolean charTyped(String s) {
      return false;
   }

   public boolean keyPressed(int keyCode) {
      return false;
   }

   public boolean closeOnEscape() {
      return true;
   }

   public void bindHost(Consumer<UiScreen> opener, Runnable closer) {
      this.opener = opener;
      this.closer = closer;
   }

   protected void open(UiScreen next) {
      if (this.opener != null) {
         this.opener.accept(next);
      }
   }

   protected void back() {
      if (this.closer != null) {
         this.closer.run();
      }
   }
}
