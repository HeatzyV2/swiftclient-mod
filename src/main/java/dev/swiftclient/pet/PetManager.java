package dev.swiftclient.pet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.swiftclient.core.cosmetics.PetState;
import dev.swiftclient.core.pet.DynamicPets;
import dev.swiftclient.core.pet.PetRegistry;
import dev.swiftclient.ui.SkinRenderer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

public class PetManager {
   public static final PetManager INSTANCE = new PetManager();
   private final Map<UUID, GeckoPet> pets = new HashMap<>();
   private final Map<String, GeckoPetRenderer> renderers = new HashMap<>();
   private boolean loggedRender = false;

   public void init() {
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         if (!client.isPaused()) {
            this.syncAndTick(client);
         }
      });
   }

   private void syncAndTick(Minecraft client) {
      if (client.level == null) {
         this.pets.clear();
      } else {
         Set<UUID> seen = new HashSet<>();

         for (AbstractClientPlayer player : client.level.players()) {
            UUID uuid = player.getUUID();
            String rawId = PetState.petFor(uuid, player.getGameProfile().name());
            if (rawId != null) {
               DynamicPets.ensure(rawId);
               if (PetRegistry.exists(rawId)) {
                  PetRegistry.PetDef def = PetRegistry.get(rawId);
                  String petId = def.id;
                  seen.add(uuid);
                  GeckoPet pet = this.pets.get(uuid);
                  if (pet == null || !pet.getPetId().equals(petId)) {
                     pet = new GeckoPet(def.id, def.loopAnim);
                     pet.resetTo(player.position().add(0.0, 1.0, 0.0));
                     this.pets.put(uuid, pet);
                     System.out.println("[SwiftClient/Pet] +pet " + petId + " pour " + uuid);
                  }

                  pet.tick(player);
                  if (PetRegistry.usesPlayerSkin(petId)) {
                     pet.skin = SkinRenderer.skinIdOf(player);
                  }
               }
            }
         }

         this.pets.keySet().retainAll(seen);
      }
   }

   public void renderInWorld(PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState, float tickProgress) {
      if (!this.pets.isEmpty() && cameraState != null) {
         if (!this.loggedRender) {
            this.loggedRender = true;
            System.out.println("[SwiftClient/Pet] rendu de " + this.pets.size() + " pet(s)");
         }

         Vec3 camPos = cameraState.pos;

         for (GeckoPet pet : this.pets.values()) {
            Vec3 lerped = pet.prevPos.lerp(pet.pos, tickProgress);
            float yawDiff = pet.yaw - pet.prevYaw;
            yawDiff %= 360.0F;
            if (yawDiff >= 180.0F) {
               yawDiff -= 360.0F;
            }

            if (yawDiff < -180.0F) {
               yawDiff += 360.0F;
            }

            float lerpedYaw = pet.prevYaw + yawDiff * tickProgress;
            GeckoPetRenderer renderer = this.renderers.computeIfAbsent(pet.getPetId(), GeckoPetRenderer::new);
            if (PetRegistry.usesPlayerSkin(pet.getPetId()) && pet.skin != null) {
               renderer.petModel.skinOverride = pet.skin;
            }

            matrices.pushPose();
            matrices.translate(lerped.x - camPos.x, lerped.y - camPos.y, lerped.z - camPos.z);
            matrices.mulPose(Axis.YP.rotationDegrees(-lerpedYaw + 180.0F));
            matrices.scale(0.4F, 0.4F, 0.4F);
            renderer.performRenderPass(pet, pet, matrices, queue, cameraState, 15728880, tickProgress);
            matrices.popPose();
         }
      }
   }

   public boolean hasPet() {
      return !this.pets.isEmpty();
   }
}
