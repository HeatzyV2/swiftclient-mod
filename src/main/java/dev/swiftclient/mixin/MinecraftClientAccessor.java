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
   void swiftclient$setUser(User var1);

   @Mutable
   @Accessor("profileKeyPairManager")
   void swiftclient$setProfileKeyPairManager(ProfileKeyPairManager var1);

   @Mutable
   @Accessor("userApiService")
   void swiftclient$setUserApiService(UserApiService var1);
}
