package dev.swiftclient.core.mods;

import dev.swiftclient.core.platform.Platform;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Module {
   public final String id;
   public final String name;
   public final String description;
   public final String category;
   public final String icon;
   public final boolean swiftPlus;
   private boolean enabled;
   private final boolean defEnabled;
   private boolean implemented = true;
   protected final List<ModuleSetting> settings = new ArrayList<>();

   protected Module(String id, String name, String description, String category, String icon, boolean defEnabled) {
      this(id, name, description, category, icon, defEnabled, false);
   }

   protected Module(String id, String name, String description, String category, String icon, boolean defEnabled, boolean swiftPlus) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.category = category;
      this.icon = icon;
      this.swiftPlus = swiftPlus;
      this.defEnabled = defEnabled;
      this.enabled = defEnabled;
   }

   /** Stays false for a module without behaviour, whatever its saved state says. */
   public boolean isEnabled() {
      return this.implemented && this.enabled;
   }

   /**
    * False for modules declared ahead of their implementation. They stay registered so their
    * saved values survive in swiftclient.properties and in profiles, but they are hidden from
    * the UI and never report themselves as enabled.
    */
   public boolean implemented() {
      return this.implemented;
   }

   protected final void notImplemented() {
      this.implemented = false;
   }

   public void setEnabled(boolean e) {
      if (this.enabled != e) {
         this.enabled = e;
         this.save();
      }
   }

   public void toggle() {
      this.setEnabled(!this.enabled);
   }

   public List<ModuleSetting> settings() {
      return this.settings;
   }

   public boolean hasSettings() {
      return !this.settings.isEmpty();
   }

   public ModuleSetting setting(String settingId) {
      for (ModuleSetting s : this.settings) {
         if (s.id.equals(settingId)) {
            return s;
         }
      }

      return null;
   }

   public Map<String, String> exporter() {
      Map<String, String> out = new LinkedHashMap<>();
      out.put("enabled", this.enabled ? "1" : "0");

      for (ModuleSetting s : this.settings) {
         String v = s.exporter();
         if (v != null) {
            out.put(s.id, v);
         }
      }

      return out;
   }

   public void importer(Map<String, String> etat) {
      if (etat != null) {
         String e = etat.get("enabled");
         if (e != null) {
            this.setEnabled("1".equals(e));
         }

         for (ModuleSetting s : this.settings) {
            s.importer(etat.get(s.id));
         }
      }
   }

   void load() {
      try {
         this.enabled = !"0".equals(Platform.game().getConfig("mod." + this.id + ".enabled", this.defEnabled ? "1" : "0"));
      } catch (Throwable var2) {
      }
   }

   private void save() {
      try {
         Platform.game().setConfig("mod." + this.id + ".enabled", this.enabled ? "1" : "0");
      } catch (Throwable var2) {
      }
   }
}
