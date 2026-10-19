package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One key copies the coordinates of the block you look at (or stand on) to the clipboard. */
@ConsumedBy("KeyboardHandlerMixin")
public final class CoordSnapperModule extends Module {
   private static final double STEP_PAST_BLOCK = 0.001;
   public final ModuleSetting key;
   public final ModuleSetting target;
   public final ModuleSetting range;
   public final ModuleSetting stopAtFluids;
   public final ModuleSetting format;
   public final ModuleSetting dimension;
   public final ModuleSetting confirm;

   public CoordSnapperModule() {
      super("coord_snapper", "Coord Snapper", "Copies the coordinates of the block you look at, or of your position, with one key.", "World", "copy", false);
      this.key = this.key("key", "Copy key", -1).desc("Press it in game to copy the coordinates. Pick a key first: none is set by default.");
      this.target = this.cycle("target", "Target", new String[]{"Looked at", "Own position"}, 0).desc("The block under your crosshair, or the block you stand on.");
      this.range = this.slider("range", "Range", 128.0, 16.0, 256.0, 8.0, " blocks").desc("How far the looked-at block can be.");
      this.stopAtFluids = this.toggle("stop_at_fluids", "Stop at fluids", false).desc("Water and lava count as a block when you look at them.");
      this.format = this.cycle("format", "Format", new String[]{"Spaces", "Commas", "/tp command"}, 0).desc("Spaces: 12 64 -30. Commas: 12, 64, -30. /tp command: ready to paste in chat.");
      this.dimension = this.toggle("dimension", "Include dimension", true).desc("Add the dimension (Overworld, Nether, End).");
      this.confirm = this.toggle("confirm", "Confirmation", true).desc("Show what was copied above the hotbar.");
   }

   /** A key went down (KeyboardHandlerMixin). Never consumes it: the key keeps its vanilla action. */
   public static boolean onKey(int key, int action) {
      if (action != 1 || !ModuleManager.isLoaded()) {
         return false;
      }

      CoordSnapperModule m = ModuleManager.get(CoordSnapperModule.class);
      int bound = m.key.keyCode();
      if (!m.isEnabled() || bound < 0 || key != bound) {
         return false;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         m.snap(mc, mc.player, mc.level);
      }

      return false;
   }

   private void snap(Minecraft mc, LocalPlayer player, ClientLevel level) {
      BlockPos pos;
      if (this.target.cycleIndex() == 1) {
         pos = player.blockPosition();
      } else {
         pos = this.raycast(player, level);
         if (pos == null) {
            Minecraft.getInstance().gui.hud.setOverlayMessage(Component.literal("Nothing copied: no block within " + this.range.intValue() + " blocks of your aim"), false);
            return;
         }

         if (this.format.cycleIndex() == 2) {
            pos = pos.above();
         }
      }

      String text = this.formatCoords(pos, level.dimension());
      mc.keyboardHandler.setClipboard(text);
      if (this.confirm.boolValue()) {
         Minecraft.getInstance().gui.hud.setOverlayMessage(Component.literal("Copied " + text), false);
      }
   }

   private BlockPos raycast(LocalPlayer player, ClientLevel level) {
      Vec3 origin = player.getEyePosition(1.0F);
      Vec3 direction = player.getViewVector(1.0F);
      Vec3 end = origin.add(direction.scale(this.range.value()));
      ClipContext.Fluid fluids = this.stopAtFluids.boolValue() ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
      ClipContext context = new ClipContext(origin, end, ClipContext.Block.OUTLINE, fluids, player);
      BlockPos viewBlock = BlockPos.containing(origin);
      if (startsInside(context, level, viewBlock, origin)) {
         // The eye is inside a block (e.g. a slab or water): start the ray where it leaves that block.
         Optional<Vec3> exit = new AABB(viewBlock).clip(end, origin);
         if (exit.isEmpty()) {
            return null;
         }

         context = new ClipContext(exit.get().add(direction.scale(STEP_PAST_BLOCK)), end, ClipContext.Block.OUTLINE, fluids, player);
      }

      BlockHitResult hit = level.clip(context);
      return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
   }

   private static boolean startsInside(ClipContext context, ClientLevel level, BlockPos pos, Vec3 point) {
      return contains(context.getBlockShape(level.getBlockState(pos), level, pos), pos, point)
         || contains(context.getFluidShape(level.getFluidState(pos), level, pos), pos, point);
   }

   private static boolean contains(VoxelShape shape, BlockPos pos, Vec3 point) {
      for (AABB box : shape.toAabbs()) {
         if (box.move(pos).contains(point)) {
            return true;
         }
      }

      return false;
   }

   private String formatCoords(BlockPos pos, ResourceKey<Level> dim) {
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      boolean withDimension = this.dimension.boolValue();
      return switch (this.format.cycleIndex()) {
         case 2 -> withDimension
            ? String.format(Locale.ROOT, "/execute in %s run tp @s %d %d %d", dim.identifier(), x, y, z)
            : String.format(Locale.ROOT, "/tp %d %d %d", x, y, z);
         case 1 -> String.format(Locale.ROOT, "%d, %d, %d", x, y, z) + suffix(dim, withDimension);
         default -> String.format(Locale.ROOT, "%d %d %d", x, y, z) + suffix(dim, withDimension);
      };
   }

   private static String suffix(ResourceKey<Level> dim, boolean enabled) {
      if (!enabled) {
         return "";
      }

      String name = Level.OVERWORLD.equals(dim) ? "Overworld" : Level.NETHER.equals(dim) ? "Nether" : Level.END.equals(dim) ? "End" : dim.identifier().toString();
      return " (" + name + ")";
   }
}
