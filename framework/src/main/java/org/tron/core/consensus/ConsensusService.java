package org.tron.core.consensus;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.backup.BackupManager;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.consensus.Consensus;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.Param.Miner;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.net.TronNetService;
import org.tron.core.store.WitnessStore;
import org.tron.protos.Protocol.Block;

@Slf4j(topic = "consensus")
@Component
public class ConsensusService {

  @Autowired
  private Manager manager;

  @Autowired
  private BackupManager backupManager;

  @Autowired
  private TronNetService tronNetService;

  @Autowired
  private Consensus consensus;

  @Autowired
  private WitnessStore witnessStore;

  @Autowired
  private BlockHandleImpl blockHandle;

  private Args args = Args.getInstance();

  public void start() {
    Param param = new Param();
    param.setEnable(args.isWitness());
    param.setGenesisBlock(args.getGenesisBlock());
    param.setMinParticipationRate(args.getMinParticipationRate());
    param.setNeedSyncCheck(args.isNeedSyncCheck());
    List<Miner> miners = new ArrayList<>();
    byte[] privateKey = ByteArray
        .fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey());
    byte[] privateKeyAddress = ECKey.fromPrivate(privateKey).getAddress();
    byte[] witnessAddress = Args.getInstance().getLocalWitnesses().getWitnessAccountAddress();
    WitnessCapsule witnessCapsule = witnessStore.get(witnessAddress);
    if (null == witnessCapsule) {
      logger.warn("Witness {} is not in witnessStore.", Hex.encodeHexString(witnessAddress));
    } else {
      Miner miner = param.new Miner(privateKey, ByteString.copyFrom(privateKeyAddress),
          ByteString.copyFrom(witnessAddress));
      miners.add(miner);
    }
    param.setMiners(miners);
    param.setBlockHandle(blockHandle);
    consensus.start(param);
  }

  public void stop() {
    consensus.stop();
  }

  public boolean validBlock(Block block){
    return consensus.validBlock(block);
  }

  public boolean applyBlock(Block block) {
    return consensus.applyBlock(block);
  }
}
