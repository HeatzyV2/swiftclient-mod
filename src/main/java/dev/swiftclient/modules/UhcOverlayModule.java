package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** UHC loot stands out: golden apples, heads, gold and diamonds lying on the ground are drawn bigger. */
@ConsumedBy("ItemEntityRendererMixin")
public final class UhcOverlayModule extends Module {
   public final ModuleSetting size;
   public final ModuleSetting ores;

   public UhcOverlayModule() {
      super("uhc_overlay", "UHC Overlay", "Golden apples, heads, gold and diamonds on the ground are drawn bigger so you spot them.", "PvP", "gapple", false);
      this.size = this.slider("size", "Size", 200.0, 125.0, 400.0, 25.0, "%").desc("How much bigger the highlighted items are drawn.");
      this.ores = this.toggle("ores", "Gold and diamonds", true).desc("Also enlarge gold ingots, nuggets, raw gold and diamonds, not only apples and heads.");
   }

   /** Size multiplier of a dropped item: 1 unless it is UHC loot and the module is on. */
   public static float scaleFor(ItemStack stack) {
      if (!ModuleManager.isLoaded()) {
         return 1.0F;
      }

      UhcOverlayModule m = ModuleManager.get(UhcOverlayModule.class);
      if (!m.isEnabled() || stack.isEmpty()) {
         return 1.0F;
      }

      boolean loot = stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE) || stack.is(Items.PLAYER_HEAD) || stack.is(Items.GOLDEN_CARROT);
      boolean ore = stack.is(Items.GOLD_INGOT) || stack.is(Items.GOLD_NUGGET) || stack.is(Items.RAW_GOLD) || stack.is(Items.GOLD_ORE) || stack.is(Items.DIAMOND)
         || stack.is(Items.DEEPSLATE_GOLD_ORE);
      return loot || ore && m.ores.boolValue() ? (float)(m.size.value() / 100.0) : 1.0F;
   }
}
