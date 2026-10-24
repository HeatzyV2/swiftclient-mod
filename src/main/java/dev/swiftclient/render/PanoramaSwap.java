package dev.swiftclient.render;

import dev.swiftclient.core.log.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Swift panorama: the textures of a panorama are registered under the vanilla panorama's id, so the vanilla
 * cube map (wherever the version keeps it) draws ours. Bound to SwiftPanorama at startup.
 */
public final class PanoramaSwap {
   private static final Identifier VANILLA = Identifier.withDefaultNamespace("textures/gui/title/background/panorama");

   private PanoramaSwap() {
   }

   public static boolean swap(String location) {
      try {
         Identifier id = id(location);
         Minecraft mc = Minecraft.getInstance();
         //? if >=1.21.6 {
         SafeCubeMapTexture tex = new SafeCubeMapTexture(id);
         var contents = tex.loadContents(mc.getResourceManager());
         if (!tex.loadedOk()) {
            // Never hand the 16x2 "missing" image to a cube map: it must be square.
            return false;
         }

         tex.apply(contents);
         mc.getTextureManager().register(VANILLA, tex);
         //?} else {
         /*/^* Before 1.21.6 the cube map draws six separate textures, panorama_0.png to panorama_5.png. ^/
         for (int i = 0; i < 6; i++) {
            Identifier side = Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_" + i + ".png");
            if (mc.getResourceManager().getResource(side).isEmpty()) {
               return false;
            }
         }

         for (int i = 0; i < 6; i++) {
            Identifier side = Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_" + i + ".png");
            mc.getTextureManager().register(VANILLA.withSuffix("_" + i + ".png"), new net.minecraft.client.renderer.texture.SimpleTexture(side));
         }
         *///?}
         Log.get("Theme").info("Panorama {} applique", location);
         return true;
      } catch (Exception e) {
         Log.get("Theme").warn("Panorama {} non applique", location, e);
         return false;
      }
   }

   private static Identifier id(String s) {
      int c = s.indexOf(58);
      return c < 0 ? Identifier.withDefaultNamespace(s) : Identifier.fromNamespaceAndPath(s.substring(0, c), s.substring(c + 1));
   }
}
