package org.tron.core.net;

import lombok.Getter;
import lombok.Setter;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;

public class TronManager {

  @Getter
  private Object blockLock;

  @Setter
  @Getter
  private boolean syncBlockFetchFlag;

  public void check (boolean flag, String msg) throws Exception {
    if (!flag){
      throw new P2pException(TypeEnum.CHECK_FAILED, msg);
    }
  }

}
