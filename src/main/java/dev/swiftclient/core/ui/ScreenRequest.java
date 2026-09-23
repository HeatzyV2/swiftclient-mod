package dev.swiftclient.core.ui;

public final class ScreenRequest {
   private static UiScreen pending;

   private ScreenRequest() {
   }

   public static void open(UiScreen s) {
      pending = s;
   }

   public static UiScreen consume() {
      UiScreen p = pending;
      pending = null;
      return p;
   }
}
