package dev.swiftclient.pet;

import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.GeoRenderState;

public class GeckoPetRenderer extends GeoObjectRenderer<GeckoPet, GeckoPet, GeoRenderState> {
   public final GeckoPetModel petModel;

   public GeckoPetRenderer(String petId) {
      this(new GeckoPetModel(petId));
   }

   private GeckoPetRenderer(GeckoPetModel model) {
      super(model);
      this.petModel = model;
   }
}
