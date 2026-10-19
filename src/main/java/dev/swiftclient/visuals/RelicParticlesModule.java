package dev.swiftclient.visuals;

import dev.swiftclient.core.ui.Px;
import net.minecraft.world.phys.Vec3;
import dev.swiftclient.world.WorldOverlays;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import java.util.Objects;
import java.util.function.Consumer;
import dev.swiftclient.visuals.glyph.GlyphCanvas;
import dev.swiftclient.visuals.glyph.GlyphSwarm;
import dev.swiftclient.visuals.glyph.GlyphTint;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

public final class RelicParticlesModule extends Module implements WorldOverlays.WorldOverlay {
   public static volatile RelicParticlesModule active;
   private static final String GLYPH_ANKH = "Ankh";
   private static final String GLYPH_SCARAB = "Scarab";
   private static final String GLYPH_SAND = "Sand";
   private static final String GLYPH_SPARK = "Spark";
   private static final String GLYPH_MIXED = "Mixed";
   private static final int CAPACITY;
   private static final float BASE_SIZE = 0.085F;
   private static final double PROJECTILE_RANGE_SQR = (double)6400.0F;
   private static final double RESTING_SQR = 9.0E-4;
   private static final double STEP_MIN_SPEED = 0.07;
   private static final int PEARL = 0;
   private static final int ARROW = 1;
   private static final int TRIDENT = 2;
   private static final int NONE = -1;
   private static final byte[] MIXED_ORDER;
   private final ModuleSetting pops = this.toggle("pops", "Totem pops", true).desc("A burst of glyphs when a player pops a totem.");
   private final ModuleSetting popGlyph = this.cycle("pop_glyph", "Pop glyph", new String[]{"Mixed", "Ankh", "Scarab", "Sand", "Spark"}, 0).desc("Which glyph the totem burst uses.");
   private final ModuleSetting popBurst = this.slider("pop_burst", "Pop burst", 26.0, 4.0, 80.0, 1.0, "").desc("How many glyphs a totem pop releases.");
   private final ModuleSetting ownPops = this.toggle("own_pops", "Your own pops", true).desc("Also burst when you pop a totem yourself.");
   private final ModuleSetting projectiles = this.toggle("projectiles", "Projectile trails", true).desc("Glyphs trail behind flying projectiles.");
   private final ModuleSetting pearls = this.toggle("pearls", "Ender pearls", true).desc("Trail behind ender pearls.");
   private final ModuleSetting arrows = this.toggle("arrows", "Arrows", true).desc("Trail behind arrows.");
   private final ModuleSetting tridents = this.toggle("tridents", "Tridents", true).desc("Trail behind tridents.");
   private final ModuleSetting trailGlyph = this.cycle("trail_glyph", "Trail glyph", new String[]{"Spark", "Sand", "Ankh", "Scarab", "Mixed"}, 0).desc("Which glyph the trails use.");
   private final ModuleSetting trailDensity = this.slider("trail_density", "Trail density", 2.0, 1.0, 6.0, 1.0, "").desc("How many glyphs a trail leaves.");
   private final ModuleSetting footsteps = this.toggle("footsteps", "Footsteps", false).desc("Glyphs where you walk.");
   private final ModuleSetting stepGlyph = this.cycle("step_glyph", "Step glyph", new String[]{"Sand", "Spark", "Ankh", "Scarab", "Mixed"}, 0).desc("Which glyph the footsteps use.");
   private final ModuleSetting stepDensity = this.slider("step_density", "Step density", 2.0, 1.0, 6.0, 1.0, "").desc("How many glyphs each step leaves.");
   private final ModuleSetting size = this.slider("size", "Size", 1.0, 0.3, 2.5, 0.05, "").desc("Size of the glyphs.");
   private final ModuleSetting lifetime = this.slider("lifetime", "Lifetime", 1.4, 0.3, 4.0, 0.1, "").desc("How long a glyph stays, in seconds.");
   private final ModuleSetting limit;
   private final ModuleSetting halo;
   private final ModuleSetting colorMode;
   private final ModuleSetting mainColor;
   private final ModuleSetting secondColor;
   private final GlyphSwarm swarm;
   private final GlyphCanvas canvas;
   private final GlyphTint palette;
   private final RandomSource random;
   private final Consumer<LivingEntity> popSink;
   private ClientLevel boundLevel;
   private long lastFrame;

   public RelicParticlesModule() {
      super("relic_particles", "Relic Particles", "Glowing glyph particles for totem pops, projectiles and footsteps. Only on your screen.", "Visual", "sparkles", false);
      this.limit = this.slider("limit", "Limit", 240.0, 40.0, CAPACITY, 10.0, "").desc("Most glyphs on screen at once. Lower it if your FPS drops.");
      this.halo = this.toggle("halo", "Halo", true).desc("A soft glow around each glyph.");
      this.colorMode = this.cycle("color_mode", "Color", new String[]{"Theme", "Manual"}, 0).desc("Theme uses the Swift accent colour, Manual the colours below.");
      this.mainColor = this.color("main_color", "Main color", -12548362).desc("Main colour of the glyphs (Manual).");
      this.secondColor = this.color("second_color", "Second color", -14992226).desc("Second colour, mixed in (Manual).");
      this.swarm = new GlyphSwarm(CAPACITY);
      this.canvas = new GlyphCanvas();
      this.palette = new GlyphTint();
      this.random = RandomSource.create();
      this.popSink = this::burst;
   }

