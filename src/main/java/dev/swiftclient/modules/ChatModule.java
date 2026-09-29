package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;

/** Chat improvements: timestamps, repeated messages stacked, longer history kept between servers. */
@ConsumedBy("ChatComponentMixin")
public final class ChatModule extends Module {
   public final ModuleSetting timestamps;
   public final ModuleSetting seconds;
   public final ModuleSetting stack;
   public final ModuleSetting history;
   public final ModuleSetting keep;

   public ChatModule() {
      super("chat", "Chat", "Timestamps, repeated messages stacked into one line, longer history kept when you change server.", "Interface", "chat", false);
      this.timestamps = this.toggle("timestamps", "Timestamps", true).desc("Write the time in front of every message.");
      this.seconds = this.toggle("seconds", "Show seconds", false).desc("Timestamps with seconds (14:02:37 instead of 14:02).");
      this.stack = this.toggle("stack", "Stack repeats", true).desc("A message sent several times in a row shows once, with [x3].");
      this.history = this.slider("history", "History size", 300.0, 100.0, 1000.0, 50.0, "").desc("How many messages you can scroll back. Vanilla keeps 100.");
      this.keep = this.toggle("keep", "Keep on disconnect", true).desc("Keep the chat history when you leave a server or a world.");
   }

   public static ChatModule active() {
      if (!ModuleManager.isLoaded()) {
         return null;
      }

      ChatModule m = ModuleManager.get(ChatModule.class);
      return m.isEnabled() ? m : null;
   }

   public static int historySize(int vanilla) {
      ChatModule m = active();
      return m == null ? vanilla : (int)Math.round(m.history.value());
   }
}
