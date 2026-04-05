package org.tron.p2p.dns.lookup;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.p2p.base.Parameter;
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

  static int maxRetryTimes = 5;
  static Random random = new Random();

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

  public static String joinTXTRecord(TXTRecord txtRecord) {
    StringBuilder sb = new StringBuilder();
    for (String s : txtRecord.getStrings()) {
      sb.append(s.trim());
    }
    return sb.toString();
  }
}
