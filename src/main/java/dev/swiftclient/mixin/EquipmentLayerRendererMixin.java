package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.swiftclient.modules.OldVisualsModule;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 1.7 Visuals: armour layers get the red hurt overlay of their wearer, like in 1.8. */
@Mixin({EquipmentLayerRenderer.class})
public abstract class EquipmentLayerRendererMixin {
   @ModifyArg(
      method = {"renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
      ),
      index = 5,
      require = 0
   )
   private int swiftclient$redArmor(int overlay, @Local(argsOnly = true) Object state) {
      if (state instanceof LivingEntityRenderState living && living.hasRedOverlay && OldVisualsModule.on(false)) {
         return OverlayTexture.pack(0, true);
      } else {
         return overlay;
      }
   }
}
