package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.swiftclient.modules.TimeChangerModule;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Time Changer: the world clocks read on the client (sky, light, stars) report the chosen hour. */
@Mixin({ClientClockManager.class})
public abstract class ClientClockMixin {
   @ModifyReturnValue(
      method = {"getTotalTicks"},
      at = {@At("RETURN")}
   )
   private long swiftclient$timeChanger(long ticks) {
      return TimeChangerModule.override(ticks);
   }
}
