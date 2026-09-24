package dev.swiftclient.core.mods;

import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.platform.Tr;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A toggleable feature. Behaviour lives in the lifecycle hooks ({@link #onEnable}, {@link #onDisable},
 * {@link #onTick}, {@link #onRender}); a module whose behaviour is read elsewhere (mixin, HUD renderer)
 * declares it with {@link ConsumedBy}. {@link ModuleManager#register} refuses a module that has neither,
 * so a module cannot be shown in the menu without doing anything.
 *
 * <p>Settings are declared as fields through the factories below ({@code toggle}, {@code slider}...),
 * and read through those fields instead of looking them up by id.
 */
public abstract class Module {
   public final String id;
   public final String name;
   public final String description;
   public final String category;
   public final String icon;
   public final boolean swiftPlus;
   private boolean enabled;
   private final boolean defEnabled;
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

   // --- Lifecycle, driven by ModuleManager. Only called once the manager has started. ---

   /** The module was switched on, or was already on when the game started. */
   protected void onEnable() {
   }

   /**
    * The module was switched off, or starts off. Must undo whatever onEnable/onTick changed, and be safe to
    * call when there is nothing to undo.
    */
   protected void onDisable() {
   }

   /** Every client tick while enabled. */
   protected void onTick() {
   }

   /** Every rendered frame while enabled, before the HUD is drawn. */
   protected void onRender(float partialTick) {
   }

   // --- Settings factories: the module id is filled in, the setting is registered in declaration order. ---

   protected final ModuleSetting toggle(String settingId, String label, boolean def) {
      return this.add(ModuleSetting.toggle(this.id, settingId, label, def));
   }

   protected final ModuleSetting slider(String settingId, String label, double def, double min, double max, double step, String unit) {
      return this.add(ModuleSetting.slider(this.id, settingId, label, def, min, max, step, unit));
   }

   protected final ModuleSetting color(String settingId, String label, int defArgb) {
      return this.add(ModuleSetting.color(this.id, settingId, label, defArgb));
   }

   protected final ModuleSetting cycle(String settingId, String label, String[] options, int def) {
      return this.add(ModuleSetting.cycle(this.id, settingId, label, options, def));
   }

   protected final ModuleSetting action(String settingId, String label, Supplier<String> buttonLabel, Runnable onClick) {
      return this.add(ModuleSetting.action(this.id, settingId, label, buttonLabel, onClick));
   }

   private ModuleSetting add(ModuleSetting s) {
      this.settings.add(s);
      return s;
   }

   // --- Display (translated; the fields above stay the identity used in config and code) ---

   /** Name shown in menus: {@code swift.module.<id>.name}, or the declared name. */
   public String displayName() {
      return Tr.orDefault("swift.module." + this.id + ".name", this.name);
   }

   public String displayDescription() {
      return Tr.orDefault("swift.module." + this.id + ".desc", this.description);
   }

   public String displayCategory() {
      return categoryLabel(this.category);
   }

   public static String categoryLabel(String category) {
      return Tr.orDefault("swift.category." + ModuleSetting.optionKey(category), category);
   }

   // --- State ---

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean e) {
      if (this.enabled != e) {
         this.enabled = e;
         this.save();
         ModuleManager.onToggled(this);
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

   /** Lookup by id, for generic code (menu, profiles, HUD elements). Modules use their fields. */
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
      } catch (Throwable ignored) {
      }
   }

   private void save() {
      try {
         Platform.game().setConfig("mod." + this.id + ".enabled", this.enabled ? "1" : "0");
      } catch (Throwable ignored) {
      }
   }

   // Package-private entry points so only ModuleManager drives the lifecycle.

   final void fireEnable() {
      this.onEnable();
   }

   final void fireDisable() {
      this.onDisable();
   }

   final void fireTick() {
      this.onTick();
   }

   final void fireRender(float partialTick) {
      this.onRender(partialTick);
   }
}
