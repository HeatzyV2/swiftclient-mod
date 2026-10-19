package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.streamonly.StreamOverlay;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Hides your real items on screen (hotbar, inventory, held item name, tooltips) so a stream or a screen
 * share never shows your loot. Chests and other containers keep their real content. With Stream Only
 * (Windows), you still see your real items: they are drawn in windows that screen capture skips.
 */
@ConsumedBy({"HideInventoryHudMixin", "HideInventoryScreenMixin", "StreamOverlayFrameMixin"})
public final class HideInventoryModule extends Module {
   private static volatile HideInventoryModule active;
   private static ItemStack totem;
   public final ModuleSetting style;
   public final ModuleSetting fillEmpty;
   public final ModuleSetting streamOnly;
   public final ModuleSetting tooltips;
   public final ModuleSetting itemName;
   public final ModuleSetting selectedSlot;
   private FakeLoadout loadout;

   public HideInventoryModule() {
      super("hide_inventory", "Hide Inventory", "Shows fake items instead of yours in the hotbar and inventory, for streams and screen shares. Containers stay real.", "Interface", "lock", false);
      this.style = this.cycle("style", "Style", new String[]{"Totem", "Random", "Empty"}, 0).desc("Totem: every slot shows a totem. Random: a believable made-up inventory. Empty: slots look empty.");
      this.fillEmpty = this.toggle("fill_empty", "Fill empty slots", true).desc("Empty slots get a fake item too, so nobody can count your free space.");
      this.streamOnly = this.toggle("stream_only", "Stream only", false)
         .desc("Windows: the stream sees the fake items, you still see your real ones (drawn in windows that OBS and Discord do not capture). Turns fullscreen into borderless.");
      this.tooltips = this.toggle("tooltips", "Hide tooltips", true).desc("No tooltip over your own items (with Stream only, only the stream loses them).");
      this.itemName = this.toggle("item_name", "Hide item name", true).desc("No item name above the hotbar when you switch items (with Stream only, only the stream loses it).");
      this.selectedSlot = this.toggle("selected_slot", "Hide selected slot", true).desc("No highlight on the selected hotbar slot (with Stream only, only the stream loses it).");
   }

   @Override
   protected void onEnable() {
      if (this.loadout == null) {
         this.loadout = new FakeLoadout();
      }

      this.loadout.reseed();
      active = this;
      StreamOverlay.retry();
      BorderlessModule.reapply();
   }

   @Override
   protected void onDisable() {
      active = null;
      StreamOverlay.stop();
      BorderlessModule.reapply();
   }

   @Override
   protected void onTick() {
      // Stream only switched on or off while the module runs: the window mode follows.
      BorderlessModule.reapply();
   }

   /** Stream only is asked for (the overlay may still be unavailable, e.g. not on Windows). */
   public static boolean overlayWanted() {
      HideInventoryModule module = active;
      return module != null && module.streamOnly.boolValue();
   }

   /** The real items are drawn in the overlay windows this frame: the game shows only the fakes. */
   private static boolean overlaying() {
      return overlayWanted() && StreamOverlay.usable();
   }

   /** Stream only needs a borderless window: an exclusive fullscreen window would cover the overlay. */
   public static boolean wantsBorderless() {
      if (!ModuleManager.isLoaded()) {
         return false;
      }

      HideInventoryModule m = ModuleManager.get(HideInventoryModule.class);
      return m.isEnabled() && m.streamOnly.boolValue() && StreamOverlay.usable();
   }

   /** Hotbar slot {@code seed} (1-9 hotbar, 10 offhand) at (x, y) as drawn on the HUD. */
   public static ItemStack hotbar(ItemStack stack, int x, int y, int seed) {
      HideInventoryModule module = active;
      if (module == null || stack == null) {
         return stack;
      }

      int slot = seed == 10 ? 40 : (seed >= 1 && seed <= 9 ? seed - 1 : FakeLoadout.UNKNOWN);
      if (overlaying()) {
         StreamOverlay.hotbar(slot, x, y, stack);
         return ItemStack.EMPTY;
      }

      return module.shown(stack, slot);
   }

   /** One of your own inventory slots at screen position (x, y) as drawn in a screen. */
   public static ItemStack container(ItemStack stack, Slot slot, int x, int y, boolean hovered) {
      HideInventoryModule module = active;
      if (module == null || stack == null || !ownSlot(slot)) {
         return stack;
      }

      if (overlaying()) {
         StreamOverlay.container(slot.getContainerSlot(), x, y, stack, hovered);
         return ItemStack.EMPTY;
      }

      return module.shown(stack, slot.getContainerSlot());
   }

   /** The stack following the mouse. */
   public static ItemStack carried(ItemStack stack, int x, int y) {
      HideInventoryModule module = active;
      if (module == null || stack == null || stack.isEmpty()) {
         return stack;
      }

      if (overlaying()) {
         StreamOverlay.carried(stack, x, y);
         return ItemStack.EMPTY;
      }

      return module.fake(FakeLoadout.UNKNOWN);
   }

   /** Your own hovered item's tooltip is hidden: with Stream only it moves to the overlay. */
   public static void hiddenTooltip(ItemStack stack, int mouseX, int mouseY) {
      if (overlaying() && stack != null && !stack.isEmpty()) {
         StreamOverlay.tooltip(stack, mouseX, mouseY);
      }
   }

   /** The held item name is hidden: with Stream only it moves to the overlay. */
   public static void hiddenItemName(ItemStack stack, int alpha, int y) {
      if (overlaying() && stack != null && !stack.isEmpty() && alpha > 0) {
         StreamOverlay.itemName(stack, alpha, y);
      }
   }

   /** The hotbar selection highlight is hidden: with Stream only it moves to the overlay. */
   public static void hiddenSelection(int x, int y) {
      if (overlaying()) {
         StreamOverlay.selection(x, y);
      }
   }

   public static boolean ownSlot(Slot slot) {
      return active != null && slot != null && slot.container instanceof Inventory;
   }

   public static boolean hidesTooltips() {
      HideInventoryModule module = active;
      return module != null && module.tooltips.boolValue();
   }

   public static boolean hidesItemName() {
      HideInventoryModule module = active;
      return module != null && module.itemName.boolValue();
   }

   public static boolean hidesSelectedSlot() {
      HideInventoryModule module = active;
      return module != null && module.selectedSlot.boolValue();
   }

   /** Fake items are drawn (Totem or Random): the overlay cells must be opaque to cover them. */
   public static boolean opaqueCells() {
      HideInventoryModule module = active;
      return module != null && module.style.cycleIndex() != 2;
   }

   public static boolean fillsEmpty() {
      HideInventoryModule module = active;
      return module != null && module.fillEmpty.boolValue() && module.style.cycleIndex() != 2;
   }

   /** The fake item of inventory slot {@code slot}, drawn on the streamed frame. */
   public static ItemStack fakeFor(int slot) {
      HideInventoryModule module = active;
      return module == null ? ItemStack.EMPTY : module.fake(slot);
   }

   private ItemStack shown(ItemStack stack, int slot) {
      return stack.isEmpty() && !this.fillEmpty.boolValue() ? stack : this.fake(slot);
   }

   private ItemStack fake(int slot) {
      return switch (this.style.cycleIndex()) {
         case 2 -> ItemStack.EMPTY;
         case 1 -> this.loadout.get(slot);
         default -> {
            if (totem == null) {
               totem = new ItemStack(Items.TOTEM_OF_UNDYING);
            }

            yield totem;
         }
      };
   }
}
