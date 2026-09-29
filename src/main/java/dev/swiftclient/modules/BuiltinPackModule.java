package dev.swiftclient.modules;

import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.mods.Module;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackRepository;

/**
 * A module that is a resource pack shipped in the mod ({@code resources/resourcepacks/<pack>}): switching it
 * on selects the pack and reloads the resources, switching it off removes it.
 */
public abstract class BuiltinPackModule extends Module {
   private final String pack;

   protected BuiltinPackModule(String id, String name, String description, String category, String icon, String pack) {
      super(id, name, description, category, icon, false);
      this.pack = pack;
   }

   /** Declares the packs to Fabric. Called once at startup, before resources load. */
   public static void registerPacks() {
      FabricLoader.getInstance().getModContainer("swiftclient").ifPresent(mod -> {
         for (String[] p : new String[][]{{"clear_glass", "Swift - Clear Glass"}, {"hide_foliage", "Swift - Hide Foliage"}, {"better_foliage", "Swift - Better Foliage"}}) {
            if (!ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath("swiftclient", p[0]), mod, Component.literal(p[1]), PackActivationType.NORMAL)) {
               Log.get("Packs").warn("Pack integre {} non enregistre", p[0]);
            }
         }
      });
   }

   /** Id of our pack in the repository (the exact format belongs to Fabric), null if unknown. */
   private String packId(PackRepository repo) {
      for (String id : repo.getAvailableIds()) {
         if (id.contains("swiftclient") && id.endsWith(this.pack)) {
            return id;
         }
      }

      return null;
   }

   private void setSelected(boolean on) {
      Minecraft mc = Minecraft.getInstance();
      PackRepository repo = mc.getResourcePackRepository();
      String id = this.packId(repo);
      if (id == null) {
         Log.get("Packs").warn("Pack integre {} introuvable", this.pack);
         return;
      }

      boolean selected = repo.getSelectedIds().contains(id);
      if (on != selected) {
         if (on) {
            repo.addPack(id);
         } else {
            repo.removePack(id);
         }

         // Saves options.txt and reloads the resources (the chunks are rebuilt with the new models).
         mc.options.updateResourcePacks(repo);
      }
   }

   @Override
   protected void onEnable() {
      this.setSelected(true);
   }

   @Override
   protected void onDisable() {
      this.setSelected(false);
   }
}
