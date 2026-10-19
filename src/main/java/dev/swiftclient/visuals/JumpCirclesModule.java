package dev.swiftclient.visuals;

import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.BindGroupLayouts;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;

public final class JumpCirclesModule extends Module {
   public static volatile JumpCirclesModule active;
   public static final Identifier LOGO_TEXTURE = Identifier.fromNamespaceAndPath("swiftclient", "textures/misc/jump_circle_logo.png");
   public static final Identifier LOGO_GLOW_TEXTURE = Identifier.fromNamespaceAndPath("swiftclient", "textures/misc/jump_circle_logo_glow.png");
   private static final String COLOR_ACCENT = "Accent";
   private static final String COLOR_CUSTOM = "Custom";
   private static final int MAX_RINGS = 24;
   private static final int SEGMENTS = 64;
   private static final float[] COS = new float[65];
   private static final float[] SIN = new float[65];
   private static final double LIFT = 0.015;
   private static final double MAX_DISTANCE_SQR = (double)16384.0F;
   private static final double JUMP_RISE = 0.2;
   private static final int RISE_TICKS = 6;
   private static final int SWEEP_TICKS = 100;
   private static final float CORE_WHITE = 0.45F;
   private static final float CORE_ALPHA = 0.95F;
   private static final float HALO_ALPHA = 0.55F;
   private static final float FADE_IN_NANOS = 8.0E7F;
   private static final float LOGO_FILL = 0.75F;
   private static final float LOGO_SPAN = 0.9277344F;
   private static final float LOGO_DROP = 0.0126953125F;
   private static final float LOGO_GLOW_SCALE = 1.5F;
   private final ModuleSetting colorMode = this.cycle("color_mode", "Color", new String[]{"Accent", "Custom"}, 0).desc("Accent uses the Swift accent colour, Custom the colour below.");
   private final ModuleSetting color = this.color("color", "Custom color", -1853860).desc("Colour of the ring (Custom).");
   private final ModuleSetting size = this.slider("size", "Size", 1.2, 0.5, 3.0, 0.1, "").desc("How wide the ring opens.");
   private final ModuleSetting duration = this.slider("duration", "Duration", 1.0, 0.3, 3.0, 0.1, "").desc("How long the ring stays, in seconds.");
   private final ModuleSetting logo = this.toggle("logo", "Swift logo", true).desc("Draw the Swift logo in the middle of the ring.");
   private final ModuleSetting others = this.toggle("others", "Other players", false).desc("Also draw rings where other players jump.");
   private final double[] ringX = new double[24];
   private final double[] ringY = new double[24];
   private final double[] ringZ = new double[24];
   private final float[] ringFacingX = new float[24];
   private final float[] ringFacingZ = new float[24];
   private final long[] ringBorn = new long[24];
   private int head;
   private int count;
   private float poseX;
   private float poseY;
   private float poseZ;
   private float poseT;
   private float poseRadius;
   private float poseFade;
   private final Int2ObjectOpenHashMap<Lift> lifts = new Int2ObjectOpenHashMap();
   private int tick;
   private int levelKey;

   public JumpCirclesModule() {
      super("jump_circles", "Jump Circles", "A fading Swift ring appears on the ground where you jump.", "Visual", "target", false);
   }

   @Override

   protected void onEnable() {
      this.clear();
      active = this;
   }

   @Override

   protected void onDisable() {
      active = null;
      this.clear();
   }

   private void clear() {
      this.head = 0;
      this.count = 0;
      this.lifts.clear();
      this.levelKey = 0;
   }

   public static void onJump(LivingEntity entity) {
      JumpCirclesModule module = active;
      if (module != null) {
         Minecraft mc = Minecraft.getInstance();
         if (entity == mc.player && mc.level != null) {
            module.syncLevel(mc.level);
            module.spawn(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot());
         }
      }
   }

   private void spawn(double x, double y, double z, float yaw) {
      if (this.count == 24) {
         this.head = (this.head + 1) % 24;
         --this.count;
      }

      int slot = (this.head + this.count) % 24;
      this.ringX[slot] = x;
      this.ringY[slot] = y;
      this.ringZ[slot] = z;
      double heading = Math.toRadians((double)yaw);
      this.ringFacingX[slot] = (float)(-Math.sin(heading));
      this.ringFacingZ[slot] = (float)Math.cos(heading);
      this.ringBorn[slot] = System.nanoTime();
      ++this.count;
   }

   private void expire(long now) {
      for(long life = this.durationNanos(); this.count > 0 && now - this.ringBorn[this.head] >= life; --this.count) {
         this.head = (this.head + 1) % 24;
      }

   }

   private long durationNanos() {
      return (long)((double)this.duration.floatValue() * (double)1.0E9F);
   }

   private void syncLevel(ClientLevel level) {
      int key = System.identityHashCode(level);
      if (key != this.levelKey) {
         this.levelKey = key;
         this.head = 0;
         this.count = 0;
         this.lifts.clear();
      }
   }

