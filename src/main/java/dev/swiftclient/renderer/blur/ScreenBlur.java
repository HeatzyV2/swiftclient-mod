package dev.swiftclient.renderer.blur;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
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

public final class ScreenBlur {
   private static final int UNIFORM_SIZE = 32;
   private static RenderPipeline pipeline;
   private static GpuBuffer uniformBuffer;
   private static GpuSampler sampler;
   private static GpuTexture captureTex;
   private static GpuTextureView captureView;

   private ScreenBlur() {
   }

   private static void init() {
      if (pipeline == null) {
         try {
            BindGroupLayout layout = BindGroupLayout.builder().withSampler("Sampler0").withUniform("Uniforms", UniformType.UNIFORM_BUFFER).build();
            pipeline = RenderPipeline.builder(new Snippet[0])
               .withLocation(Identifier.fromNamespaceAndPath("swiftclient", "pipeline/screen_blur"))
               .withVertexShader(Identifier.fromNamespaceAndPath("swiftclient", "screen_blur"))
               .withFragmentShader(Identifier.fromNamespaceAndPath("swiftclient", "screen_blur"))
               .withBindGroupLayout(layout)
               .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
               .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
               .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
               .withCull(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "SwiftClient Blur Uniforms", 136, 32L);
            sampler = RenderSystem.getDevice()
               .createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
         } catch (Throwable ignored) {
            pipeline = null;
         }
      }
   }

   public static void draw(float blurPx, int dimArgb) {
      init();
      if (pipeline != null && uniformBuffer != null) {
         try {
            RenderTarget rt = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            if (rt == null) {
               return;
            }

            GpuTexture screen = rt.getColorTexture();
            if (screen == null) {
               return;
            }

            int w = rt.width;
            int h = rt.height;
            if (w <= 0 || h <= 0) {
               return;
            }

            if (captureTex == null || captureTex.getWidth(0) != w || captureTex.getHeight(0) != h) {
               cleanupTex();
               captureTex = RenderSystem.getDevice().createTexture("SwiftClient Blur Buffer", 5, screen.getFormat(), w, h, 1, 1);
               captureView = RenderSystem.getDevice().createTextureView(captureTex);
            }

            CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
            enc.copyTextureToTexture(screen, captureTex, 0, 0, 0, 0, 0, w, h);
            ByteBuffer buf = MemoryUtil.memAlloc(32);
            buf.putFloat(Math.min(blurPx, 24.0F)).putFloat(w).putFloat(h).putFloat(0.0F);
            buf.putFloat((dimArgb >> 16 & 0xFF) / 255.0F)
               .putFloat((dimArgb >> 8 & 0xFF) / 255.0F)
               .putFloat((dimArgb & 0xFF) / 255.0F)
               .putFloat((dimArgb >>> 24 & 0xFF) / 255.0F);
            buf.flip();
            enc.writeToBuffer(uniformBuffer.slice(), buf);
            MemoryUtil.memFree(buf);
            RenderPass pass = enc.createRenderPass(
               () -> "SwiftClient Blur", rt.getColorTextureView(), Optional.empty(), rt.getDepthTextureView(), OptionalDouble.empty()
            );

            try {
               pass.setPipeline(pipeline);
               pass.bindTexture("Sampler0", captureView, sampler);
               pass.setUniform("Uniforms", uniformBuffer);
               pass.draw(0, 6, 0, 1);
            } catch (Throwable e) {
               if (pass != null) {
                  try {
                     pass.close();
                  } catch (Throwable ex) {
                     e.addSuppressed(ex);
                  }
               }

               throw e;
            }

            if (pass != null) {
               pass.close();
            }
         } catch (Throwable ignored) {
         }
      }
   }

   private static void cleanupTex() {
      if (captureView != null) {
         try {
            captureView.close();
         } catch (Throwable ignored) {
         }

         captureView = null;
      }

      if (captureTex != null) {
         try {
            captureTex.close();
         } catch (Throwable ignored) {
         }

         captureTex = null;
      }
   }
}
