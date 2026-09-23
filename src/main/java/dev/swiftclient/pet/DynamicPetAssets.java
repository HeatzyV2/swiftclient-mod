package dev.swiftclient.pet;

import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.cache.model.BakedGeoModel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

public final class DynamicPetAssets {
   static final Map<String, BakedGeoModel> MODELS = new ConcurrentHashMap<>();
   static final Map<String, BakedAnimations> ANIMS = new ConcurrentHashMap<>();
   static final Map<String, Identifier> TEXTURES = new ConcurrentHashMap<>();

   private DynamicPetAssets() {
   }

   public static boolean has(String petIdLower) {
      return MODELS.containsKey(petIdLower);
   }
}
