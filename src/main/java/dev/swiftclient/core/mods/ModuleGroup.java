package dev.swiftclient.core.mods;

import dev.swiftclient.core.platform.Platform;
import dev.swiftclient.core.platform.Tr;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * One tile of the menu standing for several modules ("parts"). The parts keep their own id, settings and
 * behaviour, so saved configs and profiles stay valid; they are only hidden from the grid and shown as the
 * tabs of the group. The switch of the group is a master switch: a part runs when it is on and its group is on.
 */
@ConsumedBy({"ModuleManager", "ModsScreen"})
public final class ModuleGroup extends Module {
   private final List<String> childIds;
   private final List<Module> children = new ArrayList<>();
   private final List<String[]> exclusive = new ArrayList<>();
   private boolean alwaysOn;

   public ModuleGroup(String id, String name, String description, String category, String icon, String... childIds) {
      super(id, name, description, category, icon, false);
      this.childIds = List.of(childIds);
   }

   /** Only one of {@code a} and {@code b} can be on: switching one on switches the other off. */
   public ModuleGroup exclusive(String a, String b) {
      this.exclusive.add(new String[]{a, b});
      return this;
   }

   /** A hub of client settings: no master switch, the tile opens the settings. */
   public ModuleGroup alwaysOn() {
      this.alwaysOn = true;
      return this;
   }

   public boolean isAlwaysOn() {
      return this.alwaysOn;
   }

   public List<String> childIds() {
      return this.childIds;
   }

   public List<Module> children() {
      return Collections.unmodifiableList(this.children);
   }

   @Override
   public boolean isEnabled() {
      return this.alwaysOn || super.isEnabled();
   }

   @Override
   public void setEnabled(boolean e) {
      if (!this.alwaysOn) {
         super.setEnabled(e);
      }
   }

   @Override
   public boolean hasSettings() {
      return !this.children.isEmpty();
   }

   /** Attaches the parts, once they are all registered. A group never saved yet is on when one of its parts is. */
   void link(Function<String, Module> lookup) {
      for (String id : this.childIds) {
         Module m = lookup.apply(id);
         if (m != null && m != this && m.parent == null && !(m instanceof ModuleGroup)) {
            m.parent = this;
            this.children.add(m);
         }
      }

      String saved = null;
      try {
         saved = Platform.game().getConfig("mod." + this.id + ".enabled", null);
      } catch (Throwable ignored) {
      }

      if (saved == null && !this.alwaysOn) {
         boolean any = false;
         for (Module c : this.children) {
            any |= c.rawEnabled();
         }

         this.setRaw(any);
      }
   }

   /** Switching the group on with every part off would do nothing: the first part comes on. */
   void ensureOneChild() {
      for (Module c : this.children) {
         if (c.rawEnabled()) {
            return;
         }
      }

      if (!this.children.isEmpty()) {
         this.children.get(0).setRaw(true);
      }
   }

   void childEnabled(Module child) {
      for (String[] pair : this.exclusive) {
         String other = pair[0].equals(child.id) ? pair[1] : (pair[1].equals(child.id) ? pair[0] : null);
         if (other != null) {
            for (Module c : this.children) {
               if (c.id.equals(other) && c.rawEnabled()) {
                  c.setEnabled(false);
               }
            }
         }
      }
   }

   /** The switch of one part, as a setting row shown on top of the tab of that part. */
   public static ModuleSetting partSwitch(Module part) {
      return ModuleSetting.bound(part.id, "enabled", Tr.of("swift.mods.part_enabled"), part::rawEnabled, part::setEnabled)
         .desc(part.displayDescription());
   }
}
