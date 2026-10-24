package dev.swiftclient.mixin;

import dev.swiftclient.visuals.ShulkerTooltipModule;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Custom tooltip images are rendered by GuiGraphicsExtractor, not vanilla's held-item tooltip path. */
@Mixin(AbstractContainerScreen.class)
public abstract class ShulkerContainerTooltipMixin {
   //? if >=26.1.2 {
   @Inject(method = "showTooltipWithItemInHand", at = @At("HEAD"), cancellable = true)
   private void swiftclient$skipVanillaCustomTooltip(ItemStack item, CallbackInfoReturnable<Boolean> cir) {
      if (item.getTooltipImage().orElse(null) instanceof ShulkerTooltipModule.ShulkerTooltip) {
         cir.setReturnValue(false);
      }
   }
   //?}
}
