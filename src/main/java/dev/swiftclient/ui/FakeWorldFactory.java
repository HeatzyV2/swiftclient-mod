package dev.swiftclient.ui;

import dev.swiftclient.core.log.Log;
import org.slf4j.Logger;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

public final class FakeWorldFactory {
   private static final Logger LOG = Log.get("Ui");
   private static RemotePlayer fakePlayer;

   private FakeWorldFactory() {
   }

   public static RemotePlayer getOrCreate() {
      if (fakePlayer != null) {
         return fakePlayer;
      } else {
         if (RegistryCache.get() == null) {
            tryLoadVanillaRegistries();
         }

         if (RegistryCache.get() == null) {
            return null;
         } else {
            try {
               fakePlayer = create();
            } catch (Throwable var1) {
               LOG.warn("Creation du monde factice impossible", var1);
            }

            return fakePlayer;
         }
      }
   }

   private static void tryLoadVanillaRegistries() {
      MultiPackResourceManager rm = null;

      try {
         VanillaPackResources vanillaPack = ServerPacksSource.createVanillaPackSource();
         rm = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(vanillaPack));
         Provider builtin = VanillaRegistries.createLookup();
         List<RegistryLookup<?>> base = builtin.listRegistries().toList();
         Frozen loaded = (Frozen)RegistryDataLoader.load(rm, base, RegistryDataLoader.WORLDGEN_REGISTRIES, Runnable::run).join();
         RegistryCache.set(loaded, null);
         LOG.debug("Registres pre-charges depuis le datapack vanilla");
      } catch (Throwable var13) {
         LOG.warn("Pre-chargement des registres vanilla impossible", var13);
      } finally {
         if (rm != null) {
            try {
               rm.close();
            } catch (Throwable var12) {
            }
         }
      }
   }

   private static RemotePlayer create() throws Throwable {
      Minecraft mc = Minecraft.getInstance();
      Holder<DimensionType> dimEntry = RegistryCache.get().lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD);
      ClientLevelData props = new ClientLevelData(Difficulty.PEACEFUL, false, true);
      ClientLevel fakeWorld = new ClientLevel(
         mc.getConnection() != null ? mc.getConnection() : RegistryCache.getHandler(), props, Level.OVERWORLD, dimEntry, 8, 8, mc.levelExtractor, false, 0L, 63
      );
      return new RemotePlayer(fakeWorld, mc.getGameProfile());
   }
}
