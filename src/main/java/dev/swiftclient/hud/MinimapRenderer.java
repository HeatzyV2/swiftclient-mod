package dev.swiftclient.hud;

import com.mojang.blaze3d.platform.NativeImage;
import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.world.Waypoints;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Builds the minimap image: one pixel per block column (top block colour, shaded by the height difference
 * with its northern neighbour, like vanilla maps). Rebuilt at most 10 times per second.
 */
final class MinimapRenderer {
   static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("swiftclient", "dynamic/minimap");
   private static final long REFRESH_MS = 100L;
   private static DynamicTexture texture;
   private static int size;
   private static long lastBuild;
   private static List<HudData.MapMarker> markers = List.of();

   private MinimapRenderer() {
   }

   static HudData.Minimap get(int radius, boolean rotate) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      ClientLevel level = mc.level;
      if (p == null || level == null) {
         return null;
      }

      int want = Math.max(16, Math.min(192, radius * 2));
      if (texture == null || size != want) {
         if (texture != null) {
            texture.close();
         }

         size = want;
         texture = new DynamicTexture(() -> "Swift minimap", size, size, true);
         mc.getTextureManager().register(TEXTURE, texture);
         lastBuild = 0L;
      }

      long now = System.currentTimeMillis();
      if (now - lastBuild >= REFRESH_MS) {
         lastBuild = now;
         build(level, p, rotate);
      }

      return new HudData.Minimap(TEXTURE, size, markers);
   }

   private static void build(ClientLevel level, LocalPlayer p, boolean rotate) {
      NativeImage img = texture.getPixels();
      if (img == null) {
         return;
      }

      int r = size / 2;
      double px = p.getX();
      double pz = p.getZ();
      double yaw = Math.toRadians(p.getYRot());
      // forward (where you look) and right, on the ground plane
      double fx = -Math.sin(yaw);
      double fz = Math.cos(yaw);
      double rx = -Math.cos(yaw);
      double rz = -Math.sin(yaw);
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int iy = 0; iy < size; iy++) {
         for (int ix = 0; ix < size; ix++) {
            double dx = ix - r + 0.5;
            double dy = iy - r + 0.5;
            int wx;
            int wz;
            if (rotate) {
               wx = (int)Math.floor(px + rx * dx + fx * -dy);
               wz = (int)Math.floor(pz + rz * dx + fz * -dy);
            } else {
               wx = (int)Math.floor(px + dx);
               wz = (int)Math.floor(pz + dy);
            }

            img.setPixel(ix, iy, color(level, pos, wx, wz));
         }
      }

      texture.upload();
      List<HudData.MapMarker> out = new ArrayList<>();

      for (AbstractClientPlayer other : level.players()) {
         if (other != p && !other.isInvisible()) {
            float[] d = project(other.getX() - px, other.getZ() - pz, rotate, fx, fz, rx, rz);
            out.add(new HudData.MapMarker(d[0], d[1], 0xFFFFFFFF, false));
         }
      }

      for (Waypoints.Waypoint w : Waypoints.visibleHere()) {
         float[] d = project(w.x() + 0.5 - px, w.z() + 0.5 - pz, rotate, fx, fz, rx, rz);
         out.add(new HudData.MapMarker(d[0], d[1], w.color(), true));
      }

      markers = out;
   }

   /** World offset to map pixels (1 block = 1 pixel), same transform as the image. */
   private static float[] project(double ox, double oz, boolean rotate, double fx, double fz, double rx, double rz) {
      if (!rotate) {
         return new float[]{(float)ox, (float)oz};
      } else {
         double right = ox * rx + oz * rz;
         double fwd = ox * fx + oz * fz;
         return new float[]{(float)right, (float)-fwd};
      }
   }

   private static int color(ClientLevel level, BlockPos.MutableBlockPos pos, int x, int z) {
      if (!level.hasChunk(x >> 4, z >> 4)) {
         return 0xFF101318;
      }

      int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
      pos.set(x, h, z);
      BlockState state = level.getBlockState(pos);
      MapColor mc = state.getMapColor(level, pos);
      if (mc == MapColor.NONE) {
         return 0xFF101318;
      }

      int north = level.hasChunk(x >> 4, (z - 1) >> 4) ? level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z - 1) - 1 : h;
      MapColor.Brightness b = h > north ? MapColor.Brightness.HIGH : (h < north ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL);
      return 0xFF000000 | mc.calculateARGBColor(b);
   }
}
