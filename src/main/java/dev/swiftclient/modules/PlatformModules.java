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
         new ModMenuModule()
      );
   }
}
