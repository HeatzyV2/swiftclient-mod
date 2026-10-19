package dev.swiftclient.mixin;

import dev.swiftclient.visuals.EnderPreviewModule;
import dev.swiftclient.visuals.ShulkerTooltipModule;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shulker Tooltip: gives shulker boxes a tooltip image (their contents) and drops the vanilla text list. */
@Mixin({ItemStack.class})
public abstract class ShulkerTooltipMixin {
   @Inject(method = {"getTooltipImage"}, at = {@At("HEAD")}, cancellable = true)
   private void swiftclient$shulkerPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
      if (ShulkerTooltipModule.active != null || EnderPreviewModule.active != null) {
         Optional<TooltipComponent> preview = ShulkerTooltipModule.preview((ItemStack)(Object)this);
         if (preview.isEmpty()) {
            preview = EnderPreviewModule.preview((ItemStack)(Object)this);
         }

         if (preview.isPresent()) {
            cir.setReturnValue(preview);
         }
      }
   }

   @Inject(method = {"addToTooltip"}, at = {@At("HEAD")}, cancellable = true)
   private <T extends TooltipProvider> void swiftclient$hideContainerList(
      DataComponentType<T> type, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag, CallbackInfo ci
   ) {
      if (type == DataComponents.CONTAINER && ShulkerTooltipModule.active != null && ShulkerTooltipModule.hidesList((ItemStack)(Object)this)) {
         ci.cancel();
      }
   }
}
