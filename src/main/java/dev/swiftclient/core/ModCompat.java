package dev.swiftclient.core;

import net.fabricmc.loader.api.FabricLoader;

/** Detect third-party UIs that clash with Swift title chrome. */
public final class ModCompat {
   private static Boolean essential;

   private ModCompat() {
   }

   public static boolean essentialLoaded() {
      if (essential == null) {
         FabricLoader fl = FabricLoader.getInstance();
         essential = fl.isModLoaded("essential")
            || fl.isModLoaded("essential-container")
            || fl.isModLoaded("essential-loader")
            || fl.getAllMods().stream().anyMatch(c -> {
               String id = c.getMetadata().getId();
               return id.startsWith("essential");
            });
      }
      return essential;
   }
}
