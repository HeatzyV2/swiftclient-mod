package dev.swiftclient.testing;

import dev.swiftclient.core.gfx.Canvas;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Canvas that draws with Java2D so a screen can be rendered to a PNG without starting Minecraft.
 * Used by {@link UiPreviewTest} to look at layouts: fonts are approximated, geometry and colours are real.
 */
public final class PreviewCanvas implements Canvas {
   private static final Map<String, BufferedImage> ICONS = new HashMap<>();
   private final BufferedImage image;
   private final Graphics2D g;
   private final FontMetrics fm;
   private final Deque<AffineTransform> transforms = new ArrayDeque<>();
   private final Deque<Shape> clips = new ArrayDeque<>();

   public PreviewCanvas(int w, int h, int zoom) {
      this.image = new BufferedImage(w * zoom, h * zoom, BufferedImage.TYPE_INT_ARGB);
      this.g = this.image.createGraphics();
      this.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      this.g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      this.g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      this.g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      this.g.scale(zoom, zoom);
      this.g.setFont(new Font("Segoe UI", Font.PLAIN, 8).deriveFont(8.6F));
      this.fm = this.g.getFontMetrics();
      // A backdrop standing in for the game world
      this.g.setPaint(new GradientPaint(0, 0, new Color(0x2E4A33), 0, h, new Color(0x16241B)));
      this.g.fillRect(0, 0, w, h);
   }

   public BufferedImage image() {
      return this.image;
   }

   public void save(java.nio.file.Path file) throws IOException {
      java.nio.file.Files.createDirectories(file.getParent());
      ImageIO.write(this.image, "png", file.toFile());
   }

   private static Color color(int argb) {
      return new Color(argb, true);
   }

   private void paint(Shape s, int argb) {
      if ((argb >>> 24) != 0) {
         this.g.setColor(color(argb));
         this.g.fill(s);
      }
   }

   @Override
   public void fill(int x, int y, int x2, int y2, int argb) {
      this.paint(new Rectangle(x, y, x2 - x, y2 - y), argb);
   }

   @Override
   public void gradientV(int x, int y, int w, int h, int top, int bottom) {
      this.g.setPaint(new GradientPaint(x, y, color(top), x, y + h, color(bottom)));
      this.g.fillRect(x, y, w, h);
   }

   @Override
   public void roundRect(int x, int y, int w, int h, float radius, int argb) {
      this.paint(new RoundRectangle2D.Float(x, y, w, h, radius * 2, radius * 2), argb);
   }

   @Override
   public void card(int x, int y, int w, int h, int bg, int border, int thickness, float radius) {
      // Same recipe as the game: the border colour as a full shape, then the fill on top, inset by the thickness
      if (thickness > 0 && (border >>> 24) != 0) {
         this.roundRect(x, y, w, h, radius, border);
         this.roundRect(x + thickness, y + thickness, w - 2 * thickness, h - 2 * thickness, Math.max(0.0F, radius - thickness), bg);
      } else {
         this.roundRect(x, y, w, h, radius, bg);
      }
   }

   @Override
   public void text(String s, int x, int y, int argb, boolean shadow) {
      if (s == null || s.isEmpty()) {
         return;
      }

      if (shadow) {
         int sh = ((argb >> 16 & 0xFF) / 4) << 16 | ((argb >> 8 & 0xFF) / 4) << 8 | (argb & 0xFF) / 4 | argb & 0xFF000000;
         this.g.setColor(color(sh));
         this.g.drawString(s, x + 1, y + 8);
      }

      this.g.setColor(color(argb));
      this.g.drawString(s, x, y + 7);
   }

   @Override
   public void centeredText(String s, int cx, int y, int argb, boolean shadow) {
      this.text(s, cx - this.textWidth(s) / 2, y, argb, shadow);
   }

   @Override
   public int textWidth(String s) {
      return s == null ? 0 : (int)Math.ceil(this.fm.stringWidth(s) * 0.92);
   }

