package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
//? if >=1.21.6 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
//?} else
/*import java.util.function.Function;*/
import dev.swiftclient.modules.HideInventoryModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hide Inventory: fake hotbar items, no held item name, no selected slot highlight. With Stream only the
 * real ones are handed to the overlay that only you see.
 */
@Mixin({Hud.class})
public abstract class HideInventoryHudMixin {
   @Unique
   private static final Identifier SWIFTCLIENT$SELECTION = Identifier.withDefaultNamespace("hud/hotbar_selection");
   @Shadow
   private int toolHighlightTimer;
   @Shadow
   private ItemStack lastToolHighlight;

   @ModifyVariable(
      method = {"extractSlot"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private ItemStack swiftclient$hideHotbarItem(
      ItemStack stack,
      @Local(argsOnly = true, ordinal = 0) int x,
      @Local(argsOnly = true, ordinal = 1) int y,
      @Local(argsOnly = true, ordinal = 2) int seed
   ) {
      return HideInventoryModule.hotbar(stack, x, y, seed);
   }

   @Inject(
      method = {"extractSelectedItemName"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void swiftclient$hideItemName(GuiGraphicsExtractor graphics, CallbackInfo ci) {
      if (HideInventoryModule.hidesItemName()) {
         if (this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
            int alpha = Math.min(255, (int)(this.toolHighlightTimer * 256.0F / 10.0F));
            int y = graphics.guiHeight() - 59;
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null && !mc.gameMode.canHurtPlayer()) {
               y += 14;
            }

            HideInventoryModule.hiddenItemName(this.lastToolHighlight, alpha, y);
         }

         ci.cancel();
      }
   }

   @WrapWithCondition(
      method = {"extractItemHotbar"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
      )}
   )
   private boolean swiftclient$hideSelection(GuiGraphicsExtractor graphics, /*? if >=1.21.6 {*/RenderPipeline/*?} else {*//*Function<Identifier, ?>*//*?}*/ pipeline, Identifier sprite, int x, int y, int width, int height) {
      if (HideInventoryModule.hidesSelectedSlot() && SWIFTCLIENT$SELECTION.equals(sprite)) {
         HideInventoryModule.hiddenSelection(x, y);
         return false;
      }

      return true;
   }
}
