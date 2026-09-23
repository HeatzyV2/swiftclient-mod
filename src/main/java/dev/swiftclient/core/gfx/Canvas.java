package dev.swiftclient.core.gfx;

public interface Canvas {
   void fill(int var1, int var2, int var3, int var4, int var5);

   void gradientV(int var1, int var2, int var3, int var4, int var5, int var6);

   void roundRect(int var1, int var2, int var3, int var4, float var5, int var6);

   void card(int var1, int var2, int var3, int var4, int var5, int var6, int var7, float var8);

   void text(String var1, int var2, int var3, int var4, boolean var5);

   void centeredText(String var1, int var2, int var3, int var4, boolean var5);

   int textWidth(String var1);

   int lineHeight();

   void pushScissor(int var1, int var2, int var3, int var4);

   void popScissor();

   void pushTranslate(int var1, int var2);

   void popTranslate();

   void pushScale(int var1, int var2, float var3);

   void popScale();

   void playerHead(String var1, int var2, int var3, int var4);

   void vanillaButton(int var1, int var2, int var3, int var4, boolean var5);

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
