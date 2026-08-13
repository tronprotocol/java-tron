package org.tron.p2p.dns.update;


public enum DnsType {
  AliYun(0, "aliyun dns server"),
  AwsRoute53(1, "aws route53 server");

  private final Integer value;
  private final String desc;

  DnsType(Integer value, String desc) {
    this.value = value;
    this.desc = desc;
  }

  public Integer getValue() {
    return value;
  }

  public String getDesc() {
    return desc;
  }
}
