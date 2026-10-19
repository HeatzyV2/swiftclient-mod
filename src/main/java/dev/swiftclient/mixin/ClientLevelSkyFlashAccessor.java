package dev.swiftclient.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Atmosphere: lightning flashes light up the custom sky too. */
@Mixin({ClientLevel.class})
public interface ClientLevelSkyFlashAccessor {
   @Accessor("skyFlashTime")
   int swiftclient$skyFlashTime();
}
