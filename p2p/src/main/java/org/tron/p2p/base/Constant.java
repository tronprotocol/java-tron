package org.tron.p2p.base;

import java.util.Arrays;
import java.util.List;

public class Constant {

  public static final int NODE_ID_LEN = 64;
  public static final List<String> ipV4Urls = Arrays.asList(
      "http://checkip.amazonaws.com", "https://ifconfig.me/ip", "https://4.ipw.cn/");
  public static final List<String> ipV6Urls = Arrays.asList(
      "https://v6.ident.me", "http://6.ipw.cn/", "https://api6.ipify.org",
      "https://ipv6.icanhazip.com");
  public static final String ipV4Hex = "00000000"; //32 bit
  public static final String ipV6Hex = "00000000000000000000000000000000"; //128 bit
}
