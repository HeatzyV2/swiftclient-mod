package dev.swiftclient.core.relay;

import dev.swiftclient.core.log.Log;
import dev.swiftclient.core.net.Backend;
import dev.swiftclient.core.net.Endpoints;
import org.slf4j.Logger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class RelayClient {
   private static final Logger LOG = Log.get("Relay");
   private static final int CONNECT_TIMEOUT_MS = 8000;
   private final String sessionId;
   private final int lanPort;
   private final Consumer<Integer> onPortAllocated;
   private final Consumer<String> onError;
   private final AtomicBoolean alive = new AtomicBoolean(false);
   private Socket controlSock;

   public RelayClient(int lanPort, Consumer<Integer> onPortAllocated, Consumer<String> onError) {
      this.sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
      this.lanPort = lanPort;
      this.onPortAllocated = onPortAllocated;
      this.onError = onError;
   }

   public String getSessionId() {
      return this.sessionId;
   }

   public boolean isAlive() {
      return this.alive.get();
   }

   public void start() {
      new Thread(this::runControl, "SwiftClient-Relay-Control").start();
   }

   public void stop() {
      this.alive.set(false);

      try {
         if (this.controlSock != null) {
            this.controlSock.close();
         }
      } catch (IOException ignored) {
      }
   }

   private void runControl() {
      try {
         // The relay only serves signed-in players: it gets the backend token of this session.
         String token = Backend.session();
         if (token == null) {
            this.onError.accept("no backend session");
            return;
         }

         this.controlSock = new Socket();
         this.controlSock.connect(new InetSocketAddress(Endpoints.relayHost(), Endpoints.relayPort()), 8000);
         this.controlSock.setKeepAlive(true);
         this.alive.set(true);
         BufferedReader in = new BufferedReader(new InputStreamReader(this.controlSock.getInputStream()));
         BufferedWriter out = new BufferedWriter(new OutputStreamWriter(this.controlSock.getOutputStream()));
         out.write("REGISTER " + this.sessionId + " " + token + "\n");
         out.flush();

         String line;
         while ((line = in.readLine()) != null) {
            if (line.startsWith("PORT ")) {
               int port = Integer.parseInt(line.substring(5).trim());
               LOG.info("Session relais {} -> port {}", this.sessionId, port);
               this.onPortAllocated.accept(port);
            } else if (line.startsWith("NEW ")) {
               int connId = Integer.parseInt(line.substring(4).trim());
               new Thread(() -> this.handleNewClient(connId), "SwiftClient-Relay-Data-" + connId).start();
            } else if (line.startsWith("ERROR ")) {
               this.onError.accept(line.substring(6));
               break;
            }
         }
      } catch (Throwable e) {
         this.onError.accept(e.getMessage() != null ? e.getMessage() : e.toString());
      } finally {
         this.alive.set(false);

         try {
            if (this.controlSock != null) {
               this.controlSock.close();
            }
         } catch (IOException ignored) {
         }
      }
   }

   private void handleNewClient(int connId) {
      Socket dataSock = null;
      Socket lanSock = null;

      try {
         dataSock = new Socket();
         dataSock.connect(new InetSocketAddress(Endpoints.relayHost(), Endpoints.relayPort()), 8000);
         dataSock.setTcpNoDelay(true);
         String header = "DATA " + this.sessionId + " " + connId + "\n";
         dataSock.getOutputStream().write(header.getBytes());
         dataSock.getOutputStream().flush();
         lanSock = new Socket();
         lanSock.connect(new InetSocketAddress("127.0.0.1", this.lanPort), 8000);
         lanSock.setTcpNoDelay(true);
         pipe(dataSock, lanSock);
      } catch (Throwable ex) {
         LOG.warn("Connexion relais {} echouee : {}", connId, ex.getMessage());

         try {
            if (dataSock != null) {
               dataSock.close();
            }
         } catch (IOException ignored) {
         }

         try {
            if (lanSock != null) {
               lanSock.close();
            }
         } catch (IOException ignored) {
         }
      }
   }

   private static void pipe(Socket a, Socket b) {
      Thread t1 = new Thread(() -> copyStream(a, b), "Relay-pipe-AB");
      Thread t2 = new Thread(() -> copyStream(b, a), "Relay-pipe-BA");
      t1.setDaemon(true);
      t2.setDaemon(true);
      t1.start();
      t2.start();
   }

   private static void copyStream(Socket from, Socket to) {
      byte[] buf = new byte[8192];

      try {
         int n;
         try (
            InputStream in = from.getInputStream();
            OutputStream out = to.getOutputStream();
         ) {
            while ((n = in.read(buf)) > 0) {
               out.write(buf, 0, n);
               out.flush();
            }
         } catch (IOException ignored) {
         }
      } finally {
         try {
            from.close();
         } catch (IOException ignored) {
         }

         try {
            to.close();
         } catch (IOException ignored) {
         }
      }
   }
}
