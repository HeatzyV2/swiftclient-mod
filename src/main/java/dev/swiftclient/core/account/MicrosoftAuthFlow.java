package dev.swiftclient.core.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MicrosoftAuthFlow {
   private static final String CLIENT_ID = "c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb";
   private static final String AUTH_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";
   private static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
   private static final String XBL_URL = "https://user.auth.xboxlive.com/user/authenticate";
   private static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
   private static final String MC_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
   private static final String MC_PROF_URL = "https://api.minecraftservices.com/minecraft/profile";
   private static final int BROWSER_TIMEOUT_MS = 180000;
   private final HttpClient http = HttpClient.newHttpClient();

   public CompletableFuture<AccountEntry> browserLogin(Consumer<String> onStatus) {
      return CompletableFuture.supplyAsync(
         () -> {
            try {
               int port;
               try (ServerSocket probe = new ServerSocket(0)) {
                  port = probe.getLocalPort();
               }

               String var18 = "http://localhost:" + port;
               String verifier = generateVerifier();
               String challenge = codeChallenge(verifier);
               String url = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_id=c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb&response_type=code&redirect_uri="
                  + enc(var18)
                  + "&scope="
                  + enc("XboxLive.signin offline_access")
                  + "&prompt=select_account&code_challenge="
                  + challenge
                  + "&code_challenge_method=S256";
               onStatus.accept("Ouverture du navigateur...");
               openBrowser(url);
               onStatus.accept("Connectez-vous dans votre navigateur...");
               String code = this.waitForCallback(port);
               onStatus.accept("Authentification Microsoft...");
               String[] msTokens = this.exchangeCode(code, var18, verifier);
               String msAccessToken = msTokens[0];
               String msRefreshToken = msTokens[1];
               onStatus.accept("Connexion Xbox Live...");
               String xblToken = this.authenticateXBL(msAccessToken);
               onStatus.accept("Vérification XSTS...");
               String[] xsts = this.authenticateXSTS(xblToken);
               onStatus.accept("Connexion Minecraft...");
               String mcToken = this.authenticateMinecraft(xsts[0], xsts[1]);
               onStatus.accept("Récupération du profil...");
               AccountEntry entry = this.fetchProfile(mcToken, msRefreshToken);
               onStatus.accept("Connecté en tant que " + entry.getUsername() + " !");
               return entry;
            } catch (Exception var17) {
               throw new RuntimeException(var17.getMessage(), var17);
            }
         }
      );
   }

   public AccountEntry refreshAccessToken(String refreshToken) throws Exception {
      String body = formEncode(
         Map.of(
            "client_id",
            "c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb",
            "refresh_token",
            refreshToken,
            "grant_type",
            "refresh_token",
            "scope",
            "XboxLive.signin offline_access"
         )
      );
      HttpResponse<String> resp = this.postForm("https://login.microsoftonline.com/consumers/oauth2/v2.0/token", body);
      JsonObject json = parse(resp.body());
      assertNoError(json, "Token refresh");
      String msAccessToken = json.get("access_token").getAsString();
      String newRefreshToken = json.has("refresh_token") ? json.get("refresh_token").getAsString() : refreshToken;
      String xblToken = this.authenticateXBL(msAccessToken);
      String[] xsts = this.authenticateXSTS(xblToken);
      String mcToken = this.authenticateMinecraft(xsts[0], xsts[1]);
      AccountEntry temp = new AccountEntry("__refresh__", UUID.randomUUID(), mcToken, newRefreshToken);
      temp.setExpiresAt(Instant.now().plusSeconds(86400L));
      return temp;
   }

   private String waitForCallback(int port) throws IOException {
      String var9;
      try (ServerSocket server = new ServerSocket(port)) {
         server.setSoTimeout(180000);

         try (Socket conn = server.accept()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String requestLine = reader.readLine();
            String code = extractQueryParam(requestLine, "code");
            String htmlBody = "<!DOCTYPE html><html><head><meta charset='utf-8'><style>body{font-family:-apple-system,Segoe UI,sans-serif;display:flex;justify-content:center;align-items:center;height:100vh;margin:0;background:#0a0a0a;color:#ffffff;}.card{text-align:center;padding:40px 56px;background:#141414;border:1px solid #262626;border-radius:14px;}.check{width:46px;height:46px;line-height:46px;border-radius:50%;background:#1f7a3d;margin:0 auto 18px;font-size:24px;}h1{margin:0 0 6px;font-size:21px;font-weight:600;color:#ffffff;}p{margin:0;color:#888;font-size:13px;}</style></head><body><div class='card'><div class='check'>✓</div><h1>Connected</h1><p>You can close this tab and return to the game.</p></div></body></html>";
            String httpResp = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "
               + htmlBody.getBytes(StandardCharsets.UTF_8).length
               + "\r\nConnection: close\r\n\r\n"
               + htmlBody;
            conn.getOutputStream().write(httpResp.getBytes(StandardCharsets.UTF_8));
            conn.getOutputStream().flush();
            if (code == null) {
               throw new IOException("Callback sans code d'autorisation.");
            }

            var9 = code;
         }
      }

      return var9;
   }

   private String[] exchangeCode(String code, String redirectUri, String verifier) throws Exception {
      String body = formEncode(
         Map.of(
            "client_id",
            "c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb",
            "code",
            code,
            "grant_type",
            "authorization_code",
            "redirect_uri",
            redirectUri,
            "code_verifier",
            verifier,
            "scope",
            "XboxLive.signin offline_access"
         )
      );
      HttpResponse<String> resp = this.postForm("https://login.microsoftonline.com/consumers/oauth2/v2.0/token", body);
      JsonObject json = parse(resp.body());
      assertNoError(json, "Échange de code");
      return new String[]{json.get("access_token").getAsString(), json.has("refresh_token") ? json.get("refresh_token").getAsString() : ""};
   }

   private String authenticateXBL(String msToken) throws IOException, InterruptedException {
      String body = "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d=%s\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}\n"
         .formatted(msToken);
      return parse(this.postJson("https://user.auth.xboxlive.com/user/authenticate", body).body()).get("Token").getAsString();
   }

   private String[] authenticateXSTS(String xblToken) throws IOException, InterruptedException {
      String body = "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"%s\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}\n"
         .formatted(xblToken);
      JsonObject json = parse(this.postJson("https://xsts.auth.xboxlive.com/xsts/authorize", body).body());
      if (json.has("XErr")) {
         throw new RuntimeException("Erreur XSTS: " + json.get("XErr").getAsLong());
      } else {
         String token = json.get("Token").getAsString();
         String hash = json.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
         return new String[]{token, hash};
      }
   }

   private String authenticateMinecraft(String xstsToken, String userHash) throws IOException, InterruptedException {
      String body = "{\"identityToken\":\"XBL3.0 x=%s;%s\"}\n".formatted(userHash, xstsToken);
      return parse(this.postJson("https://api.minecraftservices.com/authentication/login_with_xbox", body).body()).get("access_token").getAsString();
   }

   private AccountEntry fetchProfile(String mcToken, String refreshToken) throws IOException, InterruptedException {
      HttpRequest req = HttpRequest.newBuilder()
         .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
         .header("Authorization", "Bearer " + mcToken)
         .GET()
         .build();
      JsonObject json = parse(this.http.send(req, BodyHandlers.ofString()).body());
      if (!json.has("name")) {
         throw new RuntimeException("Ce compte Microsoft ne possède pas Minecraft Java.");
      } else {
         String name = json.get("name").getAsString();
         String raw = json.get("id").getAsString();
         UUID uuid = UUID.fromString(raw.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
         AccountEntry e = new AccountEntry(name, uuid, mcToken, refreshToken);
         e.setExpiresAt(Instant.now().plusSeconds(86400L));
         return e;
      }
   }

   private HttpResponse<String> postForm(String url, String body) throws IOException, InterruptedException {
      return this.http
         .send(
            HttpRequest.newBuilder()
               .uri(URI.create(url))
               .header("Content-Type", "application/x-www-form-urlencoded")
               .POST(BodyPublishers.ofString(body))
               .build(),
            BodyHandlers.ofString()
         );
   }

   private HttpResponse<String> postJson(String url, String body) throws IOException, InterruptedException {
      return this.http
         .send(
            HttpRequest.newBuilder()
               .uri(URI.create(url))
               .header("Content-Type", "application/json")
               .header("Accept", "application/json")
               .POST(BodyPublishers.ofString(body))
               .build(),
            BodyHandlers.ofString()
         );
   }

   private static JsonObject parse(String body) {
      return new JsonParser().parse(body).getAsJsonObject();
   }

   private static void assertNoError(JsonObject json, String ctx) throws IOException {
      if (json.has("error")) {
         String msg = json.has("error_description") ? json.get("error_description").getAsString() : json.get("error").getAsString();
         throw new IOException(ctx + " — " + msg);
      }
   }

   private static String generateVerifier() {
      byte[] bytes = new byte[32];
      new SecureRandom().nextBytes(bytes);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
   }

   private static String codeChallenge(String verifier) {
      try {
         MessageDigest md = MessageDigest.getInstance("SHA-256");
         return Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest(verifier.getBytes(StandardCharsets.US_ASCII)));
      } catch (Exception var2) {
         throw new RuntimeException(var2);
      }
   }

   private static String enc(String value) {
      return URLEncoder.encode(value, StandardCharsets.UTF_8);
   }

   private static String formEncode(Map<String, String> params) {
      return params.entrySet().stream().map(e -> enc(e.getKey()) + "=" + enc(e.getValue())).collect(Collectors.joining("&"));
   }

   private static void openBrowser(String url) throws IOException {
      String os = System.getProperty("os.name", "").toLowerCase();
      ProcessBuilder pb;
      if (os.contains("win")) {
         pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
      } else if (os.contains("mac")) {
         pb = new ProcessBuilder("open", url);
      } else {
         pb = new ProcessBuilder("xdg-open", url);
      }

      pb.start();
   }

   private static String extractQueryParam(String requestLine, String param) {
      if (requestLine == null) {
         return null;
      } else {
         int q = requestLine.indexOf(63);
         int space = requestLine.lastIndexOf(32);
         if (q >= 0 && space >= q) {
            for (String kv : requestLine.substring(q + 1, space).split("&")) {
               String[] parts = kv.split("=", 2);
               if (parts.length == 2 && parts[0].equals(param)) {
                  return parts[1];
               }
            }

            return null;
         } else {
            return null;
         }
      }
   }
}
