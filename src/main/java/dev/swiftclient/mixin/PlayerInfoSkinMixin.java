package dev.swiftclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import dev.swiftclient.skin.OfflineSkin;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Offline skin: your own entry (tab list, heads, your player) uses the skin chosen in the launcher. */
@Mixin({PlayerInfo.class})
public abstract class PlayerInfoSkinMixin {
   @Shadow
   public abstract GameProfile getProfile();

   @ModifyReturnValue(
      method = {"getSkin"},
      at = {@At("RETURN")}
   )
   private PlayerSkin swiftclient$offlineSkin(PlayerSkin skin) {
      return OfflineSkin.configured() && OfflineSkin.isSelf(this.getProfile().id()) ? OfflineSkin.apply(skin) : skin;
   }
}
