package org.tron.common.validator;

import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.RecentBlockStore;
import org.tron.core.exception.ItemNotFoundException;

@Component("taposTransactionValidator")
public class TaposTransactionValidator extends AbstractTransactionValidator {

  @Autowired
  private ChainBaseManager chainBaseManager;
  @Autowired
  private RecentBlockStore recentBlockStore;

  @Override
  protected String doValidate(TransactionCapsule trx) {
    byte[] refBlockHash = trx.getRefBlockHash();
    byte[] refBlockNumBytes = trx.getRefBlockBytes();
    try {
      byte[] blockHash = recentBlockStore.get(refBlockNumBytes).getData();
      if (!Arrays.equals(blockHash, refBlockHash)) {
        return String.format(
            "Tapos failed, different block hash, %s, %s , recent block %s, "
                + "solid block %s head block %s",
            ByteArray.toLong(refBlockNumBytes), Hex.toHexString(refBlockHash),
            Hex.toHexString(blockHash),
            chainBaseManager.getSolidBlockId().getString(),
            chainBaseManager.getHeadBlockId().getString());
      }
      return null;
    } catch (ItemNotFoundException e) {
      return String
          .format("Tapos failed, block not found, ref block %s, %s , solid block %s head block %s",
              ByteArray.toLong(refBlockNumBytes), Hex.toHexString(refBlockHash),
              chainBaseManager.getSolidBlockId().getString(),
              chainBaseManager.getHeadBlockId().getString());
    }
  }
}
