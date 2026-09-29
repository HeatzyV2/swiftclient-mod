package dev.swiftclient.mixin;

import dev.swiftclient.modules.ChatModule;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Chat module: timestamps, stacked repeats, longer history, history kept on disconnect. */
@Mixin({ChatComponent.class})
public abstract class ChatComponentMixin {
   @Unique
   private static final DateTimeFormatter SWIFTCLIENT$HM = DateTimeFormatter.ofPattern("HH:mm");
   @Unique
   private static final DateTimeFormatter SWIFTCLIENT$HMS = DateTimeFormatter.ofPattern("HH:mm:ss");
   @Shadow
   @Final
   private List<GuiMessage> allMessages;
   @Shadow
   @Final
   private List<GuiMessage.Line> trimmedMessages;
   @Unique
   private String swiftclient$lastRaw;
   @Unique
   private int swiftclient$lastCount;
   @Unique
   private GuiMessage swiftclient$lastMessage;

   @ModifyVariable(
      method = {"addMessage"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private Component swiftclient$decorate(Component contents) {
      ChatModule m = ChatModule.active();
      if (m == null || contents == null) {
         this.swiftclient$lastRaw = null;
         return contents;
      }

      Component out = contents;
      String raw = contents.getString();
      if (m.stack.boolValue() && raw.equals(this.swiftclient$lastRaw) && this.swiftclient$lastMessage != null && !this.allMessages.isEmpty()
         && this.allMessages.get(0) == this.swiftclient$lastMessage) {
         this.swiftclient$lastCount++;
         GuiMessage previous = this.swiftclient$lastMessage;
         this.allMessages.remove(0);
         this.trimmedMessages.removeIf(line -> line.parent() == previous);
         out = contents.copy().append(Component.literal(" [x" + this.swiftclient$lastCount + "]").withStyle(ChatFormatting.GRAY));
      } else {
         this.swiftclient$lastRaw = raw;
         this.swiftclient$lastCount = 1;
      }

      if (m.timestamps.boolValue()) {
         String time = LocalTime.now().format(m.seconds.boolValue() ? SWIFTCLIENT$HMS : SWIFTCLIENT$HM);
         MutableComponent stamp = Component.literal("[" + time + "] ").withStyle(ChatFormatting.DARK_GRAY);
         out = Component.empty().append(stamp).append(out);
      }

      return out;
   }

   @Inject(
      method = {"addMessage"},
      at = @At("TAIL")
   )
   private void swiftclient$rememberLast(CallbackInfo ci) {
      this.swiftclient$lastMessage = this.allMessages.isEmpty() ? null : this.allMessages.get(0);
   }

   @ModifyConstant(
      method = {"addMessageToDisplayQueue", "addMessageToQueue"},
      constant = {@Constant(intValue = 100)}
   )
   private int swiftclient$historySize(int vanilla) {
      return ChatModule.historySize(vanilla);
   }

   @Inject(
      method = {"clearMessages"},
      at = @At("HEAD"),
      cancellable = true
   )
   private void swiftclient$keepHistory(boolean history, CallbackInfo ci) {
      ChatModule m = ChatModule.active();
      if (m != null && m.keep.boolValue() && history) {
         ci.cancel();
      }
   }
}
