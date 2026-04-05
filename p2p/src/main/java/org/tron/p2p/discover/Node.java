package org.tron.p2p.discover;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.utils.NetUtil;

@Slf4j(topic = "net")
public class Node implements Serializable, Cloneable {

  private static final long serialVersionUID = -4267600517925770636L;

  @Setter
  @Getter
  private byte[] id;

  @Getter
  protected String hostV4;

  @Getter
  protected String hostV6;

  @Setter
  @Getter
  protected int port;

  @Setter
  private int bindPort;

  @Setter
  private int p2pVersion;

  @Getter
  private long updateTime;

  public Node(InetSocketAddress address) {
    this.id = NetUtil.getNodeId();
    if (address.getAddress() instanceof Inet4Address) {
      this.hostV4 = address.getAddress().getHostAddress();
    } else {
      this.hostV6 = address.getAddress().getHostAddress();
    }
    this.port = address.getPort();
    this.bindPort = port;
    this.updateTime = System.currentTimeMillis();
    formatHostV6();
  }

  public Node(byte[] id, String hostV4, String hostV6, int port) {
    this.id = id;
    this.hostV4 = hostV4;
    this.hostV6 = hostV6;
    this.port = port;
    this.bindPort = port;
    this.updateTime = System.currentTimeMillis();
    formatHostV6();
  }

  public Node(byte[] id, String hostV4, String hostV6, int port, int bindPort) {
    this.id = id;
    this.hostV4 = hostV4;
    this.hostV6 = hostV6;
    this.port = port;
    this.bindPort = bindPort;
    this.updateTime = System.currentTimeMillis();
    formatHostV6();
  }

  public void updateHostV4(String hostV4) {
    if (StringUtils.isEmpty(this.hostV4) && StringUtils.isNotEmpty(hostV4)) {
      log.info("update hostV4:{} with hostV6:{}", hostV4, this.hostV6);
      this.hostV4 = hostV4;
    }
  }

  public void updateHostV6(String hostV6) {
    if (StringUtils.isEmpty(this.hostV6) && StringUtils.isNotEmpty(hostV6)) {
      log.info("update hostV6:{} with hostV4:{}", hostV6, this.hostV4);
      this.hostV6 = hostV6;
    }
  }

  //use standard ipv6 format
  private void formatHostV6() {
    if (StringUtils.isNotEmpty(this.hostV6)) {
      this.hostV6 = new InetSocketAddress(hostV6, port).getAddress().getHostAddress();
    }
  }

  public boolean isConnectible(int argsP2PVersion) {
    return port == bindPort && p2pVersion == argsP2PVersion;
  }

  public InetSocketAddress getPreferInetSocketAddress() {
    if (StringUtils.isNotEmpty(hostV4) && StringUtils.isNotEmpty(Parameter.p2pConfig.getIp())) {
      return getInetSocketAddressV4();
    } else if (StringUtils.isNotEmpty(hostV6) && StringUtils.isNotEmpty(
        Parameter.p2pConfig.getIpv6())) {
      return getInetSocketAddressV6();
    } else {
      return null;
    }
  }

  public String getHexId() {
    return id == null ? null : Hex.toHexString(id);
  }

  public String getHexIdShort() {
    return getIdShort(getHexId());
  }

  public String getHostKey() {
    return getPreferInetSocketAddress().getAddress().getHostAddress();
  }

  public String getIdString() {
    if (id == null) {
      return null;
    }
    return new String(id);
  }

  public void touch() {
    updateTime = System.currentTimeMillis();
  }

  @Override
  public String toString() {
    return "Node{" + " hostV4='" + hostV4 + '\'' + ", hostV6='" + hostV6 + '\'' + ", port=" + port
        + ", id=\'" + (id == null ? "null" : Hex.toHexString(id)) + "\'}";
  }

  public String format() {
    return "Node{" + " hostV4='" + hostV4 + '\'' + ", hostV6='" + hostV6 + '\'' + ", port=" + port
        + '}';
  }

  @Override
  public int hashCode() {
    return this.format().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o == this) {
      return true;
    }

    if (o.getClass() == getClass()) {
      return StringUtils.equals(getIdString(), ((Node) o).getIdString());
    }

    return false;
  }

  private String getIdShort(String hexId) {
    return hexId == null ? "<null>" : hexId.substring(0, 8);
  }

  public InetSocketAddress getInetSocketAddressV4() {
    return StringUtils.isNotEmpty(hostV4) ? new InetSocketAddress(hostV4, port) : null;
  }

  public InetSocketAddress getInetSocketAddressV6() {
    return StringUtils.isNotEmpty(hostV6) ? new InetSocketAddress(hostV6, port) : null;
  }

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException ignored) {
    }
    return null;
  }
}
