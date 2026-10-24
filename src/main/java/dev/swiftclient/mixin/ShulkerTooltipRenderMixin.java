package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.swiftclient.visuals.ShulkerClientTooltip;
import dev.swiftclient.visuals.ShulkerTooltipModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Shulker Tooltip: vanilla only knows how to draw its own tooltip images, so ours is turned into a component here. */
@Mixin({GuiGraphicsExtractor.class})
public abstract class ShulkerTooltipRenderMixin {
   @WrapOperation(
      // The lambda of setTooltipForNextFrame that turns the tooltip image into a component: its synthetic name
      // differs between versions (and has no mapping on 1.21.x), so every method calling create(image) is matched.
      method = {"*"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;"
      )}
   )
   private static ClientTooltipComponent swiftclient$shulkerComponent(TooltipComponent component, Operation<ClientTooltipComponent> original) {
      return component instanceof ShulkerTooltipModule.ShulkerTooltip tip ? new ShulkerClientTooltip(tip) : original.call(component);
   }
}
