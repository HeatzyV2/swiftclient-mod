package dev.swiftclient.ui;

import dev.swiftclient.compat.GuiPose;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import dev.swiftclient.core.platform.Tr;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
//? if >=1.21.11
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Swift loading screen: the S logo filling up with the progress inside a ring of slowly turning rays, the name
 * and a thin bar. Shown while resources load (game start, F3+T, resource packs) and while entering or saving a
 * world. The first resource load runs before the game's resources and fonts exist, so every texture is read
 * straight from the jar and text is drawn from glyph atlases (tools/gen_loading_assets.py).
 */
public final class SwiftLoadingScreen {
   public static final int BACKGROUND = 0x070B14;
   private static final int TOP = 0x0B1424;
   private static final int BOTTOM = 0x03050A;
   private static final int ACCENT = 0x3B82F6;
   private static final int ACCENT_LIGHT = 0x93C5FD;
   private static final int MUTED = 0x7C8799;
   private static final Identifier LOGO = id("logo");
   private static final Identifier GLOW = id("glow");
   private static final Identifier RING = id("ring");
   /** Rows of logo.png the S covers: the fill runs from its bottom to its top. */
   private static final float S_TOP = 65.0F / 256.0F;
   private static final float S_BOTTOM = 191.0F / 256.0F;
   private static final int RAYS = 48;
   private static final float FADE_OUT_SECONDS = 0.45F;
   private static final long ORIGIN = System.nanoTime();
   private static Glyphs title;
   private static Glyphs text;
   private static float shownFill;
   private static long lastFrame = -1L;
   private static long lastActiveAt = -1L;
   private static float lastAlpha;
   private static float lastProgress;
   private static String lastStatus = "";

   private SwiftLoadingScreen() {
   }

   private static Identifier id(String name) {
      return Identifier.fromNamespaceAndPath("swiftclient", "loading/" + name);
   }

   /** Registers the textures with LoadingOverlay's own, so they exist for the very first resource load. */
   public static void registerTextures(TextureManager textures) {
      textures.registerAndLoad(LOGO, new JarTexture(LOGO, "textures/gui/logo.png"));
      textures.registerAndLoad(GLOW, new JarTexture(GLOW, "textures/gui/loading/glow.png"));
      textures.registerAndLoad(RING, new JarTexture(RING, "textures/gui/loading/ring.png"));
      Identifier titleId = id("title");
      Identifier textId = id("text");
      textures.registerAndLoad(titleId, new JarTexture(titleId, "textures/gui/loading/title.png"));
      textures.registerAndLoad(textId, new JarTexture(textId, "textures/gui/loading/text.png"));
      title = Glyphs.load(titleId, "title");
      text = Glyphs.load(textId, "text");
   }

   public static String loadingLabel() {
      return Tr.orDefault("swift.loading.resources", "Loading");
   }

   public static String worldLabel() {
      return Tr.orDefault("swift.loading.world", "Entering world");
   }

   /** Draws the screen and remembers it, so it can fade out once the loading is over (see {@link #fadeOut}). */
   public static void draw(GuiGraphicsExtractor g, float alpha, float progress, String status) {
      lastActiveAt = System.nanoTime();
      lastAlpha = alpha;
      lastProgress = progress;
      lastStatus = status;
      render(g, alpha, progress, status);
   }

   /** After a world screen closes, the last frame fades out over the next screen instead of cutting. */
   public static void fadeOut(GuiGraphicsExtractor g) {
      if (lastActiveAt >= 0L) {
         float elapsed = (System.nanoTime() - lastActiveAt) / 1.0E9F;
         if (elapsed >= FADE_OUT_SECONDS) {
            lastActiveAt = -1L;
         } else {
            render(g, lastAlpha * (1.0F - elapsed / FADE_OUT_SECONDS), lastProgress < 0.0F ? -1.0F : 1.0F, lastStatus);
         }
      }
   }

