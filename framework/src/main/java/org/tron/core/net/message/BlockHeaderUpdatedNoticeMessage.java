package org.tron.core.net.message;

import org.tron.protos.Protocol;

public class BlockHeaderUpdatedNoticeMessage extends TronMessage {

  private Protocol.BlockHeaderUpdatedNotice blockHeaderUpdatedNotice;

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}
