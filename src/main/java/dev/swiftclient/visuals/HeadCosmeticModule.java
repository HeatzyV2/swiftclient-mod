package dev.swiftclient.visuals;

import dev.swiftclient.compat.Mc;
import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.social.Friends;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import java.lang.ref.WeakReference;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

abstract class HeadCosmeticModule extends Module {
   static volatile HeadCosmeticModule worn;
   static final String COLOR_THEME = "Theme";
   static final String COLOR_MANUAL = "Manual";
   private static final long EPOCH = System.nanoTime();
   private static final long LOOK_REFRESH_NANOS = 4000000L;
   private static final long FRIEND_REFRESH_NANOS = 1000000000L;
   private static final float GLINT_CYCLE_SECONDS = 3.6F;
   private static final float GLINT_STRENGTH = 0.6F;
   private PharaohMesh.Look look;
   private long lookAt;
   private boolean mobileLook;
   private long lookRevision;
   private int lookAccent;
   private final Int2BooleanOpenHashMap friendCache = new Int2BooleanOpenHashMap();
   private WeakReference<ClientLevel> friendLevel = new WeakReference((Object)null);
   private long friendsCheckedAt;

   HeadCosmeticModule(String id, String name, String description) {
      super(id, name, description, "Visual", "crown", false);
   }

   abstract PharaohMesh headMesh(boolean var1);

   abstract PharaohMesh bodyMesh(boolean var1);

   abstract float scale();

   abstract int[] palette();

   abstract boolean glows();

   abstract boolean glints();

   abstract boolean onFriends();

   @Override

   protected void onEnable() {
      this.reset();
      worn = this;
   }

   @Override

   protected void onDisable() {
      if (worn == this) {
         worn = null;
      }

      this.reset();
   }

   void reset() {
      this.look = null;
      this.friendCache.clear();
      this.friendLevel = new WeakReference((Object)null);
   }

   final boolean wears(AvatarRenderState state) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer self = mc.player;
      if (self != null && mc.level != null) {
         if (state.id == self.getId()) {
            return !mc.options.getCameraType().isFirstPerson() || Mc.screen() != null;
         } else {
            return this.onFriends() && this.isFriend(mc, state.id);
         }
      } else {
         return false;
      }
   }

   private boolean isFriend(Minecraft mc, int id) {
      long now = System.nanoTime();
      if (this.friendLevel.get() != mc.level) {
         this.friendLevel = new WeakReference(mc.level);
         this.friendCache.clear();
      }

      if (now - this.friendsCheckedAt > 1000000000L) {
         this.friendCache.clear();
         this.friendsCheckedAt = now;
      }

      if (this.friendCache.containsKey(id)) {
         return this.friendCache.get(id);
      } else {
         boolean var10000;
         label20: {
            Entity entity = mc.level.getEntity(id);
            if (entity instanceof Player) {
               Player player = (Player)entity;
               if (Friends.isFriend(player.getGameProfile().name())) {
                  var10000 = true;
                  break label20;
               }
            }

            var10000 = false;
         }

         boolean friend = var10000;
         this.friendCache.put(id, friend);
         return friend;
      }
   }

   final PharaohMesh.Look look() {
      long now = System.nanoTime();
      PharaohMesh.Look current = this.look;
      boolean mobile = false;
      long revision = 0L;
      int accent = mobile ? Px.ACCENT : 0;
      long refresh = mobile ? 33333333L : 4000000L;
      if (current == null || this.mobileLook != mobile || now - this.lookAt >= refresh || mobile && (this.lookRevision != revision || this.lookAccent != accent)) {
         float seconds = (float)(now - EPOCH) / 1.0E9F;
         float phase = seconds % 3.6F / 3.6F;
         float sweep = (phase * 2.0F - 1.0F) * (float)Math.PI * 1.25F;
         float envelope = Mth.sin((phase * (float)Math.PI));
         float gain = this.glints() ? 0.6F * envelope * envelope : 0.0F;
         float pulse = 0.9F + 0.1F * Mth.sin((seconds * 2.2F));
         current = new PharaohMesh.Look(this.palette(), this.glows(), Mth.cos(sweep), Mth.sin(sweep), gain, pulse);
         this.look = current;
         this.lookAt = now;
         this.mobileLook = mobile;
         this.lookRevision = revision;
         this.lookAccent = accent;
         return current;
      } else {
         return current;
      }
   }

   static int metal(int accent) {
      return shade(accent, 0.55F, 0.96F);
   }

   static int gem(int accent) {
      return shade(accent, 1.15F, 1.0F);
   }

   static int shade(int argb, float saturation, float value) {
      int r = argb >> 16 & 255;
      int g = argb >> 8 & 255;
      int b = argb & 255;
      int max = Math.max(r, Math.max(g, b));
      int chroma = max - Math.min(r, Math.min(g, b));
      float hue = 0.0F;
      if (chroma > 0) {
         if (max == r) {
            hue = (float)(g - b) / (float)chroma;
         } else if (max == g) {
            hue = 2.0F + (float)(b - r) / (float)chroma;
         } else {
            hue = 4.0F + (float)(r - g) / (float)chroma;
         }

         hue /= 6.0F;
         if (hue < 0.0F) {
            ++hue;
         }
      }

      float s = max == 0 ? 0.0F : (float)chroma / (float)max;
      return Mth.hsvToArgb(hue, Math.min(1.0F, s * saturation), Math.min(1.0F, value), 255);
   }
}
