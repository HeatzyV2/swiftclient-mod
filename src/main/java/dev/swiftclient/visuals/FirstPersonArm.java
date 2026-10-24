package dev.swiftclient.visuals;

import dev.swiftclient.mixin.ItemLayerRenderStateAccessor;
import dev.swiftclient.mixin.ItemStackRenderStateAccessor;
import java.util.function.Consumer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.math.Axis;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Draws your own arm holding an item in first person: the grip follows the held model. */
final class FirstPersonArm {
   static final Vector3f FOREARM = (new Vector3f(0.75F, -0.45F, 0.5F)).normalize();
   static final float ELBOW = 0.6F;
   private static final Vector3f ARM_UP = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final float ARM_FIST = 0.5F;
   private static final float ARM_SHOULDER = -0.625F;
   private static final float SHOULDER_STRETCH = 3.0F;
   private static final float HANDLE_INSET = 0.1875F;
   private static final float BLOCK_BELOW = 0.17F;
   private static final Vector3f BLOCK_SHIFT = new Vector3f(0.09F, 0.0F, 0.06F);
   private static final ItemStackRenderState MODEL = new ItemStackRenderState();

   private FirstPersonArm() {
   }

   static Arm arm() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && !player.isInvisible()) {
         EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
         AvatarRenderer<AbstractClientPlayer> renderer = (AvatarRenderer<AbstractClientPlayer>)(Object)dispatcher.getRenderer(player);
         PlayerSkin skin = player.getSkin();
         return new Arm(renderer, skin.body().texturePath(), player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE), skin.model() == PlayerModelType.SLIM);
      } else {
         return null;
      }
   }

   static void hold(PoseStack pose, Arm arm, ItemStack stack, ItemDisplayContext context, LivingEntity holder, SubmitNodeCollector collector, int light, float dx, float dy, float dz) {
      Vector3f grip = grip(stack, context, holder);
      if (grip != null) {
         Vector3f elbow = (new Vector3f(grip)).fma(0.6F, FOREARM);
         grip.add(dx, dy, dz);
         boolean left = context.leftHand();
         pose.pushPose();
         if (left) {
            pose.scale(-1.0F, 1.0F, 1.0F);
         }

         place(pose, grip, elbow, arm.slim(), (posed) -> arm.draw(posed, left, collector, light));
         pose.popPose();
      }
   }

   static Vector3f grip(ItemStack stack, ItemDisplayContext context, LivingEntity holder) {
      if (!stack.isEmpty() && !stack.is(Items.BOW) && !stack.is(Items.CROSSBOW) && !stack.is(Items.SHIELD) && !stack.is(Items.TRIDENT) && !stack.is(Items.SPYGLASS)) {
         Minecraft.getInstance().getItemModelResolver().updateForTopItem(MODEL, stack, context, holder.level(), holder, holder.getId() + context.ordinal());
         ItemStackRenderStateAccessor state = (ItemStackRenderStateAccessor)MODEL;
         if (state.swiftclient$activeLayerCount() <= 0) {
            return null;
         } else {
            ItemLayerRenderStateAccessor layer = (ItemLayerRenderStateAccessor)state.swiftclient$layers()[0];
            ItemTransform transform = layer.swiftclient$itemTransform();
            PoseStack.Pose display = new PoseStack.Pose();
            if (transform != null) {
               transform.apply(context.leftHand(), display);
            }

            Matrix4f m = display.pose();
            if (MODEL.usesBlockLight()) {
               if (!(stack.getItem() instanceof BlockItem)) {
                  return null;
               } else {
                  Vector3f center = m.transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
                  if (context.leftHand()) {
                     center.x = -center.x;
                  }

                  return blockGrip(center, transform != null ? transform.scale().y() : 1.0F);
               }
            } else {
               float[] handle = handle(layer.swiftclient$extents() != null ? (Vector3fc[])layer.swiftclient$extents().get() : null);
               Vector3f grip = m.transformPosition(handle[0] - 0.5F, handle[1] - 0.5F, 0.0F, new Vector3f());
               if (context.leftHand()) {
                  grip.x = -grip.x;
               }

               return grip;
            }
         }
      } else {
         return null;
      }
   }

   static Vector3f blockGrip(Vector3f center, float scale) {
      return (new Vector3f(center)).add(BLOCK_SHIFT).sub(0.0F, 0.5F * scale + 0.17F, 0.0F);
   }

   static float[] handle(Vector3fc[] extents) {
      float lowest = Float.MAX_VALUE;
      if (extents != null) {
         for(Vector3fc p : extents) {
            if (!squareCorner(p)) {
               lowest = Math.min(lowest, p.x() + p.y());
            }
         }
      }

      if (lowest == Float.MAX_VALUE) {
         return new float[]{0.1875F, 0.1875F};
      } else {
         float x = 0.0F;
         float y = 0.0F;
         int tied = 0;

         for(Vector3fc p : extents) {
            if (!squareCorner(p) && !(p.x() + p.y() > lowest + 1.0E-4F)) {
               x += p.x();
               y += p.y();
               ++tied;
            }
         }

         return new float[]{x / (float)tied + 0.1875F, y / (float)tied + 0.1875F};
      }
   }

   private static boolean squareCorner(Vector3fc p) {
      return (p.x() < 1.0E-4F || p.x() > 0.9999F) && (p.y() < 1.0E-4F || p.y() > 0.9999F);
   }

   static void place(PoseStack pose, Vector3f hand, Vector3f elbow, boolean slim, Consumer<PoseStack> draw) {
      Vector3f along = (new Vector3f(hand)).sub(elbow).normalize();
      Vector3f front = (new Vector3f(ARM_UP)).fma(-ARM_UP.dot(along), along).normalize();
      Vector3f side = (new Vector3f(along)).cross(front);
      pose.pushPose();
      pose.translate(hand.x, hand.y, hand.z);
      pose.mulPose((new Quaternionf()).setFromNormalized(new Matrix3f(side, along, front)));
      drawPart(pose, slim, draw);
      pose.translate(0.0F, -0.625F, 0.0F);
      pose.scale(-1.0F, -3.0F, 1.0F);
      pose.translate(0.0F, 0.625F, 0.0F);
      drawPart(pose, slim, draw);
      pose.popPose();
   }

   private static void drawPart(PoseStack pose, boolean slim, Consumer<PoseStack> draw) {
      pose.pushPose();
      pose.translate(slim ? 0.03125F : 0.0625F, -0.5F, 0.0F);
      pose.mulPose(Axis.ZP.rotation(-0.1F));
      pose.translate(0.3125F, -0.125F, 0.0F);
      draw.accept(pose);
      pose.popPose();
   }

   static void shoulder(PoseStack pose, boolean right, boolean slim) {
      float pivotX = right ? -0.3125F : 0.3125F;
      float pivotY = 0.125F;
      float tilt = right ? 0.1F : -0.1F;
      float middle = (right ? -1.0F : 1.0F) * (slim ? 0.5F : 1.0F) / 16.0F;
      float top = -0.125F;
      pose.translate(pivotX, pivotY, 0.0F);
      pose.mulPose(Axis.ZP.rotation(tilt));
      pose.translate(middle, top, 0.0F);
      pose.scale(-1.0F, -3.0F, 1.0F);
      pose.translate(-middle, -top, 0.0F);
      pose.mulPose(Axis.ZP.rotation(-tilt));
      pose.translate(-pivotX, -pivotY, 0.0F);
   }

   static record Arm(AvatarRenderer<AbstractClientPlayer> renderer, Identifier texture, boolean rightSleeve, boolean leftSleeve, boolean slim) {
      void draw(PoseStack pose, boolean left, SubmitNodeCollector collector, int light) {
         if (left) {
            pose.scale(-1.0F, 1.0F, 1.0F);
            this.renderer.renderLeftHand(pose, collector, light, this.texture, this.leftSleeve);
         } else {
            this.renderer.renderRightHand(pose, collector, light, this.texture, this.rightSleeve);
         }

      }
   }
}
