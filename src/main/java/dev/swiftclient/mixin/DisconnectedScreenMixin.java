package dev.swiftclient.mixin;

import dev.swiftclient.modules.AutoReconnectModule;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Auto Reconnect: a Reconnect button (with a countdown when the module is on) on the disconnect screen. */
@Mixin({DisconnectedScreen.class})
public abstract class DisconnectedScreenMixin extends Screen {
   protected DisconnectedScreenMixin(Component title) {
      super(title);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void swiftclient$reconnectButton(CallbackInfo ci) {
      Button button = AutoReconnectModule.reconnectButton((DisconnectedScreen)(Object)this);
      if (button != null) {
         this.addRenderableWidget(button);
      }
   }
}
