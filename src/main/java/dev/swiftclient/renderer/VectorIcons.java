package dev.swiftclient.renderer;

import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class VectorIcons {
   private static final Set<String> KNOWN = Set.of("gear", "globe", "users", "cloud", "shirt", "swiftclient");
   private static GuiGraphicsExtractor CTX;

   public static void bind(GuiGraphicsExtractor ctx) {
      CTX = ctx;
   }

   public static void draw(String name, int x, int y, int size, int color) {
      if (CTX != null) {
         String n = KNOWN.contains(name) ? name : "swiftclient";
         CTX.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath("swiftclient", "icon_" + n), x, y, size, size, color);
      }
   }

   public static void draw(String name, int x, int y, int size, int color, int bg) {
      draw(name, x, y, size, color);
   }

   private VectorIcons() {
   }
}
