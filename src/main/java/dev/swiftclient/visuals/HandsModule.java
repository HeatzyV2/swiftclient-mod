package dev.swiftclient.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;

/** Moves, rotates and resizes your first-person hands, easing smoothly to the new pose. */
@ConsumedBy("ItemInHandAnimationsMixin")
public final class HandsModule extends Module {
   public static volatile HandsModule live;
   private static final float[] REST = new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};
   public final ModuleSetting link;
   private final ModuleSetting[] main;
   private final ModuleSetting[] off;
   public final ModuleSetting glide;
   private final HandState mainState = new HandState();
   private final HandState offState = new HandState();

   public HandsModule() {
      super("hand_pose", "Hand Pose", "Moves, rotates and resizes your first-person hands.", "Visual", "hand", false);
      this.link = this.toggle("link", "Link hands", false).desc("The offhand copies the main hand settings (mirrored).");
      this.glide = this.slider("glide", "Glide", 0.4, 0.0, 1.0, 0.05, "").desc("How smoothly the hands ease into a new pose. 0 = instantly.");
      this.main = this.axes("main", "Main hand");
      this.action("reset_main", "Reset main hand", () -> "Reset", () -> this.reset(this.main)).group("Main hand");
      this.off = this.axes("off", "Offhand");
      this.action("reset_off", "Reset offhand", () -> "Reset", () -> this.reset(this.off)).group("Offhand");
   }

   private ModuleSetting[] axes(String prefix, String group) {
      return new ModuleSetting[]{
         this.slider(prefix + "_x", "Offset X", REST[0], -1.0, 1.0, 0.01, "").group(group),
         this.slider(prefix + "_y", "Offset Y", REST[1], -1.0, 1.0, 0.01, "").group(group),
         this.slider(prefix + "_z", "Offset Z", REST[2], -1.0, 1.0, 0.01, "").group(group),
         this.slider(prefix + "_pitch", "Pitch", REST[3], -180.0, 180.0, 1.0, "°").group(group),
         this.slider(prefix + "_yaw", "Yaw", REST[4], -180.0, 180.0, 1.0, "°").group(group),
         this.slider(prefix + "_roll", "Roll", REST[5], -180.0, 180.0, 1.0, "°").group(group),
         this.slider(prefix + "_scale", "Scale", REST[6], 0.3, 2.0, 0.05, "x").group(group)
      };
   }

   @Override
   protected void onEnable() {
      this.mainState.stamp = 0L;
      this.offState.stamp = 0L;
      live = this;
   }

   private void reset(ModuleSetting[] axes) {
      for (int i = 0; i < axes.length; i++) {
         axes[i].setValue(REST[i]);
      }
   }

   public static void shape(PoseStack pose, boolean mainHand, int side) {
      HandsModule module = live;
      if (module != null) {
         module.shapeHand(pose, mainHand, side);
      }

   }

   private void shapeHand(PoseStack pose, boolean mainHand, int side) {
      boolean on = this.isEnabled();
      HandState state = mainHand ? this.mainState : this.offState;
      state.step(!mainHand && !this.link.boolValue() ? this.off : this.main, on, this.follow());
      if (!on && this.mainState.rested && this.offState.rested) {
         live = null;
      } else if (!state.rested) {
         float[] v = state.value;
         pose.translate((float)side * (0.56F + v[0]), -0.52F + v[1], -0.72F + v[2]);
         pose.mulPose(Axis.YP.rotationDegrees((float)side * v[4]));
         pose.mulPose(Axis.XP.rotationDegrees(v[3]));
         pose.mulPose(Axis.ZP.rotationDegrees((float)side * v[5]));
         pose.scale(v[6], v[6], v[6]);
         pose.translate((float)(-side) * 0.56F, 0.52F, 0.72F);
      }
   }

   private float follow() {
      float g = (float)this.glide.value();
      return g <= 0.0F ? -1.0F : 30.0F + -27.0F * g;
   }

   private static final class HandState {
      final float[] value;
      long stamp;
      boolean rested;

      private HandState() {
         this.value = (float[])HandsModule.REST.clone();
         this.rested = true;
      }

      void step(ModuleSetting[] target, boolean on, float rate) {
         long now = System.nanoTime();
         float dt = this.stamp == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.stamp) / 1.0E9F);
         this.stamp = now;
         float k = rate < 0.0F ? 1.0F : 1.0F - (float)Math.exp((double)(-rate * dt));
         boolean settled = true;

         for(int i = 0; i < this.value.length; ++i) {
            float goal = on ? (float)target[i].value() : HandsModule.REST[i];
            float next = this.value[i] + (goal - this.value[i]) * k;
            if (Math.abs(goal - next) < 0.001F) {
               next = goal;
            }

            this.value[i] = next;
            if (next != HandsModule.REST[i]) {
               settled = false;
            }
         }

         this.rested = settled;
      }
   }
}
