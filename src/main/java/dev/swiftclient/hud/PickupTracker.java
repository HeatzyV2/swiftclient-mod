package dev.swiftclient.hud;

import dev.swiftclient.core.hud.HudData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Item Tracker: compares the inventory totals every tick and records what came in or went out. */
public final class PickupTracker {
   /** Changes of the same item within this window are merged into one line. */
   private static final long MERGE_MS = 1500L;
   private static final int MAX = 12;
   private static Map<Item, Integer> previous;
   private static final List<Entry> FEED = new ArrayList<>();

   private PickupTracker() {
   }

   private static final class Entry {
      final Item item;
      final ItemStack icon;
      int delta;
      long time;

      Entry(Item item, ItemStack icon, int delta, long time) {
         this.item = item;
         this.icon = icon;
         this.delta = delta;
         this.time = time;
      }
   }

   public static void tick(boolean enabled) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      if (!enabled || p == null || p.isDeadOrDying()) {
         previous = null;
         if (p == null) {
            FEED.clear();
         }

         return;
      }

      Map<Item, Integer> now = new HashMap<>();
      Map<Item, ItemStack> icons = new HashMap<>();
      Inventory inv = p.getInventory();
      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack st = inv.getItem(i);
         if (!st.isEmpty()) {
            now.merge(st.getItem(), st.getCount(), Integer::sum);
            icons.putIfAbsent(st.getItem(), st);
         }
      }

      // The item on the cursor (inventory open) is still yours: do not count it as lost.
      ItemStack carried = p.containerMenu.getCarried();
      if (!carried.isEmpty()) {
         now.merge(carried.getItem(), carried.getCount(), Integer::sum);
         icons.putIfAbsent(carried.getItem(), carried);
      }

      if (previous != null) {
         long t = System.currentTimeMillis();
         for (Map.Entry<Item, Integer> e : now.entrySet()) {
            int d = e.getValue() - previous.getOrDefault(e.getKey(), 0);
            if (d != 0) {
               add(e.getKey(), icons.get(e.getKey()), d, t);
            }
         }

         for (Map.Entry<Item, Integer> e : previous.entrySet()) {
            if (!now.containsKey(e.getKey())) {
               add(e.getKey(), new ItemStack(e.getKey()), -e.getValue(), t);
            }
         }
      }

      previous = now;
   }

   private static void add(Item item, ItemStack icon, int delta, long t) {
      for (Entry e : FEED) {
         if (e.item == item && t - e.time < MERGE_MS && Integer.signum(e.delta) == Integer.signum(delta)) {
            e.delta += delta;
            e.time = t;
            FEED.remove(e);
            FEED.add(0, e);
            return;
         }
      }

      FEED.add(0, new Entry(item, icon.copyWithCount(1), delta, t));
      while (FEED.size() > MAX) {
         FEED.remove(FEED.size() - 1);
      }
   }

   public static List<HudData.Pickup> snapshot() {
      List<HudData.Pickup> out = new ArrayList<>(FEED.size());
      for (Entry e : FEED) {
         out.add(new HudData.Pickup(e.icon, e.icon.getHoverName().getString(), e.delta, e.time));
      }

      return out;
   }
}
