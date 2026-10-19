package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.swiftclient.modules.HideInventoryModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hide Inventory: fake items in your own inventory slots, the carried stack and no tooltips on them. With
 * Stream only the real ones are handed to the overlay that only you see.
 */
@Mixin({AbstractContainerScreen.class})
public abstract class HideInventoryScreenMixin {
   @Shadow
   protected Slot hoveredSlot;
   @Shadow
   protected int leftPos;
   @Shadow
   protected int topPos;
   @Shadow
   @Final
   protected AbstractContainerMenu menu;

   @WrapOperation(
      method = {"extractSlot"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"
      )}
   )
   private ItemStack swiftclient$hideSlot(Slot slot, Operation<ItemStack> original) {
      return HideInventoryModule.container(original.call(slot), slot, this.leftPos + slot.x, this.topPos + slot.y, slot == this.hoveredSlot);
   }

   @ModifyVariable(
      method = {"extractFloatingItem"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private ItemStack swiftclient$hideCarried(ItemStack stack, @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y) {
      return HideInventoryModule.carried(stack, x, y);
   }

   @Inject(
      method = {"extractTooltip"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$hideTooltip(GuiGraphicsExtractor graphics, int x, int y, CallbackInfo ci) {
      if (HideInventoryModule.hidesTooltips() && HideInventoryModule.ownSlot(this.hoveredSlot)) {
         if (this.hoveredSlot.hasItem() && this.menu.getCarried().isEmpty()) {
            HideInventoryModule.hiddenTooltip(this.hoveredSlot.getItem(), x, y);
         }

         ci.cancel();
      }
   }
}
