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
import org.tron.core.exception.ContractExeException;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j
@Component
public class ForkController {

  public static final int DISCARD_SCOPE = ContractType.UpdateAssetContract.getNumber();

  @Getter
  private Manager manager;
  private volatile int[] slots = new int[0];
  private boolean forked = true;

  public void init(Manager manager) {
    this.manager = manager;
    forked = true;
  }

  public synchronized boolean shouldBeForked() {
    if (forked) {
      if (logger.isDebugEnabled()) {
        logger.debug("*****shouldBeForked:" + true);
      }
      return true;
    }

    if (slots.length == 0) {
      return false;
    }

    for (int version : slots) {
      if (version != ChainConstant.version) {
        if (logger.isDebugEnabled()) {
          logger.debug("*****shouldBeForked:" + false);
        }
        return false;
      }
    }

    // todo add Maintenance or block number
    forked = true;
    manager.getDynamicPropertiesStore().forked();
    if (logger.isDebugEnabled()) {
      logger.debug("*****shouldBeForked:" + true);
    }
    return true;
  }

  public synchronized void hardFork(TransactionCapsule capsule) throws ContractExeException {
    boolean hardFork = shouldBeForked()
        || capsule.getInstance().getRawData().getContractList().get(0).getType().getNumber()
        <= DISCARD_SCOPE;
    if (logger.isDebugEnabled()) {
      logger.debug("*****hardFork:" + hardFork);
    }
    if (!hardFork) {
      throw new ContractExeException("not yet hard forked");
    }
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

    int version = blockCapsule.getInstance().getBlockHeader().getRawData().getVersion();
    slots[slot] = version;

    logger.info(
        "*******update hard fork:" + Arrays.toString(slots)
            + ",witness size:" + witnesses.size()
            + ",slot:" + slot
            + ",witness:" + ByteUtil.toHexString(witness.toByteArray())
            + ",version:" + version
    );
  }

  public synchronized void reset() {
    Arrays.fill(slots, 0);
  }

}
