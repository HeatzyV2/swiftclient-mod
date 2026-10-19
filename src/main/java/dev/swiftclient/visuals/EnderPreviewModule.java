package dev.swiftclient.visuals;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Hover an Ender Chest item to see what your Ender Chest held the last time you opened it. The server only
 * sends the contents while the chest is open, so the preview is a snapshot taken then.
 */
@ConsumedBy("ShulkerTooltipMixin")
public final class EnderPreviewModule extends Module {
   public static volatile EnderPreviewModule active;
   private final NonNullList<ItemStack> snapshot = NonNullList.withSize(ShulkerTooltipModule.SLOTS, ItemStack.EMPTY);
   private boolean known;
   private ClientLevel lastLevel;

   public EnderPreviewModule() {
      super("ender_preview", "Ender Chest Preview", "Hover an Ender Chest to see what it held the last time you opened it.", "Visual", "stack", false);
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
      this.reset();
   }

   private void reset() {
      this.known = false;
      for (int i = 0; i < this.snapshot.size(); i++) {
         this.snapshot.set(i, ItemStack.EMPTY);
      }
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null || mc.level == null) {
         this.reset();
         return;
      }

      if (mc.level != this.lastLevel) {
         this.lastLevel = mc.level;
         this.reset();
      }

      if (player.containerMenu instanceof ChestMenu menu && menu.getContainer() == player.getEnderChestInventory()) {
         for (int i = 0; i < ShulkerTooltipModule.SLOTS; i++) {
            this.snapshot.set(i, menu.getContainer().getItem(i).copy());
         }

         this.known = true;
      }
   }

   public static Optional<TooltipComponent> preview(ItemStack stack) {
      EnderPreviewModule module = active;
      if (module == null || !module.known || !stack.is(Items.ENDER_CHEST)) {
         return Optional.empty();
      }

      NonNullList<ItemStack> copy = NonNullList.withSize(ShulkerTooltipModule.SLOTS, ItemStack.EMPTY);
      for (int i = 0; i < copy.size(); i++) {
         copy.set(i, module.snapshot.get(i));
      }

      return Optional.of(new ShulkerTooltipModule.ShulkerTooltip(copy, DyeColor.PURPLE));
   }
}
