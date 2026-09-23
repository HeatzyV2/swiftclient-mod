package dev.swiftclient.core.cosmetics;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class CapeUploadQueue {
   private static final Queue<Runnable> Q = new ConcurrentLinkedQueue<>();

   private CapeUploadQueue() {
   }

   public static void enqueue(Runnable r) {
      Q.add(r);
   }

   public static void drain(int budget) {
      for (int i = 0; i < budget; i++) {
         Runnable r = Q.poll();
         if (r == null) {
            return;
         }

         try {
            r.run();
         } catch (Throwable var4) {
         }
      }
   }
}
