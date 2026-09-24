package dev.swiftclient.hud;

import dev.swiftclient.core.hud.HudData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public final class HudDataImpl implements HudData {
   private final Minecraft mc = Minecraft.getInstance();
   private static final int MAX_LIGNES = 15;

   @Override
   public boolean inWorld() {
      return this.mc.player != null && this.mc.level != null;
   }

   private LocalPlayer p() {
      return this.mc.player;
   }

   @Override
   public double x() {
      return this.p() == null ? 0.0 : this.p().getX();
   }

   @Override
   public double y() {
      return this.p() == null ? 0.0 : this.p().getY();
   }

   @Override
   public double z() {
      return this.p() == null ? 0.0 : this.p().getZ();
   }

   @Override
   public String facing() {
      if (this.p() == null) {
         return "";
      } else {
         String s = this.p().getDirection().getName();
         return s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
      }
   }

   @Override
   public long worldTime() {
      return this.mc.level == null ? 0L : this.mc.level.getGameTime();
   }

   @Override
   public List<HudData.Effect> effects() {
      List<HudData.Effect> out = new ArrayList<>();
      if (this.p() == null) {
         return out;
      } else {
         for (MobEffectInstance mi : this.p().getActiveEffects()) {
            String name = ((MobEffect)mi.getEffect().value()).getDisplayName().getString();
            out.add(new HudData.Effect(name, mi.getAmplifier(), mi.getDuration()));
         }

         return out;
      }
   }

   @Override
   public List<HudData.Armor> armor() {
      List<HudData.Armor> out = new ArrayList<>();
      if (this.p() == null) {
         return out;
      } else {
         for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = this.p().getItemBySlot(slot);
            if (st != null && !st.isEmpty()) {
               out.add(piece(st));
            }
         }

         return out;
      }
   }

   @Override
   public HudData.Armor heldItem() {
      if (this.p() == null) {
         return null;
      } else {
         ItemStack st = this.p().getMainHandItem();
         return st != null && !st.isEmpty() ? piece(st) : null;
      }
   }

   private static HudData.Armor piece(ItemStack st) {
      return new HudData.Armor(st.getHoverName().getString(), st.getDamageValue(), st.getMaxDamage(), st);
   }

   @Override
   public HudData.Sidebar sidebar() {
      if (this.p() != null && this.mc.level != null) {
         Scoreboard sb = this.mc.level.getScoreboard();
         if (sb == null) {
            return null;
         } else {
            Objective obj = null;
            PlayerTeam team = sb.getPlayersTeam(this.p().getScoreboardName());
            if (team != null) {
               DisplaySlot slot = team.getColor().map(c -> c.displaySlot()).orElse(null);
               if (slot != null) {
                  obj = sb.getDisplayObjective(slot);
               }
            }

            if (obj == null) {
               obj = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
            }

            if (obj == null) {
               return null;
            } else {
               List<PlayerScoreEntry> entrees = new ArrayList<>(sb.listPlayerScores(obj));
               entrees.removeIf(PlayerScoreEntry::isHidden);
               entrees.sort((a, b) -> b.value() != a.value() ? Integer.compare(b.value(), a.value()) : a.owner().compareToIgnoreCase(b.owner()));
               if (entrees.size() > 15) {
                  entrees = entrees.subList(0, 15);
               }

               List<HudData.SidebarLine> lignes = new ArrayList<>(entrees.size());

               for (PlayerScoreEntry e : entrees) {
                  MutableComponent nom = PlayerTeam.formatNameForTeam(sb.getPlayersTeam(e.owner()), e.ownerName());
                  MutableComponent score = e.formatValue(obj.numberFormatOrDefault(null));
                  lignes.add(new HudData.SidebarLine(nom.getString(), score.getString(), nom, score));
               }

               return new HudData.Sidebar(obj.getDisplayName().getString(), obj.getDisplayName(), lignes);
            }
         }
      } else {
         return null;
      }
   }

   @Override
   public int tntFuseTicks() {
      if (this.p() != null && this.mc.level != null) {
         int min = -1;

         for (PrimedTnt t : this.mc.level.getEntitiesOfClass(PrimedTnt.class, this.p().getBoundingBox().inflate(24.0), e -> true)) {
            int f = t.getFuse();
            if (min < 0 || f < min) {
               min = f;
            }
         }

         return min;
      } else {
         return -1;
      }
   }

   @Override
   public String biome() {
      if (this.p() != null && this.mc.level != null) {
         try {
            Holder<Biome> holder = this.mc.level.getBiome(this.p().blockPosition());
            ResourceKey<Biome> key = (ResourceKey<Biome>)holder.unwrapKey().orElse(null);
            return key == null ? "" : prettify(key.identifier().getPath());
         } catch (Throwable ignored) {
            return "";
         }
      } else {
         return "";
      }
   }

   private static String prettify(String raw) {
      StringBuilder sb = new StringBuilder(raw.length());
      boolean up = true;

      for (char ch : raw.toCharArray()) {
         if (ch == '_') {
            sb.append(' ');
            up = true;
         } else {
            sb.append(up ? Character.toUpperCase(ch) : ch);
            up = false;
         }
      }

      return sb.toString();
   }

   @Override
   public int inputMask() {
      Options o = this.mc.options;
      if (o == null) {
         return 0;
      } else {
         int m = 0;
         if (o.keyUp.isDown()) {
            m |= 1;
         }

         if (o.keyLeft.isDown()) {
            m |= 2;
         }

         if (o.keyDown.isDown()) {
            m |= 4;
         }

         if (o.keyRight.isDown()) {
            m |= 8;
         }

         if (o.keyJump.isDown()) {
            m |= 16;
         }

         if (o.keyShift.isDown()) {
            m |= 32;
         }

         if (o.keySprint.isDown()) {
            m |= 64;
         }

         if (o.keyAttack.isDown()) {
            m |= 128;
         }

         if (o.keyUse.isDown()) {
            m |= 256;
         }

         return m;
      }
   }
}
