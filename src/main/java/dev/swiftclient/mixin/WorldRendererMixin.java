package dev.swiftclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.swiftclient.pet.PetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public abstract class WorldRendererMixin {
   @Inject(
      method = {"submitEntities"},
      at = {@At("TAIL")}
   )
   private void swiftclient$submitPet(PoseStack poseStack, LevelRenderState state, SubmitNodeCollector collector, CallbackInfo ci) {
      float tickProgress = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
      PetManager.INSTANCE.renderInWorld(poseStack, collector, state.cameraRenderState, tickProgress);
   }
}
