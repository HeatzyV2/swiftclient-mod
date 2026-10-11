package dev.swiftclient.core.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** What the chat module decides about a line, from its plain text. Pure, so the patterns are testable. */
public final class ChatRules {
   private static final Pattern JOIN_LEAVE = Pattern.compile(
      "(?iu)^(?:\\[[+-]\\]\\s*)?\\S+\\s+(?:joined|left)\\s+the\\s+game$"
         + "|^(?:\\[[+-]\\]\\s*)?\\S+\\s+a\\s+(?:rejoint|quitt\\S*)\\s+la\\s+partie$"
         + "|^\\[[+-]\\]\\s*\\S+$"
   );
   private static final Pattern WHISPER = Pattern.compile(
      "(?iu)\\bwhispers?\\s+to\\s+you\\b"
         + "|\\bvous\\s+chuchote\\b"
         + "|^\\[?\\S+\\s*(?:->|\\u2192|\\u00bb)\\s*(?:me|moi|you|vous)\\]?"
         + "|^\\[(?:me|moi|you|vous)\\s*(?:->|\\u2192|\\u00bb)\\s*\\S+\\]"
         + "|^from\\s+\\S+:"
         + "|^de\\s+\\S+\\s*:"
   );

   private ChatRules() {
   }

   public static boolean isJoinLeave(String line) {
      return line != null && JOIN_LEAVE.matcher(line.trim()).find();
   }

   public static boolean isWhisper(String line) {
      return line != null && WHISPER.matcher(line.trim()).find();
   }

   /** "word1, word2" -> ["word1", "word2"], lower-case, blanks and one-letter entries dropped. */
   public static List<String> words(String csv) {
      List<String> out = new ArrayList<>();
      if (csv != null) {
         for (String w : csv.split("[,;]")) {
            String t = w.trim().toLowerCase(Locale.ROOT);
            if (t.length() >= 2) {
               out.add(t);
            }
         }
      }

      return out;
   }

   /** Does the line contain one of the blocked words? Whole words only, so "ass" does not hide "class". */
   public static boolean blocked(String line, List<String> blockedWords) {
      return line != null && !blockedWords.isEmpty() && containsWord(line, blockedWords);
   }

   /** Is the player named in a message somebody else wrote? */
   public static boolean mentions(String line, String playerName, List<String> extraNames) {
      if (line == null || playerName == null || playerName.isBlank()) {
         return false;
      }

      String lower = line.toLowerCase(Locale.ROOT);
      String me = playerName.toLowerCase(Locale.ROOT);
      // Your own line: "<me> hello", "me: hello", "[Rank] me » hello"
      if (lower.startsWith("<" + me + ">") || lower.matches("^(?:\\[[^\\]]*\\]\\s*)*" + Pattern.quote(me) + "\\s*(?::|\\u00bb|>).*")) {
         return false;
      }

      List<String> names = new ArrayList<>(extraNames);
      names.add(me);
      return containsWord(lower, names);
   }

   private static boolean containsWord(String text, List<String> words) {
      String lower = text.toLowerCase(Locale.ROOT);
      for (String w : words) {
         int from = 0;
         while (true) {
            int i = lower.indexOf(w, from);
            if (i < 0) {
               break;
            }

            boolean before = i == 0 || !Character.isLetterOrDigit(lower.charAt(i - 1)) && lower.charAt(i - 1) != '_';
            int end = i + w.length();
            boolean after = end >= lower.length() || !Character.isLetterOrDigit(lower.charAt(end)) && lower.charAt(end) != '_';
            if (before && after) {
               return true;
            }

            from = i + 1;
         }
      }

      return false;
   }
}
