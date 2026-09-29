package dev.swiftclient.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.world.WorldOverlays;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Marks the blocks where mobs can spawn: block light 0 on a solid top face with room above.
 * Red: spawns any time. Yellow: only at night (the sky reaches it).
 */
public final class LightOverlayModule extends Module implements WorldOverlays.WorldOverlay {
   private static final int SCAN_EVERY_TICKS = 10;
   public final ModuleSetting range;
   public final ModuleSetting nightOnly;
   private List<long[]> marks = List.of();
   private int ticks;

   public LightOverlayModule() {
      super("light_overlay", "Light Overlay", "Marks the blocks where mobs can spawn: red always, yellow at night.", "PvP", "fullbright", false);
      this.range = this.slider("range", "Range", 16.0, 8.0, 32.0, 4.0, "").desc("How many blocks around you are checked.");
      this.nightOnly = this.toggle("night", "Show night spawns", true).desc("Also mark the blocks lit by the sky only (yellow), where mobs spawn at night.");
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null) {
         this.marks = List.of();
      } else if (++this.ticks >= SCAN_EVERY_TICKS) {
         this.ticks = 0;
         this.marks = scan(mc.level, mc.player.blockPosition(), (int)Math.round(this.range.value()), this.nightOnly.boolValue());
      }
   }

   @Override
   protected void onDisable() {
      this.marks = List.of();
   }

   /** {x, y, z, skyLit} of each spawnable block top. */
   private static List<long[]> scan(ClientLevel level, BlockPos centre, int r, boolean withNight) {
      List<long[]> out = new ArrayList<>();
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();

      for (int dx = -r; dx <= r; dx++) {
         for (int dz = -r; dz <= r; dz++) {
            for (int dy = -8; dy <= 6; dy++) {
               pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
               BlockState here = level.getBlockState(pos);
               if (!here.getCollisionShape(level, pos).isEmpty() || !here.getFluidState().isEmpty()) {
                  continue;
               }

               below.set(pos.getX(), pos.getY() - 1, pos.getZ());
               BlockState ground = level.getBlockState(below);
               if (!ground.isFaceSturdy(level, below, Direction.UP) || !ground.getFluidState().isEmpty()) {
                  continue;
               }

               if (level.getBrightness(LightLayer.BLOCK, pos) == 0) {
                  boolean sky = level.getBrightness(LightLayer.SKY, pos) > 0;
                  if (!sky || withNight) {
                     out.add(new long[]{pos.getX(), pos.getY(), pos.getZ(), sky ? 1L : 0L});
                  }
               }
            }
         }
      }

      return out;
   }

   @Override
   public void emitGizmos(Minecraft mc, Vec3 camera, float partialTick) {
      for (long[] m : this.marks) {
         int color = m[3] == 0L ? 0x66FF3B3B : 0x55F2C94C;
         double x = m[0];
         double y = m[1] + 0.01;
         double z = m[2];
         Gizmos.cuboid(new AABB(x + 0.1, y, z + 0.1, x + 0.9, y + 0.02, z + 0.9), GizmoStyle.fill(color));
      }
   }
}
