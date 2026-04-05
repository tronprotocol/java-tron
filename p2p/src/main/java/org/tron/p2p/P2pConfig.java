package org.tron.p2p;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Data;
import org.tron.p2p.dns.update.PublishConfig;
import org.tron.p2p.utils.NetUtil;

@Data
public class P2pConfig {

  private List<InetSocketAddress> seedNodes = new CopyOnWriteArrayList<>();
  private List<InetSocketAddress> activeNodes = new CopyOnWriteArrayList<>();
  private List<InetAddress> trustNodes = new CopyOnWriteArrayList<>();
  private byte[] nodeID = NetUtil.getNodeId();
  private String ip = NetUtil.getExternalIpV4();
  private String lanIp = NetUtil.getLanIP();
  private String ipv6 = NetUtil.getExternalIpV6();
  private int port = 18888;
  private int networkId = 1;
  private int minConnections = 8;
  private int maxConnections = 50;
  private int minActiveConnections = 2;
  private int maxConnectionsWithSameIp = 2;
  private boolean discoverEnable = true;
  private boolean disconnectionPolicyEnable = false;
  private boolean nodeDetectEnable = false;

  //dns read config
  private List<String> treeUrls = new ArrayList<>();

  //dns publish config
  private PublishConfig publishConfig = new PublishConfig();
}
