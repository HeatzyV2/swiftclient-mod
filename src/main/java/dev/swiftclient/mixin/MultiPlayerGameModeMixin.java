package dev.swiftclient.mixin;

import dev.swiftclient.modules.ParticleChangerModule;
import dev.swiftclient.pvp.CombatTracker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every attack you send: combo, reach and PvP Info tracking, extra hit particles. */
@Mixin({MultiPlayerGameMode.class})
public abstract class MultiPlayerGameModeMixin {
   @Inject(
      method = {"attack"},
      at = {@At("HEAD")}
   )
   private void swiftclient$onAttack(Player player, Entity entity, CallbackInfo ci) {
      CombatTracker.onAttack(entity);
      ParticleChangerModule.onAttack(entity);
   }
}