   @Override
   public int lineHeight() {
      return 9;
   }

   @Override
   public void pushScissor(int x, int y, int w, int h) {
      this.clips.push(this.g.getClip() == null ? new Rectangle(-100000, -100000, 200000, 200000) : this.g.getClip());
      this.g.clipRect(x, y, w, h);
   }

   @Override
   public void popScissor() {
      this.g.setClip(this.clips.pop());
   }

   @Override
   public void pushTranslate(int dx, int dy) {
      this.transforms.push(this.g.getTransform());
      this.g.translate(dx, dy);
   }

   @Override
   public void popTranslate() {
      this.g.setTransform(this.transforms.pop());
   }

   @Override
   public void pushScale(int ox, int oy, float scale) {
      this.transforms.push(this.g.getTransform());
      this.g.translate(ox, oy);
      this.g.scale(scale, scale);
   }

   @Override
   public void popScale() {
      this.g.setTransform(this.transforms.pop());
   }

   @Override
   public void playerHead(String uuid, int x, int y, int size) {
      this.g.setColor(new Color(0xB98B63));
      this.g.fillRect(x, y, size, size);
      this.g.setColor(new Color(0x3B2A1E));
      this.g.fillRect(x, y, size, size / 4);
   }

   @Override
   public void vanillaButton(int x, int y, int w, int h, boolean hovered) {
      this.card(x, y, w, h, hovered ? 0xFF6F6F6F : 0xFF4A4A4A, 0xFF000000, 1, 0.0F);
   }

   @Override
   public void playerModel(int x, int y, int w, int h, int mouseX, int mouseY, float delta) {
      // Stand-in for the 3D skin: a blocky figure fitted in the box, centred
      int fh = Math.min(h, (int)(w * 2.1F));
      int fw = fh / 2;
      int cx = x + w / 2;
      int top = y + (h - fh) / 2;
      this.g.setColor(new Color(0x5BA83A));
      this.g.fillRect(cx - fw / 4, top, fw / 2, fh / 4);
      this.g.setColor(new Color(0x2A2A2A));
      this.g.fillRect(cx - fw / 2, top + fh / 4, fw, fh * 3 / 8);
      this.g.setColor(new Color(0x3A2A8A));
      this.g.fillRect(cx - fw / 2, top + fh * 5 / 8, fw, fh * 3 / 8);
   }

   @Override
   public void icon(String name, int x, int y, int size, int argb) {
      BufferedImage img = ICONS.computeIfAbsent(name, n -> {
         try (InputStream in = PreviewCanvas.class.getResourceAsStream("/assets/swiftclient/textures/gui/icons/" + n + ".png")) {
            return in == null ? null : ImageIO.read(in);
         } catch (IOException e) {
            return null;
         }
      });
      if (img == null) {
         this.g.setColor(new Color(0xFF00FF));
         this.g.drawRect(x, y, size - 1, size - 1);
         return;
      }

      BufferedImage tinted = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
      int tr = argb >> 16 & 0xFF;
      int tg = argb >> 8 & 0xFF;
      int tb = argb & 0xFF;
      int ta = argb >>> 24;
      for (int j = 0; j < img.getHeight(); j++) {
         for (int i = 0; i < img.getWidth(); i++) {
            int p = img.getRGB(i, j);
            int a = (p >>> 24) * ta / 255;
            int r = (p >> 16 & 0xFF) * tr / 255;
            int gg = (p >> 8 & 0xFF) * tg / 255;
            int b = (p & 0xFF) * tb / 255;
            tinted.setRGB(i, j, a << 24 | r << 16 | gg << 8 | b);
         }
      }

      this.g.setComposite(AlphaComposite.SrcOver);
      this.g.drawImage(tinted, x, y, size, size, null);
   }

   @Override
   public void logo(int x, int y, int size) {
      this.g.setColor(new Color(0x3B82F6));
      this.g.fillRoundRect(x, y, size, size, size / 4, size / 4);
   }
}
