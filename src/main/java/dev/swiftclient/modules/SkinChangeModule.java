package dev.swiftclient.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Wear any player's skin on your own screen: type a player name and their skin replaces yours (and/or
 * everyone else's). Other players still see your real skin.
 */
public final class SkinChangeModule extends Module {
   private final ModuleSetting selfPlayerSetting;
   private final ModuleSetting othersPlayerSetting;
   private volatile CustomSkin selfSkin;
   private volatile CustomSkin othersSkin;
   private String loadedSelfName = "";
   private String loadedOthersName = "";
   private boolean selfLoading;
   private boolean othersLoading;
   private final SwapCache selfSwaps = new SwapCache();
   private final SwapCache othersSwaps = new SwapCache();

   public SkinChangeModule() {
      super("skin_change", "Skin Change", "Wear any player's skin. Only on your screen: other players still see your real skin.", "Visual", "shirt", false);
      this.selfPlayerSetting = this.text("self", "Your skin", "", 16).desc("Name of the player whose skin you wear. Empty = your own skin.");
      this.othersPlayerSetting = this.text("others", "Everyone else", "", 16).desc("Name of the player whose skin every other player wears. Empty = their own skins.");
   }

   /** The skin {@code player} is drawn with (AbstractClientPlayerEntityMixin). */
   public static PlayerSkin swap(AbstractClientPlayer player, PlayerSkin original) {
      if (!ModuleManager.isLoaded()) {
         return original;
      }

      SkinChangeModule m = ModuleManager.get(SkinChangeModule.class);
      if (!m.isEnabled() || original == null) {
         return original;
      }

      Minecraft mc = Minecraft.getInstance();
      return m.skinFor(mc.player != null && player == mc.player, original);
   }

   @Override
   protected void onEnable() {
      this.reloadIfChanged();
   }

   @Override
   protected void onTick() {
      this.reloadIfChanged();
   }

   @Override
   protected void onDisable() {
      this.selfSkin = null;
      this.othersSkin = null;
      this.loadedSelfName = "";
      this.loadedOthersName = "";
      this.selfSwaps.clear();
      this.othersSwaps.clear();
   }

   private void reloadIfChanged() {
      String selfName = this.selfPlayerSetting.textValue().trim();
      String othersName = this.othersPlayerSetting.textValue().trim();
      if (!selfName.equals(this.loadedSelfName)) {
         this.selfSkin = null;
         if (selfName.isEmpty()) {
            this.loadedSelfName = "";
         } else if (!this.selfLoading) {
            this.loadedSelfName = selfName;
            this.selfLoading = true;
            this.loadFromUsername(selfName, true);
         }
      }

      if (!othersName.equals(this.loadedOthersName)) {
         this.othersSkin = null;
         if (othersName.isEmpty()) {
            this.loadedOthersName = "";
         } else if (!this.othersLoading) {
            this.loadedOthersName = othersName;
            this.othersLoading = true;
            this.loadFromUsername(othersName, false);
         }
      }

   }

   private void loadFromUsername(String username, boolean isSelf) {
      Minecraft mc = Minecraft.getInstance();
      if (!username.matches("[A-Za-z0-9_]{1,16}")) {
         mc.execute(() -> this.finishLoad(username, isSelf, null));
         return;
      }

      CompletableFuture.runAsync(() -> {
         try {
            String uuid = fetchUUID(username);
            if (uuid == null) {
               mc.execute(() -> this.finishLoad(username, isSelf, (CustomSkin)null));
               return;
            }

            SkinSource skin = fetchSkin(uuid);
            if (skin == null) {
               mc.execute(() -> this.finishLoad(username, isSelf, (CustomSkin)null));
               return;
            }

            Identifier id = isSelf ? Identifier.fromNamespaceAndPath("swiftclient", "skin_change/self") : Identifier.fromNamespaceAndPath("swiftclient", "skin_change/others");
            Path cacheDir = mc.gameDirectory.toPath().resolve("cache/swiftclient_skins");

            try {
               Files.createDirectories(cacheDir);
            } catch (IOException var9) {
            }

            Path cachePath = cacheDir.resolve((isSelf ? "self_" : "others_") + uuid + ".png");
            registerSkin(id, cachePath, skin.url()).whenCompleteAsync((texture, error) -> this.finishLoad(username, isSelf, texture == null ? null : new CustomSkin(texture, skin.model())), mc);
         } catch (Exception var10) {
            mc.execute(() -> this.finishLoad(username, isSelf, (CustomSkin)null));
         }

      });
   }

