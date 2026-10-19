package dev.swiftclient.visuals;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/** Extra facts in every item tooltip: durability in percent, food value, stack size. */
@ConsumedBy("TooltipPlusMixin")
public final class TooltipPlusModule extends Module {
   public static volatile TooltipPlusModule active;
   private final ModuleSetting durability = this.toggle("durability", "Durability", true).desc("Durability left, in percent.");
   private final ModuleSetting food = this.toggle("food", "Food value", true).desc("Hunger and saturation restored by food.");
   private final ModuleSetting stack = this.toggle("stack", "Stack size", false).desc("Count and maximum stack size.");

   public TooltipPlusModule() {
      super("tooltip_plus", "Tooltip+", "Durability in percent, food value and stack size in every item tooltip.", "Visual", "info", false);
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   public static void append(ItemStack item, List<Component> lines) {
      TooltipPlusModule module = active;
      if (module == null || item.isEmpty()) {
         return;
      }

      if (module.durability.boolValue() && item.isDamageableItem()) {
         int max = item.getMaxDamage();
         int left = max - item.getDamageValue();
         int percent = Math.round(100.0F * left / max);
         ChatFormatting color = percent > 50 ? ChatFormatting.GREEN : (percent > 20 ? ChatFormatting.YELLOW : ChatFormatting.RED);
         lines.add(Component.literal("Durability: " + percent + "% (" + left + "/" + max + ")").withStyle(color));
      }

      FoodProperties food = item.get(DataComponents.FOOD);
      if (module.food.boolValue() && food != null) {
         lines.add(Component.literal("Food: +" + food.nutrition() + " hunger, " + String.format("%.1f", food.saturation()) + " saturation").withStyle(ChatFormatting.GOLD));
      }

      if (module.stack.boolValue() && item.getMaxStackSize() > 1) {
         lines.add(Component.literal("Stack: " + item.getCount() + "/" + item.getMaxStackSize()).withStyle(ChatFormatting.GRAY));
      }
   }
}
