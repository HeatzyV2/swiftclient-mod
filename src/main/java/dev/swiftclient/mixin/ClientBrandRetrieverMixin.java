package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
   value = {ClientBrandRetriever.class},
   remap = false
)
public class ClientBrandRetrieverMixin {
   @ModifyReturnValue(
      method = {"getClientModName"},
      at = {@At("RETURN")}
   )
   private static String swiftclient$brand(String original) {
      return "SwiftClient";
   }
}
