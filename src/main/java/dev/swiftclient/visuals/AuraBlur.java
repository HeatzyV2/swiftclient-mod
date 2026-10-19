package dev.swiftclient.visuals;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.lwjgl.BufferUtils;

/** Two-pass separable blur at half resolution: spreads the captured hand into a soft halo. */
final class AuraBlur {
   private static RenderPipeline pipeline;
   private static final int CONFIG_SIZE = 16;
   private static final float TAPS = 6.0F;
   private final String name;
   private TextureTarget across;
   private TextureTarget down;
   private GpuBuffer acrossConfig;
   private GpuBuffer downConfig;
   private GpuBufferSlice acrossSlice;
   private GpuBufferSlice downSlice;
   private ByteBuffer configData;

   AuraBlur(String name) {
      this.name = name;
   }

   private static RenderPipeline pipeline() {
      if (pipeline == null) {
         pipeline = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("swiftclient", "pipeline/aura_spread"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Identifier.fromNamespaceAndPath("swiftclient", "core/aura_spread"))
            .withBindGroupLayout(BindGroupLayout.builder().withSampler("InSampler").withUniform("AuraSpread", UniformType.UNIFORM_BUFFER).build())
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .build();
      }

      return pipeline;
   }

   static boolean compiled(GpuDevice device) {
      return device.precompilePipeline(pipeline()).isValid();
   }

   GpuTextureView spread(CommandEncoder encoder, GpuTextureView source, int width, int height, float reachPx) {
      int divisor = reachPx >= 8.0F ? 4 : 2;
      int smallWidth = Math.max(1, (width + divisor - 1) / divisor);
      int smallHeight = Math.max(1, (height + divisor - 1) / divisor);
      if (this.across == null) {
         this.allocate(smallWidth, smallHeight);
      } else if (this.across.width != smallWidth || this.across.height != smallHeight) {
         this.across.resize(smallWidth, smallHeight);
         this.down.resize(smallWidth, smallHeight);
      }

      float step = Math.max(0.75F, reachPx / TAPS);
      this.run(encoder, source, this.across, this.acrossSlice, step / (float)width, 0.0F);
      this.run(encoder, this.across.getColorTextureView(), this.down, this.downSlice, 0.0F, step / (float)height);
      return this.down.getColorTextureView();
   }

   private void run(CommandEncoder encoder, GpuTextureView from, TextureTarget into, GpuBufferSlice slice, float du, float dv) {
      this.configData.putFloat(0, du);
      this.configData.putFloat(4, dv);
      encoder.writeToBuffer(slice, this.configData);
      try (RenderPass pass = encoder.createRenderPass(() -> this.name + " spread", into.getColorTextureView(), Optional.empty())) {
         pass.setPipeline(pipeline());
         pass.setUniform("AuraSpread", slice);
         pass.bindTexture("InSampler", from, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         pass.draw(3, 1, 0, 0);
      }
   }

   private void allocate(int width, int height) {
      GpuDevice device = RenderSystem.getDevice();
      this.across = new TextureTarget(this.name + " across", width, height, false, GpuFormat.RGBA8_UNORM);
      this.down = new TextureTarget(this.name + " down", width, height, false, GpuFormat.RGBA8_UNORM);
      this.acrossConfig = device.createBuffer(() -> this.name + " across config", 136, CONFIG_SIZE);
      this.downConfig = device.createBuffer(() -> this.name + " down config", 136, CONFIG_SIZE);
      this.acrossSlice = this.acrossConfig.slice();
      this.downSlice = this.downConfig.slice();
      this.configData = BufferUtils.createByteBuffer(CONFIG_SIZE);
   }

   void release() {
      if (this.across != null) {
         this.across.destroyBuffers();
         this.across = null;
      }

      if (this.down != null) {
         this.down.destroyBuffers();
         this.down = null;
      }

      if (this.acrossConfig != null) {
         this.acrossConfig.close();
         this.acrossConfig = null;
      }

      if (this.downConfig != null) {
         this.downConfig.close();
         this.downConfig = null;
      }

      this.acrossSlice = null;
      this.downSlice = null;
      this.configData = null;
   }
}
