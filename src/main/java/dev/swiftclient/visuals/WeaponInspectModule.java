package dev.swiftclient.visuals;

import dev.swiftclient.compat.Mc;
import com.mojang.blaze3d.platform.InputConstants;
import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * CS:GO-style knife: your sword becomes a butterfly knife, karambit, katana or kunai, with an inspect animation on a
 * key of your choice (hold the key to keep it spinning), a deploy animation and a slash trail. Only on your
 * screen, first person.
 */
@ConsumedBy({"ItemInHandModelsMixin", "KeyboardHandlerMixin"})
public final class WeaponInspectModule extends Module {
   public static volatile WeaponInspectModule active;
   private static final Identifier KNIFE_TEXTURE = Identifier.fromNamespaceAndPath("swiftclient", "textures/misc/knife_base.png");
   private static final float EASE_OUT_SECONDS = 0.15F;
   private static final int IDLE = 0;
   private static final int INSPECT = 1;
   private static final int DEPLOY = 2;
   private static final float[] REST = new float[16];
   private static final float SPARKLE_SIZE = 0.07F;
   public final ModuleSetting key;
   public final ModuleSetting skin;
   public final ModuleSetting style;
   public final ModuleSetting items;
   public final ModuleSetting speed;
   public final ModuleSetting knifeSize;
   public final ModuleSetting showArm;
   public final ModuleSetting deploy;
   public final ModuleSetting slashTrail;
   public final ModuleSetting trailLength;
   public final ModuleSetting sparkles;
   public final ModuleSetting sneakSkips;
   private int mode = 0;
   private InspectAnimation.Clip clip;
   private KnifeSkin clipKnife;
   private long startedAt;
   private float seconds;
   private boolean easingOut;
   private long easeStartedAt;
   private float easeFrom;
   private int slot;
   private Item item;
   private int heldSlot = -1;
   private Item heldItem;
   private final float[] state = new float[16];
   private final float[] trailSample = new float[12];
   private final SlashTrail[] trails = new SlashTrail[2];
   private final Matrix4f base = new Matrix4f();
   private final Matrix4f baseInverse = new Matrix4f();
   private final Matrix3f baseNormal = new Matrix3f();
   private boolean baseKnown;

   public WeaponInspectModule() {
      super("weapon_inspect", "CS Knife", "Knife skins for your sword and CS-style inspects on a key (F by default). Hold the key to keep the knife spinning.", "Visual", "sword", false);
      this.key = this.key("key", "Inspect key", 70).desc("Press to inspect the held weapon. While you hold a weapon, this key no longer swaps hands.");
      this.skin = this.cycle("skin", "Knife skin", skinOptions(), 1).desc("The knife your sword turns into. None keeps the sword and only adds the inspect.").group("Knife");
      this.knifeSize = this.slider("knife_size", "Knife size", 1.0, 0.5, 2.0, 0.05, "x").desc("Size of the knife in your hand.").group("Knife");
      this.deploy = this.toggle("deploy", "Deploy animation", true).desc("Play a draw animation when you switch to the knife.").group("Knife");
      this.sparkles = this.toggle("sparkles", "Blade sparkle", true).desc("A glint runs along the blade during the inspect.").group("Knife");
      this.style = this.cycle("style", "Inspect style", new String[]{"Classic", "Twirl"}, 0).desc("Motion of the inspect for items that are not knives.").group("Inspect");
      this.items = this.cycle("items", "Items", new String[]{"Swords", "Weapons", "Any item"}, 0).desc("Which held items can be inspected.").group("Inspect");
      this.speed = this.slider("speed", "Speed", 1.0, 0.5, 2.0, 0.05, "x").desc("Speed of the inspect and deploy animations.").group("Inspect");
      this.showArm = this.toggle("show_arm", "Show arm", true).desc("Draw your arm holding the weapon.").group("Inspect");
      this.sneakSkips = this.toggle("sneak_skips", "Sneak skips inspect", true).desc("While sneaking the key keeps its vanilla action (swap hands).").group("Inspect");
      this.slashTrail = this.toggle("slash_trail", "Slash trail", true).desc("A glowing streak follows the blade when you swing.").group("Trail");
      this.trailLength = this.slider("trail_length", "Trail length", 0.14, 0.05, 0.35, 0.01, "s").desc("How long the streak stays visible.").group("Trail");

      for (int i = 0; i < this.trails.length; ++i) {
         this.trails[i] = new SlashTrail();
      }
   }

