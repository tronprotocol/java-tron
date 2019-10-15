package org.tron.core.db;

import com.google.protobuf.ByteString;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;

public class BlockGenerate {

  private static Manager manager;

  public static void setManager(Manager dbManager) {
    manager = dbManager;
  }

  public Block getSignedBlock(ByteString witness, long time, byte[] privateKey) {

    BlockCapsule blockCapsule = manager.generateBlock(null, System.currentTimeMillis() + 1000);
    Block block = blockCapsule.getInstance();

    BlockHeader.raw raw = block.getBlockHeader().getRawData().toBuilder()
        .setParentHash(ByteString
            .copyFrom(manager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getBytes()))
        .setNumber(manager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1)
        .setTimestamp(time)
        .setWitnessAddress(witness)
        .build();

    ECKey ecKey = ECKey.fromPrivate(privateKey);
    ECDSASignature signature = ecKey.sign(Sha256Hash.of(raw.toByteArray()).getBytes());
    ByteString sign = ByteString.copyFrom(signature.toByteArray());

    BlockHeader blockHeader = block.getBlockHeader().toBuilder()
        .setRawData(raw)
        .setWitnessSignature(sign)
        .build();

    Block signedBlock = block.toBuilder().setBlockHeader(blockHeader).build();

    return signedBlock;
  }

}
