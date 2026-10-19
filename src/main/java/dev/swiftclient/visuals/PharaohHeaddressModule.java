package dev.swiftclient.visuals;

import dev.swiftclient.core.ui.Px;
import dev.swiftclient.core.mods.Module;
import dev.swiftclient.core.mods.ModuleSetting;
import java.util.Arrays;
import net.minecraft.util.ARGB;

public final class PharaohHeaddressModule extends HeadCosmeticModule {
   private static final String STYLE_NEMES = "Nemes";
   private static final String STYLE_KHEPRESH = "Khepresh";
   private static final String STYLE_SUN_DISC = "Sun Disc";
   private final ModuleSetting style = this.cycle("style", "Style", new String[]{"Nemes", "Khepresh", "Sun Disc"}, 0).desc("Shape of the headdress.");
   private final ModuleSetting stripes = this.slider("stripes", "Stripes", 5.0, 3.0, 9.0, 1.0, "").desc("Number of stripes on the cloth.");
   private final ModuleSetting collar = this.toggle("collar", "Broad collar", true).desc("A wide jewelled collar on the shoulders.");
   private final ModuleSetting size = this.slider("size", "Size", 1.0, 0.8, 1.3, 0.05, "").desc("Size of the headdress.");
   private final ModuleSetting color = this.cycle("color", "Color", new String[]{"Theme", "Manual"}, 0).desc("Theme uses the Swift accent colour, Manual the colours below.");
   private final ModuleSetting gold = this.color("manual_gold", "Gold color", -7688204).desc("Colour of the metal parts (Manual).");
   private final ModuleSetting inlay = this.color("manual_inlay", "Inlay color", -13738055).desc("Colour of the stripes (Manual).");
   private final ModuleSetting gem = this.color("manual_gem", "Gem color", -10313473).desc("Colour of the gem (Manual).");
   private final ModuleSetting gemGlow = this.toggle("gem_glow", "Gem glow", true).desc("The gem glows in the dark.");
   private final ModuleSetting glint = this.toggle("glint", "Glint", true).desc("A shine sweeps across the gold.");
   private final ModuleSetting friends = this.toggle("friends", "On friends", false).desc("Your friends wear it too (on your screen only).");
   private final PharaohMesh[] meshes = new PharaohMesh[4];
   private long styleRevision = -1L;
   private long stripesRevision = -1L;
   private long collarRevision = -1L;

   public PharaohHeaddressModule() {
      super("pharaoh_headdress", "Pharaoh Headdress", "Puts a pharaoh headdress on your player model. Only you see it.");
   }

   void reset() {
      super.reset();
      Arrays.fill(this.meshes, (Object)null);
      this.styleRevision = -1L;
   }

   PharaohMesh headMesh(boolean helmet) {
      this.refreshMeshes();
      int slot = helmet ? 1 : 0;
      PharaohMesh mesh = this.meshes[slot];
      if (mesh == null) {
         mesh = PharaohMesh.head(this.styleIndex(), this.stripes.intValue(), helmet);
         this.meshes[slot] = mesh;
      }

      return mesh;
   }

   PharaohMesh bodyMesh(boolean chest) {
      this.refreshMeshes();
      int slot = chest ? 3 : 2;
      PharaohMesh mesh = this.meshes[slot];
      if (mesh == null) {
         mesh = PharaohMesh.body(this.styleIndex(), this.stripes.intValue(), chest, this.collar.boolValue());
         this.meshes[slot] = mesh;
      }

      return mesh;
   }

   private void refreshMeshes() {
      long s = this.style.revision();
      long n = this.stripes.revision();
      long c = this.collar.revision();
      if (s != this.styleRevision || n != this.stripesRevision || c != this.collarRevision) {
         this.styleRevision = s;
         this.stripesRevision = n;
         this.collarRevision = c;
         Arrays.fill(this.meshes, (Object)null);
      }
   }

   private int styleIndex() {
      if (this.style.is("Khepresh")) {
         return 1;
      } else {
         return this.style.is("Sun Disc") ? 2 : 0;
      }
   }

   float scale() {
      return this.size.floatValue();
   }

   int[] palette() {
      int[] palette = new int[5];
      if (this.color.is("Manual")) {
         int metal = this.gold.colorValue() | -16777216;
         palette[0] = metal;
         palette[1] = this.inlay.colorValue() | -16777216;
         palette[2] = this.gem.colorValue() | -16777216;
         palette[3] = bronze(metal);
      } else {
         int accent = Px.ACCENT;
         int metal = metal(accent);
         palette[0] = metal;
         palette[1] = ARGB.scaleRGB(accent, 0.78F);
         palette[2] = gem(accent);
         palette[3] = ARGB.scaleRGB(metal, 0.62F);
      }

      return palette;
   }

   boolean glows() {
      return this.gemGlow.boolValue();
   }

   boolean glints() {
      return this.glint.boolValue();
   }

   boolean onFriends() {
      return this.friends.boolValue();
   }

   private static int bronze(int argb) {
      int r = (int)((float)(argb >> 16 & 255) * 0.7F);
      int g = (int)((float)(argb >> 8 & 255) * 0.55F);
      int b = (int)((float)(argb & 255) * 0.4F);
      return -16777216 | r << 16 | g << 8 | b;
   }
}
