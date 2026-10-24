package dev.swiftclient.visuals;

import dev.swiftclient.compat.Mc;
import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.util.ARGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Recolours the enchantment glint (items and armour) and changes its speed and pattern size. */
@ConsumedBy("GlintTextureTransformMixin")
public final class CustomGlintModule extends Module {
   private static final Logger LOGGER = LoggerFactory.getLogger("SwiftClient");
   public static volatile CustomGlintModule active;
   private static final String COLOR_CUSTOM = "Custom";
   private static final String COLOR_ACCENT = "Accent";
   private final ModuleSetting colorMode = this.cycle("color_mode", "Color", new String[]{"Custom", "Accent"}, 0).desc("Custom uses the colours below, Accent the Swift accent colour.");
   private final ModuleSetting itemColor = this.color("item_color", "Item color", -9524994).desc("Glint colour of enchanted items.");
   private final ModuleSetting separateArmor = this.toggle("separate_armor", "Separate armor color", false).desc("Give enchanted armour its own colour.");
   private final ModuleSetting armorColor = this.color("armor_color", "Armor color", -3697942).desc("Glint colour of enchanted armour.");
   private final ModuleSetting intensity = this.slider("intensity", "Intensity", 1.0, 0.05, 1.0, 0.05, "").desc("How bright the glint is.");
   private final ModuleSetting speed = this.slider("speed", "Speed", 1.0, 0.1, 4.0, 0.1, "").desc("How fast the glint moves. 1 = vanilla.");
   private final ModuleSetting patternSize = this.slider("pattern_size", "Pattern size", 1.0, 0.25, 4.0, 0.25, "").desc("Size of the glint pattern. 1 = vanilla.");
   private final GlintTexture items;
   private final GlintTexture armor;
   private double glintClock;
   private long glintClockReal;

   public CustomGlintModule() {
      super("custom_glint", "Custom Glint", "Recolours the enchantment glint of items and armour, and changes its speed and pattern size.", "Visual", "sparkles", false);
      this.items = new GlintTexture(ItemFeatureRenderer.ENCHANTED_GLINT_ITEM);
      this.armor = new GlintTexture(ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR);
      this.glintClock = Double.NaN;
   }

   @Override

   protected void onEnable() {
      this.glintClock = Double.NaN;
      active = this;
   }

   @Override
   protected void onTick() {
      this.refresh();
   }

   @Override

   protected void onDisable() {
      active = null;
      Minecraft mc = Minecraft.getInstance();
      if (mc.isSameThread()) {
         this.restore();
      } else {
         mc.execute(() -> {
            if (active == null) {
               this.restore();
            }

         });
      }

   }

   public long glintMillis(long realMillis) {
      if (Double.isNaN(this.glintClock)) {
         this.glintClock = (double)realMillis;
      } else {
         this.glintClock += (double)Math.max(0L, realMillis - this.glintClockReal) * (double)this.speed.floatValue();
      }

      this.glintClockReal = realMillis;
      return (long)this.glintClock;
   }

   public float uvScaleMultiplier() {
      return 1.0F / this.patternSize.floatValue();
   }

