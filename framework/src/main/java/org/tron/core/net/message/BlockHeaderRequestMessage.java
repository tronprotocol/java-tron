package org.tron.core.net.message;

import org.tron.protos.Protocol;

public class BlockHeaderRequestMessage extends TronMessage {

  private Protocol.BlockHeaderRequestMessage blockHeaderRequestMessage;

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}
