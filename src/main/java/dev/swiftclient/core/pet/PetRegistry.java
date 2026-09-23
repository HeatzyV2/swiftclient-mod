package dev.swiftclient.core.pet;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PetRegistry {
   private static final Map<String, PetRegistry.PetDef> PETS = new HashMap<>();

   public static boolean usesPlayerSkin(String id) {
      PetRegistry.PetDef def = get(id);
      return def != null && def.playerSkin;
   }

   private static String norm(String id) {
      return id == null ? "" : id.toLowerCase(Locale.ROOT);
   }

   public static void register(PetRegistry.PetDef def) {
      PETS.put(norm(def.id), def);
   }

   public static PetRegistry.PetDef get(String id) {
      return PETS.get(norm(id));
   }

   public static boolean exists(String id) {
      return PETS.containsKey(norm(id));
   }

   private PetRegistry() {
   }

   static {
      register(new PetRegistry.PetDef("light_dragon", "idle"));
      register(new PetRegistry.PetDef("mini_you", "idle", true));
   }

   public static final class PetDef {
      public final String id;
      public final String loopAnim;
      public final boolean playerSkin;

      public PetDef(String id, String loopAnim) {
         this(id, loopAnim, false);
      }

      public PetDef(String id, String loopAnim, boolean playerSkin) {
         this.id = id;
         this.loopAnim = loopAnim;
         this.playerSkin = playerSkin;
      }
   }
}
