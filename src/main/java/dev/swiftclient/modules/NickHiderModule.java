package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * Replaces your name with a nickname everywhere it is drawn on your screen (chat, tab, name tags,
 * scoreboard...), for videos and streams. Other players still see your real name.
 */
@ConsumedBy("FontNickMixin")
public final class NickHiderModule extends Module {
   public final ModuleSetting nick;
   private static volatile String name;
   private static volatile String replacement;

   public NickHiderModule() {
      super("nick_hider", "Nick Hider", "Replaces your name with a nickname everywhere on your screen, for videos and streams. Others still see your real name.", "World", "nickhider", false);
      this.nick = this.text("nick", "Nickname", "You", 16).desc("The name shown instead of yours.");
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      String real = mc.getUser() == null ? null : mc.getUser().getName();
      String n = this.nick.textValue().isBlank() ? "You" : this.nick.textValue();
      name = real == null || real.length() < 2 ? null : real;
      replacement = n;
   }

   @Override
   protected void onDisable() {
      name = null;
   }

   private static boolean active() {
      return name != null && ModuleManager.isLoaded() && ModuleManager.get(NickHiderModule.class).isEnabled();
   }

   /** Does this text contain your name (and is the module on)? */
   public static boolean affects(String s) {
      String n = name;
      return s != null && n != null && active() && s.contains(n);
   }

   public static String replace(String s) {
      String n = name;
      return s != null && n != null && active() && s.contains(n) ? s.replace(n, replacement) : s;
   }

   /** Same as {@link #replace(String)} for styled text, keeping the style of the replaced characters. */
   public static FormattedCharSequence replace(FormattedCharSequence seq) {
      String n = name;
      if (seq == null || n == null || !active()) {
         return seq;
      }

      StringBuilder text = new StringBuilder();
      List<Style> styles = new ArrayList<>();
      seq.accept((pos, style, cp) -> {
         int before = text.length();
         text.appendCodePoint(cp);
         for (int i = before; i < text.length(); i++) {
            styles.add(style);
         }

         return true;
      });
      int at = text.indexOf(n);
      if (at < 0) {
         return seq;
      }

      String repl = replacement;
      StringBuilder out = new StringBuilder();
      List<Style> outStyles = new ArrayList<>();
      int i = 0;
      while (i < text.length()) {
         if (i == at) {
            Style st = styles.get(i);
            for (int k = 0; k < repl.length(); k++) {
               out.append(repl.charAt(k));
               outStyles.add(st);
            }

            i += n.length();
            at = text.indexOf(n, i);
         } else {
            out.append(text.charAt(i));
            outStyles.add(styles.get(i));
            i++;
         }
      }

      String result = out.toString();
      return sink -> {
         int pos = 0;
         for (int k = 0; k < result.length(); ) {
            int cp = result.codePointAt(k);
            if (!sink.accept(pos++, outStyles.get(k), cp)) {
               return false;
            }

            k += Character.charCount(cp);
         }

         return true;
      };
   }
}
