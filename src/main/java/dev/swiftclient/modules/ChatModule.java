package dev.swiftclient.modules;

import dev.swiftclient.core.chat.ChatRules;
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
   public final ModuleSetting mentions;
   public final ModuleSetting mentionSound;
   public final ModuleSetting mentionWords;
   public final ModuleSetting hideJoins;
   public final ModuleSetting blocked;

   public ChatModule() {
      super("chat", "Chat", "Timestamps, repeated messages stacked, mentions highlighted, join/leave lines and chosen words hidden, longer history.", "Interface", "chat", false);
      this.timestamps = this.toggle("timestamps", "Timestamps", true).desc("Write the time in front of every message.");
      this.seconds = this.toggle("seconds", "Show seconds", false).desc("Timestamps with seconds (14:02:37 instead of 14:02).");
      this.stack = this.toggle("stack", "Stack repeats", true).desc("A message sent several times in a row shows once, with [x3].");
      this.history = this.slider("history", "History size", 300.0, 100.0, 1000.0, 50.0, "").desc("How many messages you can scroll back. Vanilla keeps 100.");
      this.keep = this.toggle("keep", "Keep on disconnect", true).desc("Keep the chat history when you leave a server or a world.");
      this.mentions = this.toggle("mentions", "Highlight mentions", true).desc("A message that names you turns yellow.").group("Mentions");
      this.mentionSound = this.toggle("mention_sound", "Mention sound", true).desc("A short blip when somebody names you.").group("Mentions");
      this.mentionWords = this.text("mention_words", "Other names", "", 60).desc("More words that count as a mention, separated by commas (a nickname, your team).").group("Mentions");
      this.hideJoins = this.toggle("hide_joins", "Hide join and leave", false).desc("Remove the \"X joined the game\" and \"X left the game\" lines.").group("Filters");
      this.blocked = this.text("blocked", "Hidden words", "", 80).desc("Messages containing one of these words never show. Separate them with commas. Whole words only.").group("Filters");
   }

   /** Should this line be dropped before it reaches the chat? Streamer mode counts even when the chat module is off. */
   public static boolean shouldHide(String raw) {
      if (dev.swiftclient.core.mods.StreamerMode.hidesWhisper(raw)) {
         return true;
      }

      ChatModule m = active();
      return m != null && (m.hideJoins.boolValue() && ChatRules.isJoinLeave(raw) || ChatRules.blocked(raw, ChatRules.words(m.blocked.textValue())));
   }

   private static long lastBlipMs;

   /** Is the player named in this line? Plays the blip (once per message) when the sound is on. */
   public boolean mentioned(String raw) {
      if (!this.mentions.boolValue()) {
         return false;
      }

      boolean hit = ChatRules.mentions(raw, dev.swiftclient.core.platform.Platform.game().getUsername(), ChatRules.words(this.mentionWords.textValue()));
      long now = System.currentTimeMillis();
      if (hit && this.mentionSound.boolValue() && now - lastBlipMs > 400L) {
         lastBlipMs = now;
         dev.swiftclient.core.ui.SwiftSounds.ping();
      }

      return hit;
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
