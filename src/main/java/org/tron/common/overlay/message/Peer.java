/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.common.overlay.message;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import org.tron.protos.Message.Capability;

/**
 * This class models a peer in the network
 */
public class Peer {

  private final InetAddress address;
  private final int port;
  private final String peerId;
  private final List<Capability> capabilities;

  public Peer(InetAddress ip, int port, String peerId) {
    this.address = ip;
    this.port = port;
    this.peerId = peerId;
    this.capabilities = new ArrayList<>();
  }

  public InetAddress getAddress() {
    return address;
  }

  public int getPort() {
    return port;
  }

  public String getPeerId() {
    return peerId == null ? "" : peerId;
  }

  public List<Capability> getCapabilities() {
    return capabilities;
  }

  public byte[] getEncoded() {
    return null;
  }

  @Override
  public String toString() {
    return "[ip=" + getAddress().getHostAddress() +
        " port=" + getPort()
        + " peerId=" + getPeerId() + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Peer)) {
      return false;
    }
    Peer peerData = (Peer) obj;
    return peerData.peerId.equals(this.peerId)
        || this.getAddress().equals(peerData.getAddress());
  }

  @Override
  public int hashCode() {
    int result = peerId.hashCode();
    result = 31 * result + address.hashCode();
    result = 31 * result + port;
    return result;
  }
}
