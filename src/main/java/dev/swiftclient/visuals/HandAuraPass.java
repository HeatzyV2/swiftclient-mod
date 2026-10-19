package dev.swiftclient.visuals;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.swiftclient.core.log.Log;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;

/**
 * Hand Aura: the first-person hands are drawn into their own target, then composed over the frame with
 * a blurred halo, a rim light and a moving sheen.
 */
final class HandAuraPass {
   private static final Logger LOGGER = Log.get("HandAura");
   private static final int CONFIG_SIZE = 48;
   private static RenderPipeline pipeline;
   private static TextureTarget hand;
   private static AuraBlur blur;
   private static GpuBuffer config;
   private static GpuBufferSlice configSlice;
   private static ByteBuffer configData;
   private static boolean capturing;
   private static boolean invalidShaderLogged;

   private HandAuraPass() {
   }

   private static RenderPipeline pipeline() {
      if (pipeline == null) {
         pipeline = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("swiftclient", "pipeline/hand_aura"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Identifier.fromNamespaceAndPath("swiftclient", "core/hand_aura"))
            .withBindGroupLayout(
               BindGroupLayout.builder().withSampler("CaptureSampler").withSampler("HaloSampler").withUniform("HandAuraConfig", UniformType.UNIFORM_BUFFER).build()
            )
            // Colour only: the frame's alpha stays untouched.
            .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA), GpuFormat.RGBA8_UNORM, 7))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .build();
      }

      return pipeline;
   }

   static boolean begin() {
      if (capturing || RenderSystem.outputColorTextureOverride != null || RenderSystem.outputDepthTextureOverride != null) {
         return false;
      }

      Minecraft mc = Minecraft.getInstance();
      RenderTarget main = mc.gameRenderer.mainRenderTarget();
      if (mc.level == null || main == null || main.getColorTexture() == null || main.width <= 0 || main.height <= 0) {
         return false;
      }

      GpuDevice device = RenderSystem.getDevice();
      if (!shaderReady(device)) {
         return false;
      }

      if (hand == null) {
         allocate(main.width, main.height);
      } else if (hand.width != main.width || hand.height != main.height) {
         hand.resize(main.width, main.height);
      }

      // 26.x uses reversed depth: 0 is the far plane.
      device.createCommandEncoder().clearColorAndDepthTextures(hand.getColorTexture(), new Vector4f(0.0F, 0.0F, 0.0F, 0.0F), hand.getDepthTexture(), 0.0);
      RenderSystem.outputColorTextureOverride = hand.getColorTextureView();
      RenderSystem.outputDepthTextureOverride = hand.getDepthTextureView();
      capturing = true;
      return true;
   }

   static boolean end() {
      if (!capturing) {
         return false;
      }

      capturing = false;
      RenderSystem.outputColorTextureOverride = null;
      RenderSystem.outputDepthTextureOverride = null;
      return true;
   }

   static void compose(int tintArgb, float aura, float reachPx, float rim, float rimPx, int sheenArgb, float sheen, float phase) {
      RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      GpuTextureView halo = aura > 0.0F ? blur.spread(encoder, hand.getColorTextureView(), hand.width, hand.height, reachPx) : hand.getColorTextureView();
      putColor(0, tintArgb, aura);
      putColor(16, sheenArgb, sheen);
      configData.putFloat(32, rim);
      configData.putFloat(36, rimPx);
      configData.putFloat(40, phase);
      configData.putFloat(44, 0.0F);
      encoder.writeToBuffer(configSlice, configData);
      try (RenderPass pass = encoder.createRenderPass(() -> "Swift hand aura", main.getColorTextureView(), Optional.empty())) {
         pass.setPipeline(pipeline());
         pass.setUniform("HandAuraConfig", configSlice);
         pass.bindTexture("CaptureSampler", hand.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         pass.bindTexture("HaloSampler", halo, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         pass.draw(3, 1, 0, 0);
      }
   }

   private static void putColor(int offset, int argb, float weight) {
      configData.putFloat(offset, (float)(argb >> 16 & 255) / 255.0F);
      configData.putFloat(offset + 4, (float)(argb >> 8 & 255) / 255.0F);
      configData.putFloat(offset + 8, (float)(argb & 255) / 255.0F);
      configData.putFloat(offset + 12, weight);
   }

   private static boolean shaderReady(GpuDevice device) {
      if (device.precompilePipeline(pipeline()).isValid() && AuraBlur.compiled(device)) {
         invalidShaderLogged = false;
         return true;
      }

      if (!invalidShaderLogged) {
         invalidShaderLogged = true;
         LOGGER.warn("Hand Aura shaders did not compile; the hand is drawn as usual");
      }

      return false;
   }

   private static void allocate(int width, int height) {
      hand = new TextureTarget("Swift hand aura", width, height, true, GpuFormat.RGBA8_UNORM);
      blur = new AuraBlur("Swift hand aura");
      config = RenderSystem.getDevice().createBuffer(() -> "Swift hand aura config", 136, CONFIG_SIZE);
      configSlice = config.slice();
      configData = BufferUtils.createByteBuffer(CONFIG_SIZE);
   }

   static void release() {
      if (capturing) {
         end();
      }

      if (hand != null) {
         hand.destroyBuffers();
         hand = null;
      }

      if (blur != null) {
         blur.release();
         blur = null;
      }

      if (config != null) {
         config.close();
         config = null;
      }

      configSlice = null;
      configData = null;
   }
}
