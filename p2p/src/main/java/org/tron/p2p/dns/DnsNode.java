package org.tron.p2p.dns;


import static org.tron.p2p.discover.message.kad.KadMessage.getEndpointFromNode;

import com.google.protobuf.InvalidProtocolBufferException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tron.p2p.base.Constant;
import org.tron.p2p.discover.Node;
import org.tron.p2p.dns.tree.Algorithm;
import org.tron.p2p.protos.Discover;
import org.tron.p2p.protos.Discover.EndPoints;
import org.tron.p2p.protos.Discover.EndPoints.Builder;
import org.tron.p2p.protos.Discover.Endpoint;
import org.tron.p2p.utils.ByteArray;

@Slf4j(topic = "net")
public class DnsNode extends Node implements Comparable<DnsNode> {

  private static final long serialVersionUID = 6689513341024130226L;
  private String v4Hex = Constant.ipV4Hex;
  private String v6Hex = Constant.ipV6Hex;

  public DnsNode(byte[] id, String hostV4, String hostV6, int port) throws UnknownHostException {
    super(null, hostV4, hostV6, port);
    if (StringUtils.isNotEmpty(hostV4)) {
      this.v4Hex = ipToString(hostV4);
    }
    if (StringUtils.isNotEmpty(hostV6)) {
      this.v6Hex = ipToString(hostV6);
    }
  }

  public static String compress(List<DnsNode> nodes) {
    Builder builder = Discover.EndPoints.newBuilder();
    nodes.forEach(node -> {
      Endpoint endpoint = getEndpointFromNode(node);
      builder.addNodes(endpoint);
    });
    return Algorithm.encode64(builder.build().toByteArray());
  }

  public static List<DnsNode> decompress(String base64Content)
      throws InvalidProtocolBufferException, UnknownHostException {
    byte[] data = Algorithm.decode64(base64Content);
    EndPoints endPoints = EndPoints.parseFrom(data);

    List<DnsNode> dnsNodes = new ArrayList<>();
    for (Endpoint endpoint : endPoints.getNodesList()) {
      DnsNode dnsNode = new DnsNode(endpoint.getNodeId().toByteArray(),
          new String(endpoint.getAddress().toByteArray()),
          new String(endpoint.getAddressIpv6().toByteArray()),
          endpoint.getPort());
      dnsNodes.add(dnsNode);
    }
    return dnsNodes;
  }

  public String ipToString(String ip) throws UnknownHostException {
    byte[] bytes = InetAddress.getByName(ip).getAddress();
    return ByteArray.toHexString(bytes);
  }

  public int getNetworkA() {
    if (StringUtils.isNotEmpty(hostV4)) {
      return Integer.parseInt(hostV4.split("\\.")[0]);
    } else {
      return 0;
    }
  }

  @Override
  public int compareTo(DnsNode o) {
    if (this.v4Hex.compareTo(o.v4Hex) != 0) {
      return this.v4Hex.compareTo(o.v4Hex);
    } else if (this.v6Hex.compareTo(o.v6Hex) != 0) {
      return this.v6Hex.compareTo(o.v6Hex);
    } else {
      return this.port - o.port;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DnsNode)) {
      return false;
    }
    DnsNode other = (DnsNode) o;
    return v4Hex.equals(other.v4Hex) && v6Hex.equals(other.v6Hex) && port == other.port;
  }
}
