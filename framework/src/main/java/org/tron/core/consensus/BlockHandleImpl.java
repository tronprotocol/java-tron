package org.tron.core.consensus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.backup.BackupManager;
import org.tron.common.backup.BackupManager.BackupStatusEnum;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.Consensus;
import org.tron.consensus.base.BlockHandle;
import org.tron.consensus.base.Param.Miner;
import org.tron.consensus.base.State;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.Manager;
import org.tron.core.net.TronNetService;
import org.tron.core.net.message.BlockMessage;
import org.tron.protos.Protocol;

@Slf4j(topic = "consensus")
@Component
public class BlockHandleImpl implements BlockHandle {

  @Autowired
  private Manager manager;

  @Autowired
  private BackupManager backupManager;

  @Autowired
  private TronNetService tronNetService;

  @Autowired
  private Consensus consensus;

  @Override
  public State getState() {
    if (!backupManager.getStatus().equals(BackupStatusEnum.MASTER)) {
      return State.BACKUP_IS_NOT_MASTER;
    }
    return State.OK;
  }

  public Object getLock() {
    return manager;
  }

  public BlockCapsule produce(Miner miner, long blockTime, long timeout) {
    BlockCapsule blockCapsule = manager.generateBlock(miner, blockTime, timeout);
    Protocol.BlockHeader.raw raw = blockCapsule.getInstance().getBlockHeader().getRawData();
    logger.info("g-block: {}, ID:{}, f:{},rawHexString:{}, rawString:{}",
            blockCapsule,
            new Sha256Hash(raw.getNumber(), Sha256Hash.of(CommonParameter
                    .getInstance().isECKeyCryptoEngine(), raw.toByteArray())),
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            org.apache.commons.codec.binary.Hex.encodeHexString(raw.toByteArray()),
            raw.toString());
    if (blockCapsule == null) {
      return null;
    }

    BlockCapsule blockCapsule2 = new BlockCapsule(blockCapsule.getInstance());
    raw = blockCapsule2.getInstance().getBlockHeader().getRawData();
    logger.info("g-block 2 : {}, ID:{}, f:{},rawHexString:{}, rawString:{}",
            blockCapsule2,
            new Sha256Hash(raw.getNumber(), Sha256Hash.of(CommonParameter
                    .getInstance().isECKeyCryptoEngine(), raw.toByteArray())),
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            org.apache.commons.codec.binary.Hex.encodeHexString(raw.toByteArray()),
            raw.toString());

    try {
      consensus.receiveBlock(blockCapsule);
      BlockMessage blockMessage = new BlockMessage(blockCapsule);
      tronNetService.fastForward(blockMessage);
      tronNetService.broadcast(blockMessage);
      manager.pushBlock(blockCapsule);
    } catch (Exception e) {
      logger.error("Handle block {} failed.", blockCapsule.getBlockId().getString(), e);
      return null;
    }
    return blockCapsule;
  }
}
