package org.tron.common.overlay.discover;

import org.tron.common.crypto.ECKey;
import org.tron.common.utils.Sha256Hash;

import java.io.Serializable;

import static org.tron.common.crypto.Hash.sha3;

public class Star implements Serializable {

  private static final long serialVersionUID = -4267600517925770636L;

  private Sha256Hash id;

  private String host;

  private int port;

  private boolean isFakeNodeId = false;

  public static Star instanceOf(String address) {
    final ECKey key = ECKey.fromPrivate(sha3(address.getBytes()));
    final Sha256Hash StartId = Sha256Hash.wrap(key.getNodeId());
    final Star star = new Star(StartId, address);
    star.isFakeNodeId = true;
    return star;
  }

  public Star(Sha256Hash id, String address) {
    this.id = id;

    int colon = address.indexOf(":");//TODO: throw exception here.
    this.host = address.substring(0, colon - 1);
    //TODO: throw exception here
    this.port = Integer.parseInt(address.substring(colon, address.length() - 1));
  }

  public boolean isDiscovery() {
    return isFakeNodeId;
  }

  public Sha256Hash getId() {
    return id;
  }

  public void setId(Sha256Hash id) {
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

    if (o instanceof Star) {
      return getId().equals(((Star) o).getId());
    }

    return false;
  }
}