   @Override

   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level == null) {
         this.clear();
      } else {
         this.syncLevel(level);
         if (!this.others.boolValue()) {
            if (!this.lifts.isEmpty()) {
               this.lifts.clear();
            }

         } else {
            ++this.tick;
            List<AbstractClientPlayer> players = level.players();

            for(int i = 0; i < players.size(); ++i) {
               AbstractClientPlayer player = (AbstractClientPlayer)players.get(i);
               if (player != mc.player) {
                  Lift lift = (Lift)this.lifts.get(player.getId());
                  if (lift == null) {
                     lift = new Lift();
                     lift.airTicks = 6;
                     lift.judged = true;
                     this.lifts.put(player.getId(), lift);
                  }

                  lift.seen = this.tick;
                  this.track(player, lift);
               }
            }

            if (this.tick % 100 == 0) {
               int now = this.tick;
               this.lifts.values().removeIf((liftx) -> liftx.seen != now);
            }

         }
      }
   }

   private void track(AbstractClientPlayer player, Lift lift) {
      if (player.onGround()) {
         lift.x = player.getX();
         lift.y = player.getY();
         lift.z = player.getZ();
         lift.yaw = player.getYRot();
         lift.airTicks = -1;
         lift.judged = false;
      } else if (!lift.judged) {
         ++lift.airTicks;
         if (player.getY() - lift.y > 0.2) {
            lift.judged = true;
            if (canLeaveRing(player)) {
               this.spawn(lift.x, lift.y, lift.z, lift.yaw);
            }
         } else if (lift.airTicks >= 6) {
            lift.judged = true;
         }

      }
   }

   private static boolean canLeaveRing(AbstractClientPlayer player) {
      return !player.isPassenger() && !player.isFallFlying() && !player.isInLiquid() && !player.onClimbable() && player.hurtTime == 0 && !player.isSpectator() && !player.isInvisible();
   }

   /** Called with the world's entities (WorldRendererMixin): rings are submitted with the frame. */
   public static void submit(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      JumpCirclesModule module = active;
      if (module != null && module.count > 0 && camera.initialized) {
         module.render(poseStack, collector, camera.pos);
      }
   }

   private void render(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera) {
      long now = System.nanoTime();
      this.expire(now);
      if (this.count != 0) {
         int argb = this.colorMode.is("Custom") ? this.color.colorValue() : Px.ACCENT;
         int rgb = argb & 16777215;
         float radiusMax = this.size.floatValue();
         float life = (float)this.durationNanos();
         float core = 0.025F + 0.02F * radiusMax;
         int coreRgb = towardsWhite(rgb, 0.45F);
         if (this.logo.boolValue()) {
            float alpha = (float)(argb >>> 24) / 255.0F;
            collector.submitCustomGeometry(poseStack, JumpCirclesModule.Logo.glowType(), (pose, out) -> {
               for (int n = 0; n < this.count; ++n) {
                  int slot = (this.head + n) % 24;
                  if (this.pose(slot, now, life, radiusMax, camera)) {
                     this.logo(out, slot, this.poseRadius - core, 1.5F, withAlpha(rgb, alpha * 0.55F * this.poseFade));
                  }
               }
            });
            collector.submitCustomGeometry(poseStack, JumpCirclesModule.Logo.type(), (pose, out) -> {
               for (int n = 0; n < this.count; ++n) {
                  int slot = (this.head + n) % 24;
                  if (this.pose(slot, now, life, radiusMax, camera)) {
                     this.logo(out, slot, this.poseRadius - core, 1.0F, withAlpha(coreRgb, alpha * 0.95F * this.poseFade));
                  }
               }
            });
         }

         collector.submitCustomGeometry(poseStack, JumpCirclesModule.Ring.TYPE, (pose, out) -> {
            for (int n = 0; n < this.count; ++n) {
               int slot = (this.head + n) % 24;
               if (this.pose(slot, now, life, radiusMax, camera)) {
                  float glow = (0.08F + 0.08F * radiusMax) * (1.0F - 0.35F * this.poseT);
                  int halo = withAlpha(rgb, 0.55F * this.poseFade);
                  int line = withAlpha(coreRgb, 0.95F * this.poseFade);
                  float cx = this.poseX;
                  float cy = this.poseY;
                  float cz = this.poseZ;
                  float radius = this.poseRadius;
                  float in = radius - core;
                  float outR = radius + core;
                  band(out, cx, cy, cz, Math.max(0.0F, in - glow * 0.7F), rgb, in, halo);
                  band(out, cx, cy, cz, in, halo, radius, line);
                  band(out, cx, cy, cz, radius, line, outR, halo);
                  band(out, cx, cy, cz, outR, halo, outR + glow, rgb);
               }
            }
         });
      }
   }

   private boolean pose(int slot, long now, float life, float radiusMax, Vec3 camera) {
      float cx = (float)(this.ringX[slot] - camera.x);
      float cy = (float)(this.ringY[slot] + 0.015 - camera.y);
      float cz = (float)(this.ringZ[slot] - camera.z);
      if ((double)cx * (double)cx + (double)cy * (double)cy + (double)cz * (double)cz > (double)16384.0F) {
         return false;
      } else {
         long age = now - this.ringBorn[slot];
         float t = Math.min(1.0F, (float)age / life);
         float open = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
         float fade = (1.0F - t) * (1.0F - t) * Math.min(1.0F, (float)age / 8.0E7F);
         if (fade <= 0.004F) {
            return false;
         } else {
            this.poseX = cx;
            this.poseY = cy;
            this.poseZ = cz;
            this.poseT = t;
            this.poseRadius = radiusMax * (0.18F + 0.82F * open);
            this.poseFade = fade;
            return true;
         }
      }
   }

   private void logo(VertexConsumer out, int slot, float inner, float scale, int argb) {
      if (!(inner <= 0.0F)) {
         float half = 0.75F * inner / 0.9277344F;
         float fx = this.ringFacingX[slot];
         float fz = this.ringFacingZ[slot];
         float lead = 0.025390625F * half;
         float x = this.poseX + fx * lead;
         float y = this.poseY;
         float z = this.poseZ + fz * lead;
         half *= scale;
         float ax = (fx + fz) * half;
         float az = (fz - fx) * half;
         float bx = (fx - fz) * half;
         float bz = (fz + fx) * half;
         out.addVertex(x + ax, y, z + az).setUv(0.0F, 0.0F).setColor(argb);
         out.addVertex(x + bx, y, z + bz).setUv(1.0F, 0.0F).setColor(argb);
         out.addVertex(x - ax, y, z - az).setUv(1.0F, 1.0F).setColor(argb);
         out.addVertex(x - bx, y, z - bz).setUv(0.0F, 1.0F).setColor(argb);
      }
   }

   private static void band(VertexConsumer out, float x, float y, float z, float r0, int c0, float r1, int c1) {
      int segments = 64;

      for(int i = 0; i < segments; ++i) {
         int from = i * 64 / segments;
         int to = (i + 1) * 64 / segments;
         float cosA = COS[from];
         float sinA = SIN[from];
         float cosB = COS[to];
         float sinB = SIN[to];
         out.addVertex(x + cosA * r0, y, z + sinA * r0).setColor(c0);
         out.addVertex(x + cosB * r0, y, z + sinB * r0).setColor(c0);
         out.addVertex(x + cosB * r1, y, z + sinB * r1).setColor(c1);
         out.addVertex(x + cosA * r1, y, z + sinA * r1).setColor(c1);
      }

   }

   private static int withAlpha(int rgb, float alpha) {
      int a = (int)(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F + 0.5F);
      return a << 24 | rgb;
   }

   private static int towardsWhite(int rgb, float amount) {
      int r = rgb >> 16 & 255;
      int g = rgb >> 8 & 255;
      int b = rgb & 255;
      r += (int)((float)(255 - r) * amount);
      g += (int)((float)(255 - g) * amount);
      b += (int)((float)(255 - b) * amount);
      return r << 16 | g << 8 | b;
   }

   static {
      for(int i = 0; i <= 64; ++i) {
         double angle = (Math.PI * 2D) * (double)i / (double)64.0F;
         COS[i] = (float)Math.cos(angle);
         SIN[i] = (float)Math.sin(angle);
      }

   }

   private static final class Lift {
      double x;
      double y;
      double z;
      float yaw;
      int airTicks;
      boolean judged;
      int seen;
   }

   private static final class Ring {
      static final RenderType TYPE;

      static {
         TYPE = RenderType.create("swiftclient_jump_circles", RenderSetup.builder(RenderPipelines.DEBUG_QUADS).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup());
      }
   }

   private static final class Logo {
      private static RenderPipeline PIPELINE;
      private static RenderType TYPE;
      private static RenderType GLOW_TYPE;

      static RenderType type() {
         init();
         return TYPE;
      }

      static RenderType glowType() {
         init();
         return GLOW_TYPE;
      }

      private static void init() {
         if (TYPE != null) {
            return;
         }

         PIPELINE = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withLocation(Identifier.fromNamespaceAndPath("swiftclient", "pipeline/jump_circle_logo"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build();
         TYPE = RenderType.create("swiftclient_jump_circle_logo", RenderSetup.builder(PIPELINE).withTexture("Sampler0", JumpCirclesModule.LOGO_TEXTURE).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup());
         GLOW_TYPE = RenderType.create("swiftclient_jump_circle_logo_glow", RenderSetup.builder(PIPELINE).withTexture("Sampler0", JumpCirclesModule.LOGO_GLOW_TEXTURE).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup());
      }
   }
}
