package org.tron.p2p.dns.lookup;


import com.google.common.annotations.VisibleForTesting;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.p2p.base.Parameter;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

@Slf4j(topic = "net")
public class LookUpTxt {

  static String[] publicDnsV4 = new String[] {
      "114.114.114.114", "114.114.115.115", //114 DNS
      "223.5.5.5", "223.6.6.6", //AliDNS
      //"180.76.76.76", //BaiduDNS slow
      "119.29.29.29", //DNSPod DNS+
      // "182.254.116.116", //DNSPod DNS+ slow
      //"1.2.4.8", "210.2.4.8", //CNNIC SDNS
      "117.50.11.11", "117.50.22.22", //oneDNS
      "101.226.4.6", "218.30.118.6", "123.125.81.6", "140.207.198.6", //DNS pai
      "8.8.8.8", "8.8.4.4", //Google DNS
      "9.9.9.9", //IBM Quad9
      //"208.67.222.222", "208.67.220.220", //OpenDNS slow
      //"199.91.73.222", "178.79.131.110" //V2EX DNS
  };

  static String[] publicDnsV6 = new String[] {
      "2606:4700:4700::1111", "2606:4700:4700::1001", //Cloudflare
      "2400:3200::1", "2400:3200:baba::1", //AliDNS
      //"2400:da00::6666", //BaiduDNS
      "2a00:5a60::ad1:0ff", "2a00:5a60::ad2:0ff", //AdGuard
      "2620:74:1b::1:1", "2620:74:1c::2:2", //Verisign
      //"2a05:dfc7:5::53", "2a05:dfc7:5::5353", //OpenNIC
      "2a02:6b8::feed:0ff", "2a02:6b8:0:1::feed:0ff",  //Yandex
      "2001:4860:4860::8888", "2001:4860:4860::8844", //Google DNS
      "2620:fe::fe", "2620:fe::9", //IBM Quad9
      //"2620:119:35::35", "2620:119:53::53", //OpenDNS
      "2a00:5a60::ad1:0ff", "2a00:5a60::ad2:0ff" //AdGuard
  };

  @VisibleForTesting
  static int maxRetryTimes = 5;
  static Random random = new Random();
  static final ExecutorService OS_RESOLVER_EXECUTOR = new ThreadPoolExecutor(
      1, 8, 60L, TimeUnit.SECONDS,
      new LinkedBlockingQueue<>(16),
      r -> {
        Thread t = new Thread(r, "dns-os-resolver");
        t.setDaemon(true);
        return t;
      },
      new ThreadPoolExecutor.AbortPolicy()
  );

  public static TXTRecord lookUpTxt(String hash, String domain)
      throws TextParseException, UnknownHostException {
    return lookUpTxt(hash + "." + domain);
  }

  // only get first Record.
  // as dns server has dns cache, we may get the name's latest TXTRecord ttl later after it changes
  public static TXTRecord lookUpTxt(String name) throws TextParseException, UnknownHostException {
    TXTRecord txt = null;
    log.info("LookUp name: {}", name);
    Lookup lookup = new Lookup(name, Type.TXT);
    int times = 0;
    Record[] records = null;
    long start = System.currentTimeMillis();
    while (times < maxRetryTimes) {
      String publicDns;
      if (StringUtils.isNotEmpty(Parameter.p2pConfig.getIp())) {
        publicDns = publicDnsV4[random.nextInt(publicDnsV4.length)];
      } else {
        publicDns = publicDnsV6[random.nextInt(publicDnsV6.length)];
      }
      SimpleResolver simpleResolver = new SimpleResolver(InetAddress.getByName(publicDns));
      simpleResolver.setTimeout(Duration.ofMillis(1000));
      lookup.setResolver(simpleResolver);
      long thisTime = System.currentTimeMillis();
      records = lookup.run();
      long end = System.currentTimeMillis();
      times += 1;
      if (records != null) {
        log.debug("Succeed to use dns: {}, cur cost: {}ms, total cost: {}ms", publicDns,
            end - thisTime, end - start);
        break;
      } else {
        log.debug("Failed to use dns: {}, cur cost: {}ms", publicDns, end - thisTime);
      }
    }
    if (records == null) {
      log.error("Failed to lookUp name:{}", name);
      return null;
    }
    for (Record item : records) {
      txt = (TXTRecord) item;
    }
    return txt;
  }

