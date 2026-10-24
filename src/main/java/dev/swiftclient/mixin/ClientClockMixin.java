package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.swiftclient.modules.TimeChangerModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Time Changer: the world clocks read on the client (sky, light, stars) report the chosen hour. */
//? if >=26.3 {
/*@Mixin({net.minecraft.client.ClientClockManager.ClientClockInstance.class})
*///?} else if >=26.1 {
@Mixin({net.minecraft.client.ClientClockManager.class})
//?} else
/*@Mixin({net.minecraft.client.multiplayer.ClientLevel.ClientLevelData.class})*/
public abstract class ClientClockMixin {
   @ModifyReturnValue(
      // 26.3: ClientClockInstance.totalTicks, 26.1: ClientClockManager.getTotalTicks, before: the level's day time.
      method = {"totalTicks", "getTotalTicks", "getDayTime"},
      at = {@At("RETURN")}
   )
   private long swiftclient$timeChanger(long ticks) {
      return TimeChangerModule.override(ticks);
   }
}
