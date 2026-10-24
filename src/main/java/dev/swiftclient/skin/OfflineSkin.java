package dev.swiftclient.skin;

import com.mojang.blaze3d.platform.NativeImage;
import dev.swiftclient.core.log.Log;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.slf4j.Logger;

/**
 * Skin of an offline ("crack") account, chosen in the launcher. Mojang does not know these accounts, so the
 * skin only exists on this computer: the launcher passes the PNG with {@code -Dswiftclient.offlineSkin=<file>}
 * (and {@code -Dswiftclient.offlineSkinSlim=true} for the slim model), and the game shows it on your own
 * player. Other players still see the default skin.
 */
public final class OfflineSkin {
   private static final Logger LOG = Log.get("Skin");
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("swiftclient", "dynamic/offline_skin");
   private static final String PATH = System.getProperty("swiftclient.offlineSkin");
   private static final boolean SLIM = Boolean.getBoolean("swiftclient.offlineSkinSlim");
   private static boolean loaded;
   private static boolean ok;

   private OfflineSkin() {
   }

   /** A skin was chosen in the launcher for this (offline) session. */
   public static boolean configured() {
      return PATH != null && !PATH.isBlank();
   }

   /** Loads the PNG once, on the render thread. False when there is none or it is unreadable. */
   private static boolean ensureLoaded() {
      if (!loaded && configured() && Minecraft.getInstance().isSameThread()) {
         loaded = true;
         try (InputStream in = Files.newInputStream(Path.of(PATH))) {
            NativeImage image = NativeImage.read(in);
            if (image.getWidth() != 64 || image.getHeight() != 64 && image.getHeight() != 32) {
               LOG.warn("Skin hors ligne ignore : {}x{} (64x64 ou 64x32 attendu)", image.getWidth(), image.getHeight());
               image.close();
               return false;
            }

            if (image.getHeight() == 32) {
               image = legacyToModern(image);
            }

            Minecraft.getInstance().getTextureManager().register(TEXTURE, //? if >=1.21.5 {
               new DynamicTexture(() -> "Swift offline skin", image)
               //?} else
               /*new DynamicTexture(image)*/);
            ok = true;
            LOG.info("Skin hors ligne charge ({})", SLIM ? "slim" : "classique");
         } catch (Exception e) {
            LOG.warn("Skin hors ligne illisible : {}", e.getMessage());
         }
      }

      return ok;
   }

   /** Is this the player of this computer? */
   public static boolean isSelf(UUID id) {
      Minecraft mc = Minecraft.getInstance();
      return id != null && mc.getUser() != null && id.equals(mc.getUser().getProfileId());
   }

   /** {@code base} with its body replaced by the offline skin, or {@code base} when there is none. */
   public static PlayerSkin apply(PlayerSkin base) {
      if (!configured() || !ensureLoaded()) {
         return base;
      }

      return dev.swiftclient.compat.Skins.withBody(base, TEXTURE, SLIM, false);
   }

   public static Identifier texture() {
      return configured() && ensureLoaded() ? TEXTURE : null;
   }

   /** Old 64x32 skins: the left arm and leg are mirrored copies of the right ones (what vanilla does too). */
   private static NativeImage legacyToModern(NativeImage old) {
      NativeImage img = new NativeImage(64, 64, true);
      img.copyFrom(old);
      old.close();
      img.fillRect(0, 32, 64, 32, 0);
      img.copyRect(4, 16, 16, 32, 4, 4, true, false);
      img.copyRect(8, 16, 16, 32, 4, 4, true, false);
      img.copyRect(0, 20, 24, 32, 4, 12, true, false);
      img.copyRect(4, 20, 16, 32, 4, 12, true, false);
      img.copyRect(8, 20, 8, 32, 4, 12, true, false);
      img.copyRect(12, 20, 16, 32, 4, 12, true, false);
      img.copyRect(44, 16, -8, 32, 4, 4, true, false);
      img.copyRect(48, 16, -8, 32, 4, 4, true, false);
      img.copyRect(40, 20, 0, 32, 4, 12, true, false);
      img.copyRect(44, 20, -8, 32, 4, 12, true, false);
      img.copyRect(48, 20, -16, 32, 4, 12, true, false);
      img.copyRect(52, 20, -8, 32, 4, 12, true, false);
      return img;
   }
}
