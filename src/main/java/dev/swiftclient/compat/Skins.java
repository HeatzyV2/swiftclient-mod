package dev.swiftclient.compat;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
//? if >=1.21.9 {
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
//?}

/** Skin records changed in 1.21.9; keep downloads and offline skins shared. */
public final class Skins {
   private Skins() {}

   public static PlayerSkin withBody(PlayerSkin base, Identifier texture, boolean slim, boolean secure) {
      //? if >=1.21.9 {
      return new PlayerSkin(new ClientAsset.DownloadedTexture(texture, "file:swiftclient"), base.cape(), base.elytra(),
         slim ? PlayerModelType.SLIM : PlayerModelType.WIDE, secure);
      //?} else {
      /*return new PlayerSkin(texture, null, base.capeTexture(), base.elytraTexture(),
         slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, secure);
      *///?}
   }
}
