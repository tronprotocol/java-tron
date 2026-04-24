package org.tron.core.config.args;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.es.ExecutorServiceManager;
import org.tron.p2p.dns.lookup.LookUpTxt;
import org.tron.p2p.utils.NetUtil;

@Slf4j(topic = "app")
public class InetUtil {

  private static final String DNS_POOL_NAME = "args-dns-lookup";
  private static final int DNS_POOL_MAX_SIZE = 10;

  /**
   * Converts a list of {@code ipOrDomain:port} config strings into resolved {@link
   * InetSocketAddress} objects, preserving the original order.
   *
   * <p>IP literals (IPv4 and IPv6) are used as-is. Domain names are resolved via DNS: when there
   * are multiple domains, they are resolved in parallel using a dedicated thread pool; a single
   * domain is resolved inline. Entries that fail DNS resolution are silently dropped.
   *
   * <p>Supported formats:
   * <ul>
   *   <li>{@code 192.168.100.0:18888}
   *   <li>{@code [fe80::48ff:fe00:1122]:18888}
   *   <li>{@code example.com:18888}
   *   <li>{@code hostname:18888}
   * </ul>
   *
   * @param ipOrDomainWithPortList list of address strings in {@code ipOrDomain:port} format,
   *     may mix IP literals and domain names
   * @return resolved addresses in the same order as the input, omitting unresolvable entries
   */
  public static List<InetSocketAddress> resolveInetSocketAddressList(
      List<String> ipOrDomainWithPortList) {
    List<InetSocketAddress> result = new ArrayList<>();
    if (ipOrDomainWithPortList.isEmpty()) {
      return result;
    }

    // Collect entries whose host part is a domain name (not an IP literal).
    List<String> domainEntries = new ArrayList<>();
    for (String item : ipOrDomainWithPortList) {
      if (!isIpLiteral(NetUtil.parseInetSocketAddress(item).getHostString())) {
        domainEntries.add(item);
      }
    }

    // Resolve domain names: spin up a thread pool only when there are multiple domains.
    Map<String, InetSocketAddress> resolvedDomains = new HashMap<>();
    if (domainEntries.size() > 1) {
      int poolSize = StrictMath.min(domainEntries.size(), DNS_POOL_MAX_SIZE);
      ExecutorService dnsPool = ExecutorServiceManager
          .newFixedThreadPool(DNS_POOL_NAME, poolSize, true);
      List<Future<InetSocketAddress>> futures = new ArrayList<>(domainEntries.size());
      for (String entry : domainEntries) {
        futures.add(dnsPool.submit(() -> resolveInetSocketAddress(entry)));
      }
      for (int i = 0; i < domainEntries.size(); i++) {
        String entry = domainEntries.get(i);
        try {
          resolvedDomains.put(entry, futures.get(i).get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          logger.warn("DNS lookup interrupted for: {}", entry);
        } catch (ExecutionException e) {
          logger.warn("Failed to resolve address, skip: {}", entry);
        }
      }
      ExecutorServiceManager.shutdownAndAwaitTermination(dnsPool, DNS_POOL_NAME);
    } else if (domainEntries.size() == 1) {
      String entry = domainEntries.get(0);
      resolvedDomains.put(entry, resolveInetSocketAddress(entry));
    }

    // Build the result list preserving the original config order.
    for (String item : ipOrDomainWithPortList) {
      InetSocketAddress parsed = NetUtil.parseInetSocketAddress(item);
      InetSocketAddress resolved = isIpLiteral(parsed.getHostString())
          ? parsed
          : resolvedDomains.get(item);
      if (resolved != null) {
        result.add(resolved);
      }
    }
    return result;
  }

  /**
   * Resolves a {@code ipOrDomain:port} config string to an {@link InetSocketAddress} via DNS.
   *
   * <p>The host is looked up first over IPv4, then over IPv6 as a fallback. Returns {@code null}
   * if DNS resolution fails for both address families.
   *
   * @param ipOrDomainWithPort address string in {@code ipOrDomain:port} format
   * @return resolved {@link InetSocketAddress}, or {@code null} if the host cannot be resolved
   */
  private static InetSocketAddress resolveInetSocketAddress(String ipOrDomainWithPort) {
    InetSocketAddress parsed = NetUtil.parseInetSocketAddress(ipOrDomainWithPort);
    String host = parsed.getHostString();
    int port = parsed.getPort();
    InetAddress address = LookUpTxt.lookUpIp(host, true);
    if (address == null) {
      address = LookUpTxt.lookUpIp(host, false);
    }
    if (address == null) {
      return null;
    }
    logger.info("Resolve {} to {}", host, address.getHostAddress());
    return new InetSocketAddress(address, port);
  }

  /**
   * Resolves {@code ipOrDomain} to an {@link InetAddress}.
   *
   * <p>IP literals are converted directly without a DNS lookup. Domain names are first resolved
   * over IPv4, then retried over IPv6 if the first attempt fails.
   *
   * @param ipOrDomain IPv4/IPv6 literal or a domain name to resolve
   * @return the resolved {@link InetAddress}, or {@code null} if resolution fails
   */
  public static InetAddress resolveInetAddress(String ipOrDomain) {
    // Fast path: already a numeric address — no lookup needed.
    if (isIpLiteral(ipOrDomain)) {
      try {
        return InetAddress.getByName(ipOrDomain);
      } catch (UnknownHostException e) {
        return null;
      }
    }
    InetAddress address = LookUpTxt.lookUpIp(ipOrDomain, true);
    if (address == null) {
      address = LookUpTxt.lookUpIp(ipOrDomain, false);
    }
    return address;
  }

  private static boolean isIpLiteral(String host) {
    return NetUtil.validIpV4(host) || NetUtil.validIpV6(host);
  }
}
