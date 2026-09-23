package dev.swiftclient.platform;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.blaze3d.platform.NativeImage;
import dev.swiftclient.core.account.AccountEntry;
import dev.swiftclient.core.account.AccountManager;
import dev.swiftclient.core.cosmetics.CapeLayout;
import dev.swiftclient.core.cosmetics.CapeUploadQueue;
import dev.swiftclient.core.platform.Account;
import dev.swiftclient.core.platform.Game;
import dev.swiftclient.core.platform.Lang;
import dev.swiftclient.core.theme.LcPanorama;
import dev.swiftclient.mixin.MinecraftClientAccessor;
import dev.swiftclient.relay.InviteManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import dev.swiftclient.core.ui.SwiftSounds;

public final class GameImpl implements Game {
   private static final Map<String, Boolean> ELYTRA_ZONE = new ConcurrentHashMap<>();

   private Minecraft mc() {
      return Minecraft.getInstance();
   }

   @Override
   public List<Lang> languages() {
      LanguageManager lm = this.mc().getLanguageManager();
      List<Lang> out = new ArrayList<>();

      for (Entry<String, LanguageInfo> e : lm.getLanguages().entrySet()) {
         LanguageInfo i = e.getValue();
         out.add(new Lang(e.getKey(), i.name(), i.region()));
      }

      return out;
   }

   @Override
   public String currentLanguage() {
      return this.mc().getLanguageManager().getSelected();
   }

   @Override
   public void setLanguage(String code) {
      LanguageManager lm = this.mc().getLanguageManager();
      lm.setSelected(code);
      this.mc().options.languageCode = code;
      this.mc().options.save();
      this.mc().reloadResourcePacks().thenRun(() -> this.mc().execute(() -> {
         try {
            dev.swiftclient.rpc.RpcManager.refresh();
         } catch (Throwable ignored) {
         }
      }));
   }

   @Override
   public String translate(String key) {
      return I18n.get(key, new Object[0]);
   }

   @Override
   public void closeScreen() {
      this.mc().setScreenAndShow(null);
   }

   @Override
   public void playClick() {
      SwiftSounds.click();
   }

   @Override
   public List<Account> accounts() {
      AccountManager mgr = AccountManager.get();
      String activeUuid = mgr.getActive().map(ax -> ax.getUuid().toString()).orElse(null);
      List<Account> out = new ArrayList<>();

      for (AccountEntry a : mgr.getAccounts()) {
         String uuid = a.getUuid().toString();
         out.add(new Account(uuid, a.getUsername(), uuid.equals(activeUuid), a.isExpired() && a.isMicrosoft(), a.isOffline()));
      }

      return out;
   }

   private AccountEntry find(String uuid) {
      for (AccountEntry a : AccountManager.get().getAccounts()) {
         if (a.getUuid().toString().equals(uuid)) {
            return a;
         }
      }

      return null;
   }

   @Override
   public void switchAccount(String uuid) {
      AccountEntry a = this.find(uuid);
      if (a != null) {
         AccountManager.get().refreshAndSwitch(a, ok -> {});
      }
   }

   @Override
   public void removeAccount(String uuid) {
      AccountEntry a = this.find(uuid);
      if (a != null) {
         AccountManager.get().remove(a);
      }
   }

   @Override
   public void addAccount(Consumer<String> onStatus) {
      AccountManager.get().startBrowserLogin(onStatus::accept, entry -> {});
   }

   @Override
   public String gameVersion() {
      try {
         return FabricLoader.getInstance().getModContainer("minecraft").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
      } catch (Throwable var2) {
         return "";
      }
   }

   @Override
   public boolean inSingleplayer() {
      return Minecraft.getInstance().getSingleplayerServer() != null;
   }

   @Override
   public CompletableFuture<String> hostWorld(String modeJeu, String difficulte, boolean triche) {
      return InviteManager.startSession(modeJeu, difficulte, triche);
   }

   @Override
   public String worldName() {
      IntegratedServer s = Minecraft.getInstance().getSingleplayerServer();
      return s == null ? null : s.getWorldData().getLevelName();
   }

   @Override
   public void stopHosting() {
      InviteManager.stop();
   }

   @Override
   public String hostAddress() {
      return InviteManager.currentAddress();
   }

   @Override
   public void copyToClipboard(String texte) {
      Minecraft.getInstance().keyboardHandler.setClipboard(texte);
   }

   @Override
   public Path configDir() {
      return FabricLoader.getInstance().getConfigDir();
   }

   @Override
   public void runOnGameThread(Runnable task) {
      this.mc().execute(task);
   }

