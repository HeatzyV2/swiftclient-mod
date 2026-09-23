package dev.swiftclient.pet;

import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

public class GeckoPetModel extends GeoModel<GeckoPet> {
   private final String petId;
   public Identifier skinOverride;

   public GeckoPetModel(String petId) {
      this.petId = petId;
   }

   public Identifier getModelResource(GeoRenderState renderState) {
      return Identifier.fromNamespaceAndPath("swiftclient", "pet/" + this.petId);
   }

   public Identifier getTextureResource(GeoRenderState renderState) {
      if (this.skinOverride != null) {
         return this.skinOverride;
      } else {
         Identifier dyn = DynamicPetAssets.TEXTURES.get(this.petId);
         return dyn != null ? dyn : Identifier.fromNamespaceAndPath("swiftclient", "textures/entity/pet/" + this.petId + ".png");
      }
   }

   public Identifier getAnimationResource(GeckoPet pet) {
      return Identifier.fromNamespaceAndPath("swiftclient", "pet/" + this.petId);
   }

   public BakedGeoModel getBakedModel(Identifier location) {
      BakedGeoModel dyn = DynamicPetAssets.MODELS.get(this.petId);
      return dyn != null ? dyn : super.getBakedModel(location);
   }

   public Animation getBakedAnimation(GeckoPet animatable, String name) {
      BakedAnimations dyn = DynamicPetAssets.ANIMS.get(this.petId);
      if (dyn != null) {
         Animation a = dyn.getAnimation(name);
         if (a != null) {
            return a;
         }
      }

      return super.getBakedAnimation(animatable, name);
   }
}
