package dev.swiftclient.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Minecraft.class})
public interface MinecraftClientAccessor {
   @Mutable
   @Accessor("user")
   void lightclient$setUser(User var1);

   @Mutable
   @Accessor("profileKeyPairManager")
   void lightclient$setProfileKeyPairManager(ProfileKeyPairManager var1);

   @Mutable
   @Accessor("userApiService")
   void lightclient$setUserApiService(UserApiService var1);
}
