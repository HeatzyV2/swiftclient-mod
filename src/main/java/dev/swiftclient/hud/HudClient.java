package dev.swiftclient.hud;

import dev.swiftclient.core.cosmetics.CapeUploadQueue;
import dev.swiftclient.core.cosmetics.CosmeticHttp;
import dev.swiftclient.core.cosmetics.HeartbeatManager;
import dev.swiftclient.core.gfx.Shaders;
import dev.swiftclient.core.hud.HudManager;
import dev.swiftclient.core.hud.HudVisibilite;
import dev.swiftclient.core.mods.CapeSimManager;
import dev.swiftclient.core.mods.ModsScreen;
import dev.swiftclient.core.mods.ModuleManager;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.core.mods.RealisticCapeState;
import dev.swiftclient.core.mods.ZoomState;
import dev.swiftclient.core.mods.modules.CrosshairModule;
import dev.swiftclient.core.mods.modules.FreelookModule;
import dev.swiftclient.core.mods.modules.GuiBlurModule;
import dev.swiftclient.freelook.Freelook;
import dev.swiftclient.mixin.SimpleOptionAccessor;
import dev.swiftclient.platform.CanvasImpl;
import dev.swiftclient.platform.CoreScreenHost;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class HudClient {
   private static boolean menuKeyWasDown = false;
   private static Double savedGamma;
   private static Boolean savedAutoJump;
   private static Integer savedFov;
   private static boolean sprintForced = false;
   private static boolean sneakForced = false;
   private static final HudDataImpl HUD_DATA = new HudDataImpl();

   private HudClient() {
   }

   public static void init() {
      ModuleManager.init();
      if (CosmeticHttp.backendConfigured()) {
         HeartbeatManager.start(() -> Minecraft.getInstance().player != null);
      }
      ModuleManager.register(new CrosshairModule());
      ModuleManager.register(new FreelookModule());
      ModuleManager.register(new GuiBlurModule());
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)mc -> {
         CapeUploadQueue.drain(8);
         long handle = mc.getWindow().handle();
         boolean down = GLFW.glfwGetKey(handle, 344) == 1;
         if (down && !menuKeyWasDown && mc.gui.screen() == null && mc.player != null) {
            mc.setScreenAndShow(new CoreScreenHost(new ModsScreen(), null));
         }

         menuKeyWasDown = down;
         Freelook.tick(mc);
         applyEffects(mc);
         tickCapes(mc);
      });
      HudElementRegistry.attachElementAfter(
         VanillaHudElements.MISC_OVERLAYS, Identifier.fromNamespaceAndPath("swiftclient", "hud"), (graphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            zoomFrame(mc);
            Screen ecran = mc.gui.screen();
            if (HudVisibilite.afficher(ecran != null, ecran instanceof ChatScreen)) {
               HudManager.setGuiScale(mc.getWindow().getGuiScale());
               HudManager.render(new CanvasImpl(graphics), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), HUD_DATA);
            }
         }
      );
   }

   /** Beyond this distance the cape is drawn with the plain vanilla model (see CapeLayerMixin). */
   private static final double CAPE_SIM_RANGE_SQ = 32.0 * 32.0;

   private static void tickCapes(Minecraft mc) {
      if (RealisticCapeState.active && mc.level != null && mc.player != null) {
         long now = System.currentTimeMillis();
         Set<UUID> simulated = new HashSet<>();

         for (AbstractClientPlayer p : mc.level.players()) {
            if (p == mc.player || p.distanceToSqr(mc.player) <= CAPE_SIM_RANGE_SQ) {
               simulated.add(p.getUUID());
               CapeSimManager.tick(p.getUUID(), p.getX(), p.getY(), p.getZ(), p.yOld, p.yBodyRot, p.isCrouching(), p.isUnderWater(), now);
            }
         }

         CapeSimManager.retainOnly(simulated);
      } else {
         CapeSimManager.clear();
      }
   }

   public static void zoomFrame(Minecraft mc) {
      if (savedFov != null) {
         OptionInstance<Integer> fov = mc.options.fov();
         if (!ZoomState.holding && ZoomState.arrive()) {
            fov.set(savedFov);
            savedFov = null;
         } else {
            ((SimpleOptionAccessor)(Object)fov).swiftclient$setRaw((int)Math.round(ZoomState.courant()));
         }
      }
   }

   private static void applyEffects(Minecraft mc) {
      if (mc.level != null && ModuleManager.active("norain")) {
         mc.level.setRainLevel(0.0F);
         mc.level.setThunderLevel(0.0F);
      }

      OptionInstance<Double> gamma = mc.options.gamma();
      if (ModuleManager.active("fullbright")) {
         if (savedGamma == null) {
            savedGamma = (Double)gamma.get();
         }

         ModuleSetting lvl = ModuleManager.byId("fullbright").setting("level");
         ((SimpleOptionAccessor)(Object)gamma).swiftclient$setRaw(Shaders.gammaFullBright(lvl != null ? lvl.value() : 100.0));
      } else if (savedGamma != null) {
         gamma.set(savedGamma);
         savedGamma = null;
      }

      OptionInstance<Integer> fov = mc.options.fov();
      ModuleSetting zf = ModuleManager.byId("zoom").setting("fov");
      ZoomState.ensureTarget(zf != null ? zf.value() : 30.0);
      ModuleSetting zk = ModuleManager.byId("zoom").setting("key");
      ModuleSetting zm = ModuleManager.byId("zoom").setting("mode");
      ModuleSetting zs = ModuleManager.byId("zoom").setting("smooth");
      ModuleSetting zd = ModuleManager.byId("zoom").setting("duration");
      int zoomKey = zk != null ? zk.keyCode() : 67;
      boolean zoomOn = ModuleManager.active("zoom") && mc.player != null;
      boolean zoomKeyDown = zoomOn && mc.gui.screen() == null && GLFW.glfwGetKey(mc.getWindow().handle(), zoomKey) == 1;
      if (!zoomOn) {
         ZoomState.stop();
      } else {
         ZoomState.update(zoomKeyDown, zm != null && zm.cycleIndex() == 1);
      }

      boolean zoomSmooth = zs == null || zs.boolValue();
      double zoomDuree = zd != null ? zd.value() : 180.0;
      boolean zoomHeld = ZoomState.holding;
      if (zoomHeld) {
         if (savedFov == null) {
            savedFov = (Integer)fov.get();
            ZoomState.depuis(savedFov.intValue());
         }

         ZoomState.viser(ZoomState.target(), zoomSmooth, zoomDuree);
      } else if (savedFov != null) {
         ZoomState.viser(savedFov.intValue(), zoomSmooth, zoomDuree);
      }

      zoomFrame(mc);
      boolean wantAutoJump = ModuleManager.active("autojump");
      OptionInstance<Boolean> autoJump = mc.options.autoJump();
      if ((Boolean)autoJump.get() != wantAutoJump) {
         autoJump.set(wantAutoJump);
      }

      boolean inGame = mc.player != null && mc.gui.screen() == null;
      if (inGame && ModuleManager.active("togglesprint")) {
         ModuleSetting back = ModuleManager.byId("togglesprint").setting("backwards");
         boolean allowBack = back != null && back.boolValue();
         boolean move = mc.player.zza > 0.0F || allowBack && mc.player.zza < 0.0F;
         mc.options.keySprint.setDown(move);
         sprintForced = true;
      } else if (sprintForced) {
         mc.options.keySprint.setDown(false);
         sprintForced = false;
      }

      if (inGame && ModuleManager.active("togglesneak")) {
         mc.options.keyShift.setDown(true);
         sneakForced = true;
      } else if (sneakForced) {
         mc.options.keyShift.setDown(false);
         sneakForced = false;
      }

      RealisticCapeState.active = ModuleManager.active("realisticcape");
   }
}
