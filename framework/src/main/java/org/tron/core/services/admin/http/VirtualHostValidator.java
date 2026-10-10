package org.tron.core.services.admin.http;

import com.google.common.net.InetAddresses;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Applies the Admin HTTP virtual-host policy without resolving request hostnames through DNS.
 */
final class VirtualHostValidator {

  private final Set<String> virtualHosts;

  VirtualHostValidator(List<String> configuredHosts) {
    virtualHosts = normalizeVirtualHosts(configuredHosts);
  }

  boolean isAllowedHost(String hostHeader) {
    if (hostHeader == null || hostHeader.isEmpty()) {
      // A browser always sends Host. Preserve compatibility for non-browser HTTP/1.0 clients.
      return true;
    }
    String host = extractHost(hostHeader);
    if (host == null) {
      return false;
    }
    if (InetAddresses.isInetAddress(host)) {
      return true;
    }
    return virtualHosts.contains("*")
        || virtualHosts.contains(host.toLowerCase(Locale.ROOT));
  }

  private String extractHost(String hostHeader) {
    // IPv6
    if (hostHeader.startsWith("[")) {
      int closingBracket = hostHeader.indexOf(']');
      if (closingBracket <= 1) {
        return null;
      }
      String suffix = hostHeader.substring(closingBracket + 1);
      if (!suffix.isEmpty() && !isPortSuffix(suffix)) {
        return null;
      }
      return hostHeader.substring(1, closingBracket);
    }

    // Hostname or IPv4, with an optional port.
    int firstColon = hostHeader.indexOf(':');
    if (firstColon < 0) {
      return hostHeader;
    }
    if (firstColon != hostHeader.lastIndexOf(':')) {
      return hostHeader;
    }
    String suffix = hostHeader.substring(firstColon);
    if (!isPortSuffix(suffix)) {
      return null;
    }
    return hostHeader.substring(0, firstColon);
  }

  private boolean isPortSuffix(String suffix) {
    if (suffix.length() <= 1 || suffix.charAt(0) != ':') {
      return false;
    }
    for (int i = 1; i < suffix.length(); i++) {
      if (!Character.isDigit(suffix.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private Set<String> normalizeVirtualHosts(List<String> configuredHosts) {
    Set<String> normalizedHosts = new HashSet<>();
    if (configuredHosts == null) {
      return normalizedHosts;
    }
    for (String configuredHost : configuredHosts) {
      if (configuredHost != null && !configuredHost.trim().isEmpty()) {
        normalizedHosts.add(configuredHost.trim().toLowerCase(Locale.ROOT));
      }
    }
    return normalizedHosts;
  }
}
