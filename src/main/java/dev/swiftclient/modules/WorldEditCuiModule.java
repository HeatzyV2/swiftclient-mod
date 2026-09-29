package dev.swiftclient.modules;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.world.WorldEditCui;
import dev.swiftclient.world.WorldOverlays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shows your WorldEdit selection (cuboid or polygon) on servers running WorldEdit. */
public final class WorldEditCuiModule extends Module implements WorldOverlays.WorldOverlay {
   public final ModuleSetting color;
   public final ModuleSetting fill;

   public WorldEditCuiModule() {
      super("worldedit_cui", "WorldEdit CUI", "Shows your WorldEdit selection on servers running WorldEdit (wand, //pos1, //pos2).", "World", "worldedit", false);
      this.color = this.color("color", "Selection color", 0xFFFF6B6B).desc("Colour of the selection edges.");
      this.fill = this.toggle("fill", "Fill faces", false).desc("Tint the inside of the selection lightly.");
   }

   @Override
   protected void onEnable() {
      WorldEditCui.setEnabled(true);
   }

   @Override
   protected void onDisable() {
      WorldEditCui.setEnabled(false);
   }

   @Override
   public void emitGizmos(Minecraft mc, Vec3 camera, float partialTick) {
      int col = this.color.colorValue();
      String shape = WorldEditCui.shape();
      if (shape.equals("polygon2d")) {
         List<int[]> pts = WorldEditCui.polygon();
         int[] h = WorldEditCui.polygonHeight();
         for (int i = 0; i < pts.size(); i++) {
            int[] a = pts.get(i);
            int[] b = pts.get((i + 1) % pts.size());
            for (int y : new int[]{h[0], h[1] + 1}) {
               Gizmos.line(new Vec3(a[0] + 0.5, y, a[1] + 0.5), new Vec3(b[0] + 0.5, y, b[1] + 0.5), col, 2.5F);
            }

            Gizmos.line(new Vec3(a[0] + 0.5, h[0], a[1] + 0.5), new Vec3(a[0] + 0.5, h[1] + 1, a[1] + 0.5), col, 2.5F);
         }
      } else {
         BlockPos[] c = WorldEditCui.cuboid();
         if (c[0] != null) {
            Gizmos.cuboid(c[0], 0.002F, GizmoStyle.stroke(0xFF3BE07A, 2.0F));
         }

         if (c[1] != null) {
            Gizmos.cuboid(c[1], 0.002F, GizmoStyle.stroke(0xFF3B82F6, 2.0F));
         }

         if (c[0] != null && c[1] != null) {
            AABB box = new AABB(
               Math.min(c[0].getX(), c[1].getX()), Math.min(c[0].getY(), c[1].getY()), Math.min(c[0].getZ(), c[1].getZ()),
               Math.max(c[0].getX(), c[1].getX()) + 1, Math.max(c[0].getY(), c[1].getY()) + 1, Math.max(c[0].getZ(), c[1].getZ()) + 1
            );
            GizmoStyle style = this.fill.boolValue() ? GizmoStyle.strokeAndFill(col, 3.0F, col & 0x00FFFFFF | 0x22000000) : GizmoStyle.stroke(col, 3.0F);
            Gizmos.cuboid(box, style);
         }
      }
   }
}
