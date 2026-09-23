package dev.swiftclient.core.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Distinctive Swift UI SFX — pitched vanilla stack, not the flat LC click. */
public final class SwiftSounds {
   private static long lastHoverMs;

   private SwiftSounds() {
   }

   public static void click() {
      try {
         var sm = Minecraft.getInstance().getSoundManager();
         sm.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.42F));
         sm.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.75F));
      } catch (Throwable ignored) {
      }
   }

   public static void whoosh() {
      try {
         var sm = Minecraft.getInstance().getSoundManager();
         sm.play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, 1.55F));
         sm.play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, 0.9F));
      } catch (Throwable ignored) {
      }
   }

   public static void hover() {
      long now = System.currentTimeMillis();
      if (now - lastHoverMs < 90L) {
         return;
      }
      lastHoverMs = now;
      try {
         Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.85F));
      } catch (Throwable ignored) {
      }
   }
}
