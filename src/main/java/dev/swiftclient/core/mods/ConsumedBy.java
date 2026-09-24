package dev.swiftclient.core.mods;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares where a module's behaviour lives when it is not in its own lifecycle hooks, e.g. a mixin
 * that reads its settings. Required by {@link ModuleManager#register} for modules without hooks.
 *
 * <p>Names are strings on purpose: referencing a mixin class literal would load it, which Mixin forbids.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConsumedBy {
   /** Simple names of the classes that implement the behaviour (e.g. "HudCrosshairMixin"). */
   String[] value();
}
