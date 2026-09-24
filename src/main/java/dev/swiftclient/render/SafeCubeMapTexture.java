package dev.swiftclient.render;

import dev.swiftclient.core.log.Log;
import org.slf4j.Logger;
import net.minecraft.client.renderer.texture.CubeMapTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public class SafeCubeMapTexture extends CubeMapTexture {
   private static final Logger LOG = Log.get("Theme");
   private volatile boolean ok = true;

   public SafeCubeMapTexture(Identifier location) {
      super(location);
   }

   public boolean loadedOk() {
      return this.ok;
   }

   public TextureContents loadContents(ResourceManager resourceManager) {
      try {
         this.ok = true;
         return super.loadContents(resourceManager);
      } catch (Throwable var3) {
         this.ok = false;
         LOG.warn("Panorama illisible : {}", var3.toString());
         return TextureContents.createMissing();
      }
   }
}
