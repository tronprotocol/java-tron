package org.tron.core.db;

import com.google.protobuf.ByteString;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.Param.Miner;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;

public class BlockGenerate {

  private static Manager manager;

  private static ChainBaseManager chainBaseManager;


  public static void setManager(Manager dbManager) {
    manager = dbManager;
    chainBaseManager = dbManager.getChainBaseManager();
  }

  public Block getSignedBlock(ByteString witness, long time, byte[] privateKey) {
    long blockTime = System.currentTimeMillis() / 3000 * 3000;
    if (time != 0) {
      blockTime = time;
    } else {
      if (chainBaseManager.getHeadBlockId().getNum() != 0) {
        blockTime = chainBaseManager.getHeadBlockTimeStamp() + 3000;
      }
    }
    Param param = Param.getInstance();
    Miner miner = param.new Miner(privateKey, witness, witness);
    BlockCapsule blockCapsule = manager
        .generateBlock(miner, time, System.currentTimeMillis() + 1000);
    Block block = blockCapsule.getInstance();

    BlockHeader.raw raw = block.getBlockHeader().getRawData().toBuilder()
        .setParentHash(ByteString
            .copyFrom(chainBaseManager.getDynamicPropertiesStore()
                .getLatestBlockHeaderHash().getBytes()))
        .setNumber(chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1)
        .setTimestamp(blockTime)
        .setWitnessAddress(witness)
        .build();

    ECKey ecKey = ECKey.fromPrivate(privateKey);
    ECDSASignature signature = ecKey.sign(Sha256Hash.of(CommonParameter
        .getInstance().isECKeyCryptoEngine(), raw.toByteArray()).getBytes());
    ByteString sign = ByteString.copyFrom(signature.toByteArray());

    BlockHeader blockHeader = block.getBlockHeader().toBuilder()
        .setRawData(raw)
        .setWitnessSignature(sign)
        .build();

    Block signedBlock = block.toBuilder().setBlockHeader(blockHeader).build();

    return signedBlock;
  }

}
