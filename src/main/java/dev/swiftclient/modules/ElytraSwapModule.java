package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;

/**
 * One key swaps the chest piece: elytra on, or your best chestplate back. Same clicks as doing it by hand
 * in the inventory, never while a container is open or while you use an item. Some servers forbid
 * inventory shortcuts: check the rules before using it.
 */
@ConsumedBy("KeyboardHandlerMixin")
public final class ElytraSwapModule extends Module {
   private static final int CHEST_SLOT = 6;
   private static final int OFFHAND_INDEX = 40;
   public final ModuleSetting key;
   public final ModuleSetting swapBack;
   private boolean pending;
   private boolean armed;
   private boolean glided;
   private int groundTicks;

   public ElytraSwapModule() {
      super("elytra_swap", "Elytra Swap", "Swaps between your chestplate and an elytra with one key. Some servers forbid inventory shortcuts.", "World", "swap", false);
      this.key = this.key("key", "Swap key", -1).desc("Press it in game to swap. Pick a key first: none is set by default.");
      this.swapBack = this.toggle("swap_back", "Swap back on landing", false).desc("Put the chestplate back on when you land after a flight.");
   }

   /** A key went down (KeyboardHandlerMixin). Never consumes it. */
   public static boolean onKey(int key, int action) {
      if (action != 1 || !ModuleManager.isLoaded()) {
         return false;
      }

      ElytraSwapModule m = ModuleManager.get(ElytraSwapModule.class);
      int bound = m.key.keyCode();
      if (m.isEnabled() && bound >= 0 && key == bound) {
         m.pending = true;
      }

      return false;
   }

   @Override
   protected void onDisable() {
      this.pending = false;
      this.armed = false;
      this.glided = false;
   }

   @Override
   protected void onTick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null || mc.gameMode == null) {
         this.onDisable();
         return;
      }

      if (this.pending) {
         this.pending = false;
         this.swap(mc, player);
      } else if (this.armed) {
         this.followFlight(mc, player);
      }
   }

   private void swap(Minecraft mc, LocalPlayer player) {
      if (player.isSpectator() || player.isUsingItem() || !inventoryReady(player)) {
         return;
      }

      ItemStack worn = player.getItemBySlot(EquipmentSlot.CHEST);
      if (isBound(player, worn)) {
         say(player, "Can't swap: your chest piece has Curse of Binding");
         return;
      }

      if (worn.has(DataComponents.GLIDER)) {
         int chestplate = findBestChestplate(player);
         if (chestplate < 0) {
            say(player, "Can't swap: no chestplate in your inventory");
         } else {
            equip(mc, player, chestplate);
         }

         this.armed = false;
      } else {
         int elytra = findElytra(player);
         if (elytra < 0) {
            say(player, "Can't swap: no usable elytra in your inventory");
         } else {
            boolean hadChestplate = isChestplate(worn);
            equip(mc, player, elytra);
            this.armed = hadChestplate && this.swapBack.boolValue();
            this.glided = false;
            this.groundTicks = 0;
         }
      }
   }

   private void followFlight(Minecraft mc, LocalPlayer player) {
      if (!player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER)) {
         this.armed = false;
      } else if (player.isFallFlying()) {
         this.glided = true;
         this.groundTicks = 0;
      } else if (this.glided && (player.onGround() || player.isInWater()) && ++this.groundTicks >= 4) {
         this.armed = false;
         if (mc.gui.screen() == null && inventoryReady(player) && !player.isUsingItem()) {
            int chestplate = findBestChestplate(player);
            if (chestplate >= 0) {
               equip(mc, player, chestplate);
            }
         }
      }
   }

   /** Moves inventory item {@code index} into the chest slot, the old chest piece going where it was. */
   private static void equip(Minecraft mc, LocalPlayer player, int index) {
      int menu = player.inventoryMenu.containerId;
      if (Inventory.isHotbarSlot(index)) {
         mc.gameMode.handleContainerInput(menu, CHEST_SLOT, index, ContainerInput.SWAP, player);
      } else {
         int slot = index == OFFHAND_INDEX ? 45 : index;
         mc.gameMode.handleContainerInput(menu, slot, 0, ContainerInput.PICKUP, player);
         mc.gameMode.handleContainerInput(menu, CHEST_SLOT, 0, ContainerInput.PICKUP, player);
         mc.gameMode.handleContainerInput(menu, slot, 0, ContainerInput.PICKUP, player);
      }
   }

   private static void say(LocalPlayer player, String text) {
      Minecraft.getInstance().gui.hud.setOverlayMessage(Component.literal(text), false);
   }

   private static boolean inventoryReady(LocalPlayer player) {
      return player.containerMenu == player.inventoryMenu && player.containerMenu.getCarried().isEmpty();
   }

   private static boolean isBound(LocalPlayer player, ItemStack stack) {
      return !player.isCreative() && EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
   }

   private static int findElytra(LocalPlayer player) {
      Inventory inventory = player.getInventory();
      int best = -1;
      int bestLeft = -1;
      for (int index = 0; index < 36; index++) {
         ItemStack stack = inventory.getItem(index);
         if (LivingEntity.canGlideUsing(stack, EquipmentSlot.CHEST) && !isBound(player, stack)) {
            int left = durabilityLeft(stack);
            if (left > bestLeft) {
               best = index;
               bestLeft = left;
            }
         }
      }

      return best;
   }

   private static int findBestChestplate(LocalPlayer player) {
      Inventory inventory = player.getInventory();
      int best = -1;
      double bestScore = -1.0;
      int bestLeft = -1;
      for (int index = 0; index < 36; index++) {
         ItemStack stack = inventory.getItem(index);
         if (isChestplate(stack) && !isBound(player, stack)) {
            double score = armorScore(stack);
            int left = durabilityLeft(stack);
            if (score > bestScore || score == bestScore && left > bestLeft) {
               best = index;
               bestScore = score;
               bestLeft = left;
            }
         }
      }

      return best;
   }

   private static boolean isChestplate(ItemStack stack) {
      if (stack.isEmpty() || stack.has(DataComponents.GLIDER)) {
         return false;
      }

      Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
      return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
   }

   private static double armorScore(ItemStack stack) {
      ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      return modifiers.compute(Attributes.ARMOR, 0.0, EquipmentSlot.CHEST) + modifiers.compute(Attributes.ARMOR_TOUGHNESS, 0.0, EquipmentSlot.CHEST);
   }

   private static int durabilityLeft(ItemStack stack) {
      return stack.isDamageableItem() ? stack.getMaxDamage() - stack.getDamageValue() : Integer.MAX_VALUE;
   }
}
