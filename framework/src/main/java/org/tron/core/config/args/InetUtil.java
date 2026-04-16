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

  public static List<InetSocketAddress> getInetSocketAddressList(List<String> items) {
    List<InetSocketAddress> ret = new ArrayList<>();
    if (items.isEmpty()) {
      return ret;
    }
    // Collect entries whose host part is a domain name (not an IP literal).
    List<String> domainEntries = new ArrayList<>();
    for (String item : items) {
      String host = NetUtil.parseInetSocketAddress(item).getHostString();
      if (!NetUtil.validIpV4(host) && !NetUtil.validIpV6(host)) {
        domainEntries.add(item);
      }
    }

    // Resolve domain names: spin up a thread pool only when there are multiple domains
    Map<String, InetSocketAddress> domainResolved = new HashMap<>();
    if (domainEntries.size() > 1) {
      String poolName = "args-dns-lookup";
      ExecutorService dnsPool = ExecutorServiceManager
          .newFixedThreadPool(poolName, domainEntries.size(), true);
      List<Future<InetSocketAddress>> futures = new ArrayList<>(domainEntries.size());
      for (String entry : domainEntries) {
        futures.add(dnsPool.submit(() -> resolveInetSocketAddress(entry)));
      }
      for (int i = 0; i < domainEntries.size(); i++) {
        String entry = domainEntries.get(i);
        try {
          domainResolved.put(entry, futures.get(i).get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          logger.warn("DNS lookup interrupted for: {}", entry);
        } catch (ExecutionException e) {
          logger.warn("Failed to resolve address, skip: {}", entry);
        }
      }
      ExecutorServiceManager.shutdownAndAwaitTermination(dnsPool, poolName);
    } else if (domainEntries.size() == 1) {
      String entry = domainEntries.get(0);
      domainResolved.put(entry, resolveInetSocketAddress(entry));
    }

    // Build the result list preserving the original config order.
    for (String configString : items) {
      InetSocketAddress inetSocketAddress;
      InetSocketAddress parsed = NetUtil.parseInetSocketAddress(configString);
      if (NetUtil.validIpV4(parsed.getHostString()) || NetUtil.validIpV6(parsed.getHostString())) {
        inetSocketAddress = parsed;
      } else {
        inetSocketAddress = domainResolved.get(configString);
      }
      if (inetSocketAddress == null) {
        continue;
      }
      ret.add(inetSocketAddress);
    }
    return ret;
  }

  /**
   * Resolves a {@code domain:port} address string to an {@link InetSocketAddress} via DNS.
   */
  private static InetSocketAddress resolveInetSocketAddress(String configString) {
    InetSocketAddress parsed = NetUtil.parseInetSocketAddress(configString);
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
   * Resolves a hostname or IP string to a numeric IP address string.
   */
  public static InetAddress resolveInetAddress(String ipOrDomain) {
    // Fast path: already a numeric address — no lookup needed.
    if (NetUtil.validIpV4(ipOrDomain) || NetUtil.validIpV6(ipOrDomain)) {
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
}
