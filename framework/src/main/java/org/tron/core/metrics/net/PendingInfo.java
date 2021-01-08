package org.tron.core.metrics.net;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class PendingInfo {

  private AtomicLong trxFromUser = new AtomicLong();
  private AtomicLong trxFromUserAccepted = new AtomicLong();
  private AtomicLong trxFromNode = new AtomicLong();
  private AtomicLong trxFromNodeAccepted = new AtomicLong();

  private AtomicLong repushOnChain = new AtomicLong();

  public AtomicLong getTrxFromUser() {
    return this.trxFromUser;
  }

  public AtomicLong getTrxFromNode() {
    return this.trxFromNode;
  }

  public AtomicLong getTrxFromUserAccepted() {
    return this.trxFromUserAccepted;
  }

  public AtomicLong getTrxFromNodeAccepted() {
    return this.trxFromNodeAccepted;
  }

  public AtomicLong getRepushOnChain() {
    return this.repushOnChain;
  }

  public String toString() {
    return String.format("trxFromUser: %d, trxFromUserAccepted:%d, " +
            "trxFromNode: %d, trxFromNodeAccepted: %d, repushOnChain: %d",
            trxFromUser.get(), trxFromUserAccepted.get(), trxFromNode.get(),
            trxFromNodeAccepted.get(), repushOnChain.get());
  }
}
