package dev.swiftclient.hud;

import dev.swiftclient.compat.Mc;
import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.modules.BlockOverlayModule;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Block Outline's block info: a small panel with the looked-at block's icon, its name, the mod it comes from, and
 * the tool that mines it, ticked green when the held item breaks it properly (red cross otherwise).
 */
public final class BlockInfoHud {
   private static final int BG = 0xE60B0F17;
   private static final int FRAME = 0xFF2A3A5C;
   private static final int ACCENT = 0xFF3B82F6;
   private static final int MOD_BLUE = 0xFF6FA8FF;
   private static final int OK = 0xFF3ECF8E;
   private static final int NO = 0xFFE05A5A;

   private BlockInfoHud() {
   }

   public static void render(Canvas c, int screenW, int screenH) {
      BlockOverlayModule module = ModuleManager.get(BlockOverlayModule.class);
      Minecraft mc = Minecraft.getInstance();
      if (module == null || !module.isEnabled() || !module.info.boolValue() || Mc.screen() != null || mc.level == null || mc.player == null) {
         return;
      }

      HitResult hit = mc.hitResult;
      if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
         return;
      }

      BlockPos pos = blockHit.getBlockPos();
      BlockState state = mc.level.getBlockState(pos);
      if (state.isAir()) {
         return;
      }

      ItemStack icon = new ItemStack(state.getBlock().asItem());
      String name = state.getBlock().getName().getString();
      String mod = modName(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
      ItemStack tool = module.infoTool.boolValue() ? toolFor(state) : ItemStack.EMPTY;
      boolean harvest = !state.requiresCorrectToolForDrops() || mc.player.hasCorrectToolForDrops(state);

      int textW = Math.max(c.textWidth(name), c.textWidth(mod));
      int hasIcon = icon.isEmpty() ? 0 : 22;
      int toolW = tool.isEmpty() ? 0 : 26;
      int w = 8 + hasIcon + textW + toolW + 10;
      int h = 30;
      int x = (screenW - w) / 2;
      int y = module.infoPosition.cycleIndex() == 1 ? screenH / 2 + 14 : 6;

      c.card(x, y, w, h, BG, FRAME, 1, 4.0F);
      c.fill(x + 1, y + 6, x + 3, y + h - 6, ACCENT);
      int tx = x + 9;
      if (hasIcon > 0) {
         c.itemStack(icon, tx, y + 7);
         tx += hasIcon;
      }

      c.text(name, tx, y + 6, 0xFFFFFFFF, true);
      c.text(mod, tx, y + 17, MOD_BLUE, false);
      if (!tool.isEmpty()) {
         int ix = x + w - 8 - 20;
         c.itemStack(tool, ix, y + 7);
         drawMark(c, ix + 12, y + 17, harvest);
      }
   }

   /** Green tick (the held item breaks the block properly) or red cross, drawn with small fills. */
   private static void drawMark(Canvas c, int x, int y, boolean ok) {
      int col = ok ? OK : NO;
      if (ok) {
         c.fill(x, y + 3, x + 2, y + 5, col);
         c.fill(x + 2, y + 5, x + 4, y + 7, col);
         c.fill(x + 4, y + 3, x + 6, y + 5, col);
         c.fill(x + 6, y + 1, x + 8, y + 3, col);
      } else {
         for (int i = 0; i < 6; i++) {
            c.fill(x + i, y + i, x + i + 2, y + i + 2, col);
            c.fill(x + 5 - i, y + i, x + 7 - i, y + i + 2, col);
         }
      }
   }

   /** The vanilla tool that mines the block, or nothing when any hand works. */
   private static ItemStack toolFor(BlockState state) {
      if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
         return new ItemStack(Items.IRON_PICKAXE);
      } else if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
         return new ItemStack(Items.IRON_AXE);
      } else if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
         return new ItemStack(Items.IRON_SHOVEL);
      } else if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
         return new ItemStack(Items.IRON_HOE);
      } else {
         return ItemStack.EMPTY;
      }
   }

   private static String modName(String namespace) {
      if ("minecraft".equals(namespace)) {
         return "Minecraft";
      }

      return FabricLoader.getInstance().getModContainer(namespace).map(m -> m.getMetadata().getName()).orElse(namespace);
   }
}
