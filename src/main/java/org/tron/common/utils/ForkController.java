package org.tron.common.utils;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
  private boolean fork;

  public void init(Manager manager) {
    this.manager = manager;
    fork = manager.getDynamicPropertiesStore().getForkController();
  }

  public synchronized boolean shouldBeForked() {
    if (fork) {
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
    fork = true;
    manager.getDynamicPropertiesStore().setForkController(true);
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
    List<ByteString> witnesses = manager.getWitnessController().getActiveWitnesses();
    if (witnesses.size() != slots.length) {
      slots = new int[witnesses.size()];
    }

    ByteString witness = blockCapsule.getWitnessAddress();
    int slot = 0;
    for (ByteString scheduledWitness : witnesses) {
      if (!scheduledWitness.equals(witness)) {
        ++slot;
      }
    }

    logger.info(
        "*******update:" + Arrays.toString(slots)
            + ",witness size:" + witnesses.size()
            + "," + slots
            + ",slot:" + slot
            + ",version:" + blockCapsule.getInstance().getBlockHeader().getRawData().getVersion()
            + ",block witness:" + ByteUtil.toHexString(witness.toByteArray())
            + "witnesses:" + witnesses.stream().map(w -> ByteUtil.toHexString(w.toByteArray()))
            .collect(Collectors.toList())

    );
    slots[slot] = blockCapsule.getInstance().getBlockHeader().getRawData().getVersion();
  }

}
