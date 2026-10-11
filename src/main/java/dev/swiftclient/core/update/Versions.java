package dev.swiftclient.core.update;

/** Version strings such as {@code 2.3.0}, {@code v2.3.0} or {@code 2.3.0-beta.1}. */
public final class Versions {
   private Versions() {
   }

   private static int[] parse(String raw) {
      String s = raw == null ? "" : raw.trim();
      if (s.startsWith("v") || s.startsWith("V")) {
         s = s.substring(1);
      }

      int cut = s.indexOf('-');
      boolean pre = cut >= 0;
      if (pre) {
         s = s.substring(0, cut);
      }

      int plus = s.indexOf('+');
      if (plus >= 0) {
         s = s.substring(0, plus);
      }

      int[] out = new int[4];
      String[] parts = s.split("\\.");
      for (int i = 0; i < 3 && i < parts.length; i++) {
         try {
            out[i] = Integer.parseInt(parts[i].replaceAll("\\D", ""));
         } catch (NumberFormatException e) {
            out[i] = 0;
         }
      }

      out[3] = pre ? 0 : 1; // a release is newer than its own pre-release
      return out;
   }

   /** True if {@code remote} is a newer version than {@code local}. Unknown versions never count as newer. */
   public static boolean isNewer(String local, String remote) {
      if (local == null || remote == null || local.equals("?") || remote.isBlank()) {
         return false;
      }

      int[] a = parse(local);
      int[] b = parse(remote);
      for (int i = 0; i < 4; i++) {
         if (b[i] != a[i]) {
            return b[i] > a[i];
         }
      }

      return false;
   }
}
