package dev.swiftclient.core.ui.widget;

import dev.swiftclient.core.gfx.Canvas;

public interface CoreWidget {
   void draw(Canvas c, int x, int y, int w, int h, boolean hovered, boolean active, int mouseX, int mouseY, float delta);

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
