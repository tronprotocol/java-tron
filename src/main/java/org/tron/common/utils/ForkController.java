package org.tron.common.utils;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j
@Component
public class ForkController {

  public static final int DISCARD_SCOPE = ContractType.UpdateAssetContract.getNumber();

  @Getter
  private Manager manager;
  private volatile int[] slots = new int[0];
  private boolean forked;

  public void init(Manager manager) {
    this.manager = manager;
    forked = manager.getDynamicPropertiesStore().getForked();
  }

  public synchronized boolean shouldBeForked() {
    if (forked) {
      logger.info("*****shouldBeForked:" + true);
      return true;
    }

    for (int version : slots) {
      if (version != ChainConstant.version) {
        logger.info("*****shouldBeForked:" + false);
        return false;
      }
    }

    // todo add Maintenance or block number
    forked = true;
    manager.getDynamicPropertiesStore().forked();
    logger.info("*****shouldBeForked:" + true);
    return true;
  }

  public synchronized boolean forkOrNot(TransactionCapsule capsule) {
    logger.info("*****forkOrNot:" + (shouldBeForked()
        || capsule.getInstance().getRawData().getContractList().get(0).getType().getNumber()
        <= DISCARD_SCOPE));
    return shouldBeForked()
        || capsule.getInstance().getRawData().getContractList().get(0).getType().getNumber()
        <= DISCARD_SCOPE;
  }

  public synchronized void update(BlockCapsule blockCapsule) {
    if (forked) {
      return;
    }

    List<ByteString> witnesses = manager.getWitnessController().getActiveWitnesses();
    if (witnesses.size() != slots.length) {
      slots = new int[witnesses.size()];
    }

    ByteString witness = blockCapsule.getWitnessAddress();
    int slot = witnesses.indexOf(witness);
    if (slot < 0) {
      return;
    }
    slots[slot] = blockCapsule.getInstance().getBlockHeader().getRawData().getVersion();

    logger.info(
        "*******update:" + Arrays.toString(slots)
            + ",witness size:" + witnesses.size()
            + "," + slots
            + ",slot:" + slot
            + ",version:" + blockCapsule.getInstance().getBlockHeader().getRawData().getVersion()
    );
  }

  public void reset() {
    Arrays.fill(slots, 0);
  }

}
