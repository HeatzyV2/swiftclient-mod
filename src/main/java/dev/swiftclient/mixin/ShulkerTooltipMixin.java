package dev.swiftclient.mixin;

import dev.swiftclient.visuals.EnderPreviewModule;
import dev.swiftclient.visuals.ShulkerTooltipModule;
import java.util.Optional;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import com.llamalad7.mixinextras.sugar.Local;
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
   private void swiftclient$hideContainerList(CallbackInfo ci, @Local(argsOnly = true) DataComponentType<?> type) {
      if (type == DataComponents.CONTAINER && ShulkerTooltipModule.active != null && ShulkerTooltipModule.hidesList((ItemStack)(Object)this)) {
         ci.cancel();
      }
   }
}
