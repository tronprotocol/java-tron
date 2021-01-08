package org.tron.core.metrics.net;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class PendingInfo {

  private AtomicLong trxFromUser = new AtomicLong();
  private AtomicLong trxFromUserAccepted = new AtomicLong();
  private AtomicLong trxFromNode = new AtomicLong();
  private AtomicLong trxFromNodeAccepted = new AtomicLong();

  private ConcurrentHashMap<String, Long> trxByUser = new ConcurrentHashMap();
  private ConcurrentHashMap<String, Long> trxByNode = new ConcurrentHashMap();


  public ConcurrentHashMap<String, Long> getTrxByUser() {
    return trxByUser;
  }

  public ConcurrentHashMap<String, Long> getTrxByNode() {
    return trxByNode;
  }


  public AtomicLong getTrxFromUser() {
    return trxFromUser;
  }

  public AtomicLong getTrxFromNode() {
    return trxFromNode;
  }

  public AtomicLong getTrxFromUserAccepted() {
    return trxFromUserAccepted;
  }

  public AtomicLong getTrxFromNodeAccepted() {
    return trxFromNodeAccepted;
  }

  public void removeTransaction(String txId) {
    trxByUser.remove(txId);
    trxByNode.remove(txId);
  }

  public String toString() {
    return String.format("trxFromUser: %d, trxFromUserAccepted:%d, " +
            "trxFromNode: %d, trxFromNodeAccepted: %d",
            trxFromUser.get(), trxFromUserAccepted.get(), trxFromNode.get(), trxFromNodeAccepted.get());
  }

}
