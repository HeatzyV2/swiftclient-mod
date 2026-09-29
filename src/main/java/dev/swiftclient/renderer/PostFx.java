package dev.swiftclient.renderer;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

/**
 * Full-screen effects applied to the world image before the HUD: colour grading (Color Saturation) and
 * motion blur (the previous frame faded over the new one). Same fullscreen-pass technique as ScreenBlur.
 */
public final class PostFx {
   private static RenderPipeline pipeline;
   private static GpuBuffer gradeUniforms;
   private static GpuBuffer blendUniforms;
   private static GpuSampler sampler;
   private static GpuTexture captureTex;
   private static GpuTextureView captureView;
   private static GpuTexture historyTex;
   private static GpuTextureView historyView;
   private static boolean historyValid;
   private static boolean broken;

   private PostFx() {
   }

   private static boolean init() {
      if (pipeline == null && !broken) {
         try {
            BindGroupLayout layout = BindGroupLayout.builder().withSampler("Sampler0").withUniform("Uniforms", UniformType.UNIFORM_BUFFER).build();
            pipeline = RenderPipeline.builder(new Snippet[0])
               .withLocation(Identifier.fromNamespaceAndPath("swiftclient", "pipeline/post_fx"))
               .withVertexShader(Identifier.fromNamespaceAndPath("swiftclient", "post_fx"))
               .withFragmentShader(Identifier.fromNamespaceAndPath("swiftclient", "post_fx"))
               .withBindGroupLayout(layout)
               .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
               .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
               .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
               .withCull(false)
               .build();
            gradeUniforms = RenderSystem.getDevice().createBuffer(() -> "SwiftClient Grade Uniforms", 136, 32L);
            blendUniforms = RenderSystem.getDevice().createBuffer(() -> "SwiftClient Motion Blur Uniforms", 136, 32L);
            sampler = RenderSystem.getDevice()
               .createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
         } catch (Throwable t) {
            broken = true;
            pipeline = null;
         }
      }

      return pipeline != null;
   }

   /**
    * @param saturation 1 = unchanged, 0 = greyscale, 2 = twice as vivid (negative: grading off)
    * @param blur motion blur strength 0..0.95 (0 = off)
    */
   public static void apply(float saturation, float contrast, float brightness, float blur) {
      boolean grade = saturation >= 0.0F;
      boolean motion = blur > 0.001F;
      if (!motion) {
         historyValid = false;
      }

      if (!grade && !motion || !init()) {
         return;
      }

      try {
         RenderTarget rt = Minecraft.getInstance().gameRenderer.mainRenderTarget();
         if (rt == null || rt.getColorTexture() == null || rt.width <= 0 || rt.height <= 0) {
            return;
         }

         GpuTexture screen = rt.getColorTexture();
         int w = rt.width;
         int h = rt.height;
         CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
         if (grade) {
            captureTex = ensure(captureTex, screen, w, h, "SwiftClient Grade Buffer", false);
            enc.copyTextureToTexture(screen, captureTex, 0, 0, 0, 0, 0, w, h);
            write(enc, gradeUniforms, 0.0F, saturation, contrast, brightness);
            draw(enc, rt, captureView, gradeUniforms, "SwiftClient Color Grade");
         }

         if (motion) {
            if (historyTex == null || historyTex.getWidth(0) != w || historyTex.getHeight(0) != h) {
               historyTex = ensure(historyTex, screen, w, h, "SwiftClient Motion Blur History", true);
               historyValid = false;
            }

            if (historyValid) {
               write(enc, blendUniforms, 1.0F, Math.min(0.95F, blur), 0.0F, 0.0F);
               draw(enc, rt, historyView, blendUniforms, "SwiftClient Motion Blur");
            }

            enc.copyTextureToTexture(screen, historyTex, 0, 0, 0, 0, 0, w, h);
            historyValid = true;
         }
      } catch (Throwable ignored) {
         // The effect is cosmetic: on any GPU error the frame is shown untouched.
      }
   }

   private static GpuTexture ensure(GpuTexture tex, GpuTexture screen, int w, int h, String label, boolean history) {
      if (tex != null && tex.getWidth(0) == w && tex.getHeight(0) == h) {
         return tex;
      }

      if (history) {
         close(historyView, historyTex);
      } else {
         close(captureView, captureTex);
      }

      GpuTexture t = RenderSystem.getDevice().createTexture(label, 5, screen.getFormat(), w, h, 1, 1);
      GpuTextureView v = RenderSystem.getDevice().createTextureView(t);
      if (history) {
         historyView = v;
      } else {
         captureView = v;
      }

      return t;
   }

   private static void close(GpuTextureView v, GpuTexture t) {
      try {
         if (v != null) {
            v.close();
         }

         if (t != null) {
            t.close();
         }
      } catch (Throwable ignored) {
      }
   }

   private static void write(CommandEncoder enc, GpuBuffer target, float a, float b, float c, float d) {
      ByteBuffer buf = MemoryUtil.memAlloc(32);
      try {
         buf.putFloat(a).putFloat(b).putFloat(c).putFloat(d);
         buf.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
         buf.flip();
         enc.writeToBuffer(target.slice(), buf);
      } finally {
         MemoryUtil.memFree(buf);
      }
   }

   private static void draw(CommandEncoder enc, RenderTarget rt, GpuTextureView source, GpuBuffer uniforms, String label) {
      try (RenderPass pass = enc.createRenderPass(() -> label, rt.getColorTextureView(), Optional.empty(), rt.getDepthTextureView(), OptionalDouble.empty())) {
         pass.setPipeline(pipeline);
         pass.bindTexture("Sampler0", source, sampler);
         pass.setUniform("Uniforms", uniforms);
         pass.draw(0, 6, 0, 1);
      }
   }
}
