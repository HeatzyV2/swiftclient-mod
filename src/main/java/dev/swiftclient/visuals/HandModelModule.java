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
@ConsumedBy("ItemInHandModelsMixin")
public final class HandModelModule extends Module {
   public static volatile HandModelModule active;
   public final ModuleSetting items;
   public final ModuleSetting mainHand;
   public final ModuleSetting offHand;
   public final ModuleSetting raise;
   public final ModuleSetting shift;
   public final ModuleSetting depth;

   public HandModelModule() {
      super("hand_model", "3D Hand", "Shows your own arm holding the item in first person: swords, tools, items and blocks.", "Visual", "hand", false);
      this.items = this.cycle("items", "Items", new String[]{"All items", "Weapons & tools"}, 0).desc("Which held items get your arm.");
      this.mainHand = this.toggle("main_hand", "Main hand", true).desc("Show the arm holding the main hand item.");
      this.offHand = this.toggle("off_hand", "Offhand", true).desc("Show the arm holding the offhand item.");
      this.raise = this.slider("raise", "Raise", 0.09, -0.3, 0.3, 0.005, "").desc("Lifts the item and your arm so more of the arm is on screen.").group("Position");
      this.shift = this.slider("shift", "Shift", 0.04, -0.3, 0.3, 0.005, "").desc("Moves the item and your arm towards the centre of the screen (negative: away).").group("Position");
      this.depth = this.slider("depth", "Depth", 0.0, -0.3, 0.3, 0.005, "").desc("Pushes the item and your arm away from the camera (negative: closer).").group("Position");
   }

   /**
    * Moves the item and its arm together, for items that get an arm. Pushes a pose the caller must pop when this
    * returns true. Knives bring their own arm and are left alone.
    */
   public static boolean nudge(PoseStack pose, ItemStack stack, ItemDisplayContext context, boolean mainHand) {
      HandModelModule module = active;
      if (module == null || !covers(stack, mainHand) || WeaponInspectModule.active != null && stack.is(ItemTags.SWORDS)) {
         return false;
      }

      float side = context.leftHand() ? -1.0F : 1.0F;
      pose.pushPose();
      pose.translate(-side * module.shift.floatValue(), module.raise.floatValue(), -module.depth.floatValue());
      return true;
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
         //? if >=1.21.11
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
