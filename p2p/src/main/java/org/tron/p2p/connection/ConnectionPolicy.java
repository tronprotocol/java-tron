package org.tron.p2p.connection;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Runtime connection policy shared by TCP connection paths.
 */
public final class ConnectionPolicy {

  private static volatile Set<InetAddress> blockedIps = Collections.emptySet();

  private ConnectionPolicy() {
  }

  public static void replaceBlockedIps(Set<InetAddress> blockedIps) {
    if (blockedIps == null) {
      throw new IllegalArgumentException("blockedIps must not be null");
    }
    Set<InetAddress> replacement = new HashSet<>();
    for (InetAddress address : blockedIps) {
      if (address == null) {
        throw new IllegalArgumentException("blockedIps must not contain null");
      }
      replacement.add(normalize(address));
    }
    ConnectionPolicy.blockedIps = Collections.unmodifiableSet(replacement);
  }

  public static boolean isBlocked(InetAddress address) {
    Set<InetAddress> currentBlockedIps = blockedIps;
    return address != null && !currentBlockedIps.isEmpty()
        && currentBlockedIps.contains(normalize(address));
  }

  /**
   * Checks resolved endpoints used by internal TCP and connection-candidate paths. Blocking is
   * IP-based, so the endpoint port is ignored. UDP discovery traffic is intentionally outside the
   * first-phase blacklist scope. Null or unresolved endpoints return {@code false}; public
   * management APIs must validate their input separately.
   */
  public static boolean isBlocked(InetSocketAddress address) {
    return address != null && !address.isUnresolved() && isBlocked(address.getAddress());
  }

  /**
   * Normalizes IPv4-mapped IPv6 to IPv4 and removes host names and scope IDs without DNS lookup.
   */
  private static InetAddress normalize(InetAddress address) {
    try {
      return InetAddress.getByAddress(address.getAddress());
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Unsupported IP address", e);
    }
  }
}
