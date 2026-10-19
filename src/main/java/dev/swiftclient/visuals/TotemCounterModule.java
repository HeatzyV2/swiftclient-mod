package dev.swiftclient.visuals;

import dev.swiftclient.core.gfx.Canvas;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

/** How many totems you have left, and a message when one saves you. */
public final class TotemCounterModule extends Module {
   public static volatile TotemCounterModule active;
   private final ModuleSetting counter = this.toggle("counter", "Counter", true).desc("Shows how many totems of undying you carry.");
   private final ModuleSetting alert = this.toggle("alert", "Pop alert", true).desc("A message when a totem saves you.");
   private final ModuleSetting hideWhenZero = this.toggle("hide_zero", "Hide at zero", false).desc("Hides the counter when you carry no totem.");
   private long popUntil;
   private int count;

   public TotemCounterModule() {
      super("totem_counter", "Totem Counter", "How many totems of undying you have left, and an alert when one saves you.", "Visual", "star", false);
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
      this.popUntil = 0L;
   }

   @Override
   protected void onTick() {
      LocalPlayer player = Minecraft.getInstance().player;
      int total = 0;
      if (player != null) {
         for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) {
               total += player.getInventory().getItem(i).getCount();
            }
         }
      }

      this.count = total;
   }

   /** Called by TotemPopMixin on the game thread. */
   public static void onTotemPop(LivingEntity entity) {
      TotemCounterModule module = active;
      if (module != null && entity == Minecraft.getInstance().player && module.alert.boolValue()) {
         module.popUntil = System.nanoTime() + 3000000000L;
      }
   }

   /** Drawn with the HUD (ExtraHud). */
   public static void render(Canvas dl, int w, int h) {
      TotemCounterModule module = active;
      if (module == null) {
         return;
      }

      long now = System.nanoTime();
      if (module.counter.boolValue() && !(module.hideWhenZero.boolValue() && module.count == 0)) {
         String text = "Totems  " + module.count;
         int color = module.count == 0 ? 0xFFFF5C5C : (module.count == 1 ? 0xFFFFC857 : 0xFFE8F0FF);
         int width = dl.textWidth(text) + 14;
         int x = w / 2 - width / 2;
         int y = h - 74;
         dl.roundRect(x, y - 3, width, dl.lineHeight() + 6, 5.0F, 0x99101520);
         dl.centeredText(text, w / 2, y, color, true);
      }

      if (now < module.popUntil) {
         float left = (float)(module.popUntil - now) / 3.0E9F;
         int alpha = Math.round(255.0F * Math.min(1.0F, left * 3.0F));
         String text = "Totem popped!  " + module.count + " left";
         dl.centeredText(text, w / 2, h / 3, alpha << 24 | 0xFFD84A, true);
      }
   }
}
