package dev.swiftclient.pet;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public class GeckoPet implements GeoAnimatable {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final String petId;
   private final RawAnimation loopAnim;
   public Vec3 pos = Vec3.ZERO;
   public Vec3 prevPos = Vec3.ZERO;
   public float yaw = 0.0F;
   public float prevYaw = 0.0F;
   public Identifier skin;
   private int age = 0;

   public GeckoPet(String petId, String loopAnimName) {
      this.petId = petId;
      this.loopAnim = RawAnimation.begin().thenLoop(loopAnimName);
   }

   public String getPetId() {
      return this.petId;
   }

   public void tick(AbstractClientPlayer owner) {
      this.prevPos = this.pos;
      this.prevYaw = this.yaw;
      this.age++;
      float bodyYaw = owner.yBodyRot;
      double yawRad = Math.toRadians(bodyYaw);
      double rx = Math.cos(yawRad);
      double rz = Math.sin(yawRad);
      double fx = -Math.sin(yawRad);
      double fz = Math.cos(yawRad);
      Vec3 ownerPos = owner.position();
      double hover = Math.sin(this.age * 0.08) * 0.15;
      Vec3 target = new Vec3(ownerPos.x - rx * 0.9 - fx * 0.3, ownerPos.y + 0.4 + hover, ownerPos.z - rz * 0.9 - fz * 0.3);
      this.pos = this.pos.lerp(target, 0.18);
      Vec3 delta = this.pos.subtract(this.prevPos);
      float targetYaw = delta.lengthSqr() > 0.004 ? (float)Math.toDegrees(Math.atan2(-delta.x, delta.z)) : bodyYaw;
      float yawDiff = wrapDegrees(targetYaw - this.yaw);
      this.yaw = wrapDegrees(this.yaw + yawDiff * 0.2F);
   }

   private static float wrapDegrees(float d) {
      d %= 360.0F;
      if (d >= 180.0F) {
         d -= 360.0F;
      }

      if (d < -180.0F) {
         d += 360.0F;
      }

      return d;
   }

   public void resetTo(Vec3 p) {
      this.pos = p;
      this.prevPos = p;
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("main", 5, state -> state.setAndContinue(this.loopAnim)));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
