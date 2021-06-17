package org.tron.core.db.api;

import java.util.Iterator;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.store.AbiStore;
import org.tron.core.store.ContractStore;

@Slf4j(topic = "DB")
public class MoveAbiHelper {

  private int count;

  private final ChainBaseManager chainBaseManager;

  public MoveAbiHelper(ChainBaseManager chainBaseManager) {
    this.chainBaseManager = chainBaseManager;
  }

  public void doWork() {
    long start = System.currentTimeMillis();
    logger.info("Start to copy abi");
    AbiStore abiStore = chainBaseManager.getAbiStore();
    ContractStore contractStore = chainBaseManager.getContractStore();
    Iterator<Map.Entry<byte[], ContractCapsule>> it = contractStore.iterator();
    it.forEachRemaining(e -> {
      ContractCapsule contractCapsule = e.getValue();
      if (!contractCapsule.getInstance().getAbi().getEntrysList().isEmpty()) {
        abiStore.put(e.getKey(), contractCapsule.getInstance().getAbi().toByteArray());
      }
      count += 1;
      if (count % 100_000 == 0) {
        logger.info("Doing the abi copy, current count: {} {}", count,
            System.currentTimeMillis());
      }
    });
    chainBaseManager.getDynamicPropertiesStore().saveAbiMoveDone(1);
    logger.info(
        "Complete the abi copy, total time: {} milliseconds, total count: {}",
        System.currentTimeMillis() - start, count);
  }
}
