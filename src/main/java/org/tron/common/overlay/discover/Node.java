package org.tron.common.overlay.discover;

import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import static org.tron.common.crypto.Hash.sha3;

public class Node implements Serializable {

  private static final long serialVersionUID = -4267600517925770636L;

  private byte[] id;

  private String host;

  private int port;

  private boolean isFakeNodeId = false;

  public static Node instanceOf(String addressOrEnode) {
    try {
      URI uri = new URI(addressOrEnode);
      if (uri.getScheme().equals("enode")) {
        return new Node(addressOrEnode);
      }
    } catch (URISyntaxException e) {
      // continue
    }

    final ECKey generatedNodeKey = ECKey.fromPrivate(sha3(addressOrEnode.getBytes()));
    final String generatedNodeId = Hex.toHexString(generatedNodeKey.getNodeId());
    final Node node = new Node("enode://" + generatedNodeId + "@" + addressOrEnode);
    node.isFakeNodeId = true;
    return node;
  }

  public Node(String enodeURL) {
    try {
      URI uri = new URI(enodeURL);
      if (!uri.getScheme().equals("enode")) {
        throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT");
      }
      this.id = Hex.decode(uri.getUserInfo());
      this.host = uri.getHost();
      this.port = uri.getPort();
    } catch (URISyntaxException e) {
      throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT", e);
    }
  }

  public Node(byte[] id, String host, int port) {
    this.id = id;
    this.host = host;
    this.port = port;
  }

  public Node(byte[] id, String address) {
    this.id = id;

    int colon = address.indexOf(":");//TODO: throw exception here.
    this.host = address.substring(0, colon - 1);
    //TODO: throw exception here
    this.port = Integer.parseInt(address.substring(colon, address.length() - 1));
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

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setDiscovery(boolean isDiscoveryNode) {
    isFakeNodeId = isDiscoveryNode;
  }

  @Override
  public String toString() {
    return "Node{" +
        " host='" + host + '\'' +
        ", port=" + port +
        ", id=" + id +
        '}';
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

    if (o instanceof Node) {
      return getId().equals(((Node) o).getId());
    }

    return false;
  }
}
