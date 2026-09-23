package dev.swiftclient.mixin;

import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList.OnlineServerEntry;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({OnlineServerEntry.class})
public interface OnlineServerEntryInvoker {
   @Invoker("<init>")
   static OnlineServerEntry lightclient$create(ServerSelectionList list, JoinMultiplayerScreen screen, ServerData data) {
      throw new AssertionError();
   }
}
