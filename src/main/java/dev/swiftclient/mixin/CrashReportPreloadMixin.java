package dev.swiftclient.mixin;

import net.minecraft.CrashReport;
import net.minecraft.util.MemoryReserve;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CrashReport.class})
public abstract class CrashReportPreloadMixin {
   @Inject(
      method = {"preload"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void swiftclient$skipDummyReport(CallbackInfo ci) {
      MemoryReserve.allocate();
      ci.cancel();
   }
}
