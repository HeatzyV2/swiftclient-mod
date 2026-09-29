package dev.swiftclient.core.ui;

import dev.swiftclient.core.mods.ModuleManager;
import java.util.ArrayList;
import java.util.List;

/**
 * In-game toasts (friend online, screenshot saved...), drawn by the Notifications HUD element.
 * Nothing is queued while that module is off, so a stale backlog never pops up when it is switched on.
 */
public final class Notifications {
   public static final String MODULE_ID = "hud_notifications";
   private static final int MAX = 4;
   private static final List<Toast> ACTIVE = new ArrayList<>();

   private Notifications() {
   }

   /** One toast. {@code icon} is a Swift icon name (see Canvas.icon). */
   public record Toast(String title, String body, String icon, long start, long durationMs) {
      public float age(long now) {
         return (now - this.start) / (float)this.durationMs;
      }
   }

   public static void push(String title, String body, String icon) {
      if (ModuleManager.isLoaded() && ModuleManager.active(MODULE_ID)) {
         long duration = 4000L;
         try {
            duration = Math.round(ModuleManager.byId(MODULE_ID).setting("duration").value() * 1000.0);
         } catch (RuntimeException ignored) {
         }

         synchronized (ACTIVE) {
            ACTIVE.add(0, new Toast(title, body == null ? "" : body, icon == null ? "swiftclient" : icon, System.currentTimeMillis(), duration));
            while (ACTIVE.size() > MAX) {
               ACTIVE.remove(ACTIVE.size() - 1);
            }
         }
      }
   }

   /** Toasts still on screen, newest first. */
   public static List<Toast> active() {
      long now = System.currentTimeMillis();
      synchronized (ACTIVE) {
         ACTIVE.removeIf(t -> now - t.start() > t.durationMs());
         return List.copyOf(ACTIVE);
      }
   }

   public static void clear() {
      synchronized (ACTIVE) {
         ACTIVE.clear();
      }
   }
}
