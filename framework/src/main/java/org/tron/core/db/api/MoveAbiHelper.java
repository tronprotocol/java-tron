package org.tron.core.db.api;

import lombok.extern.slf4j.Slf4j;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AbiCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.store.AbiStore;
import org.tron.core.store.ContractStore;

import java.util.Iterator;
import java.util.Map;

@Slf4j(topic = "DB")
public class MoveAbiHelper {

  private int count;

  private final ChainBaseManager chainBaseManager;

  public MoveAbiHelper(ChainBaseManager chainBaseManager) {
    this.chainBaseManager = chainBaseManager;
  }

  public void doWork() {
    long start = System.currentTimeMillis();
    logger.info("Start to move abi");
    AbiStore abiStore = chainBaseManager.getAbiStore();
    ContractStore contractStore = chainBaseManager.getContractStore();
    Iterator<Map.Entry<byte[], ContractCapsule>> it = contractStore.iterator();
    it.forEachRemaining(e -> {
      ContractCapsule contractCapsule = e.getValue();
      if (!abiStore.has(e.getKey())) {
        abiStore.put(e.getKey(), new AbiCapsule(contractCapsule));
      }
      contractStore.put(e.getKey(), contractCapsule);
      count += 1;
      if (count % 100_000 == 0) {
        logger.info("Doing the abi move, current contracts: {} {}", count,
            System.currentTimeMillis());
      }
    });
    chainBaseManager.getDynamicPropertiesStore().saveAbiMoveDone(1);
    logger.info("Check store size: contract {} abi {}",
        contractStore.getTotalContracts(), abiStore.getTotalABIs());
    logger.info(
        "Complete the abi move, total time:{} milliseconds",
        System.currentTimeMillis() - start);
  }
}
