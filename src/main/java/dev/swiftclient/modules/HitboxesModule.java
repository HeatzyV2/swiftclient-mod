package dev.swiftclient.modules;

import dev.swiftclient.core.mods.ConsumedBy;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import dev.swiftclient.world.WorldOverlays;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Entity hitboxes with their own colour per kind, and where each one is looking. */
@ConsumedBy("WorldOverlays")
public final class HitboxesModule extends Module implements WorldOverlays.WorldOverlay {
   public final ModuleSetting players;
   public final ModuleSetting mobs;
   public final ModuleSetting items;
   public final ModuleSetting projectiles;
   public final ModuleSetting lookLine;
   public final ModuleSetting playerColor;
   public final ModuleSetting mobColor;
   public final ModuleSetting range;

   public HitboxesModule() {
      super("hitboxes", "Hitboxes", "Shows entity hitboxes, coloured by kind, and the direction each one looks.", "PvP", "hitbox", false);
      this.players = this.toggle("players", "Players", true).desc("Outline other players.").group("Entities");
      this.mobs = this.toggle("mobs", "Mobs", true).desc("Outline animals and monsters.").group("Entities");
      this.items = this.toggle("items", "Dropped items", false).desc("Outline items lying on the ground.").group("Entities");
      this.projectiles = this.toggle("projectiles", "Projectiles", true).desc("Outline arrows, pearls, snowballs...").group("Entities");
      this.lookLine = this.toggle("look", "Look direction", true).desc("Draw a short line showing where players and mobs look.").group("Style");
      this.playerColor = this.color("player_color", "Player color", 0xFFFFFFFF).desc("Colour of player hitboxes.").group("Style");
      this.mobColor = this.color("mob_color", "Mob color", 0xFFF2C94C).desc("Colour of mob, item and projectile hitboxes.").group("Style");
      this.range = this.slider("range", "Range", 32.0, 8.0, 64.0, 8.0, "").desc("Entities further than this are skipped.").group("Style");
   }

   @Override
   public void emitGizmos(Minecraft mc, Vec3 camera, float partialTick) {
      double r = this.range.value();
      boolean firstPerson = mc.options.getCameraType().isFirstPerson();

      for (Entity e : mc.level.entitiesForRendering()) {
         if (e == mc.player && firstPerson || e.isInvisible() && !(e instanceof Player) || e.distanceToSqr(mc.player) > r * r) {
            continue;
         }

         int color;
         if (e instanceof Player) {
            if (!this.players.boolValue()) {
               continue;
            }

            color = this.playerColor.colorValue();
         } else if (e instanceof ItemEntity) {
            if (!this.items.boolValue()) {
               continue;
            }

            color = this.mobColor.colorValue();
         } else if (e instanceof Projectile) {
            if (!this.projectiles.boolValue()) {
               continue;
            }

            color = this.mobColor.colorValue();
         } else {
            if (!this.mobs.boolValue() || !e.isAttackable()) {
               continue;
            }

            color = this.mobColor.colorValue();
         }

         Vec3 now = e.getPosition(partialTick);
         AABB box = e.getBoundingBox().move(now.subtract(e.position()));
         Gizmos.cuboid(box, GizmoStyle.stroke(color, 1.5F));
         if (this.lookLine.boolValue() && !(e instanceof ItemEntity) && !(e instanceof Projectile)) {
            Vec3 eye = now.add(0.0, e.getEyeHeight(), 0.0);
            Gizmos.line(eye, eye.add(e.getViewVector(partialTick).scale(2.0)), 0xFF3B82F6, 1.5F);
         }
      }
   }
}
