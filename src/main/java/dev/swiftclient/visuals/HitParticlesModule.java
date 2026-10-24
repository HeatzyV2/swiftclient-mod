package dev.swiftclient.visuals;

import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.util.RandomSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

/** Particles burst where your hits land (crit, flame, totem, hearts, coloured dust...). Only on your screen. */
@ConsumedBy("MultiPlayerGameModeMixin")
public final class HitParticlesModule extends Module {
   public static volatile HitParticlesModule active;
   private static final String TYPE_CRIT = "Crit";
   private static final String TYPE_MAGIC_CRIT = "Magic Crit";
   private static final String TYPE_SPARK = "Spark";
   private static final String TYPE_FLAME = "Flame";
   private static final String TYPE_SOUL_FLAME = "Soul Flame";
   private static final String TYPE_END_ROD = "End Rod";
   private static final String TYPE_TOTEM = "Totem";
   private static final String TYPE_HEART = "Heart";
   private static final String TYPE_DUST = "Dust";
   private static final String ORIGIN_IMPACT = "Impact";
   private static final String ORIGIN_BODY = "Body";
   private static final String TARGETS_LIVING = "Living";
   private static final String TARGETS_PLAYERS = "Players";
   private static final String TARGETS_ALL = "All";
   private static final String COLOR_CUSTOM = "Custom";
   private static final String COLOR_ACCENT = "Accent";
   private static final double LIFT = (double)0.25F;
   private final ModuleSetting type = this.cycle("type", "Type", new String[]{"Crit", "Magic Crit", "Spark", "Flame", "Soul Flame", "End Rod", "Totem", "Heart", "Dust"}, 0).desc("The particles that burst on a hit.");
   private final ModuleSetting amount = this.slider("amount", "Amount", 10.0, 1.0, 40.0, 1.0, "").desc("Particles per hit.");
   private final ModuleSetting spread = this.slider("spread", "Spread", 1.0, 0.25, 3.0, 0.25, "").desc("How wide the burst is.");
   private final ModuleSetting origin = this.cycle("origin", "Origin", new String[]{"Impact", "Body"}, 0).desc("Impact: where your hit lands. Body: around the whole target.");
   private final ModuleSetting onlyCrits = this.toggle("only_crits", "Only crits", false).desc("Only on critical hits.");
   private final ModuleSetting targets = this.cycle("targets", "Targets", new String[]{"Living", "Players", "All"}, 0).desc("Which hits make particles: living mobs, players only, or anything.");
   private final ModuleSetting dustColorMode = this.cycle("dust_color_mode", "Dust color", new String[]{"Custom", "Accent"}, 1).desc("Colour of the Dust particles: Swift accent or your own.");
   private final ModuleSetting dustColor = this.color("dust_color", "Custom color", -657929).desc("Colour of the Dust particles (Custom).");
   private final ModuleSetting dustSize = this.slider("dust_size", "Dust size", 1.0, 0.5, 2.0, 0.1, "").desc("Size of the Dust particles.");
   private final RandomSource random = RandomSource.create();

   public HitParticlesModule() {
      super("hit_particles", "Hit Particles", "Particles burst where your hits land. Only on your screen.", "Visual", "combo", false);
   }

   @Override

   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   /** You attacked {@code target} (MultiPlayerGameModeMixin). */
   public static void onAttack(Entity target) {
      HitParticlesModule module = active;
      if (module != null) {
         module.attacked(target);
      }
   }

