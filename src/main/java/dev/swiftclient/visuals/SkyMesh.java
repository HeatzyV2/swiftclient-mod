package dev.swiftclient.visuals;

import dev.swiftclient.compat.Mc;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import net.minecraft.client.renderer.BindGroupLayouts;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.OptionalDouble;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

final class SkyMesh {
   static final RenderPipeline GLOW;
   static final RenderPipeline PAINT;
   private static final int VERTEX_SIZE;
   private final String label;
   private final RenderPipeline pipeline;
   private final int maxQuads;
   private ByteBufferBuilder bytes;
   private GpuBuffer buffer;
   private int indexCount;
   private boolean built;

   SkyMesh(String label, RenderPipeline pipeline, int maxQuads) {
      this.label = label;
      this.pipeline = pipeline;
      this.maxQuads = maxQuads;
   }

   private static RenderPipeline pipeline(String name, BlendFunction blend) {
      return RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
         .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
         .withLocation(Identifier.fromNamespaceAndPath("swiftclient", "pipeline/" + name))
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withColorTargetState(blend == null ? ColorTargetState.DEFAULT : new ColorTargetState(blend))
         .withCull(false)
         .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
         .withPrimitiveTopology(PrimitiveTopology.QUADS)
         .build();
   }

   BufferBuilder begin() {
      if (this.bytes == null) {
         this.bytes = new ByteBufferBuilder(this.maxQuads * 4 * VERTEX_SIZE);
      }

      return new BufferBuilder(this.bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
   }

   void upload(BufferBuilder builder) {
      this.built = true;
      MeshData mesh = builder.build();

      label47: {
         try {
            if (mesh == null) {
               this.indexCount = 0;
               break label47;
            }

            if (this.buffer == null) {
               this.buffer = RenderSystem.getDevice().createBuffer(() -> this.label, 40, (long)this.maxQuads * 4L * (long)VERTEX_SIZE);
            }

            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(0L, (long)mesh.vertexBuffer().remaining()), mesh.vertexBuffer());
            this.indexCount = mesh.drawState().indexCount();
         } catch (Throwable var6) {
            if (mesh != null) {
               try {
                  mesh.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (mesh != null) {
            mesh.close();
         }

         return;
      }

      if (mesh != null) {
         mesh.close();
      }

   }

   boolean isBuilt() {
      return this.built;
   }

   void draw(PoseStack pose, float alpha) {
      if (this.indexCount != 0 && !(alpha <= 0.002F)) {
         RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
         Matrix4fStack modelView = RenderSystem.getModelViewStack();
         modelView.pushMatrix();

         try {
            modelView.mul(pose.last().pose());
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(modelView), new Vector4f(1.0F, 1.0F, 1.0F, alpha), new Vector3f(), new Matrix4f());
            RenderTarget target = Mc.mainTarget();
            RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> this.label, target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty());

            try {
               dev.swiftclient.compat.Gpu.setPipeline(pass, this.pipeline);
               RenderSystem.bindDefaultUniforms(pass);
               pass.setUniform("DynamicTransforms", transforms);
               pass.setVertexBuffer(0, this.buffer.slice());
               pass.setIndexBuffer(indices.getBuffer(this.indexCount), indices.type());
               pass.drawIndexed(this.indexCount, 1, 0, 0, 0);
            } catch (Throwable var15) {
               if (pass != null) {
                  try {
                     pass.close();
                  } catch (Throwable var14) {
                     var15.addSuppressed(var14);
                  }
               }

               throw var15;
            }

            if (pass != null) {
               pass.close();
            }
         } finally {
            modelView.popMatrix();
         }

      }
   }

   void close() {
      if (this.buffer != null) {
         this.buffer.close();
      }

      if (this.bytes != null) {
         this.bytes.close();
      }

      this.buffer = null;
      this.bytes = null;
      this.indexCount = 0;
      this.built = false;
   }

   static void vertex(BufferBuilder builder, float x, float y, float z, int argb) {
      builder.addVertex(x, y, z).setColor(argb);
   }

   static {
      GLOW = pipeline("sky_glow", BlendFunction.LIGHTNING);
      PAINT = pipeline("sky_paint", (BlendFunction)null);
      VERTEX_SIZE = DefaultVertexFormat.POSITION_COLOR.getVertexSize();
   }
}
