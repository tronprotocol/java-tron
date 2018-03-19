package org.tron.common.overlay.discover.message;

import org.tron.common.overlay.message.Message;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Message.P2pMessageCode;

public abstract class P2pMessage extends Message {

  public P2pMessage() {
  }

  public P2pMessage(byte[] data) {
    super(data);
  }

  public P2pMessageCode getCommand() {
    return P2pMessageCode.forNumber(ByteArray.toInt(data));
  }
}
