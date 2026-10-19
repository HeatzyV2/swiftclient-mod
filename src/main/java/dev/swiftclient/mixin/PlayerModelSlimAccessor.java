package dev.swiftclient.mixin;

import net.minecraft.client.model.player.PlayerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 3D Hand: slim (Alex) arms are narrower, so the stretched arm is shifted less. */
@Mixin({PlayerModel.class})
public interface PlayerModelSlimAccessor {
   @Accessor("slim")
   boolean swiftclient$slim();
}
