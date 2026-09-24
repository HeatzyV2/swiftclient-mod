package dev.swiftclient.mixin;

import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.theme.SwiftPanorama;
import dev.swiftclient.core.theme.ThemeManager;
import dev.swiftclient.render.SafeCubeMapTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GuiRenderer.class})
public abstract class GuiRendererMixin {
   @Shadow
   @Final
   @Mutable
   private CubeMap cubeMap;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void swiftclient$bind(CallbackInfo ci) {
      SwiftPanorama.bind(this::swiftclient$swap);
   }

   @Inject(
      method = {"registerPanoramaTextures"},
      at = {@At("TAIL")}
   )
   private void swiftclient$applyThemeAtLoad(TextureManager tm, CallbackInfo ci) {
      try {
         String loc = ThemeManager.currentPanorama();
         if (loc != null && !loc.isBlank()) {
            this.swiftclient$swap(loc);
         }
      } catch (Throwable var4) {
      }
   }

   private boolean swiftclient$swap(String location) {
      try {
         Identifier id = swiftclient$id(location);
         Minecraft mc = Minecraft.getInstance();
         SafeCubeMapTexture tex = new SafeCubeMapTexture(id);
         tex.apply(tex.loadContents(mc.getResourceManager()));
         if (!tex.loadedOk()) {
            return false;
         } else {
            mc.getTextureManager().register(id, tex);
            this.cubeMap = new CubeMap(id);
            return true;
         }
      } catch (Exception var5) {
         Log.get("Theme").warn("Panorama {} non applique", location, var5);
         return false;
      }
   }

   private static Identifier swiftclient$id(String s) {
      int c = s.indexOf(58);
      return c < 0 ? Identifier.withDefaultNamespace(s) : Identifier.fromNamespaceAndPath(s.substring(0, c), s.substring(c + 1));
   }
}
