package dev.swiftclient.visuals;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

/** Draws the 9x3 grid of a shulker box inside its tooltip. */
public final class ShulkerClientTooltip implements ClientTooltipComponent {
   private static final int COLUMNS = 9;
   private static final int ROWS = 3;
   private static final int CELL = 18;
   private static final int PAD = 2;
   private final ShulkerTooltipModule.ShulkerTooltip tip;

   public ShulkerClientTooltip(ShulkerTooltipModule.ShulkerTooltip tip) {
      this.tip = tip;
   }

   @Override
   public int getWidth(Font font) {
      return COLUMNS * CELL + PAD * 2;
   }

   @Override
   public int getHeight(Font font) {
      return ROWS * CELL + PAD * 2 + 2;
   }

   @Override
   public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
      int base = this.tip.color() != null ? this.tip.color().getTextureDiffuseColor() : 0x8B6BA8;
      int panel = 0xFF000000 | darker(base, 0.55F);
      int cell = 0xFF000000 | darker(base, 0.32F);
      int edge = 0xFF000000 | darker(base, 0.8F);
      int width = this.getWidth(font);
      int height = ROWS * CELL + PAD * 2;
      int left = x + (w - width) / 2;
      int top = y + 2;
      graphics.fill(left, top, left + width, top + height, panel);
      graphics.fill(left, top, left + width, top + 1, edge);
      graphics.fill(left, top + height - 1, left + width, top + height, edge);
      graphics.fill(left, top + 1, left + 1, top + height - 1, edge);
      graphics.fill(left + width - 1, top + 1, left + width, top + height - 1, edge);
      for (int i = 0; i < ShulkerTooltipModule.SLOTS; i++) {
         int cx = left + PAD + i % COLUMNS * CELL;
         int cy = top + PAD + i / COLUMNS * CELL;
         graphics.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, cell);
         ItemStack stack = this.tip.items().get(i);
         if (!stack.isEmpty()) {
            graphics.item(stack, cx + 1, cy + 1, i);
            graphics.itemDecorations(font, stack, cx + 1, cy + 1);
         }
      }
   }

   private static int darker(int rgb, float factor) {
      int r = (int)(((rgb >> 16) & 0xFF) * factor);
      int g = (int)(((rgb >> 8) & 0xFF) * factor);
      int b = (int)((rgb & 0xFF) * factor);
      return r << 16 | g << 8 | b;
   }
}
