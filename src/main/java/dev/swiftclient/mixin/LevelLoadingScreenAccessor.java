package dev.swiftclient.mixin;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LevelLoadingScreen.class})
public interface LevelLoadingScreenAccessor {
   @Accessor("smoothedProgress")
   float swiftclient$smoothedProgress();
}