   @Override
   public boolean applySession(String username, UUID uuid, String accessToken) {
      try {
         Minecraft mc = this.mc();
         User current = mc.getUser();
         User next = new User(username, uuid, accessToken == null ? "" : accessToken, current.getXuid(), current.getClientId());
         MinecraftClientAccessor acc = (MinecraftClientAccessor)mc;
         acc.lightclient$setUser(next);
         acc.lightclient$setUserApiService(UserApiService.OFFLINE);
         acc.lightclient$setProfileKeyPairManager(ProfileKeyPairManager.EMPTY_KEY_MANAGER);
         return true;
      } catch (Exception var8) {
         System.err.println("[LC-Account] applySession failed: " + var8);
         return false;
      }
   }

   @Override
   public void applyPanorama(String location) {
      LcPanorama.apply(location);
   }

   private static Path configFile() {
      return FabricLoader.getInstance().getConfigDir().resolve("swiftclient.properties");
   }

   @Override
   public String getConfig(String key, String def) {
      Path p = configFile();
      if (!Files.exists(p)) {
         return def;
      } else {
         Properties props = new Properties();

         try (InputStream in = Files.newInputStream(p)) {
            props.load(in);
         } catch (IOException var10) {
            return def;
         }

         return props.getProperty(key, def);
      }
   }

   @Override
   public void setConfig(String key, String value) {
      Path p = configFile();
      Properties props = new Properties();
      if (Files.exists(p)) {
         try (InputStream in = Files.newInputStream(p)) {
            props.load(in);
         } catch (IOException var13) {
         }
      }

      props.setProperty(key, value);

      try (OutputStream out = Files.newOutputStream(p)) {
         props.store(out, "SwiftClient");
      } catch (IOException var11) {
      }
   }

   @Override
   public String getAccessToken() {
      User u = this.mc().getUser();
      return u == null ? "" : u.getAccessToken();
   }

   @Override
   public String getUuid() {
      User u = this.mc().getUser();
      return u != null && u.getProfileId() != null ? u.getProfileId().toString().replace("-", "").toLowerCase() : "";
   }

   @Override
   public String getUsername() {
      User u = this.mc().getUser();
      return u == null ? "" : u.getName();
   }

   @Override
   public boolean capeHasElytra(String capeId) {
      return Boolean.TRUE.equals(ELYTRA_ZONE.get(capeId));
   }

   private static boolean porteUnElytra(NativeImage img) {
      if (!CapeLayout.zonePresente(img.getWidth(), img.getHeight())) {
         return false;
      } else {
         int vus = 0;

         for (int y = 1; y < 21; y++) {
            for (int x = 23; x < 44; x++) {
               if ((img.getPixel(x, y) >>> 24 & 0xFF) >= 16) {
                  if (CapeLayout.assezDePixels(++vus)) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   @Override
   public void loadCapeFrames(String capeId, byte[] png, int frameW, int frameH, BiConsumer<String, Object[]> onReady) {
      NativeImage full;
      try {
         full = NativeImage.read(png);
      } catch (IOException var16) {
         return;
      }

      int fw = frameW > 0 ? frameW : full.getWidth();
      int fh = frameH > 0 ? frameH : full.getWidth() / 2;
      int frames = Math.max(1, full.getHeight() / fh);
      NativeImage[] imgs = new NativeImage[frames];

      for (int f = 0; f < frames; f++) {
         NativeImage fi = new NativeImage(fw, fh, false);

         for (int y = 0; y < fh; y++) {
            for (int x = 0; x < fw; x++) {
               fi.setPixel(x, y, full.getPixel(x, f * fh + y));
            }
         }

         imgs[f] = fi;
      }

      ELYTRA_ZONE.put(capeId, imgs.length > 0 && porteUnElytra(imgs[0]));
      full.close();
      int n = frames;
      String safeId = capeId.toLowerCase().replaceAll("[^a-z0-9._-]", "_") + "-" + Integer.toHexString(capeId.hashCode());
      Object[] handles = new Object[frames];

      for (int f = 0; f < n; f++) {
         int fi = f;
         CapeUploadQueue.enqueue(() -> {
            Identifier id = Identifier.fromNamespaceAndPath("swiftclient", "cape/" + safeId + "/" + fi);
            this.mc().getTextureManager().register(id, new DynamicTexture(() -> "swiftclient-cape", imgs[fi]));
            handles[fi] = id;
         });
      }

      onReady.accept(capeId, handles);
   }

   @Override
   public void loadImage(String key, byte[] png, BiConsumer<String, Object> onReady) {
      NativeImage img;
      try {
         img = NativeImage.read(png);
      } catch (IOException var6) {
         return;
      }

      this.mc().execute(() -> {
         Identifier id = Identifier.fromNamespaceAndPath("swiftclient", "art/nowplaying");
         this.mc().getTextureManager().register(id, new DynamicTexture(() -> "swiftclient-art", img));
         onReady.accept(key, id);
      });
   }
}