   private void attacked(Entity eventTarget) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.isSameThread()) {
         LocalPlayer player = mc.player;
         ClientLevel level = mc.level;
         Entity target = eventTarget;
         if (player != null && level != null && target != null && target != player) {
            if (target.isAttackable() && this.accepts(target)) {
               if (!this.onlyCrits.boolValue() || isCritical(player, target)) {
                  this.spawnBurst(mc, level, player, target);
               }
            }
         }
      }
   }

   private boolean accepts(Entity target) {
      boolean var10000;
      switch (this.targets.label()) {
         case "Players" -> var10000 = target instanceof Player;
         case "All" -> var10000 = true;
         default -> var10000 = target instanceof LivingEntity;
      }

      return var10000;
   }

   private static boolean isCritical(LocalPlayer player, Entity target) {
      return player.getAttackStrengthScale(0.5F) > 0.9F && player.fallDistance > (double)0.0F && !player.onGround() && !player.onClimbable() && !player.isInWater() && /*? if >=1.21.9 {*/!player.isMobilityRestricted()/*?} else {*//*!player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)*//*?}*/ && !player.isPassenger() && !player.isSprinting() && target instanceof LivingEntity;
   }

   private void spawnBurst(Minecraft mc, ClientLevel level, LocalPlayer player, Entity target) {
      Burst burst = this.burst();
      AABB box = target.getBoundingBox();
      Vec3 center = box.getCenter();
      boolean body = this.origin.is("Body");
      Vec3 impact = body ? null : impactPoint(mc, player, target, box);
      Vec3 fallbackOut = player.getLookAngle().reverse();
      double spreadScale = (double)this.spread.floatValue();
      double jitter = burst.jitter() * spreadScale;
      int count = this.amount.intValue();

      for(int i = 0; i < count; ++i) {
         Vec3 from = body ? this.randomPointIn(box) : impact;
         Vec3 out = from.subtract(center);
         out = out.lengthSqr() < 1.0E-6 ? fallbackOut : out.normalize();
         Vec3 dir = out.scale(0.8).add(this.randomUnit()).add((double)0.0F, (double)0.25F, (double)0.0F).normalize();
         double speed = burst.speed() * spreadScale * (0.6 + 0.4 * this.random.nextDouble());
         level.addParticle(burst.particle(), true, false, from.x + this.random.nextGaussian() * jitter, from.y + this.random.nextGaussian() * jitter, from.z + this.random.nextGaussian() * jitter, dir.x * speed, dir.y * speed, dir.z * speed);
      }

   }

   private Burst burst() {
      Burst var10000;
      switch (this.type.label()) {
         case "Magic Crit" -> var10000 = new Burst(ParticleTypes.ENCHANTED_HIT, 0.8, 0.1);
         case "Spark" -> var10000 = new Burst(ParticleTypes.ELECTRIC_SPARK, 0.16, 0.1);
         case "Flame" -> var10000 = new Burst(ParticleTypes.FLAME, 0.035, 0.12);
         case "Soul Flame" -> var10000 = new Burst(ParticleTypes.SOUL_FIRE_FLAME, 0.035, 0.12);
         case "End Rod" -> var10000 = new Burst(ParticleTypes.END_ROD, 0.09, 0.1);
         case "Totem" -> var10000 = new Burst(ParticleTypes.TOTEM_OF_UNDYING, 0.45, 0.1);
         case "Heart" -> var10000 = new Burst(ParticleTypes.HEART, (double)0.0F, 0.35);
         case "Dust" -> var10000 = new Burst(new DustParticleOptions(this.dustRgb(), this.dustSize.floatValue()), (double)1.0F, 0.3);
         default -> var10000 = new Burst(ParticleTypes.CRIT, 0.8, 0.1);
      }

      return var10000;
   }

   private int dustRgb() {
      int argb = this.dustColorMode.is("Accent") ? Px.ACCENT : this.dustColor.colorValue();
      return argb & 16777215;
   }

   private static Vec3 impactPoint(Minecraft mc, LocalPlayer player, Entity target, AABB box) {
      HitResult var5 = mc.hitResult;
      if (var5 instanceof EntityHitResult hit) {
         if (hit.getEntity() == target) {
            return hit.getLocation();
         }
      }

      Vec3 eye = player.getEyePosition();
      return new Vec3(Mth.clamp(eye.x, box.minX, box.maxX), Mth.clamp(eye.y, box.minY, box.maxY), Mth.clamp(eye.z, box.minZ, box.maxZ));
   }

   private Vec3 randomPointIn(AABB box) {
      return new Vec3(box.minX + box.getXsize() * (0.1 + 0.8 * this.random.nextDouble()), box.minY + box.getYsize() * (0.1 + 0.8 * this.random.nextDouble()), box.minZ + box.getZsize() * (0.1 + 0.8 * this.random.nextDouble()));
   }

   private Vec3 randomUnit() {
      Vec3 v = new Vec3(this.random.nextGaussian(), this.random.nextGaussian(), this.random.nextGaussian());
      return v.lengthSqr() < 1.0E-9 ? new Vec3((double)0.0F, (double)1.0F, (double)0.0F) : v.normalize();
   }

   private static record Burst(ParticleOptions particle, double speed, double jitter) {
   }
}
