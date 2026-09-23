package dev.swiftclient.core.ui.widget;

import dev.swiftclient.core.gfx.Canvas;

public interface CoreWidget {
   void draw(Canvas var1, int var2, int var3, int var4, int var5, boolean var6, boolean var7, int var8, int var9, float var10);

   default void onClick() {
   }

   default boolean enabled() {
      return true;
   }

   default boolean clickWhenDisabled() {
      return false;
   }

   default String tooltip() {
      return null;
   }
}
