package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.world.WorldOverlays;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

/** Outlines the chunk you stand in (and the corners of its neighbours), without opening F3. */
@ConsumedBy("WorldOverlays")
public final class ChunkBordersModule extends Module implements WorldOverlays.WorldOverlay {
   public final ModuleSetting color;
   public final ModuleSetting grid;
   public final ModuleSetting neighbours;
   public final ModuleSetting height;

   public ChunkBordersModule() {
      super("chunk_borders", "Chunk Borders", "Outlines the chunk you are standing in, without opening F3.", "World", "grid", false);
      this.color = this.color("color", "Border color", 0xFFF2C94C).desc("Colour of the walls of your chunk.");
      this.grid = this.toggle("grid", "Block grid", true).desc("Mark every 2 blocks on the walls, to count easily.");
      this.neighbours = this.toggle("neighbours", "Neighbour corners", true).desc("Also draw the corners of the chunks around yours.");
      this.height = this.slider("height", "Height", 32.0, 8.0, 128.0, 8.0, "").desc("How many blocks above and below you the walls reach.");
   }

   @Override
   public void emitGizmos(Minecraft mc, Vec3 camera, float partialTick) {
      int cx = mc.player.getBlockX() >> 4 << 4;
      int cz = mc.player.getBlockZ() >> 4 << 4;
      int h = (int)Math.round(this.height.value());
      int y0 = Math.max(mc.level.getMinY(), mc.player.getBlockY() - h);
      int y1 = Math.min(mc.level.getMaxY() + 1, mc.player.getBlockY() + h);
      int col = this.color.colorValue();
      int faint = col & 0x00FFFFFF | 0x66000000;

      for (int x = 0; x <= 16; x += 16) {
         for (int z = 0; z <= 16; z += 16) {
            Gizmos.line(new Vec3(cx + x, y0, cz + z), new Vec3(cx + x, y1, cz + z), col, 3.0F);
         }
      }

      if (this.grid.boolValue()) {
         for (int i = 2; i < 16; i += 2) {
            for (int side = 0; side <= 16; side += 16) {
               Gizmos.line(new Vec3(cx + i, y0, cz + side), new Vec3(cx + i, y1, cz + side), faint, 1.0F);
               Gizmos.line(new Vec3(cx + side, y0, cz + i), new Vec3(cx + side, y1, cz + i), faint, 1.0F);
            }
         }

         int start = y0 + Math.floorMod(-y0, 2);
         for (int y = start; y <= y1; y += 2) {
            Gizmos.line(new Vec3(cx, y, cz), new Vec3(cx + 16, y, cz), faint, 1.0F);
            Gizmos.line(new Vec3(cx, y, cz + 16), new Vec3(cx + 16, y, cz + 16), faint, 1.0F);
            Gizmos.line(new Vec3(cx, y, cz), new Vec3(cx, y, cz + 16), faint, 1.0F);
            Gizmos.line(new Vec3(cx + 16, y, cz), new Vec3(cx + 16, y, cz + 16), faint, 1.0F);
         }
      }

      if (this.neighbours.boolValue()) {
         int n = 0x994DD0E1;
         for (int x = -16; x <= 32; x += 16) {
            for (int z = -16; z <= 32; z += 16) {
               if ((x == -16 || x == 32) || (z == -16 || z == 32)) {
                  Gizmos.line(new Vec3(cx + x, y0, cz + z), new Vec3(cx + x, y1, cz + z), n, 2.0F);
               }
            }
         }
      }
   }
}