   @Override

   protected void onEnable() {
      active = this;
      this.reset();
   }

   @Override

   protected void onDisable() {
      active = null;
      this.reset();
   }

   private void reset() {
      this.swarm.clear();
      this.boundLevel = null;
      this.lastFrame = 0L;
   }

   private boolean bind(ClientLevel level) {
      if (level == null) {
         if (this.boundLevel != null) {
            this.reset();
         }

         return false;
      } else {
         if (level != this.boundLevel) {
            this.swarm.clear();
            this.boundLevel = level;
         }

         return true;
      }
   }

   /** A living entity popped a totem (TotemPopMixin). */
   public static void onTotemPop(LivingEntity entity) {
      RelicParticlesModule module = active;
      if (module != null && module.pops.boolValue()) {
         module.popSink.accept(entity);
      }
   }

   @Override

   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (this.bind(mc.level) && player != null) {
         this.refreshPalette();
         if (this.projectiles.boolValue()) {
            this.trails(mc.level, player);
         }

         if (this.footsteps.boolValue()) {
            this.steps(player);
         }

      }
   }

   @Override

   public void emitGizmos(Minecraft overlayMc, Vec3 overlayCamera, float overlayPartialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (this.bind(mc.level)) {
         long now = System.nanoTime();
         float dt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1.0E9F);
         this.lastFrame = now;
         if (mc.isPaused()) {
            dt = 0.0F;
         }

         this.swarm.trim(this.particleLimit());
         this.swarm.step(dt);
         if (this.swarm.count() != 0) {
            this.canvas.begin(mc.gameRenderer.mainCamera());
            this.swarm.draw(this.canvas, 1.0F, this.halo.boolValue(), false ? null : null);
            this.canvas.submit(false);
         }
      }
   }

   private void burst(LivingEntity entity) {
      Minecraft mc = Minecraft.getInstance();
      if (this.bind(mc.level) && entity.level() == mc.level) {
         if (entity != mc.player || this.ownPops.boolValue()) {
            this.refreshPalette();
            AABB box = entity.getBoundingBox();
            int total = this.popBurst.intValue();
            String mode = this.popGlyph.label();

            for(int i = 0; i < total; ++i) {
               byte kind = pick(mode, i);
               double px = box.minX + box.getXsize() * this.random.nextDouble();
               double py = box.minY + box.getYsize() * (0.15 + 0.8 * this.random.nextDouble());
               double pz = box.minZ + box.getZsize() * this.random.nextDouble();
               float heading = this.random.nextFloat() * ((float)Math.PI * 2F);
               float out = outward(kind) * (0.45F + this.random.nextFloat());
               float up = upward(kind) * (0.55F + 0.8F * this.random.nextFloat());
               this.emit(kind, px, py, pz, Mth.cos((double)heading) * out, up, Mth.sin((double)heading) * out, 1.0F, 1.0F, this.colorFor(kind, -1));
            }

         }
      }
   }

   private void trails(ClientLevel level, LocalPlayer player) {
      int density = this.trailDensity.intValue();
      String mode = this.trailGlyph.label();

      for(Entity entity : level.entitiesForRendering()) {
         int role = this.role(entity);
         if (role != -1) {
            double dx = entity.getX() - entity.xo;
            double dy = entity.getY() - entity.yo;
            double dz = entity.getZ() - entity.zo;
            if (!(dx * dx + dy * dy + dz * dz < 9.0E-4) && !(entity.distanceToSqr(player) > (double)6400.0F)) {
               double mid = (double)entity.getBbHeight() * (double)0.5F;

               for(int i = 0; i < density; ++i) {
                  float f = ((float)i + this.random.nextFloat()) / (float)density;
                  byte kind = pick(mode, entity.tickCount * density + i);
                  this.emit(kind, entity.xo + dx * (double)f + (double)this.jitter(0.06F), entity.yo + dy * (double)f + mid + (double)this.jitter(0.06F), entity.zo + dz * (double)f + (double)this.jitter(0.06F), this.jitter(0.3F), this.jitter(0.3F) + 0.15F, this.jitter(0.3F), 0.55F, 0.8F, this.colorFor(kind, role));
               }
            }
         }
      }

   }

   private void steps(LocalPlayer player) {
      if (player.onGround() && !player.isSpectator() && !player.isPassenger() && !player.isInLiquid()) {
         double dx = player.getX() - player.xo;
         double dz = player.getZ() - player.zo;
         double speed = Math.sqrt(dx * dx + dz * dz);
         if (!(speed < 0.07)) {
            int count = this.stepDensity.intValue() + (player.isSprinting() ? 1 : 0);
            String mode = this.stepGlyph.label();

            for(int i = 0; i < count; ++i) {
               byte kind = pick(mode, player.tickCount * count + i);
               double back = 0.2 + 0.3 * this.random.nextDouble();
               this.emit(kind, player.getX() - dx / speed * back + (double)this.jitter(0.22F), player.getY() + 0.04, player.getZ() - dz / speed * back + (double)this.jitter(0.22F), (float)(-dx * (double)7.0F) + this.jitter(0.5F), 0.8F + this.random.nextFloat() * 1.1F, (float)(-dz * (double)7.0F) + this.jitter(0.5F), 0.7F, 0.75F, this.colorFor(kind, -1));
            }

         }
      }
   }

   private void emit(byte kind, double x, double y, double z, float vx, float vy, float vz, float lifeScale, float sizeScale, int rgb) {
      float life = this.lifetime.floatValue() * lifeScale * (0.75F + 0.5F * this.random.nextFloat());
      float scale = 0.085F * this.size.floatValue() * sizeScale * sizeOf(kind) * (0.8F + 0.4F * this.random.nextFloat());
      float turn = spinOf(kind) * (this.random.nextBoolean() ? 1.0F : -1.0F) * (0.5F + this.random.nextFloat());
      float angle = kind == 0 ? this.jitter(0.35F) : this.random.nextFloat() * ((float)Math.PI * 2F);
      this.swarm.spawn(kind, x, y, z, vx, vy, vz, life, scale, angle, turn, this.random.nextFloat(), rgb, this.particleLimit());
   }

   private int particleLimit() {
      return this.limit.intValue();
   }

   private void refreshPalette() {
      this.palette.resolve(this.colorMode.label(), Px.ACCENT, this.mainColor.colorValue(), this.secondColor.colorValue());
   }

   private int role(Entity entity) {
      if (entity.isRemoved()) {
         return -1;
      } else if (entity instanceof ThrownEnderpearl) {
         return this.pearls.boolValue() ? 0 : -1;
      } else if (entity instanceof ThrownTrident) {
         return this.tridents.boolValue() ? 2 : -1;
      } else if (entity instanceof AbstractArrow) {
         return this.arrows.boolValue() ? 1 : -1;
      } else {
         return -1;
      }
   }

   private static byte pick(String mode, int index) {
      byte var10000;
      switch (mode) {
         case "Ankh" -> var10000 = 0;
         case "Scarab" -> var10000 = 1;
         case "Sand" -> var10000 = 2;
         case "Spark" -> var10000 = 3;
         default -> var10000 = MIXED_ORDER[Math.floorMod(index, MIXED_ORDER.length)];
      }

      return var10000;
   }

   private int colorFor(byte kind, int role) {
      float r = this.random.nextFloat();
      int var10000;
      switch (kind) {
         case 0 -> var10000 = r < 0.6F ? this.palette.primary() : this.palette.highlight();
         case 1 -> var10000 = r < 0.55F ? this.palette.secondary() : this.palette.companion();
         case 2 -> var10000 = this.dust(r);
         default -> var10000 = GlyphTint.mix(this.palette.highlight(), -1, r * 0.35F);
      }

      int base = var10000;
      switch (role) {
         case 0 -> var10000 = GlyphTint.mix(base, this.palette.companion(), 0.55F);
         case 2 -> var10000 = GlyphTint.mix(base, this.palette.secondary(), 0.45F);
         default -> var10000 = base;
      }

      return var10000;
   }

   private int dust(float r) {
      int dust = this.palette.dust();
      if (r < 0.4F) {
         return dust;
      } else {
         return r < 0.75F ? GlyphTint.lift(dust, 0.4F) : GlyphTint.mix(dust, this.palette.secondary(), 0.3F);
      }
   }

   private static float sizeOf(byte kind) {
      float var10000;
      switch (kind) {
         case 0 -> var10000 = 1.35F;
         case 1 -> var10000 = 1.15F;
         case 2 -> var10000 = 0.5F;
         default -> var10000 = 0.9F;
      }

      return var10000;
   }

   private static float spinOf(byte kind) {
      float var10000;
      switch (kind) {
         case 0 -> var10000 = 0.5F;
         case 1 -> var10000 = 1.1F;
         case 2 -> var10000 = 5.0F;
         default -> var10000 = 2.8F;
      }

      return var10000;
   }

   private static float outward(byte kind) {
      float var10000;
      switch (kind) {
         case 0 -> var10000 = 0.8F;
         case 1 -> var10000 = 2.1F;
         case 2 -> var10000 = 1.5F;
         default -> var10000 = 3.4F;
      }

      return var10000;
   }

   private static float upward(byte kind) {
      float var10000;
      switch (kind) {
         case 0 -> var10000 = 1.1F;
         case 1 -> var10000 = 1.7F;
         case 2 -> var10000 = 2.6F;
         default -> var10000 = 2.4F;
      }

      return var10000;
   }

   private float jitter(float amount) {
      return (this.random.nextFloat() * 2.0F - 1.0F) * amount;
   }

   static {
      CAPACITY = 12000 / GlyphSwarm.WORST_QUADS / 10 * 10;
      MIXED_ORDER = new byte[]{0, 3, 1, 3, 2, 3};
   }
}