   /** Draws the screen without remembering it: the resource overlay fades itself out. */
   public static void overlay(GuiGraphicsExtractor g, float alpha, float progress, String status) {
      render(g, alpha, progress, status);
   }

   /** Forgets the last frame: the overlay fades itself out. */
   public static void forget() {
      lastActiveAt = -1L;
   }

   private static void render(GuiGraphicsExtractor g, float a, float progress, String status) {
      if (a <= 0.004F) {
         return;
      }

      long now = System.nanoTime();
      float t = (now - ORIGIN) / 1.0E9F;
      float dt = lastFrame < 0L ? 0.016F : Math.min(0.1F, Math.max(0.001F, (now - lastFrame) / 1.0E9F));
      lastFrame = now;
      int w = g.guiWidth();
      int h = g.guiHeight();
      float u = Math.min(h / 270.0F, w / 300.0F);
      float cx = w * 0.5F;
      float cy = h * 0.42F;

      g.fillGradient(0, 0, w, h, col(TOP, a), col(BOTTOM, a));
      image(g, GLOW, cx, cy, 340.0F * u, 340.0F * u, 256, 256, col(ACCENT, 0.07F * a));
      float breath = 0.5F + 0.5F * (float)Math.sin(t * 1.6F);
      image(g, GLOW, cx, cy, 150.0F * u, 150.0F * u, 256, 256, col(ACCENT, (0.16F + 0.06F * breath) * a));

      float ringR = 45.0F * u;
      rays(g, cx, cy, ringR, u, t, a);
      image(g, RING, cx, cy, ringR * 2.0F, ringR * 2.0F, 512, 512, col(ACCENT, 0.55F * a));
      image(g, RING, cx, cy, ringR * 1.86F, ringR * 1.86F, 512, 512, col(ACCENT, 0.22F * a));
      float orbit = t * 0.9F;
      float ox = cx + (float)Math.cos(orbit) * ringR;
      float oy = cy + (float)Math.sin(orbit) * ringR;
      image(g, GLOW, ox, oy, 14.0F * u, 14.0F * u, 256, 256, col(ACCENT_LIGHT, 0.85F * a));
      image(g, GLOW, ox, oy, 4.0F * u, 4.0F * u, 256, 256, col(0xFFFFFF, a));

      float target = progress >= 0.0F ? clamp01(progress) : t * 0.45F % 1.0F;
      if (progress >= 0.0F && target >= shownFill) {
         shownFill += (target - shownFill) * Math.min(1.0F, dt * 6.0F);
      } else {
         shownFill = target;
      }

      logo(g, cx, cy, 80.0F * u * (1.0F + 0.012F * breath), shownFill, a);

      float nameY = cy + ringR + 22.0F * u;
      float capH = 5.0F * u;
      float spacing = 1.6F * u;
      String swift = "SWIFT";
      String client = " CLIENT";
      float nameW = title.width(swift + client, capH, spacing);
      float x = cx - nameW * 0.5F;
      x = title.draw(g, swift, x, nameY, capH, spacing, col(ACCENT, a));
      title.draw(g, client, x, nameY, capH, spacing, col(0xFFFFFF, a));

      float barW = 66.0F * u;
      float barY = nameY + capH + 7.0F * u;
      float line = Math.max(0.5F, 0.5F * u);
      float bx = cx - barW * 0.5F;
      rect(g, bx, barY, barW, line, col(ACCENT, 0.18F * a));
      float end = progress >= 0.0F ? bx + barW * shownFill : bx + barW * (0.5F + 0.5F * (float)Math.sin(t * 2.2F));
      float start = progress >= 0.0F ? bx : Math.max(bx, end - barW * 0.25F);
      rect(g, start, barY, end - start, line, col(ACCENT, a));
      image(g, GLOW, end, barY + line * 0.5F, 9.0F * u, 9.0F * u, 256, 256, col(ACCENT, 0.5F * a));
      diamond(g, end, barY + line * 0.5F, 1.4F * u, col(ACCENT_LIGHT, a));

      String label = progress >= 0.0F ? status + "  ·  " + Math.round(clamp01(progress) * 100.0F) + "%" : status;
      label = label.toUpperCase(Locale.ROOT);
      float smallCap = 2.6F * u;
      float smallSpacing = 0.9F * u;
      text.draw(g, label, cx - text.width(label, smallCap, smallSpacing) * 0.5F, barY + 5.0F * u, smallCap, smallSpacing, col(MUTED, a));
   }

