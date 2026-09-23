package dev.swiftclient.core.platform;

public record Account(String uuid, String username, boolean active, boolean expired, boolean offline) {
}
