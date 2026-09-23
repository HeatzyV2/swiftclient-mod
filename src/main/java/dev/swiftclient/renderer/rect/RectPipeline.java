package dev.swiftclient.renderer.rect;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class RectPipeline {
   public static final VertexFormat FORMAT = VertexFormat.builder(0)
      .addAttribute("Position", GpuFormat.RGB32_FLOAT)
      .addAttribute("Color", GpuFormat.RGBA8_UNORM)
      .addAttribute("UV0", GpuFormat.RG32_FLOAT)
      .addAttribute("UV1", GpuFormat.RG16_SINT)
      .addAttribute("UV2", GpuFormat.RG16_SINT)
      .build();
   public static final float FIXED = 16.0F;
   public static final int FIXED_MAX = 32767;
   private static RenderPipeline pipeline;

   private RectPipeline() {
   }

   private static Snippet getGuiSnippet() {
      try {
         java.lang.reflect.Field field = RenderPipelines.class.getDeclaredField("GUI_SNIPPET");
         field.setAccessible(true);
         return (Snippet) field.get(null);
      } catch (Throwable t) {
         throw new RuntimeException("Failed to access GUI_SNIPPET", t);
      }
   }

   public static RenderPipeline get() {
      if (pipeline == null) {
         pipeline = RenderPipeline.builder(new Snippet[]{getGuiSnippet()})
            .withLocation(Identifier.fromNamespaceAndPath("swiftclient", "pipeline/rounded_rect"))
            .withVertexShader(Identifier.fromNamespaceAndPath("swiftclient", "rounded_rect"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("swiftclient", "rounded_rect"))
            .withVertexBinding(0, FORMAT)
            .build();
      }

      return pipeline;
   }
}
