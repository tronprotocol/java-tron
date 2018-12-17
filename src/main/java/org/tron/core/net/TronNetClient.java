package org.tron.core.net;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.message.Message;
import org.tron.core.net.service.AdvService;

@Component
public class TronNetClient {

  @Autowired
  private AdvService advService;

  public void broadcast(Message msg) {
    advService.broadcast(msg);
  }

}
