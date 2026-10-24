package dev.swiftclient.compat;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;

/** Custom-pass changes at the RenderPearl boundary. */
public final class Gpu {
   private Gpu() {}

   public static void setPipeline(RenderPass pass, RenderPipeline pipeline) {
      //? if >=26.3 {
      /*pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
      *///?} else
      pass.setPipeline(pipeline);
   }

   public static boolean compiled(RenderPipeline pipeline) {
      //? if >=26.3 {
      /*return RenderSystem.getCompiledPipelineNullable(pipeline) != null;
      *///?} else
      return RenderSystem.getDevice().precompilePipeline(pipeline).isValid();
   }

   public static TextureTarget target(String name, int width, int height, boolean depth, GpuFormat format) {
      //? if >=26.3 {
      /*return new TextureTarget(name, width, height, format, depth ? GpuFormat.D32_FLOAT : null);
      *///?} else
      return new TextureTarget(name, width, height, depth, format);
   }
}

