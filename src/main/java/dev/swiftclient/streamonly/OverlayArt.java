package dev.swiftclient.streamonly;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Optional;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.network.chat.FormattedText;

final class OverlayArt {
   private static final int GLYPH = 8;
   private static final int SHADOW_MASK = 16579836;
   private static Image selection;
   private static Slice tooltipBackground;
   private static Slice tooltipFrame;
   private static boolean[] glyphs;
   private static boolean loaded;

   private OverlayArt() {
   }

   static void reset() {
      selection = null;
      tooltipBackground = null;
      tooltipFrame = null;
      glyphs = null;
      loaded = false;
   }

   static Image selection() {
      load();
      return selection;
   }

   static Slice tooltipBackground() {
      load();
      return tooltipBackground;
   }

   static Slice tooltipFrame() {
      load();
      return tooltipFrame;
   }

   private static void load() {
      if (!loaded) {
         loaded = true;
         selection = image("textures/gui/sprites/hud/hotbar_selection.png");
         tooltipBackground = slice("textures/gui/sprites/tooltip/background.png", 9, false);
         tooltipFrame = slice("textures/gui/sprites/tooltip/frame.png", 10, true);
         Image font = image("textures/font/ascii.png");
         if (font != null) {
            glyphs = new boolean[font.width() * font.height()];

            for(int i = 0; i < glyphs.length; ++i) {
               glyphs[i] = font.pixels()[i] >>> 24 != 0;
            }
         }

      }
   }

   private static Image image(String path) {
      Resource resource = (Resource)Minecraft.getInstance().getResourceManager().getResource(Identifier.withDefaultNamespace(path)).orElse(null);
      if (resource == null) {
         return null;
      } else {
         try {
            InputStream in = resource.open();

            Image var13;
            try {
               NativeImage image = NativeImage.read(in);

               try {
                  int[] abgr = image.getPixelsABGR();
                  int[] out = new int[abgr.length];

                  for(int i = 0; i < abgr.length; ++i) {
                     int a = abgr[i] >>> 24;
                     out[i] = Canvas.premultiply(a << 24 | (abgr[i] & 255) << 16 | (abgr[i] >>> 8 & 255) << 8 | abgr[i] >>> 16 & 255, a);
                  }

                  var13 = new Image(out, image.getWidth(), image.getHeight());
               } catch (Throwable var10) {
                  if (image != null) {
                     try {
                        image.close();
                     } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                     }
                  }

                  throw var10;
               }

               if (image != null) {
                  image.close();
               }
            } catch (Throwable var11) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var8) {
                     var11.addSuppressed(var8);
                  }
               }

               throw var11;
            }

            if (in != null) {
               in.close();
            }

            return var13;
         } catch (RuntimeException | IOException var12) {
            return null;
         }
      }
   }

   private static Slice slice(String path, int border, boolean stretch) {
      Image image = image(path);
      if (image == null) {
         return null;
      } else {
         Resource meta = (Resource)Minecraft.getInstance().getResourceManager().getResource(Identifier.withDefaultNamespace(path + ".mcmeta")).orElse(null);
         if (meta != null) {
            try {
               Reader reader = meta.openAsReader();

               try {
                  JsonObject scaling = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("gui").getAsJsonObject("scaling");
                  if (scaling.has("border")) {
                     border = scaling.get("border").getAsInt();
                  }

                  stretch = scaling.has("stretch_inner") && scaling.get("stretch_inner").getAsBoolean();
               } catch (Throwable var9) {
                  if (reader != null) {
                     try {
                        reader.close();
                     } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                     }
                  }

                  throw var9;
               }

               if (reader != null) {
                  reader.close();
               }
            } catch (RuntimeException | IOException var10) {
            }
         }

         return new Slice(image, border, stretch);
      }
   }

   static void slice(Canvas canvas, Slice slice, int x, int y, int width, int height, int scale) {
      if (slice != null) {
         Image img = slice.image();
         int b = slice.border();
         int innerW = img.width() - 2 * b;
         int innerH = img.height() - 2 * b;
         if (innerW > 0 && innerH > 0) {
            int[] column = new int[width];
            int[] row = new int[height];

            for(int dx = 0; dx < width; ++dx) {
               column[dx] = sourceAt(dx, width, b, innerW, slice.stretch());
            }

            for(int dy = 0; dy < height; ++dy) {
               row[dy] = sourceAt(dy, height, b, innerH, slice.stretch());
            }

            for(int dy = 0; dy < height; ++dy) {
               for(int dx = 0; dx < width; ++dx) {
                  int src = img.pixels()[row[dy] * img.width() + column[dx]];
                  if (src >>> 24 != 0) {
                     canvas.blendPixel(src, (x + dx) * scale, (y + dy) * scale, scale);
                  }
               }
            }

         }
      }
   }

   private static int sourceAt(int d, int size, int b, int inner, boolean stretch) {
      if (d < b) {
         return d;
      } else if (d >= size - b) {
         return b + inner + (d - (size - b));
      } else {
         int into = d - b;
         int middle = size - 2 * b;
         return b + (stretch ? into * inner / Math.max(middle, 1) : into % inner);
      }
   }

   static void text(Canvas canvas, Component text, int x, int y, int scale, int argb, boolean shadow) {
      load();
      if (glyphs != null) {
         int[] cursor = new int[]{x};
         text.visit((style, piece) -> {
            int color = style.getColor() != null ? argb & -16777216 | style.getColor().getValue() : argb;

            int advance;
            for(int i = 0; i < piece.length(); cursor[0] += advance) {
               int cp = piece.codePointAt(i);
               i += Character.charCount(cp);
               advance = advance(cp, style);
               if (cp < 128 && cp > 32 && !style.isObfuscated()) {
                  if (shadow) {
                     glyph(canvas, cp, cursor[0] + 1, y + 1, scale, color & -16777216 | (color & 16579836) >> 2, style);
                  }

                  glyph(canvas, cp, cursor[0], y, scale, color, style);
               }

               if (style.isStrikethrough()) {
                  canvas.rect(cursor[0] * scale, (y + 4) * scale, advance * scale, scale, color);
               }

               if (style.isUnderlined()) {
                  canvas.rect(cursor[0] * scale, (y + 8) * scale, advance * scale, scale, color);
               }
            }

            return Optional.empty();
         }, Style.EMPTY);
      }
   }

   private static int advance(int cp, Style style) {
      return Minecraft.getInstance().font.width(FormattedText.of(new String(Character.toChars(cp)), style));
   }

   private static void glyph(Canvas canvas, int cp, int x, int y, int scale, int argb, Style style) {
      int cellX = cp % 16 * 8;
      int cellY = cp / 16 * 8;
      int stride = 128;

      for(int row = 0; row < 8; ++row) {
         int shear = style.isItalic() && row < 4 ? 1 : 0;
         int col = 0;

         while(col < 8) {
            if (!glyphs[(cellY + row) * stride + cellX + col]) {
               ++col;
            } else {
               int start;
               for(start = col; col < 8 && glyphs[(cellY + row) * stride + cellX + col]; ++col) {
               }

               canvas.rect((x + start + shear) * scale, (y + row) * scale, (col - start) * scale, scale, argb);
               if (style.isBold()) {
                  canvas.rect((x + start + shear + 1) * scale, (y + row) * scale, (col - start) * scale, scale, argb);
               }
            }
         }
      }

   }

   static record Image(int[] pixels, int width, int height) {
   }

   static record Slice(Image image, int border, boolean stretch) {
   }
}
