package dev.swiftclient.compat;

import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/** Text styles using one of the mod's fonts (fonts are FontDescription objects since 1.21.9, plain ids before). */
public final class Fonts {
   private Fonts() {
   }

   public static Style style(Identifier font) {
      //? if >=1.21.9 {
      return Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(font));
      //?} else
      /*return Style.EMPTY.withFont(font);*/
   }
}
