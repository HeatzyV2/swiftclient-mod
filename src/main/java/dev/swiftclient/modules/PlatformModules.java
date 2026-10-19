package dev.swiftclient.modules;

import dev.swiftclient.core.mods.Module;
import java.util.List;

/** The modules that need the game. Kept apart from HudClient so tests can build them without the client. */
public final class PlatformModules {
   private PlatformModules() {
   }

   /** Modules that need the game, in no particular order (the menu order is ModuleManager.ORDER). */
   public static List<Module> all() {
      return List.of(
         new ZoomModule(),
         new FullBrightModule(),
         new ToggleSprintModule(),
         new ToggleSneakModule(),
         new AutoJumpModule(),
         new FreelookModule(),
         new WeatherChangerModule(),
         new StopwatchModule(),
         new SnaplookModule(),
         new HitColorModule(),
         new HitboxesModule(),
         new ItemPhysicsModule(),
         new UhcOverlayModule(),
         new TeamViewModule(),
         new WaypointsModule(),
         new LightOverlayModule(),
         new OldVisualsModule(),
         new MotionBlurModule(),
         new SaturationModule(),
         new FogModule(),
         new TimeChangerModule(),
         new ParticleChangerModule(),
         new NameTagsModule(),
         new Items2dModule(),
         new FireModule(),
         new ClearGlassModule(),
         new HideFoliageModule(),
         new BetterFoliageModule(),
         new ChunkBordersModule(),
         new WorldEditCuiModule(),
         new NickHiderModule(),
         new ScreenshotModule(),
         new ChatModule(),
         new AutoProfileModule(),
         new AuraModule(),
         new ModMenuModule(),
         new SkinChangeModule(),
         new dev.swiftclient.visuals.SwingAnimationModule(),
         new dev.swiftclient.visuals.HandsModule(),
         new dev.swiftclient.visuals.HandModelModule(),
         new dev.swiftclient.visuals.WeaponInspectModule(),
         new dev.swiftclient.visuals.TotemSpiritModule(),
         new dev.swiftclient.visuals.FloatingRelicsModule(),
         new dev.swiftclient.visuals.RelicParticlesModule(),
         new dev.swiftclient.visuals.JumpCirclesModule(),
         new dev.swiftclient.visuals.PharaohHeaddressModule(),
         new dev.swiftclient.visuals.AtmosphereModule(),
         new dev.swiftclient.visuals.LowHealthWarningModule(),
         new dev.swiftclient.visuals.CustomGlintModule(),
         new dev.swiftclient.visuals.HitParticlesModule(),
         new dev.swiftclient.modules.CoordSnapperModule(),
         new dev.swiftclient.modules.ElytraSwapModule(),
         new dev.swiftclient.modules.AutoReconnectModule(),
         new dev.swiftclient.modules.WeatherNotifierModule(),
         new dev.swiftclient.modules.FriendGuardModule(),
         new dev.swiftclient.modules.BorderlessModule(),
         new dev.swiftclient.modules.HideInventoryModule(),
         new dev.swiftclient.visuals.HandAuraModule()
      );
   }
}
