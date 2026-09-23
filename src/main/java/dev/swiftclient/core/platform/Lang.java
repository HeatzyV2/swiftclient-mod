package dev.swiftclient.core.platform;

public record Lang(String code, String name, String region) {
   public String display() {
      return this.region != null && !this.region.isBlank() ? this.name + " (" + this.region + ")" : this.name;
   }
}
