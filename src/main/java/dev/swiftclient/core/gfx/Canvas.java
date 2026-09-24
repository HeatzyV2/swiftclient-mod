package dev.swiftclient.core.gfx;

public interface Canvas {
   void fill(int x, int y, int x2, int y2, int argb);

   void gradientV(int x, int y, int w, int h, int top, int bottom);

   void roundRect(int x, int y, int w, int h, float radius, int argb);

   void card(int x, int y, int w, int h, int bg, int border, int thickness, float radius);

   void text(String s, int x, int y, int argb, boolean shadow);

   void centeredText(String s, int cx, int y, int argb, boolean shadow);

   int textWidth(String s);

   int lineHeight();

   void pushScissor(int x, int y, int w, int h);

   void popScissor();

   void pushTranslate(int dx, int dy);

   void popTranslate();

   void pushScale(int ox, int oy, float scale);

   void popScale();

   void playerHead(String uuid, int x, int y, int size);

   void vanillaButton(int x, int y, int w, int h, boolean hovered);

   default void playerModel(int x, int y, int w, int h, int mouseX, int mouseY, float delta) {
   }

   default void buttonBackground(int x, int y, int w, int h, boolean hovered) {
      this.card(x, y, w, h, hovered ? -1441125824 : -1726998496, hovered ? -1606712586 : 0, hovered ? 1 : 0, Math.max(2.0F, h * 0.5F));
   }

   default void textureRegion(Object textureHandle, int x, int y, int w, int h, int u, int v, int regionW, int regionH, int texW, int texH) {
   }

   default void icon(String name, int x, int y, int size, int argb) {
   }

   default boolean richText(Object handle, int x, int y, int argb, boolean shadow) {
      return false;
   }

   default int richWidth(Object handle) {
      return -1;
   }

   default boolean itemStack(Object handle, int x, int y) {
      return false;
   }

   default void logo(int x, int y, int size) {
   }

   default boolean glassRect(int x, int y, int w, int h, float radius, int tintArgb, float blurPx) {
      return false;
   }

   default boolean blurBackground(int x, int y, int w, int h, int tintArgb, float blurPx) {
      return false;
   }
}
