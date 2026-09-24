package dev.swiftclient.mixin;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({OptionInstance.class})
public interface SimpleOptionAccessor {
   @Accessor("value")
   void swiftclient$setRaw(Object value);
}