  /**
   * Resolves a domain name to an IP address. Resolution order:
   *   <li>OS name resolver ({@link InetAddress}) — reads {@code /etc/hosts} first,
   *       so LAN IP mappings configured there are returned immediately without a DNS query.</li>
   *   <li>Random public DNS server (fallback, retried up to {@link #maxRetryTimes} times).</li>
   *
   * @param domain the domain name to resolve (e.g. {@code "nodes.example.com"})
   * @param useIPv4 {@code true} to query A records (IPv4); {@code false} to query AAAA records (IPv6)
   * @return the resolved {@link InetAddress}, or {@code null} if resolution fails
   */
  public static InetAddress lookUpIp(String domain, boolean useIPv4) {
    if (StringUtils.isEmpty(domain)) {
      return null;
    }
    log.debug("LookUp {} for domain: {}", useIPv4 ? "IPv4" : "IPv6", domain);

    // Step 1: OS name resolver — honours /etc/hosts, so LAN mappings work without a DNS query.
    Future<InetAddress[]> future = OS_RESOLVER_EXECUTOR.submit(
        () -> InetAddress.getAllByName(domain));
    try {
      for (InetAddress addr : future.get(2000, TimeUnit.MILLISECONDS)) {
        if ((useIPv4 && addr instanceof Inet4Address)
            || (!useIPv4 && addr instanceof Inet6Address)) {
          log.debug("Resolved {} via OS name resolver (may be /etc/hosts): {}", domain,
              addr.getHostAddress());
          return addr;
        }
      }
    } catch (TimeoutException e) {
      // cancel(true) sends an interrupt, but InetAddress.getAllByName() is a
      // native blocking call and does NOT respond to interrupts. The thread
      // will keep running until the OS-level resolution completes or times out.
      // This is an accepted limitation of wrapping non-interruptible I/O in a Future.
      future.cancel(true);
      log.debug("OS name resolver timed out for {}", domain);
    } catch (ExecutionException e) {
      log.debug("OS name resolver failed for {}: {}", domain, e.getCause().getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // restore interrupt flag
      log.debug("OS name resolver interrupted for {}", domain);
    }

    // Step 2: fall back to random public DNS servers.
    int recordType = useIPv4 ? Type.A : Type.AAAA;
    String[] publicDns = useIPv4 ? publicDnsV4 : publicDnsV6;
    long start = System.currentTimeMillis();
    for (int times = 0; times < maxRetryTimes; times++) {
      String dns = publicDns[random.nextInt(publicDns.length)];
      try {
        Lookup lookup = new Lookup(domain, recordType);
        SimpleResolver simpleResolver = new SimpleResolver(InetAddress.getByName(dns));
        simpleResolver.setTimeout(Duration.ofMillis(1000));
        lookup.setResolver(simpleResolver);
        long thisTime = System.currentTimeMillis();
        Record[] records = lookup.run();
        long end = System.currentTimeMillis();
        if (records != null && records.length > 0) {
          InetAddress address = useIPv4
              ? ((ARecord) records[0]).getAddress()
              : ((AAAARecord) records[0]).getAddress();
          log.debug("Resolved {} via public DNS {}, cur cost: {}ms, total cost: {}ms",
              domain, dns, end - thisTime, end - start);
          return address;
        }
        log.debug("Public DNS {} failed for {}, cur cost: {}ms", dns, domain,
            System.currentTimeMillis() - thisTime);
      } catch (TextParseException | UnknownHostException e) {
        log.debug("Public DNS {} error for {}: {}", dns, domain, e.getMessage());
      }
    }

    log.warn("Failed to resolve {} for domain: {}", useIPv4 ? "IPv4" : "IPv6", domain);
    return null;
  }

  public static String joinTXTRecord(TXTRecord txtRecord) {
    StringBuilder sb = new StringBuilder();
    for (String s : txtRecord.getStrings()) {
      sb.append(s.trim());
    }
    return sb.toString();
  }
}
