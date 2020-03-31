package org.tron.core.consensus;

import com.google.protobuf.ByteString;
import com.sun.org.apache.xpath.internal.Arg;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.SignUtils;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.consensus.Consensus;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.Param.Miner;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.store.WitnessStore;

@Slf4j(topic = "consensus")
@Component
public class ConsensusService {

  @Autowired
  private Consensus consensus;

  @Autowired
  private WitnessStore witnessStore;

  @Autowired
  private BlockHandleImpl blockHandle;

  private CommonParameter parameter = Args.getInstance();

  public void start() {
    Param param = new Param();
    param.setEnable(parameter.isWitness());
    param.setGenesisBlock(parameter.getGenesisBlock());
    param.setMinParticipationRate(parameter.getMinParticipationRate());
    param.setBlockProduceTimeoutPercent(Args.getInstance().getBlockProducedTimeOut());
    param.setNeedSyncCheck(parameter.isNeedSyncCheck());
    List<Miner> miners = new ArrayList<>();
    byte[] privateKey = ByteArray
        .fromHexString(Args.getLocalWitnesses().getPrivateKey());
    byte[] privateKeyAddress = SignUtils.fromPrivate(privateKey,
        Args.getInstance().isECKeyCryptoEngine()).getAddress();
    byte[] witnessAddress = Args.getLocalWitnesses().getWitnessAccountAddress(Args
        .getInstance().isECKeyCryptoEngine());
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

}
