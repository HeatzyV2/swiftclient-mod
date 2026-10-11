package dev.swiftclient.core.mods.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;

/** Keeps private information off a stream or a screenshot: server address, coordinates, private messages. */
@ConsumedBy({"StreamerMode", "ServerElement", "CoordsElement", "ChatComponentMixin"})
public final class StreamerModule extends Module {
   public StreamerModule() {
      super("streamer", "Streamer mode", "Hides the server address, your coordinates and private messages so nothing private ends up on a stream.", "Interface", "eye", false);
      this.toggle("address", "Hide server address", true).desc("The Server Address element shows dots instead of the IP.");
      this.toggle("coords", "Hide coordinates", true).desc("The Coordinates element shows dots instead of your position.");
      this.toggle("whispers", "Hide private messages", true).desc("Whispers and private messages never appear in your chat while this is on.");
   }
}
