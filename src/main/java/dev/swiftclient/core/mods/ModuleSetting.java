package dev.swiftclient.core.mods;

import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.platform.Platform;
import java.util.Locale;
import java.util.function.Supplier;
import org.slf4j.Logger;

/**
 * One module setting. Stored under {@code mod.<module>.<id>}:
 * toggles as booleans, sliders as numbers, colours as {@code #AARRGGBB}, cycles by option <b>key</b>
 * (e.g. {@code "custom_outline"}), never by position, so reordering options cannot change a choice.
 * Values written by older builds (cycle index, bare hex colour) are still read and rewritten.
 */
public final class ModuleSetting {
   private static final Logger LOG = Log.get("Config");
   public final String id;
   public final String name;
   public final ModuleSetting.Type type;
   private String description = "";
   private String group = "General";
   private boolean bool;
   private double value;
   private int color;
   public final double min;
   public final double max;
   public final double step;
   public final String unit;
   private Supplier<String> actionLabel;
   private Runnable action;
   private String[] options;
   private String[] optionKeys;
   private final String key;

   private ModuleSetting(
      String moduleId, String id, String name, ModuleSetting.Type type, boolean bool, double value, int color, double min, double max, double step, String unit
   ) {
      this.id = id;
      this.name = name;
      this.type = type;
      this.bool = bool;
      this.value = value;
      this.color = color;
      this.min = min;
      this.max = max;
      this.step = step;
      this.unit = unit;
      this.key = "mod." + moduleId + "." + id;
   }

   public static ModuleSetting toggle(String moduleId, String id, String name, boolean def) {
      return new ModuleSetting(moduleId, id, name, ModuleSetting.Type.TOGGLE, def, 0.0, 0, 0.0, 0.0, 0.0, "").loaded();
   }

   public static ModuleSetting slider(String moduleId, String id, String name, double def, double min, double max, double step, String unit) {
      return new ModuleSetting(moduleId, id, name, ModuleSetting.Type.SLIDER, false, def, 0, min, max, step, unit).loaded();
   }

   public static ModuleSetting color(String moduleId, String id, String name, int defArgb) {
      return new ModuleSetting(moduleId, id, name, ModuleSetting.Type.COLOR, false, 0.0, defArgb, 0.0, 0.0, 0.0, "").loaded();
   }

   public static ModuleSetting action(String moduleId, String id, String name, Supplier<String> buttonLabel, Runnable onClick) {
      ModuleSetting s = new ModuleSetting(moduleId, id, name, ModuleSetting.Type.ACTION, false, 0.0, 0, 0.0, 0.0, 0.0, "");
      s.actionLabel = buttonLabel;
      s.action = onClick;
      return s;
   }

   /**
    * {@code options} are the labels shown in the menu. Each one gets a stable storage key derived from it
    * ({@code "Custom outline"} → {@code "custom_outline"}): that key is what the config remembers.
    */
   public static ModuleSetting cycle(String moduleId, String id, String name, String[] options, int def) {
      ModuleSetting s = new ModuleSetting(moduleId, id, name, ModuleSetting.Type.CYCLE, false, def, 0, 0.0, 0.0, 0.0, "");
      s.options = options;
      s.optionKeys = new String[options.length];

      for (int i = 0; i < options.length; i++) {
         s.optionKeys[i] = optionKey(options[i]);
      }

      return s.loaded();
   }

   public static ModuleSetting key(String moduleId, String id, String name, int defGlfwKey) {
      return new ModuleSetting(moduleId, id, name, ModuleSetting.Type.KEY, false, defGlfwKey, 0, 0.0, 0.0, 0.0, "").loaded();
   }

   /** Storage key of a cycle option: lower case, anything else than a-z/0-9 becomes '_'. */
   public static String optionKey(String label) {
      String k = label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
      return k.isEmpty() ? "option" : k;
   }

   private ModuleSetting loaded() {
      this.load();
      return this;
   }

   public ModuleSetting desc(String d) {
      this.description = d == null ? "" : d;
      return this;
   }

   public ModuleSetting group(String g) {
      this.group = g != null && !g.isBlank() ? g : "General";
      return this;
   }

   public String description() {
      return this.description;
   }

   public String group() {
      return this.group;
   }

   public String actionLabel() {
      return this.actionLabel == null ? "" : this.actionLabel.get();
   }

   public void run() {
      if (this.action != null) {
         this.action.run();
      }
   }

   public int cycleIndex() {
      int n = this.options == null ? 1 : this.options.length;
      int i = (int)this.value;
      return (i % n + n) % n;
   }

   public String cycleLabel() {
      return this.options != null && this.options.length != 0 ? this.options[this.cycleIndex()] : "";
   }

   /** Storage key of the selected option. */
   public String cycleKey() {
      return this.optionKeys != null && this.optionKeys.length != 0 ? this.optionKeys[this.cycleIndex()] : "";
   }

   public String[] optionKeys() {
      return this.optionKeys == null ? new String[0] : this.optionKeys.clone();
   }

   public void cycleNext() {
      this.value = this.cycleIndex() + 1;
      this.save();
   }

