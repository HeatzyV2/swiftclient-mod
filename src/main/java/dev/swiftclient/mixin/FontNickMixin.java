package dev.swiftclient.mixin;

import dev.swiftclient.modules.NickHiderModule;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Nick Hider: every text goes through Font.prepareText, so the name is swapped there (and in widths, to keep layouts aligned). */
@Mixin({Font.class})
public abstract class FontNickMixin {
   @ModifyVariable(
      method = {"prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private String swiftclient$nickString(String text) {
      return NickHiderModule.replace(text);
   }

   @ModifyVariable(
      method = {"prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private FormattedCharSequence swiftclient$nickSequence(FormattedCharSequence text) {
      return NickHiderModule.replace(text);
   }

   @ModifyVariable(
      method = {"width(Ljava/lang/String;)I"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private String swiftclient$nickWidth(String text) {
      return NickHiderModule.replace(text);
   }

   @Inject(
      method = {"width(Lnet/minecraft/network/chat/FormattedText;)I"},
      at = @At("HEAD"),
      cancellable = true
   )
   private void swiftclient$nickTextWidth(FormattedText text, CallbackInfoReturnable<Integer> cir) {
      if (NickHiderModule.affects(text.getString())) {
         cir.setReturnValue(((Font)(Object)this).width(Language.getInstance().getVisualOrder(text)));
      }
   }

   @ModifyVariable(
      method = {"width(Lnet/minecraft/util/FormattedCharSequence;)I"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private FormattedCharSequence swiftclient$nickSequenceWidth(FormattedCharSequence text) {
      return NickHiderModule.replace(text);
   }
}
