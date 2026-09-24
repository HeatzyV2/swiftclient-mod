package dev.swiftclient.core.ipc;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

public final class LauncherIpc {
   private static final PrintStream BRUT = ouvrirBrut();

   private LauncherIpc() {
   }

   public static void open(String page) {
      send("{\"action\":\"open\",\"page\":\"" + page + "\"}");
   }

   private static PrintStream ouvrirBrut() {
      try {
         return new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8");
      } catch (Exception var1) {
         return System.out;
      }
   }

   public static void send(String json) {
      synchronized (BRUT) {
         BRUT.println("[SC-IPC] " + json);
         BRUT.flush();
      }
   }
}
