package dev.swiftclient.core.partner;

public record PartnerServer(String name, String ip, int port, String description) {
   public String address() {
      return this.port == 25565 ? this.ip : this.ip + ":" + this.port;
   }
}
