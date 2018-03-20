package org.tron.core.net.message.p2p;

import org.tron.core.net.message.Message;
import org.tron.core.net.message.MessageTypes;

public abstract class P2pMessage extends Message {

  public P2pMessage(byte[] packed) {
    super(packed);
  }

  @Override
  public MessageTypes getType() {
    //TODO: here need let the type embed in the data, maybe in the zero position ind the data.
    return null;
  }
}