   public int keyCode() {
      return (int)this.value;
   }

   public void setKey(int glfwKey) {
      this.value = glfwKey;
      this.save();
   }

   public String keyName() {
      return keyName(this.keyCode());
   }

   public static String keyName(int k) {
      if (k < 0) {
         return "None";
      } else if (k >= 65 && k <= 90) {
         return String.valueOf((char)k);
      } else if (k >= 48 && k <= 57) {
         return String.valueOf((char)k);
      } else if (k >= 290 && k <= 301) {
         return "F" + (k - 289);
      } else {
         return switch (k) {
            case 32 -> "Space";
            case 45 -> "-";
            case 61 -> "=";
            case 96 -> "`";
            case 257 -> "Enter";
            case 258 -> "Tab";
            case 259 -> "Backspace";
            case 260 -> "Insert";
            case 261 -> "Delete";
            case 262 -> "Right";
            case 263 -> "Left";
            case 264 -> "Down";
            case 265 -> "Up";
            case 266 -> "PageUp";
            case 267 -> "PageDown";
            case 268 -> "Home";
            case 269 -> "End";
            case 340 -> "L-Shift";
            case 341 -> "L-Ctrl";
            case 342 -> "L-Alt";
            case 344 -> "R-Shift";
            case 345 -> "R-Ctrl";
            case 346 -> "R-Alt";
            default -> "Key " + k;
         };
      }
   }

   public boolean boolValue() {
      return this.bool;
   }

   public double value() {
      return this.value;
   }

   public int colorValue() {
      return this.color;
   }

   public double fraction() {
      return this.max <= this.min ? 0.0 : (this.value - this.min) / (this.max - this.min);
   }

   public void setBool(boolean b) {
      this.bool = b;
      this.save();
   }

   public void setColor(int argb) {
      this.color = argb;
      this.save();
   }

   public void setFraction(double f) {
      f = f < 0.0 ? 0.0 : Math.min(f, 1.0);
      double raw = this.min + f * (this.max - this.min);
      if (this.step > 0.0) {
         raw = Math.round(raw / this.step) * this.step;
      }

      this.value = raw < this.min ? this.min : Math.min(raw, this.max);
      this.save();
   }

   public void toggle() {
      this.setBool(!this.bool);
   }

   // --- Storage format ---

   /** Value as stored (config and profiles). Null for actions, which hold no value. */
   public String exporter() {
      return switch (this.type) {
         case ACTION -> null;
         case TOGGLE -> this.bool ? "1" : "0";
         case COLOR -> String.format(Locale.ROOT, "#%08X", this.color);
         case CYCLE -> this.cycleKey();
         default -> Double.toString(this.value);
      };
   }

   /** Applies a stored value (profile import). Unreadable values are ignored. */
   public void importer(String v) {
      if (v != null && this.type != ModuleSetting.Type.ACTION && this.decode(v) != Decoded.INVALID) {
         this.save();
      }
   }

   private enum Decoded {
      CURRENT,
      LEGACY,
      INVALID;
   }

   /** Reads {@code v} into this setting. LEGACY means it was in an older format and should be rewritten. */
   private Decoded decode(String v) {
      try {
         switch (this.type) {
            case TOGGLE:
               this.bool = "1".equals(v) || "true".equalsIgnoreCase(v);
               return Decoded.CURRENT;
            case COLOR: {
               boolean current = v.startsWith("#");
               this.color = (int)Long.parseLong(current ? v.substring(1) : v, 16);
               return current ? Decoded.CURRENT : Decoded.LEGACY;
            }
            case CYCLE: {
               for (int i = 0; i < this.optionKeys.length; i++) {
                  if (this.optionKeys[i].equals(v)) {
                     this.value = i;
                     return Decoded.CURRENT;
                  }
               }

               // Older builds stored the position ("1.0"): keep the same choice, rewrite it as a key.
               int index = (int)Double.parseDouble(v);
               if (index >= 0 && index < this.optionKeys.length) {
                  this.value = index;
                  return Decoded.LEGACY;
               }

               LOG.warn("{} : option inconnue '{}', valeur par defaut conservee", this.key, v);
               return Decoded.INVALID;
            }
            default:
               this.value = Double.parseDouble(v);
               return Decoded.CURRENT;
         }
      } catch (RuntimeException e) {
         LOG.warn("{} : valeur illisible '{}', valeur par defaut conservee", this.key, v);
         return Decoded.INVALID;
      }
   }

   private void load() {
      if (this.type != ModuleSetting.Type.ACTION) {
         try {
            String v = Platform.game().getConfig(this.key, null);
            if (v != null && this.decode(v) == Decoded.LEGACY) {
               this.save();
            }
         } catch (Throwable ignored) {
         }
      }
   }

   private void save() {
      if (this.type != ModuleSetting.Type.ACTION) {
         try {
            Platform.game().setConfig(this.key, this.exporter());
         } catch (Throwable ignored) {
         }
      }
   }

   public static enum Type {
      TOGGLE,
      SLIDER,
      COLOR,
      ACTION,
      CYCLE,
      KEY;
   }
}
