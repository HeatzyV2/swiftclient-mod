package dev.swiftclient.visuals;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** Hover a shulker box to see what is inside as a grid, tinted with the colour of the box. */
@ConsumedBy("ShulkerTooltipMixin")
public final class ShulkerTooltipModule extends Module {
   public static volatile ShulkerTooltipModule active;
   public static final int SLOTS = 27;
   public final ModuleSetting shiftOnly;
   public final ModuleSetting tint;
   public final ModuleSetting hideList;
   public final ModuleSetting emptyBoxes;

   public ShulkerTooltipModule() {
      super("shulker_tooltip", "Shulker Tooltip", "Hover a shulker box to preview its contents as a grid, in the colour of the box.", "Visual", "grid", false);
      this.shiftOnly = this.toggle("shift_only", "Only while holding Shift", false).desc("Show the preview only while Shift is held.");
      this.tint = this.toggle("tint", "Colour of the box", true).desc("Tint the preview with the dye colour of the shulker box.");
      this.hideList = this.toggle("hide_list", "Hide text list", true).desc("Removes the vanilla text lines listing the first items of the box.");
      this.emptyBoxes = this.toggle("empty_boxes", "Show empty boxes", false).desc("Also preview boxes that contain nothing.");
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   public static boolean isShulker(ItemStack stack) {
      return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock;
   }

   private static boolean shown(ShulkerTooltipModule module) {
      return !module.shiftOnly.boolValue() || dev.swiftclient.compat.Input.shiftDown();
   }

   /** The preview for this stack, or empty when the module is off or the stack is not a (visible) shulker box. */
   public static Optional<TooltipComponent> preview(ItemStack stack) {
      ShulkerTooltipModule module = active;
      if (module == null || !isShulker(stack) || !shown(module)) {
         return Optional.empty();
      }

      ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
      boolean empty = contents == null || contents.nonEmptyItemCopyStream().findAny().isEmpty();
      if (empty && !module.emptyBoxes.boolValue()) {
         return Optional.empty();
      }

      NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
      if (contents != null) {
         contents.copyInto(items);
      }

      DyeColor color = ((ShulkerBoxBlock)((BlockItem)stack.getItem()).getBlock()).getColor();
      return Optional.of(new ShulkerTooltip(items, module.tint.boolValue() ? color : null));
   }

   public static boolean hidesList(ItemStack stack) {
      ShulkerTooltipModule module = active;
      return module != null && module.hideList.boolValue() && isShulker(stack) && shown(module);
   }

   public record ShulkerTooltip(NonNullList<ItemStack> items, DyeColor color) implements TooltipComponent {
   }
}
