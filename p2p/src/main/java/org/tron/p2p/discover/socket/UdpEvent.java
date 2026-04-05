package org.tron.p2p.discover.socket;

import java.net.InetSocketAddress;
import org.tron.p2p.discover.message.Message;

public class UdpEvent {
  private Message message;
  //when receive UdpEvent, this is sender address
  //when send UdpEvent, this is target address
  private InetSocketAddress address;

  public UdpEvent(Message message, InetSocketAddress address) {
    this.message = message;
    this.address = address;
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public InetSocketAddress getAddress() {
    return address;
  }

  public void setAddress(InetSocketAddress address) {
    this.address = address;
  }
}