   private void finishLoad(String username, boolean isSelf, CustomSkin loaded) {
      if (isSelf) {
         this.selfLoading = false;
      } else {
         this.othersLoading = false;
      }

      if (this.isEnabled()) {
         String current = (isSelf ? this.selfPlayerSetting : this.othersPlayerSetting).textValue().trim();
         if (loaded != null && username.equals(current) && username.equals(isSelf ? this.loadedSelfName : this.loadedOthersName)) {
            if (isSelf) {
               this.selfSkin = loaded;
            } else {
               this.othersSkin = loaded;
            }
         }

         this.reloadIfChanged();
      }
   }

   private static CompletableFuture<ClientAsset.Texture> registerSkin(Identifier id, Path cachePath, String url) {
      Minecraft mc = Minecraft.getInstance();
      SkinTextureDownloader downloader = new SkinTextureDownloader(mc.getProxy(), mc.getTextureManager(), mc);
      return downloader.downloadAndRegisterSkin(id, cachePath, url, true);
   }

   private static String fetchUUID(String username) throws Exception {
      HttpURLConnection conn = openGet("https://api.mojang.com/users/profiles/minecraft/" + username);
      int status = conn.getResponseCode();
      if (status != 204 && status != 404) {
         BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

         String var7;
         try {
            StringBuilder sb = new StringBuilder();

            String line;
            while((line = reader.readLine()) != null) {
               sb.append(line);
            }

            JsonObject obj = JsonParser.parseString(sb.toString()).getAsJsonObject();
            var7 = obj.get("id").getAsString();
         } catch (Throwable var9) {
            try {
               reader.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         reader.close();
         return var7;
      } else {
         return null;
      }
   }

   private static SkinSource fetchSkin(String uuid) throws Exception {
      HttpURLConnection conn = openGet("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
      if (conn.getResponseCode() != 200) {
         return null;
      } else {
         BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

         String json;
         try {
            StringBuilder sb = new StringBuilder();

            String line;
            while((line = reader.readLine()) != null) {
               sb.append(line);
            }

            json = sb.toString();
         } catch (Throwable var13) {
            try {
               reader.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }

            throw var13;
         }

         reader.close();
         JsonObject profile = JsonParser.parseString(json).getAsJsonObject();

         for(JsonElement elem : profile.getAsJsonArray("properties")) {
            JsonObject prop = elem.getAsJsonObject();
            if ("textures".equals(prop.get("name").getAsString())) {
               String decoded = new String(Base64.getDecoder().decode(prop.get("value").getAsString()));
               JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject().getAsJsonObject("textures");
               if (!textures.has("SKIN")) {
                  return null;
               }

               JsonObject skin = textures.getAsJsonObject("SKIN");
               JsonObject metadata = skin.has("metadata") ? skin.getAsJsonObject("metadata") : null;
               String model = metadata != null && metadata.has("model") ? metadata.get("model").getAsString() : null;
               return new SkinSource(skin.get("url").getAsString(), PlayerModelType.byLegacyServicesName(model));
            }
         }

         return null;
      }
   }

   private static HttpURLConnection openGet(String url) throws Exception {
      HttpURLConnection conn = (HttpURLConnection)URI.create(url).toURL().openConnection();
      conn.setConnectTimeout(5000);
      conn.setReadTimeout(5000);
      conn.setRequestProperty("User-Agent", "SwiftClient");
      return conn;
   }

   private PlayerSkin skinFor(boolean isSelf, PlayerSkin original) {
      CustomSkin custom = isSelf ? this.selfSkin : this.othersSkin;
      return custom == null ? original : (isSelf ? this.selfSwaps : this.othersSwaps).swap(original, custom);
   }

   private static record CustomSkin(ClientAsset.Texture body, PlayerModelType model) {
   }

   private static record SkinSource(String url, PlayerModelType model) {
   }

   private static final class SwapCache {
      private static final int MAX = 256;
      private final Map<PlayerSkin, PlayerSkin> swapped = new IdentityHashMap();
      private CustomSkin wearing;

      PlayerSkin swap(PlayerSkin original, CustomSkin custom) {
         if (custom != this.wearing || this.swapped.size() >= 256) {
            this.swapped.clear();
            this.wearing = custom;
         }

         PlayerSkin skin = (PlayerSkin)this.swapped.get(original);
         if (skin == null) {
            skin = new PlayerSkin(custom.body(), original.cape(), original.elytra(), custom.model(), original.secure());
            this.swapped.put(original, skin);
         }

         return skin;
      }

      void clear() {
         this.swapped.clear();
         this.wearing = null;
      }
   }
}
