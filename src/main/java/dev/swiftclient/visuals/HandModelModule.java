package dev.swiftclient.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.mixin.PlayerModelSlimAccessor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Your own arm holds the item in first person (vanilla only shows it when the hand is empty). */
@ConsumedBy("ItemInHandAnimationsMixin")
public final class HandModelModule extends Module {
   public static volatile HandModelModule active;
   public final ModuleSetting items;
   public final ModuleSetting mainHand;
   public final ModuleSetting offHand;

   public HandModelModule() {
      super("hand_model", "3D Hand", "Shows your own arm holding the item in first person: swords, tools, items and blocks.", "Visual", "hand", false);
      this.items = this.cycle("items", "Items", new String[]{"All items", "Weapons & tools"}, 0).desc("Which held items get your arm.");
      this.mainHand = this.toggle("main_hand", "Main hand", true).desc("Show the arm holding the main hand item.");
      this.offHand = this.toggle("off_hand", "Offhand", true).desc("Show the arm holding the offhand item.");
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   static boolean wants(boolean mainHand) {
      HandModelModule module = active;
      return module != null && (mainHand ? module.mainHand.boolValue() : module.offHand.boolValue());
   }

   static boolean covers(ItemStack stack, boolean mainHand) {
      HandModelModule module = active;
      return wants(mainHand) && module != null && (module.items.cycleIndex() == 0 || weaponOrTool(stack));
   }

   private static boolean weaponOrTool(ItemStack stack) {
      return stack.is(ItemTags.SWORDS)
         || stack.is(ItemTags.AXES)
         || stack.is(ItemTags.PICKAXES)
         || stack.is(ItemTags.SHOVELS)
         || stack.is(ItemTags.HOES)
         || stack.is(ItemTags.SPEARS)
         || stack.is(Items.MACE)
         || stack.is(Items.FISHING_ROD)
         || stack.is(Items.SHEARS)
         || stack.is(Items.FLINT_AND_STEEL)
         || stack.is(Items.BRUSH)
         || stack.is(Items.CARROT_ON_A_STICK)
         || stack.is(Items.WARPED_FUNGUS_ON_A_STICK);
   }

   /** Stretches the vanilla first-person arm up to the shoulder. */
   public static void shoulder(PoseStack pose, boolean right, AvatarRenderer<?> renderer) {
      FirstPersonArm.shoulder(pose, right, ((PlayerModelSlimAccessor)renderer.getModel()).swiftclient$slim());
   }

   public static void renderArm(PoseStack pose, LivingEntity holder, ItemStack stack, ItemDisplayContext context, SubmitNodeCollector collector, int light, boolean mainHand) {
      if (covers(stack, mainHand)) {
         FirstPersonArm.Arm arm = FirstPersonArm.arm();
         if (arm != null) {
            FirstPersonArm.hold(pose, arm, stack, context, holder, collector, light, 0.0F, 0.0F, 0.0F);
         }
      }
   }
}
