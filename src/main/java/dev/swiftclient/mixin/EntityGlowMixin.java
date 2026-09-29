package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.swiftclient.core.social.Friends;
import dev.swiftclient.modules.TeamViewModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Team View and Friends outlines: teammates / friends are drawn with the entity outline, through walls. */
@Mixin({Minecraft.class})
public abstract class EntityGlowMixin {
   @ModifyReturnValue(
      method = {"shouldEntityAppearGlowing"},
      at = {@At("RETURN")}
   )
   private boolean swiftclient$teamGlow(boolean original, Entity entity) {
      return original || TeamViewModule.shouldGlow(entity);
   }
}