   private void refresh() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && Mc.overlay() == null && RenderSystem.tryGetDevice() != null) {
         int itemRgb = this.colorMode.is("Accent") ? Px.ACCENT & 16777215 : (this.itemColor.colorValue() & 0xFFFFFF);
         int armorRgb = this.colorMode.is("Custom") && this.separateArmor.boolValue() ? (this.armorColor.colorValue() & 0xFFFFFF) : itemRgb;
         float strength = this.intensity.floatValue();
         this.items.tint(mc, itemRgb, strength);
         this.armor.tint(mc, armorRgb, strength);
      }
   }

   private void restore() {
      Minecraft mc = Minecraft.getInstance();
      this.items.restore(mc);
      this.armor.restore(mc);
   }

   private static final class GlintTexture {
      private final Identifier id;
      private int width;
      private int height;
      private int[] original;
      private int[] pattern;
      private GpuTexture written;
      private long writtenKey;
      private GpuTexture unusable;

      GlintTexture(Identifier id) {
         this.id = id;
      }

      void tint(Minecraft mc, int rgb, float intensity) {
         GpuTexture gpu = this.gpuTexture(mc);
         if (gpu != null && !gpu.isClosed() && gpu != this.unusable) {
            long key = (long)(rgb & 16777215) << 8 | (long)Math.round(intensity * 255.0F);
            if (gpu != this.written || key != this.writtenKey) {
               if (gpu != this.written) {
                  this.written = null;
                  if ((gpu.usage() & 1) == 0 || !this.readShipped(mc, gpu)) {
                     this.unusable = gpu;
                     return;
                  }
               }

               if (!this.upload(gpu, this.tinted(rgb, intensity))) {
                  this.unusable = gpu;
               } else {
                  this.written = gpu;
                  this.writtenKey = key;
               }
            }
         }
      }

      void restore(Minecraft mc) {
         GpuTexture gpu = this.written != null ? this.gpuTexture(mc) : null;
         if (gpu != null && gpu == this.written && !gpu.isClosed() && this.original != null) {
            this.upload(gpu, this.original);
         }

         this.written = null;
         this.unusable = null;
         this.original = null;
         this.pattern = null;
      }

      private GpuTexture gpuTexture(Minecraft mc) {
         try {
            AbstractTexture texture = mc.getTextureManager().getTexture(this.id);
            return texture == null ? null : texture.getTexture();
         } catch (IllegalStateException var3) {
            return null;
         }
      }

      private boolean readShipped(Minecraft mc, GpuTexture gpu) {
         Optional<Resource> resource = mc.getResourceManager().getResource(this.id);
         if (resource.isEmpty()) {
            return false;
         }

         try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            int w = image.getWidth();
            int h = image.getHeight();
            if (w != gpu.getWidth(0) || h != gpu.getHeight(0)) {
               return false;
            }

            int[] pixels = new int[w * h];
            int[] luma = new int[w * h];
            int i = 0;
            for (int y = 0; y < h; y++) {
               for (int x = 0; x < w; x++, i++) {
                  int argb = image.getPixel(x, y);
                  pixels[i] = argb;
                  luma[i] = Math.max(ARGB.red(argb), Math.max(ARGB.green(argb), ARGB.blue(argb)));
               }
            }

            this.width = w;
            this.height = h;
            this.original = pixels;
            this.pattern = luma;
            return true;
         } catch (RuntimeException | IOException e) {
            CustomGlintModule.LOGGER.warn("Custom Glint could not read {}", this.id, e);
            return false;
         }
      }

      private int[] tinted(int rgb, float intensity) {
         float r = (float)ARGB.red(rgb) * intensity / 255.0F;
         float g = (float)ARGB.green(rgb) * intensity / 255.0F;
         float b = (float)ARGB.blue(rgb) * intensity / 255.0F;
         int[] out = new int[this.pattern.length];

         for(int i = 0; i < out.length; ++i) {
            int l = this.pattern[i];
            out[i] = ARGB.color(ARGB.alpha(this.original[i]), Math.round((float)l * r), Math.round((float)l * g), Math.round((float)l * b));
         }

         return out;
      }

      private boolean upload(GpuTexture gpu, int[] argb) {
         try (NativeImage image = new NativeImage(this.width, this.height, false)) {
            int i = 0;
            for (int y = 0; y < this.height; y++) {
               for (int x = 0; x < this.width; x++, i++) {
                  image.setPixel(x, y, argb[i]);
               }
            }

            RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpu, image);
            return true;
         } catch (RuntimeException e) {
            CustomGlintModule.LOGGER.warn("Custom Glint could not write {}", this.id, e);
            return false;
         }
      }
   }
}
