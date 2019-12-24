package org.tron.core.net.message;

import org.tron.protos.Protocol;

public class BlockHeaderInventoryMesasge extends TronMessage {

  private Protocol.BlockHeaderInventory blockHeaderInventory;

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }
}
