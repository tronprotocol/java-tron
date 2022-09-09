package org.tron.core.witness;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Utils;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.Param.Miner;
import org.tron.consensus.dpos.DposSlot;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;

public class Witness {

  protected static Manager dbManager;

  protected static ChainBaseManager chainManager;

  protected static DposSlot dposSlot;

  protected final String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";
  protected final byte[] privateKey = ByteArray.fromHexString(key);

  protected final ECKey ecKey = ECKey.fromPrivate(privateKey);
  protected final byte[] address = ecKey.getAddress();

  public static void setManager(Manager manager) {
    dbManager = manager;
    chainManager = dbManager.getChainBaseManager();
  }


  public Block getSignedBlock(ByteString witness, long time, byte[] privateKey) {
    long blockTime = System.currentTimeMillis() / 3000 * 3000;
    if (time != 0) {
      blockTime = time;
    } else {
      if (chainManager.getHeadBlockId().getNum() != 0) {
        blockTime = chainManager.getHeadBlockTimeStamp() + 3000;
      }
    }
    Param param = Param.getInstance();
    Miner miner = param.new Miner(privateKey, witness, witness);
    BlockCapsule blockCapsule = dbManager
        .generateBlock(miner, time, System.currentTimeMillis() + 1000);
    Block block = blockCapsule.getInstance();

    BlockHeader.raw raw = block.getBlockHeader().getRawData().toBuilder()
        .setParentHash(ByteString
            .copyFrom(chainManager.getDynamicPropertiesStore()
                .getLatestBlockHeaderHash().getBytes()))
        .setNumber(chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1)
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

  public BlockCapsule getSignedBlock(Map<ByteString, String> witnessAndAccount) {

    long time = chainManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 3000;
    long number = chainManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1;

    ByteString hash = chainManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
        .getByteString();
    ByteString witnessAddress = dposSlot.getScheduledWitness(dposSlot.getSlot(time));
    BlockCapsule blockCapsule = new BlockCapsule(number, Sha256Hash.wrap(hash), time,
        witnessAddress);
    blockCapsule.generatedByMyself = true;
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(ByteArray.fromHexString(witnessAndAccount.get(witnessAddress)));
    return blockCapsule;
  }


  public Map<ByteString, String> addWitnessAndAccount() {
    return addWitnessAndAccount(2);
  }

  public Map<ByteString, String> addWitnessAndAccount(int size) {
    return IntStream.range(0, size)
        .mapToObj(
            i -> {
              ECKey ecKey = new ECKey(Utils.getRandom());
              String privateKey = ByteArray.toHexString(ecKey.getPrivKey().toByteArray());
              ByteString address = ByteString.copyFrom(ecKey.getAddress());

              WitnessCapsule witnessCapsule = new WitnessCapsule(address);
              chainManager.getWitnessStore().put(address.toByteArray(), witnessCapsule);
              chainManager.addWitness(address);

              AccountCapsule accountCapsule =
                  new AccountCapsule(Protocol.Account.newBuilder().setAddress(address).build());
              chainManager.getAccountStore().put(address.toByteArray(), accountCapsule);

              return Maps.immutableEntry(address, privateKey);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

}
