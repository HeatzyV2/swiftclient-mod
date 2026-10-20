package dev.swiftclient.mixin;

import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ProgressScreen.class})
public interface ProgressScreenAccessor {
   @Accessor("header")
   Component swiftclient$header();

   @Accessor("progress")
   int swiftclient$progress();
}
