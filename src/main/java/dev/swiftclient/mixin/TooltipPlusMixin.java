package dev.swiftclient.mixin;

import dev.swiftclient.visuals.TooltipPlusModule;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tooltip+: appends its lines to the tooltip text of every item. */
@Mixin({ItemStack.class})
public abstract class TooltipPlusMixin {
   @Inject(method = {"getTooltipLines"}, at = {@At("RETURN")})
   private void swiftclient$tooltipPlus(CallbackInfoReturnable<List<Component>> cir) {
      if (TooltipPlusModule.active != null) {
         TooltipPlusModule.append((ItemStack)(Object)this, cir.getReturnValue());
      }
   }
}
