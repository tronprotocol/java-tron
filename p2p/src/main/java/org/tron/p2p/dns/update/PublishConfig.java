package org.tron.p2p.dns.update;


import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class PublishConfig {

  private boolean dnsPublishEnable = false;
  private String dnsPrivate = null;
  private List<String> knownTreeUrls = new ArrayList<>();
  private List<InetSocketAddress> staticNodes = new ArrayList<>();
  private String dnsDomain = null;
  private double changeThreshold = 0.1;
  private int maxMergeSize = 5;
  private DnsType dnsType = null;
  private String accessKeyId = null;
  private String accessKeySecret = null;
  private String aliDnsEndpoint = null; //for aliYun
  private String awsHostZoneId = null; //for aws
  private String awsRegion = null; //for aws
}
