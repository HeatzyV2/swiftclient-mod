package dev.swiftclient.core.social;

import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.ui.Notifications;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The friends list of the Swift account (same backend list as the launcher), refreshed in the background
 * while the Friends module is on. Read by the tab list, the world highlight and the Friends screen.
 */
public final class Friends {
   public static final String MODULE_ID = "friends";
   private static final long REFRESH_MS = 45000L;
   private static volatile List<SocialApi.Friend> list = List.of();
   private static volatile Set<String> names = Set.of();
   private static volatile Set<String> uuids = Set.of();
   private static volatile String error;
   private static volatile boolean loading;
   private static volatile long lastRefresh;
   /** Online state seen at the previous refresh, to notify only on transitions. */
   private static final Map<String, Boolean> WAS_ONLINE = new HashMap<>();
   private static boolean firstLoad = true;

   private Friends() {
   }

   public static List<SocialApi.Friend> list() {
      return list;
   }

   public static String error() {
      return error;
   }

   public static boolean loading() {
      return loading;
   }

   public static boolean isFriend(String name) {
      return name != null && names.contains(name.toLowerCase(Locale.ROOT));
   }

   public static boolean isFriendUuid(String uuid) {
      return uuid != null && uuids.contains(uuid.toLowerCase(Locale.ROOT).replace("-", ""));
   }

   private static Module module() {
      return ModuleManager.isLoaded() ? ModuleManager.byId(MODULE_ID) : null;
   }

   private static boolean option(String id) {
      Module m = module();
      return m != null && m.isEnabled() && m.setting(id) != null && m.setting(id).boolValue();
   }

   /** Star in front of friends in the tab list. */
   public static boolean highlightInTab(String name) {
      return option("tab_star") && isFriend(name);
   }

   /** Outline friends in the world. */
   public static boolean glowInWorld() {
      return option("glow");
   }

   /** Called every tick by the module; refreshes when the list is stale. */
   public static void tick() {
      if (!loading && System.currentTimeMillis() - lastRefresh > REFRESH_MS) {
         refresh();
      }
   }

   public static void refresh() {
      if (loading) {
         return;
      }

      loading = true;
      lastRefresh = System.currentTimeMillis();
      SocialApi.listFriends().whenComplete((res, t) -> {
         loading = false;
         if (t != null || res == null) {
            error = Tr.of("swift.net.unreachable");
            return;
         }

         if (!res.ok()) {
            error = res.error();
            return;
         }

         error = null;
         apply(res.value());
      });
   }

   private static synchronized void apply(List<SocialApi.Friend> friends) {
      Set<String> n = new HashSet<>();
      Set<String> u = new HashSet<>();
      boolean notify = !firstLoad && option("notify");

      for (SocialApi.Friend f : friends) {
         n.add(f.username().toLowerCase(Locale.ROOT));
         if (f.uuid() != null) {
            u.add(f.uuid().toLowerCase(Locale.ROOT).replace("-", ""));
         }

         Boolean before = WAS_ONLINE.put(f.id(), f.online());
         if (notify && f.online() && Boolean.FALSE.equals(before) && notificationsWanted()) {
            Notifications.push(Tr.of("swift.friends.online_title"), Tr.of("swift.friends.online_body", f.username()), "users");
         }
      }

      firstLoad = false;
      list = List.copyOf(friends);
      names = Set.copyOf(n);
      uuids = Set.copyOf(u);
   }

   private static boolean notificationsWanted() {
      Module n = ModuleManager.byId(Notifications.MODULE_ID);
      return n == null || n.setting("friends") == null || n.setting("friends").boolValue();
   }

   /** Local update after an add or a remove, before the next refresh. */
   public static void invalidate() {
      lastRefresh = 0L;
   }
}