   /** The S: a dim silhouette, and the part already loaded in full colour, rising from the bottom. */
   private static void logo(GuiGraphicsExtractor g, float cx, float cy, float size, float fill, float a) {
      image(g, LOGO, cx, cy, size, size, 256, 256, col(0x8FA3C4, 0.16F * a));
      float edge = S_BOTTOM - (S_BOTTOM - S_TOP) * fill;
      int v = Math.round(edge * 256.0F);
      int rows = 256 - v;
      if (rows > 0) {
         GuiPose p = GuiPose.of(g);
         p.pushMatrix();
         p.translate(cx - size * 0.5F, cy - size * 0.5F);
         p.scale(size / 256.0F, size / 256.0F);
         g.blit(RenderPipelines.GUI_TEXTURED, LOGO, 0, v, 0.0F, v, 256, rows, 256, rows, 256, 256, col(0xFFFFFF, a));
         p.popMatrix();
      }

      if (fill > 0.0F && fill < 0.999F) {
         float y = cy - size * 0.5F + size * edge;
         image(g, GLOW, cx, y, size * 0.6F, size * 0.16F, 256, 256, col(ACCENT_LIGHT, 0.35F * a));
      }
   }

   /** Thin rays around the ring, long and short in turn, turning slowly. */
   private static void rays(GuiGraphicsExtractor g, float cx, float cy, float ringR, float u, float t, float a) {
      GuiPose p = GuiPose.of(g);
      float thick = Math.max(0.35F, 0.35F * u);
      for (int i = 0; i < RAYS; i++) {
         boolean big = i % 2 == 0;
         boolean major = i % 6 == 0;
         float inner = ringR + (big ? 5.0F : 7.0F) * u;
         float len = (major ? 16.0F : big ? 11.0F : 5.0F) * u;
         float shimmer = 0.75F + 0.25F * (float)Math.sin(t * 1.3F + i * 0.7F);
         p.pushMatrix();
         p.translate(cx, cy);
         p.rotate((float)(Math.PI * 2.0 * i / RAYS) + t * 0.12F);
         rect(g, inner, -thick * 0.5F, len, thick, col(ACCENT, (big ? 0.42F : 0.22F) * shimmer * a));
         p.popMatrix();
      }
   }

   private static void diamond(GuiGraphicsExtractor g, float x, float y, float r, int argb) {
      GuiPose p = GuiPose.of(g);
      p.pushMatrix();
      p.translate(x, y);
      p.rotate((float)(Math.PI / 4.0));
      rect(g, -r * 0.7071F, -r * 0.7071F, r * 1.4142F, r * 1.4142F, argb);
      p.popMatrix();
   }

   /** A rectangle with float position and size (fill only takes ints: draw a unit square, scaled). */
   private static void rect(GuiGraphicsExtractor g, float x, float y, float w, float h, int argb) {
      if (w <= 0.0F || h <= 0.0F) {
         return;
      }

      GuiPose p = GuiPose.of(g);
      p.pushMatrix();
      p.translate(x, y);
      p.scale(w / 64.0F, h / 64.0F);
      g.fill(0, 0, 64, 64, argb);
      p.popMatrix();
   }

