package org.tron.common.utils;

import com.google.protobuf.ByteString;
import java.util.List;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

@Component
public class ForkController {

  public static final int DISCARD_SCOPE = ContractType.UpdateAssetContract.getNumber();

  @Getter
  private Manager manager;
  private volatile int[] slots = new int[0];
  private boolean fork = false;

  public void init(Manager manager) {
    this.manager = manager;
  }

  public synchronized boolean shouldBeForked() {
    if (fork) {
      return true;
    }

    for (int version : slots) {
      if (version != ChainConstant.version) {
        return false;
      }
    }

    fork = true;
    return true;
  }

  public synchronized boolean forkOrNot(TransactionCapsule capsule) {
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

    slots[slot] = blockCapsule.getInstance().getBlockHeader().getRawData().getVersion();
  }

}
