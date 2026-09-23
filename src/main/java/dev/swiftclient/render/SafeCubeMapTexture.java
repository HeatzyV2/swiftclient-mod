package dev.swiftclient.render;

import net.minecraft.client.renderer.texture.CubeMapTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public class SafeCubeMapTexture extends CubeMapTexture {
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
         System.out.println("[SwiftClient] panorama illisible : " + var3);
         return TextureContents.createMissing();
      }
   }
}
