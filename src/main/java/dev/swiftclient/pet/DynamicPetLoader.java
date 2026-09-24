package dev.swiftclient.pet;

import dev.swiftclient.core.log.Log;
import org.slf4j.Logger;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.loading.math.MathParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import dev.swiftclient.core.pet.DynamicPets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class DynamicPetLoader implements DynamicPets.Sink {
   private static final Logger LOG = Log.get("Pet");
   @Override
   public void inject(String petIdLower, String geoJson, String animJson, byte[] texturePng) {
      GeckoLibGsonLoader loader = new GeckoLibGsonLoader();
      Identifier id = Identifier.fromNamespaceAndPath("swiftclient", "pet/" + petIdLower);
      JsonObject geoObj = JsonParser.parseString(geoJson).getAsJsonObject();
      BakedGeoModel baked = loader.bakeGeckoLibModelFile(id, geoObj);
      DynamicPetAssets.MODELS.put(petIdLower, baked);
      if (animJson != null && !animJson.isEmpty()) {
         JsonObject animObj = JsonParser.parseString(animJson).getAsJsonObject();
         BakedAnimations anims = loader.bakeGeckoLibAnimationsFile(id, animObj, MathParser.createWithDeduplication());
         DynamicPetAssets.ANIMS.put(petIdLower, anims);
      }

      if (texturePng != null && texturePng.length > 0) {
         NativeImage img;
         try {
            img = NativeImage.read(texturePng);
         } catch (Exception e) {
            LOG.warn("Texture de familier illisible pour {}", petIdLower, e);
            return;
         }

         Minecraft mc = Minecraft.getInstance();
         mc.execute(() -> {
            Identifier texId = Identifier.fromNamespaceAndPath("swiftclient", "textures/dynpet/" + petIdLower + ".png");
            mc.getTextureManager().register(texId, new DynamicTexture(() -> "swiftclient-dynpet", img));
            DynamicPetAssets.TEXTURES.put(petIdLower, texId);
         });
      }
   }
}
