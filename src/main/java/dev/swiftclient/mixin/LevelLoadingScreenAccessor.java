package dev.swiftclient.mixin;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LevelLoadingScreen.class})
public interface LevelLoadingScreenAccessor {
   //? if >=1.21.9 {
   @Accessor("smoothedProgress")
   float swiftclient$smoothedProgress();
   //?} else {
   /*@Accessor("progressListener")
   net.minecraft.server.level.progress.StoringChunkProgressListener swiftclient$progressListener();
   *///?}
}
