package dev.swiftclient.modules;

import com.mojang.blaze3d.platform.NativeImage;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.mixin.OverlayTextureAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;

/** Colour of the flash on entities when they take damage (vanilla: translucent red). */
public final class HitColorModule extends Module {
   /** Vanilla hurt tint, ARGB. */
   private static final int VANILLA = 0xB2FF0000;
   public final ModuleSetting color;
   private int applied = VANILLA;

   public HitColorModule() {
      super("hit_color", "Hit Color", "Changes the colour entities flash when they take damage.", "PvP", "hitcolor", false);
      this.color = this.color("color", "Hit color", 0x9955AAFF).desc("Flash colour. The alpha sets how strong the flash is.");
   }

   @Override
   protected void onTick() {
      int want = this.color.colorValue();
      if (want != this.applied) {
         paint(want);
         this.applied = want;
      }
   }

   @Override
   protected void onDisable() {
      if (this.applied != VANILLA) {
         paint(VANILLA);
         this.applied = VANILLA;
      }
   }

   /** Rows 0-7 of the 16x16 overlay texture hold the hurt colour (see OverlayTexture). */
   private static void paint(int argb) {
      DynamicTexture tex = ((OverlayTextureAccessor)Minecraft.getInstance().gameRenderer.overlayTexture()).swiftclient$texture();
      NativeImage px = tex.getPixels();
      if (px != null) {
         for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) {
               px.setPixel(x, y, argb);
            }
         }

         tex.upload();
      }
   }
}
