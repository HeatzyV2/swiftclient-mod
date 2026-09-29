package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.swiftclient.core.social.Friends;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Friends without a scoreboard team get a gold outline instead of the default white one. */
@Mixin({Entity.class})
public abstract class EntityTeamColorMixin {
   @ModifyReturnValue(
      method = {"getTeamColor"},
      at = {@At("RETURN")}
   )
   private int swiftclient$friendColor(int original) {
      Entity self = (Entity)(Object)this;
      if (original == 0xFFFFFF && self instanceof AbstractClientPlayer p && p.getTeam() == null && Friends.glowInWorld() && Friends.isFriend(p.getGameProfile().name())) {
         return 0xFFD45A;
      } else {
         return original;
      }
   }
}
