package dev.swiftclient.modules;

import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.platform.Tr;
import dev.swiftclient.core.ui.Notifications;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Better screenshots (F2): copied to the clipboard, confirmed with a notification, chat kept clean. */
@ConsumedBy("ScreenshotMixin")
public final class ScreenshotModule extends Module {
   public final ModuleSetting clipboard;
   public final ModuleSetting quiet;

   public ScreenshotModule() {
      super("screenshot", "Screenshot", "Screenshots (F2) are copied to your clipboard, ready to paste on Discord, and confirmed with a notification.", "World", "screenshot", true);
      this.clipboard = this.toggle("clipboard", "Copy to clipboard", true).desc("Copy every screenshot so you can paste it right away (Windows, macOS, Linux with xclip).");
      this.quiet = this.toggle("quiet", "No chat message", false).desc("Do not write \"Saved screenshot as...\" in the chat. The Notifications module still confirms it.");
   }

   /** Wraps the vanilla callback that writes "Saved screenshot as ..." in the chat. */
   public static Consumer<Component> wrap(Consumer<Component> vanilla) {
      if (!ModuleManager.isLoaded() || !ModuleManager.get(ScreenshotModule.class).isEnabled()) {
         return vanilla;
      }

      ScreenshotModule m = ModuleManager.get(ScreenshotModule.class);
      return message -> {
         boolean ok = message.getContents() instanceof TranslatableContents tc && tc.getKey().equals("screenshot.success");
         if (ok) {
            latest().ifPresent(file -> {
               boolean copied = m.clipboard.boolValue() && copy(file);
               Module notifications = ModuleManager.byId(Notifications.MODULE_ID);
               if (notifications == null || notifications.setting("screenshots") == null || notifications.setting("screenshots").boolValue()) {
                  Notifications.push(Tr.of("swift.screenshot.saved"), copied ? Tr.of("swift.screenshot.copied", file.getName()) : file.getName(), "screenshot");
               }
            });
         }

         if (!ok || !m.quiet.boolValue()) {
            vanilla.accept(message);
         }
      };
   }

   private static Optional<File> latest() {
      File dir = new File(Minecraft.getInstance().gameDirectory, "screenshots");
      File[] files = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".png"));
      return files == null ? Optional.empty() : Arrays.stream(files).max(Comparator.comparingLong(File::lastModified));
   }

   /** The game runs headless (no AWT clipboard): the OS tools do the copy. Runs on the IO thread already. */
   private static boolean copy(File png) {
      String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      String path = png.getAbsolutePath();
      ProcessBuilder pb;
      if (os.contains("win")) {
         String ps = "Add-Type -AssemblyName System.Windows.Forms,System.Drawing; $i=[System.Drawing.Image]::FromFile('" + path.replace("'", "''")
            + "'); [System.Windows.Forms.Clipboard]::SetImage($i); $i.Dispose()";
         pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-STA", "-Command", ps);
      } else if (os.contains("mac")) {
         pb = new ProcessBuilder("osascript", "-e", "set the clipboard to (read (POSIX file \"" + path.replace("\"", "\\\"") + "\") as «class PNGf»)");
      } else {
         pb = new ProcessBuilder("xclip", "-selection", "clipboard", "-t", "image/png", "-i", path);
      }

      try {
         Process p = pb.redirectErrorStream(true).start();
         p.getInputStream().readAllBytes();
         return p.waitFor() == 0;
      } catch (Exception e) {
         Log.get("Screenshot").warn("Copie de la capture dans le presse-papiers impossible: {}", e.getMessage());
         return false;
      }
   }
}
