package dev.swiftclient.hud;

import dev.swiftclient.core.hud.HudData;
import dev.swiftclient.mixin.BossHealthOverlayAccessor;
import dev.swiftclient.pvp.CombatTracker;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
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
   public int ping() {
      if (this.p() == null || this.mc.getConnection() == null || this.mc.getSingleplayerServer() != null) {
         return -1;
      }

      PlayerInfo info = this.mc.getConnection().getPlayerInfo(this.p().getUUID());
      return info == null ? -1 : info.getLatency();
   }

   @Override
   public float yaw() {
      return this.p() == null ? 0.0F : this.p().getViewYRot(this.mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
   }

   @Override
   public String serverAddress() {
      if (this.mc.getSingleplayerServer() != null) {
         return "";
      }

      ServerData sd = this.mc.getCurrentServer();
      return sd == null ? "" : sd.ip;
   }

   @Override
   public boolean sprinting() {
      return this.p() != null && this.p().isSprinting();
   }

   @Override
   public boolean sneaking() {
      return this.p() != null && this.p().isShiftKeyDown();
   }

   @Override
   public List<HudData.BossBar> bossBars() {
      List<HudData.BossBar> out = new ArrayList<>();
      for (LerpingBossEvent e : ((BossHealthOverlayAccessor)this.mc.gui.hud.getBossOverlay()).swiftclient$events().values()) {
         out.add(new HudData.BossBar(e.getName().getString(), e.getName(), e.getProgress(), e.getColor().ordinal()));
      }

      return out;
   }

   /** Default icons of the Item Counter, built on first use (items need the registries). */
   private static ItemStack[] countIcons;

   private static ItemStack[] countIcons() {
      if (countIcons == null) {
         countIcons = new ItemStack[]{
            new ItemStack(Items.ARROW), new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.GOLDEN_APPLE), new ItemStack(Items.SPLASH_POTION),
            new ItemStack(Items.TOTEM_OF_UNDYING), new ItemStack(Items.COBBLESTONE)
         };
      }

      return countIcons;
   }

   private static int countKey(ItemStack st) {
      if (st.is(Items.ARROW) || st.is(Items.TIPPED_ARROW) || st.is(Items.SPECTRAL_ARROW)) {
         return 0;
      } else if (st.is(Items.ENDER_PEARL)) {
         return 1;
      } else if (st.is(Items.GOLDEN_APPLE) || st.is(Items.ENCHANTED_GOLDEN_APPLE)) {
         return 2;
      } else if (st.is(Items.SPLASH_POTION) || st.is(Items.LINGERING_POTION)) {
         return 3;
      } else if (st.is(Items.TOTEM_OF_UNDYING)) {
         return 4;
      } else {
         return st.getItem() instanceof BlockItem ? 5 : -1;
      }
   }

   @Override
   public List<HudData.ItemCount> itemCounts() {
      int[] n = new int[6];
      ItemStack[] icon = countIcons().clone();
      if (this.p() != null) {
         Inventory inv = this.p().getInventory();
         int biggestBlocks = 0;
         for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            int k = st.isEmpty() ? -1 : countKey(st);
            if (k >= 0) {
               n[k] += st.getCount();
               if (k == 5 && st.getCount() > biggestBlocks) {
                  biggestBlocks = st.getCount();
                  icon[5] = st;
               } else if (k == 3 || k == 2) {
                  icon[k] = st;
               }
            }
         }
      }

      List<HudData.ItemCount> out = new ArrayList<>(6);
      for (int i = 0; i < 6; i++) {
         out.add(new HudData.ItemCount(HudData.COUNTED_ITEMS.get(i), icon[i], n[i]));
      }

      return out;
   }

   @Override
   public List<HudData.Pickup> pickups() {
      return PickupTracker.snapshot();
   }

   @Override
   public List<HudData.Cooldown> cooldowns() {
      List<HudData.Cooldown> out = new ArrayList<>();
      if (this.p() == null) {
         return out;
      }

      float partial = this.mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
      Set<Identifier> seen = new HashSet<>();
      Inventory inv = this.p().getInventory();
      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack st = inv.getItem(i);
         if (!st.isEmpty() && this.p().getCooldowns().isOnCooldown(st) && seen.add(this.p().getCooldowns().getCooldownGroup(st))) {
            out.add(new HudData.Cooldown(st, this.p().getCooldowns().getCooldownPercent(st, partial)));
         }
      }

      return out;
   }

   @Override
   public HudData.Combat combat() {
      return CombatTracker.snapshot();
   }

   @Override
   public List<String> resourcePacks() {
      List<String> out = new ArrayList<>();
      for (Pack pack : this.mc.getResourcePackRepository().getSelectedPacks()) {
         if (!pack.isRequired() && !pack.getId().contains("fabric") && !pack.getId().contains("swiftclient")) {
            out.add(0, pack.getTitle().getString());
         }
      }

      return out;
   }

   @Override
   public HudData.Minimap minimap(int radius, boolean rotate) {
      return MinimapRenderer.get(radius, rotate);
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