   private static void image(GuiGraphicsExtractor g, Identifier tex, float cx, float cy, float w, float h, int texW, int texH, int argb) {
      GuiPose p = GuiPose.of(g);
      p.pushMatrix();
      p.translate(cx - w * 0.5F, cy - h * 0.5F);
      p.scale(w / texW, h / texH);
      g.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 0.0F, 0.0F, texW, texH, texW, texH, argb);
      p.popMatrix();
   }

   private static int col(int rgb, float alpha) {
      return Math.round(clamp01(alpha) * 255.0F) << 24 | rgb & 0xFFFFFF;
   }

   private static float clamp01(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }

   /** A texture read from the jar, smoothed when scaled. */
   private static final class JarTexture extends ReloadableTexture {
      private final String path;

      JarTexture(Identifier id, String path) {
         super(id);
         this.path = path;
      }

      @Override
      public TextureContents loadContents(ResourceManager resources) throws IOException {
         try (InputStream in = SwiftLoadingScreen.class.getResourceAsStream("/assets/swiftclient/" + this.path)) {
            if (in == null) {
               throw new IOException("Missing " + this.path);
            }

            return new TextureContents(NativeImage.read(in), new TextureMetadataSection(true, true/*? if >=1.21.11 {*/, MipmapStrategy.MEAN, 0.0F/*?}*/));
         }
      }
   }

   /** Upper-case text from a glyph atlas: one cell per character, advances from the font. */
   private static final class Glyphs {
      private final Identifier texture;
      private final String chars;
      private final int cols;
      private final int cellW;
      private final int cellH;
      private final int capTop;
      private final int capHeight;
      private final int texW;
      private final int texH;
      private final float[] advance;

      private Glyphs(Identifier texture, JsonObject meta) {
         this.texture = texture;
         this.chars = meta.get("chars").getAsString();
         this.cols = meta.get("cols").getAsInt();
         this.cellW = meta.get("cellW").getAsInt();
         this.cellH = meta.get("cellH").getAsInt();
         this.capTop = meta.get("capTop").getAsInt();
         this.capHeight = meta.get("capHeight").getAsInt();
         this.texW = meta.get("width").getAsInt();
         this.texH = meta.get("height").getAsInt();
         JsonArray adv = meta.getAsJsonArray("advance");
         this.advance = new float[adv.size()];
         for (int i = 0; i < this.advance.length; i++) {
            this.advance[i] = adv.get(i).getAsFloat();
         }
      }

      static Glyphs load(Identifier texture, String name) {
         try (InputStream in = SwiftLoadingScreen.class.getResourceAsStream("/assets/swiftclient/textures/gui/loading/" + name + ".json")) {
            return new Glyphs(texture, JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
         } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Loading screen glyphs " + name, e);
         }
      }

      private int index(char c) {
         int i = this.chars.indexOf(c);
         return i >= 0 ? i : 0;
      }

      float width(String s, float capH, float spacing) {
         float k = capH / this.capHeight;
         float w = 0.0F;
         for (int i = 0; i < s.length(); i++) {
            w += this.advance[this.index(s.charAt(i))] * k + (i + 1 < s.length() ? spacing : 0.0F);
         }

         return w;
      }

      /** Draws {@code s} with its capitals from {@code top} to {@code top + capH}; returns the pen position after it. */
      float draw(GuiGraphicsExtractor g, String s, float x, float top, float capH, float spacing, int argb) {
         float k = capH / this.capHeight;
         GuiPose p = GuiPose.of(g);
         for (int i = 0; i < s.length(); i++) {
            int idx = this.index(s.charAt(i));
            if (idx > 0) {
               p.pushMatrix();
               p.translate(x - 2.0F * k, top - this.capTop * k);
               p.scale(k, k);
               g.blit(RenderPipelines.GUI_TEXTURED, this.texture, 0, 0, (float)(idx % this.cols * this.cellW), (float)(idx / this.cols * this.cellH),
                  this.cellW, this.cellH, this.cellW, this.cellH, this.texW, this.texH, argb);
               p.popMatrix();
            }

            x += this.advance[idx] * k + spacing;
         }

         return x;
      }
   }
}
