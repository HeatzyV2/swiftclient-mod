package dev.swiftclient.visuals;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import dev.swiftclient.core.log.Log;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;

/** Atmosphere "Sky Shader": a full sky drawn by a fragment shader on a cube around the camera. */
final class AtmosphereShaderSky {
   static final String OFF = "Off";
   static final String SPACE = "Space";
   static final String NORTHERN_LIGHTS = "Northern Lights";
   static final String RINGED_PLANET = "Ringed Planet";
   static final String BLACK_HOLE = "Black Hole";
   private static final List<String> THEMES = List.of("Space", "Northern Lights", "Ringed Planet", "Black Hole");
   private static final Logger LOGGER = Log.get("Atmosphere");
   private static RenderPipeline pipeline;
   private static final int UNIFORM_SIZE = 112;
   private static final int CUBE_INDICES = 36;
   private GpuBuffer cube;
   private GpuBuffer uniforms;
   private ByteBuffer uniformData;
   private boolean invalidLogged;
   private float time;
   private long lastNanos;

   private static RenderPipeline pipeline() {
      if (pipeline == null) {
         pipeline = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withLocation(Identifier.fromNamespaceAndPath("swiftclient", "pipeline/sky_theme"))
            .withVertexShader(Identifier.fromNamespaceAndPath("swiftclient", "core/sky_theme"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("swiftclient", "core/sky_theme"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("SwiftSky", UniformType.UNIFORM_BUFFER).build())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.POSITION)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build();
      }

      return pipeline;
   }

   boolean render(String theme, float visibility, float brightness, float speed, float stars, float effect, float band, int low, int high, int bandColor, int zenith, int horizon) {
      int mode = THEMES.indexOf(theme);
      long now = System.nanoTime();
      float dt = this.lastNanos == 0L ? 0.0F : Math.min((float)(now - this.lastNanos) / 1.0E9F, 0.1F);
      this.lastNanos = now;
      this.time = (this.time + dt * speed) % 4096.0F;
      if (mode < 0 || visibility <= 0.002F) {
         return false;
      }

      GpuDevice device = RenderSystem.getDevice();
      RenderPipeline pipeline = pipeline();
      if (!device.precompilePipeline(pipeline).isValid()) {
         if (!this.invalidLogged) {
            this.invalidLogged = true;
            LOGGER.warn("Atmosphere sky shader did not compile; using the gradient sky");
         }

         return false;
      }

      RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
      if (target == null || target.width <= 0 || target.height <= 0) {
         return false;
      }

      if (this.cube == null) {
         this.cube = buildCube(device);
      }

      if (this.uniforms == null) {
         this.uniforms = device.createBuffer(() -> "Swift sky theme settings", 136, UNIFORM_SIZE);
         this.uniformData = BufferUtils.createByteBuffer(UNIFORM_SIZE);
      }

      this.putVec(0, (float)mode, visibility, this.time, brightness);
      this.putVec(16, stars, effect, band, 1.0F);
      this.putColor(32, low);
      this.putColor(48, high);
      this.putColor(64, bandColor);
      this.putColor(80, zenith);
      this.putColor(96, horizon);
      CommandEncoder encoder = device.createCommandEncoder();
      encoder.writeToBuffer(this.uniforms.slice(), this.uniformData);
      GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
         .writeTransform(new Matrix4f(RenderSystem.getModelViewStack()), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
      RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
      try (RenderPass pass = encoder.createRenderPass(() -> "Swift sky theme", target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
         pass.setPipeline(pipeline);
         RenderSystem.bindDefaultUniforms(pass);
         pass.setUniform("DynamicTransforms", transforms);
         pass.setUniform("SwiftSky", this.uniforms.slice());
         pass.setVertexBuffer(0, this.cube.slice());
         pass.setIndexBuffer(indices.getBuffer(CUBE_INDICES), indices.type());
         pass.drawIndexed(CUBE_INDICES, 1, 0, 0, 0);
      }

      return true;
   }

   void close() {
      if (this.cube != null) {
         this.cube.close();
      }

      if (this.uniforms != null) {
         this.uniforms.close();
      }

      this.cube = null;
      this.uniforms = null;
      this.uniformData = null;
      this.lastNanos = 0L;
      this.invalidLogged = false;
   }

   private void putVec(int offset, float x, float y, float z, float w) {
      this.uniformData.putFloat(offset, x);
      this.uniformData.putFloat(offset + 4, y);
      this.uniformData.putFloat(offset + 8, z);
      this.uniformData.putFloat(offset + 12, w);
   }

   private void putColor(int offset, int rgb) {
      this.putVec(offset, (float)(rgb >> 16 & 255) / 255.0F, (float)(rgb >> 8 & 255) / 255.0F, (float)(rgb & 255) / 255.0F, 1.0F);
   }

   private static GpuBuffer buildCube(GpuDevice device) {
      try (ByteBufferBuilder bytes = new ByteBufferBuilder(24 * DefaultVertexFormat.POSITION.getVertexSize())) {
         BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION);
         float s = 10.0F;
         face(builder, s, -s, -s, s, s, -s, s, s, s, s, -s, s);
         face(builder, -s, -s, s, -s, s, s, -s, s, -s, -s, -s, -s);
         face(builder, -s, s, -s, -s, s, s, s, s, s, s, s, -s);
         face(builder, -s, -s, s, -s, -s, -s, s, -s, -s, s, -s, s);
         face(builder, s, -s, s, s, s, s, -s, s, s, -s, -s, s);
         face(builder, -s, -s, -s, -s, s, -s, s, s, -s, s, -s, -s);
         try (MeshData mesh = builder.buildOrThrow()) {
            return device.createBuffer(() -> "Swift sky theme cube", 32, mesh.vertexBuffer());
         }
      }
   }

   private static void face(BufferBuilder builder, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
      builder.addVertex(ax, ay, az);
      builder.addVertex(bx, by, bz);
      builder.addVertex(cx, cy, cz);
      builder.addVertex(dx, dy, dz);
   }
}
