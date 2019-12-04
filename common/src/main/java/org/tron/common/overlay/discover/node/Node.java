package org.tron.common.overlay.discover.node;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.option.KademliaOptions;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;

public class Node implements Serializable {

  private static final long serialVersionUID = -4267600517925770636L;

  private byte[] id;

  private String host;

  private int port;

  @Getter
  private int bindPort;

  @Setter
  private int p2pVersion;

  private int reputation = 0;

  private boolean isFakeNodeId = false;

  public Node(String encodeURI) {
    try {
      URI uri = new URI(encodeURI);
      if (!"encode".equals(uri.getScheme())) {
        throw new RuntimeException("expecting URL in the format encode://PUBKEY@HOST:PORT");
      }
      this.id = Hex.decode(uri.getUserInfo());
      this.host = uri.getHost();
      this.port = uri.getPort();
      this.bindPort = uri.getPort();
      this.isFakeNodeId = true;
    } catch (URISyntaxException e) {
      throw new RuntimeException("expecting URL in the format encode://PUBKEY@HOST:PORT", e);
    }
  }

  public Node(byte[] id, String host, int port) {
    if (id != null) {
      this.id = id.clone();
    }
    this.host = host;
    this.port = port;
    this.isFakeNodeId = true;
  }

  public Node(byte[] id, String host, int port, int bindPort) {
    if (id != null) {
      this.id = id.clone();
    }
    this.host = host;
    this.port = port;
    this.bindPort = bindPort;
  }

  public static Node instanceOf(String addressOrEncode) {
    try {
      URI uri = new URI(addressOrEncode);
      if ("encode".equals(uri.getScheme())) {
        return new Node(addressOrEncode);
      }
    } catch (URISyntaxException e) {
      // continue
    }

    final String generatedNodeId = Hex.toHexString(getNodeId());
    final Node node = new Node("encode://" + generatedNodeId + "@" + addressOrEncode);
    return node;
  }

  public static byte[] getNodeId() {
    Random gen = new Random();
    byte[] id = new byte[KademliaOptions.NODE_ID_LEN];
    gen.nextBytes(id);
    return id;
  }

  public int getReputation() {
    return reputation;
  }

  public void setReputation(int reputation) {
    this.reputation = reputation;
  }

  public String getEnodeURL() {
    return new StringBuilder("encode://")
        .append(ByteArray.toHexString(id)).append("@")
        .append(host).append(":")
        .append(port).toString();
  }

  public boolean isConnectible(int argsP2pversion) {
    return port == bindPort && p2pVersion == argsP2pversion;
  }

  public String getHexId() {
    return Hex.toHexString(id);
  }

  public String getHexIdShort() {
    return Utils.getIdShort(getHexId());
  }

  public boolean isDiscoveryNode() {
    return isFakeNodeId;
  }

  public byte[] getId() {
    return id;
  }

  public void setId(byte[] id) {
    this.id = id;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getIdString() {
    if (id == null) {
      return null;
    }
    return new String(id);
  }

  @Override
  public String toString() {
    return "Node{" + " host='" + host + '\'' + ", port=" + port
        + ", id=" + ByteArray.toHexString(id) + '}';
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
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
}