   @Override
   protected void onEnable() {
      this.mode = 0;
      this.heldItem = null;
      this.clearTrails();
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
      this.mode = 0;
      this.clearTrails();
   }

   public static void handsBegin(PoseStack pose) {
      WeaponInspectModule module = active;
      if (module != null) {
         module.base.set(pose.last().pose());
         module.base.invert(module.baseInverse);
         module.baseNormal.set(pose.last().normal());
         module.baseKnown = true;
      }
   }

   public static boolean renderItem(PoseStack pose, LivingEntity holder, ItemStack stack, ItemDisplayContext context, SubmitNodeCollector collector, int light, boolean mainHand) {
      WeaponInspectModule module = active;
      return module != null && module.render(pose, holder, stack, context, collector, light, mainHand);
   }
   /** A key went down (KeyboardHandlerMixin). True = consumed: the vanilla action of the key does not run. */
   public static boolean onKey(int key, int action) {
      WeaponInspectModule module = active;
      return module != null && action != 0 && module.pressed(key, action == 2);
   }

   private boolean pressed(int pressed, boolean repeat) {
      int bound = this.key.keyCode();
      if (bound >= 0 && pressed == bound) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         if (Mc.screen() == null && player != null && !player.isSpectator()) {
            if (this.sneakSkips.boolValue() && player.isShiftKeyDown()) {
               return false;
            } else {
               ItemStack held = player.getMainHandItem();
               if (!this.inspectable(held)) {
                  return false;
               } else {
                  if (!repeat && !player.isUsingItem()) {
                     this.start(1, player.getInventory().getSelectedSlot(), held);
                  }

                  return true;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean held() {
      int bound = this.key.keyCode();
      Minecraft mc = Minecraft.getInstance();
      return bound >= 0 && Mc.screen() == null && dev.swiftclient.compat.Input.keyDown(dev.swiftclient.compat.Input.fromCoreKey(bound));
   }

   private boolean inspectable(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (this.items.cycleIndex() != 2 && !isSword(stack)) {
         return this.items.cycleIndex() == 1 && (stack.is(ItemTags.AXES) || /*? if >=1.21.11 {*/stack.is(ItemTags.SPEARS) ||/*?}*/ stack.is(Items.MACE) || stack.is(Items.TRIDENT));
      } else {
         return true;
      }
   }

   /** Built-in knives, then the textured model knives (CounterCraft pack). */
   private static String[] skinOptions() {
      String[] models = ModelKnife.labels();
      String[] base = {"None", "Butterfly", "Karambit", "Katana", "Kunai"};
      String[] all = java.util.Arrays.copyOf(base, base.length + models.length);
      System.arraycopy(models, 0, all, base.length, models.length);
      return all;
   }

   private String skinLabel() {
      return this.skin.cycleLabelAt(this.skin.cycleIndex());
   }

   private static boolean isSword(ItemStack stack) {
      return !stack.isEmpty() && (stack.is(ItemTags.SWORDS) || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_sword"));
   }

   private static boolean handheld(ItemStack stack) {
      return isSword(stack) || stack.is(ItemTags.AXES) || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES);
   }

   private void start(int next, int selected, ItemStack held) {
      KnifeSkin knife = isSword(held) ? KnifeSkin.of(this.skinLabel()) : null;
      this.clip = next == 2 ? knife.deploy : (knife != null ? knife.inspect : InspectAnimation.clip(this.style.cycleLabelAt(this.style.cycleIndex())));
      this.clipKnife = knife;
      this.mode = next;
      this.seconds = this.clip.seconds;
      this.easingOut = false;
      this.startedAt = System.nanoTime();
      this.slot = selected;
      this.item = held.getItem();
   }

   private void easeOut() {
      long now = System.nanoTime();
      this.easeFrom = Math.min(this.progress(now), 1.0F);
      this.easeStartedAt = now;
      this.easingOut = true;
   }

   @Override
   protected void onTick() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         this.mode = 0;
         this.heldItem = null;
      } else {
         ItemStack held = player.getMainHandItem();
         int selected = player.getInventory().getSelectedSlot();
         if (selected == this.heldSlot && held.is(this.heldItem)) {
            if (this.mode != 0 && !this.easingOut) {
               if (/*? if >=26.3 {*//*player.isSwinging()*//*?} else {*/player.swinging/*?}*/) {
                  this.mode = 0;
               } else if (player.isUsingItem()) {
                  this.easeOut();
               } else if (this.loopedProgress(System.nanoTime()) >= 1.0F) {
                  this.mode = 0;
               }

            }
         } else {
            this.heldSlot = selected;
            this.heldItem = held.getItem();
            if (this.deploy.boolValue() && isSword(held) && KnifeSkin.of(this.skinLabel()) != null) {
               this.start(2, selected, held);
            } else if (this.mode != 0 && !this.easingOut) {
               this.easeOut();
            }

         }
      }
   }

   private float progress(long now) {
      return (float)(now - this.startedAt) / 1.0E9F / (this.seconds / (float)this.speed.value());
   }

   private float loopedProgress(long now) {
      float progress = this.progress(now);
      if (this.mode == 1 && this.clip.loops() && !(progress < this.clip.loopExit) && this.held()) {
         float duration = this.seconds / (float)this.speed.value();
         long cycle = (long)((this.clip.loopExit - this.clip.loopEnter) * duration * 1.0E9F);
         if (cycle <= 0L) {
            return progress;
         } else {
            long over = (long)((progress - this.clip.loopExit) * duration * 1.0E9F);
            this.startedAt += (over / cycle + 1L) * cycle;
            return this.progress(now);
         }
      } else {
         return progress;
      }
   }

   private float animate(ItemStack stack, KnifeSkin knife, long now) {
      if (this.mode != 0 && stack.is(this.item)) {
         if (knife != this.clipKnife) {
            this.mode = 0;
            return 0.0F;
         } else {
            float weight = 1.0F;
            float progress;
            if (this.easingOut) {
               float t = (float)(now - this.easeStartedAt) / 1.0E9F / 0.15F;
               if (t >= 1.0F) {
                  this.mode = 0;
                  return 0.0F;
               }

               progress = this.easeFrom;
               weight = 1.0F - t * t * (3.0F - 2.0F * t);
            } else {
               progress = this.loopedProgress(now);
               if (progress >= 1.0F) {
                  this.mode = 0;
                  return 0.0F;
               }
            }

            this.clip.sample(progress, this.state);
            return weight;
         }
      } else {
         return 0.0F;
      }
   }

   private boolean render(PoseStack pose, LivingEntity holder, ItemStack stack, ItemDisplayContext context, SubmitNodeCollector collector, int light, boolean mainHand) {
      int side = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
      boolean sword = isSword(stack);
      KnifeSkin knife = sword ? KnifeSkin.of(this.skinLabel()) : null;
      long now = System.nanoTime();
      float weight = mainHand ? this.animate(stack, knife, now) : 0.0F;
      float[] at = weight > 0.0F ? this.state : REST;
      boolean trailing = mainHand && sword && this.slashTrail.boolValue() && this.baseKnown;
      if (mainHand && !trailing) {
         this.clearTrails();
      }

      if (knife == null) {
         if (this.showArm.boolValue() && handheld(stack) || HandModelModule.covers(stack, mainHand)) {
            FirstPersonArm.Arm arm = FirstPersonArm.arm();
            if (arm != null) {
               FirstPersonArm.hold(pose, arm, stack, context, holder, collector, light, at[0] * weight, at[1] * weight, at[2] * weight);
            }
         }

         if (weight > 0.0F) {
            InspectAnimation.place(pose, at, weight, side, 0.07F, 0.2F, 0.07F);
         }

         if (trailing) {
            this.swordTrail(pose.last().pose(), side);
            this.record(now, 1);
            this.submitGlow(collector, now, KnifeMaterial.of(stack).glow, 0.0F, (KnifeSkin)null);
         }

         ModelKnife model = sword ? ModelKnife.of(this.skinLabel()) : null;
         if (model != null) {
            model.draw(pose, collector, light, side < 0, stack.hasFoil());
            return true;
         }

         return false;
      } else {
         KnifeMaterial material = KnifeMaterial.of(stack);
         FirstPersonArm.Arm arm = !this.showArm.boolValue() && !HandModelModule.wants(mainHand) ? null : FirstPersonArm.arm();
         boolean slim = arm != null && arm.slim();
         pose.pushPose();
         if (side < 0) {
            pose.scale(-1.0F, 1.0F, 1.0F);
         }

         knife.draw(pose, at, weight, slim, (float)this.knifeSize.value(), sink(collector, light, side, arm, mainHand && offHandFree(), material, stack.hasFoil()), trailing ? this.trailSample : null);
         pose.popPose();
         if (trailing) {
            this.record(now, knife.streakCount());
            float flash = this.sparkles.boolValue() && weight > 0.0F ? knife.sparkle(at, weight) : 0.0F;
            this.submitGlow(collector, now, material.glow, flash, knife);
         }

         return true;
      }
   }

   private static KnifeSkin.Sink sink(final SubmitNodeCollector collector, final int light, final int side, final FirstPersonArm.Arm arm, final boolean otherFree, final KnifeMaterial material, final boolean foil) {
      final int[] palette = material.palette;
      final RenderType solid = RenderTypes.entityCutout(KNIFE_TEXTURE);
      return new KnifeSkin.Sink() {
         public void part(PoseStack part, KnifeMesh mesh, KnifeMesh.Shine shine) {
            collector.submitCustomGeometry(part, solid, (p, out) -> mesh.emit(p, out, palette, light, shine));
            if (foil) {
               collector.order(1).submitCustomGeometry(part, RenderTypes.entityGlint(), (p, out) -> mesh.emit(p, out, palette, light, KnifeMesh.Shine.NONE));
            }

         }

         public void glow(PoseStack part, KnifeMesh mesh, KnifeMesh.Shine shine) {
            collector.order(1).submitCustomGeometry(part, RenderTypes.eyes(WeaponInspectModule.KNIFE_TEXTURE), (p, out) -> mesh.emitGlow(p, out, material.glow, shine));
         }

         public void arm(PoseStack hand, boolean other) {
            if (arm != null && (!other || otherFree)) {
               arm.draw(hand, side < 0 != other, collector, light);
            }

         }
      };
   }

   private static boolean offHandFree() {
      LocalPlayer player = Minecraft.getInstance().player;
      return player != null && player.getOffhandItem().isEmpty();
   }

   private void swordTrail(Matrix4f pose, int side) {
      Matrix4f m = (new Matrix4f(pose)).translate((float)side * 1.13F / 16.0F, 0.2F, 0.070625F).rotate((new Quaternionf()).rotationXYZ(0.0F, (-(float)Math.PI / 2F), 0.43633232F)).scale(0.68F).translate(-0.5F, -0.5F, -0.5F);
      Vector3f p = m.transformPosition(0.3125F, 0.3125F, 0.5F, new Vector3f());
      this.trailSample[0] = p.x;
      this.trailSample[1] = p.y;
      this.trailSample[2] = p.z;
      m.transformPosition(0.96875F, 0.96875F, 0.5F, p);
      this.trailSample[3] = p.x;
      this.trailSample[4] = p.y;
      this.trailSample[5] = p.z;
   }

   private void record(long now, int streaks) {
      Vector3f p = new Vector3f();

      for(int s = 0; s < this.trails.length; ++s) {
         if (s >= streaks) {
            this.trails[s].clear();
         } else {
            for(int i = s * 6; i < s * 6 + 6; i += 3) {
               this.baseInverse.transformPosition(this.trailSample[i], this.trailSample[i + 1], this.trailSample[i + 2], p);
               this.trailSample[i] = p.x;
               this.trailSample[i + 1] = p.y;
               this.trailSample[i + 2] = p.z;
            }

            this.trails[s].add(now, this.trailSample, s * 6);
         }
      }

   }

   private void clearTrails() {
      for(SlashTrail trail : this.trails) {
         trail.clear();
      }

   }

   private void submitGlow(SubmitNodeCollector collector, long now, int glow, float flash, KnifeSkin knife) {
      long lifetime = (long)((float)this.trailLength.value() * 1.0E9F);
      int streaks = knife != null ? knife.streakCount() : 1;
      boolean showing = flash > 0.0F;

      for(int s = 0; s < streaks && !showing; ++s) {
         showing = !this.trails[s].isEmpty(now, lifetime);
      }

      if (showing) {
         float tipX = this.trailSample[3];
         float tipY = this.trailSample[4];
         float tipZ = this.trailSample[5];
         float turn = (float)(now % 6283185307L) / 1.0E9F * 1.5F;
         PoseStack hand = new PoseStack();
         hand.last().pose().set(this.base);
         hand.last().normal().set(this.baseNormal);
         collector.order(1).submitCustomGeometry(hand, RenderTypes.eyes(KNIFE_TEXTURE), (p, out) -> {
            for(int s = 0; s < streaks; ++s) {
               this.trails[s].emit(p, out, now, lifetime, glow, knife != null ? knife.streakStrength(s) : 1.0F);
            }

            if (flash > 0.0F) {
               SlashTrail.sparkle(p, out, tipX, tipY, tipZ, 0.07F * flash, turn, glow, flash);
            }

         });
      }
   }
}
