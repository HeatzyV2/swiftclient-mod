package dev.swiftclient.visuals;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** A warning on screen (and a sound) when armor or your tool is about to break. */
public final class ArmorWarningModule extends Module {
   public static volatile ArmorWarningModule active;
   private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
   private static final String[] NAMES = {"Helmet", "Chestplate", "Leggings", "Boots", "Held item"};
   private final ModuleSetting threshold = this.slider("threshold", "Warn below", 15.0, 3.0, 50.0, 1.0, "%").desc("Durability left under which the warning shows.");
   private final ModuleSetting hand = this.toggle("hand", "Held item", true).desc("Also watches the tool or weapon in your main hand.");
   private final ModuleSetting sound = this.toggle("sound", "Sound", true).desc("Plays a sound when a piece drops under the limit.");
   private final Map<Integer, String> warned = new HashMap<>();
   private String text = "";
   private long until;

   public ArmorWarningModule() {
      super("armor_warning", "Armor Warning", "A warning and a sound when your armor or tool is about to break.", "Visual", "shield", false);
   }

   @Override
   protected void onEnable() {
      active = this;
      this.warned.clear();
   }

   @Override
   protected void onDisable() {
      active = null;
      this.until = 0L;
   }

   @Override
   protected void onTick() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         this.warned.clear();
         return;
      }

      float limit = this.threshold.floatValue() / 100.0F;
      for (int i = 0; i < SLOTS.length; i++) {
         if (SLOTS[i] == EquipmentSlot.MAINHAND && !this.hand.boolValue()) {
            continue;
         }

         ItemStack stack = player.getItemBySlot(SLOTS[i]);
         String key = stack.isEmpty() ? "" : stack.getItem().toString();
         if (stack.isEmpty() || !stack.isDamageableItem()) {
            this.warned.remove(i);
            continue;
         }

         float left = 1.0F - (float)stack.getDamageValue() / (float)stack.getMaxDamage();
         if (left >= limit + 0.04F) {
            this.warned.remove(i);
         } else if (left < limit && !key.equals(this.warned.get(i))) {
            this.warned.put(i, key);
            this.text = NAMES[i] + " almost broken: " + Math.max(0, Math.round(left * 100.0F)) + "%";
            this.until = System.nanoTime() + 4000000000L;
            if (this.sound.boolValue()) {
               player.playSound(SoundEvents.ANVIL_LAND, 0.6F, 1.4F);
            }
         }
      }
   }

   /** Drawn with the HUD (ExtraHud). */
   public static void render(Canvas dl, int w, int h) {
      ArmorWarningModule module = active;
      if (module != null && System.nanoTime() < module.until) {
         float left = (float)(module.until - System.nanoTime()) / 4.0E9F;
         float pulse = 0.65F + 0.35F * (float)Math.sin(System.nanoTime() / 1.0E8D);
         int alpha = Math.round(255.0F * Math.min(1.0F, left * 3.0F) * pulse);
         int y = h / 5;
         int width = dl.textWidth(module.text) + 20;
         dl.roundRect(w / 2 - width / 2, y - 5, width, dl.lineHeight() + 10, 6.0F, (alpha * 6 / 10) << 24 | 0x2A0A0A);
         dl.centeredText(module.text, w / 2, y, alpha << 24 | 0xFF6B6B, true);
      }
   }
}
