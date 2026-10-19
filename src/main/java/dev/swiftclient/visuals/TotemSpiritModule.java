package dev.swiftclient.visuals;

import dev.swiftclient.core.ui.Px;
import net.minecraft.world.phys.Vec3;
import dev.swiftclient.world.WorldOverlays;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import java.util.function.Consumer;
import dev.swiftclient.visuals.glyph.GlyphCanvas;
import dev.swiftclient.visuals.glyph.GlyphShapes;
import dev.swiftclient.visuals.glyph.GlyphTint;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.client.multiplayer.ClientLevel;

public final class TotemSpiritModule extends Module implements WorldOverlays.WorldOverlay {
   public static volatile TotemSpiritModule active;
   private static final String LOOK_GLASS = "Glass";
   private static final String LOOK_WIRE = "Wire";
   private static final int MAX_SPIRITS = 12;
   private static final double MAX_DISTANCE_SQR = (double)9216.0F;
   private static final float MODEL_UNIT = 0.05859375F;
   private static final float HIP = 12.0F;
   private static final float HEAD_TOP = 32.0F;
   private static final float CROUCH_LEAN = 0.45F;
   private static final float CROUCH_DROP = 3.0F;
   private static final float KA_ARM_PITCH = 2.75F;
   private static final float KA_ARM_SPREAD = 0.32F;
   private static final float SHELL = 0.7F;
   private static final float SEAL_SPAN = 0.6F;
   private static final float[][] PARTS = new float[][]{{-1.9F, 12.0F, 0.0F, -2.0F, -12.0F, -2.0F, 2.0F, 0.0F, 2.0F, 0.0F}, {1.9F, 12.0F, 0.0F, -2.0F, -12.0F, -2.0F, 2.0F, 0.0F, 2.0F, 0.0F}, {0.0F, 12.0F, 0.0F, -4.0F, 0.0F, -2.0F, 4.0F, 12.0F, 2.0F, 0.4F}, {-5.0F, 22.0F, 0.0F, -3.0F, -10.0F, -2.0F, 1.0F, 2.0F, 2.0F, 0.6F}, {5.0F, 22.0F, 0.0F, -1.0F, -10.0F, -2.0F, 3.0F, 2.0F, 2.0F, 0.6F}, {0.0F, 24.0F, 0.0F, -4.0F, 0.0F, -4.0F, 4.0F, 8.0F, 4.0F, 1.0F}};
   private static final int FIRST_TORSO_PART = 2;
   private static final int[] FACES = new int[]{1, 3, 7, 5, 0, 4, 6, 2, 2, 6, 7, 3, 0, 1, 5, 4, 4, 5, 7, 6, 0, 2, 3, 1};
   private static final int[] EDGES = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 0, 2, 1, 3, 4, 6, 5, 7, 0, 4, 1, 5, 2, 6, 3, 7};
   private final ModuleSetting kaTime = this.slider("ka_time", "Duration", 2.4, 0.6, 6.0, 0.1, "").desc("How long the spirit stays visible, in seconds.");
   private final ModuleSetting ascent = this.slider("ascent", "Rise height", 1.6, 0.0, 4.0, 0.1, "").desc("How high the spirit floats up while it fades.");
   private final ModuleSetting look = this.cycle("look", "Look", new String[]{"Glass", "Wire"}, 0).desc("Glass: soft see-through body. Wire: only the outline.");
   private final ModuleSetting opacity = this.slider("opacity", "Opacity", 0.65, 0.15, 1.0, 0.05, "").desc("How visible the spirit is.");
   private final ModuleSetting kaPose = this.toggle("ka_pose", "Arms raised", true).desc("The spirit raises its arms as it rises.");
   private final ModuleSetting crest = this.toggle("crest", "Ankh crest", true).desc("An ankh glows above the spirit's head.");
   private final ModuleSetting seal = this.toggle("seal", "Ground seal", true).desc("A glowing circle on the ground where the totem popped.");
   private final ModuleSetting own = this.toggle("own", "Your own pops", false).desc("Also show a spirit when you pop a totem yourself.");
   private final ModuleSetting colorMode = this.cycle("color_mode", "Color", new String[]{"Theme", "Manual"}, 0).desc("Theme uses the Swift accent colour, Manual the colours below.");
   private final ModuleSetting bodyColor = this.color("body_color", "Body color", -11891723).desc("Colour of the spirit's body (Manual).");
   private final ModuleSetting auraColor = this.color("aura_color", "Aura color", -14860125).desc("Colour of the glow around it (Manual).");
   private final Spirit[] spirits = new Spirit[12];
   private final GlyphCanvas canvas = new GlyphCanvas();
   private final GlyphTint palette = new GlyphTint();
   private final Consumer<LivingEntity> popSink = this::capture;
   private final double[] corners = new double[24];
   private final float[] pitch;
   private final float[] yaw;
   private final float[] roll;
   private final float[] rotations;
   private boolean cachedPose;
   private int head;
   private int count;
   private ClientLevel boundLevel;
   private double originX;
   private double originY;
   private double originZ;
   private float leftX;
   private float leftZ;
   private float forwardX;
   private float forwardZ;
   private float unit;
   private float leanSin;
   private float leanCos;
   private float drop;

   public TotemSpiritModule() {
      super("totem_spirit", "Totem Spirit", "A glowing ghost rises when a player pops a totem.", "Visual", "crown", false);
      this.pitch = new float[PARTS.length];
      this.yaw = new float[PARTS.length];
      this.roll = new float[PARTS.length];
      this.rotations = new float[PARTS.length * 6];

      for(int i = 0; i < 12; ++i) {
         this.spirits[i] = new Spirit();
      }
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
      this.head = 0;
      this.count = 0;
      this.boundLevel = null;
   }

   private boolean bind(ClientLevel level) {
      if (level == null) {
         if (this.boundLevel != null) {
            this.reset();
         }

         return false;
      } else {
         if (level != this.boundLevel) {
            this.head = 0;
            this.count = 0;
            this.boundLevel = level;
         }

         return true;
      }
   }

   @Override

   protected void onTick() {
      if (this.boundLevel != null) {
         this.bind(Minecraft.getInstance().level);
      }

   }

   /** A living entity popped a totem (TotemPopMixin). */
   public static void onTotemPop(LivingEntity entity) {
      TotemSpiritModule module = active;
      if (module != null && true) {
         module.popSink.accept(entity);
      }
   }

   private void capture(LivingEntity entity) {
      Minecraft mc = Minecraft.getInstance();
      if (entity instanceof Player player) {
         if (this.bind(mc.level) && player.level() == mc.level) {
            if (player.isSpectator()) {
               return;
            }

            boolean mine = player == mc.player;
            if (mine && !this.own.boolValue()) {
               return;
            }

            if (this.count == 12) {
               this.head = (this.head + 1) % 12;
               --this.count;
            }

            Spirit spirit = this.spirits[(this.head + this.count) % 12];
            ++this.count;
            spirit.x = player.getX();
            spirit.y = player.getY();
            spirit.z = player.getZ();
            spirit.bodyYaw = player.yBodyRot;
            spirit.headYaw = Mth.wrapDegrees(player.yHeadRot - player.yBodyRot);
            spirit.headPitch = player.getXRot();
            spirit.walk = player.walkAnimation.position();
            spirit.stride = Math.min(1.0F, player.walkAnimation.speed());
            spirit.scale = player.getScale();
            spirit.crouch = player.isCrouching();
            spirit.mine = mine;
            spirit.born = System.nanoTime();
            return;
         }
      }

   }

   @Override

   public void emitGizmos(Minecraft overlayMc, Vec3 overlayCamera, float overlayPartialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (this.bind(mc.level) && this.count != 0) {
         long now = System.nanoTime();

         float life;
         for(life = this.kaTime.floatValue() * 1.0E9F; this.count > 0 && (float)(now - this.spirits[this.head].born) >= life; --this.count) {
            this.head = (this.head + 1) % 12;
         }

         if (this.count != 0) {
            this.palette.resolve(this.colorMode.label(), Px.ACCENT, this.bodyColor.colorValue(), this.auraColor.colorValue());
            this.canvas.begin(mc.gameRenderer.mainCamera());

            for(int n = 0; n < this.count; ++n) {
               Spirit spirit = this.spirits[(this.head + n) % 12];
               if ((!spirit.mine || this.own.boolValue()) && !(this.canvas.distanceSqr(spirit.x, spirit.y, spirit.z) > (double)9216.0F)) {
                  this.draw(spirit, Math.min(1.0F, (float)(now - spirit.born) / life));
               }
            }

            this.canvas.submit(false);
         }
      }
   }

   private AABB renderBounds(Spirit spirit) {
      double scale = (double)Math.max(0.01F, spirit.scale);
      double radius = scale * (double)2.0F + (double)0.25F;
      return new AABB(spirit.x - radius, spirit.y - scale, spirit.z - radius, spirit.x + radius, spirit.y + (double)this.ascent.floatValue() * 1.3 + scale * (double)3.5F + (double)0.5F, spirit.z + radius);
   }

   private void draw(Spirit s, float t) {
      float alpha = this.opacity.floatValue() * Math.min(1.0F, t / 0.06F);
      float ka = this.kaPose.boolValue() ? smooth((t - 0.08F) / 0.45F) : 0.0F;
      float lift = this.ascent.floatValue() * ascend(t);
      if (this.seal.boolValue() && t < 0.6F) {
         this.groundSeal(s, t / 0.6F, alpha);
      }

      double yawRad = Math.toRadians((double)s.bodyYaw);
      float cos = (float)Math.cos(yawRad);
      float sin = (float)Math.sin(yawRad);
      this.leftX = cos;
      this.leftZ = sin;
      this.forwardX = -sin;
      this.forwardZ = cos;
      this.originX = s.x;
      this.originY = s.y + (double)lift;
      this.originZ = s.z;
      this.unit = 0.05859375F * s.scale * (1.0F + 0.08F * t);
      float lean = s.crouch ? 0.45F * (1.0F - ka) : 0.0F;
      this.leanSin = Mth.sin((double)lean);
      this.leanCos = Mth.cos((double)lean);
      this.drop = s.crouch ? 3.0F * (1.0F - ka) : 0.0F;
      this.pose(s, ka);
      this.cachedPose = false;
      if (this.cachedPose) {
         this.cachePose();
      }

      boolean glass = this.look.is("Glass");
      int gold = this.palette.primary();
      int lapis = this.palette.secondary();
      int light = this.palette.highlight();

      for(int i = 0; i < PARTS.length; ++i) {
         float fade = alpha * dissolve(t, PARTS[i][9]);
         if (!(fade < 0.01F)) {
            this.place(i, 0.7F);
            this.fill(GlyphTint.alpha(lapis, fade * (glass ? 0.16F : 0.1F)));
         }
      }

      for(int i = 0; i < PARTS.length; ++i) {
         float tier = PARTS[i][9];
         float fade = alpha * dissolve(t, tier);
         if (!(fade < 0.01F)) {
            this.place(i, 0.0F);
            if (glass) {
               this.fill(GlyphTint.alpha(GlyphTint.mix(gold, lapis, (1.0F - tier) * 0.45F), fade * 0.32F));
            }

            this.edges(GlyphTint.alpha(light, fade * (glass ? 0.8F : 0.95F)), glass ? 1.5F : 2.0F);
         }
      }

      if (this.crest.boolValue()) {
         this.ankhCrest(s, t, lift, alpha * dissolve(t, 1.1F));
      }

   }

   private void pose(Spirit s, float ka) {
      float swing = Mth.cos((double)(s.walk * 0.6662F)) * s.stride;
      float settle = 1.0F - ka;
      float crouchArms = s.crouch ? 0.35F : 0.0F;
      this.pitch[0] = swing * 1.4F * settle - 0.05F * ka;
      this.pitch[1] = -swing * 1.4F * settle - 0.15F * ka;
      this.pitch[2] = 0.0F;
      this.pitch[3] = Mth.lerp(ka, -swing + crouchArms, 2.75F);
      this.pitch[4] = Mth.lerp(ka, swing + crouchArms, 2.75F);
      this.pitch[5] = -s.headPitch * ((float)Math.PI / 180F) * settle + 0.3F * ka;
      this.yaw[5] = s.headYaw * ((float)Math.PI / 180F) * settle;
      this.roll[3] = 0.32F * ka;
      this.roll[4] = -0.32F * ka;
   }

   private void place(int index, float inflate) {
      float[] part = PARTS[index];
      boolean torso = index >= 2;
      int r = index * 6;
      float cp = this.cachedPose ? this.rotations[r] : Mth.cos((double)this.pitch[index]);
      float sp = this.cachedPose ? this.rotations[r + 1] : Mth.sin((double)this.pitch[index]);
      float cy = this.cachedPose ? this.rotations[r + 2] : Mth.cos((double)this.yaw[index]);
      float sy = this.cachedPose ? this.rotations[r + 3] : Mth.sin((double)this.yaw[index]);
      float cr = this.cachedPose ? this.rotations[r + 4] : Mth.cos((double)this.roll[index]);
      float sr = this.cachedPose ? this.rotations[r + 5] : Mth.sin((double)this.roll[index]);

      for(int c = 0; c < 8; ++c) {
         float lx = (c & 1) == 0 ? part[3] - inflate : part[6] + inflate;
         float ly = (c & 2) == 0 ? part[4] - inflate : part[7] + inflate;
         float lz = (c & 4) == 0 ? part[5] - inflate : part[8] + inflate;
         float ay = ly * cp + lz * sp;
         float az = lz * cp - ly * sp;
         float bx = lx * cy - az * sy;
         float bz = lx * sy + az * cy;
         float px = bx * cr - ay * sr + part[0];
         float py = bx * sr + ay * cr + part[1];
         float pz = bz + part[2];
         if (torso && this.lean()) {
            float hy = py - 12.0F;
            float ny = hy * this.leanCos - pz * this.leanSin;
            pz = hy * this.leanSin + pz * this.leanCos;
            py = ny + 12.0F;
         }

         py -= this.drop;
         int o = c * 3;
         this.corners[o] = this.originX + (double)((px * this.leftX + pz * this.forwardX) * this.unit);
         this.corners[o + 1] = this.originY + (double)(py * this.unit);
         this.corners[o + 2] = this.originZ + (double)((px * this.leftZ + pz * this.forwardZ) * this.unit);
      }

   }

   private void cachePose() {
      for(int i = 0; i < PARTS.length; ++i) {
         int r = i * 6;
         this.rotations[r] = Mth.cos((double)this.pitch[i]);
         this.rotations[r + 1] = Mth.sin((double)this.pitch[i]);
         this.rotations[r + 2] = Mth.cos((double)this.yaw[i]);
         this.rotations[r + 3] = Mth.sin((double)this.yaw[i]);
         this.rotations[r + 4] = Mth.cos((double)this.roll[i]);
         this.rotations[r + 5] = Mth.sin((double)this.roll[i]);
      }

   }

   private boolean lean() {
      return this.leanSin != 0.0F;
   }

   private void fill(int argb) {
      double[] v = this.corners;

      for(int f = 0; f < FACES.length; f += 4) {
         int a = FACES[f] * 3;
         int b = FACES[f + 1] * 3;
         int c = FACES[f + 2] * 3;
         int d = FACES[f + 3] * 3;
         this.canvas.quad(v[a], v[a + 1], v[a + 2], v[b], v[b + 1], v[b + 2], v[c], v[c + 1], v[c + 2], v[d], v[d + 1], v[d + 2], argb);
      }

   }

   private void edges(int argb, float width) {
      double[] v = this.corners;

      for(int e = 0; e < EDGES.length; e += 2) {
         int a = EDGES[e] * 3;
         int b = EDGES[e + 1] * 3;
         this.canvas.line(v[a], v[a + 1], v[a + 2], v[b], v[b + 1], v[b + 2], argb, width);
      }

   }

   private void groundSeal(Spirit s, float st, float alpha) {
      float open = 1.0F - (1.0F - st) * (1.0F - st) * (1.0F - st);
      float radius = (0.35F + 1.15F * open) * s.scale;
      float a = alpha * (1.0F - st) * 0.9F;
      double y = s.y + 0.02;
      int gold = this.palette.primary();
      int lapis = this.palette.secondary();
      int light = this.palette.highlight();
      this.canvas.ring(s.x, y, s.z, radius * 0.93F, radius, 40, GlyphTint.alpha(gold, a));
      this.canvas.ring(s.x, y, s.z, radius, radius * 1.12F, 40, GlyphTint.alpha(gold, a * 0.25F));
      this.canvas.ring(s.x, y, s.z, radius * 0.55F, radius * 0.6F, 32, GlyphTint.alpha(lapis, a * 0.7F));
      float base = (float)Math.toRadians((double)s.bodyYaw) + open * 0.6F;

      for(int i = 0; i < 4; ++i) {
         float angle = base + (float)i * ((float)Math.PI / 2F);
         this.canvas.flat(GlyphShapes.ANKH, s.x + (double)(Mth.cos((double)angle) * radius * 0.78F), y + 0.004, s.z + (double)(Mth.sin((double)angle) * radius * 0.78F), 0.13F * s.scale, -angle - ((float)Math.PI / 2F), GlyphTint.alpha(light, a));
      }

   }

   private void ankhCrest(Spirit s, float t, float lift, float alpha) {
      if (!(alpha < 0.01F)) {
         float pop = Math.min(1.0F, t / 0.2F);
         float bloom = 1.0F + 1.7F * (pop - 1.0F) * (pop - 1.0F) * (pop - 1.0F) + 0.7F * (pop - 1.0F) * (pop - 1.0F);
         float size = 0.2F * s.scale * Math.max(0.0F, bloom);
         if (!(size <= 0.001F)) {
            double y = this.originY + (double)((32.0F - this.drop) * this.unit) + (double)(0.35F * s.scale) + (double)(lift * 0.3F);
            this.canvas.sprite(GlyphShapes.OCTAGON, s.x, y, s.z, size * 2.2F, 0.0F, GlyphTint.alpha(this.palette.secondary(), alpha * 0.2F));
            this.canvas.sprite(GlyphShapes.ANKH, s.x, y, s.z, size, 0.0F, GlyphTint.alpha(this.palette.highlight(), alpha));
         }
      }
   }

   private static float dissolve(float t, float tier) {
      return 1.0F - Mth.clamp((t - 0.45F - tier * 0.22F) / 0.3F, 0.0F, 1.0F);
   }

   private static float ascend(float t) {
      float k = Math.max(0.0F, (t - 0.12F) / 0.88F);
      return k * k * (2.2F - 1.2F * k);
   }

   private static float smooth(float t) {
      float k = Mth.clamp(t, 0.0F, 1.0F);
      return k * k * (3.0F - 2.0F * k);
   }

   private static final class Spirit {
      double x;
      double y;
      double z;
      float bodyYaw;
      float headYaw;
      float headPitch;
      float walk;
      float stride;
      float scale;
      boolean crouch;
      boolean mine;
      long born;
   }
}
