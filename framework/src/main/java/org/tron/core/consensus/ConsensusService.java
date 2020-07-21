package org.tron.core.consensus;

import static org.tron.common.utils.ByteArray.fromHexString;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.SignUtils;
import org.tron.common.parameter.CommonParameter;
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

  @Autowired
  private PbftBaseImpl pbftBaseImpl;

  private CommonParameter parameter = Args.getInstance();

  public void start() {
    Param param = Param.getInstance();
    param.setEnable(parameter.isWitness());
    param.setGenesisBlock(parameter.getGenesisBlock());
    param.setMinParticipationRate(parameter.getMinParticipationRate());
    param.setBlockProduceTimeoutPercent(Args.getInstance().getBlockProducedTimeOut());
    param.setNeedSyncCheck(parameter.isNeedSyncCheck());
    param.setAgreeNodeCount(parameter.getAgreeNodeCount());
    List<Miner> miners = new ArrayList<>();
    List<String> privateKeys = Args.getLocalWitnesses().getPrivateKeys();
    if (privateKeys.size() > 1) {
      for (String key : privateKeys) {
        byte[] privateKey = fromHexString(key);
        byte[] privateKeyAddress = SignUtils
            .fromPrivate(privateKey, Args.getInstance().isECKeyCryptoEngine()).getAddress();
        WitnessCapsule witnessCapsule = witnessStore.get(privateKeyAddress);
        if (null == witnessCapsule) {
          logger.warn("Witness {} is not in witnessStore.", Hex.toHexString(privateKeyAddress));
        }
        Miner miner = param.new Miner(privateKey, ByteString.copyFrom(privateKeyAddress),
            ByteString.copyFrom(privateKeyAddress));
        miners.add(miner);
        logger.info("Add witness: {}, size: {}",
            Hex.toHexString(privateKeyAddress), miners.size());
      }
    } else {
      byte[] privateKey =
          fromHexString(Args.getLocalWitnesses().getPrivateKey());
      byte[] privateKeyAddress = SignUtils.fromPrivate(privateKey,
          Args.getInstance().isECKeyCryptoEngine()).getAddress();
      byte[] witnessAddress = Args.getLocalWitnesses().getWitnessAccountAddress(
          Args.getInstance().isECKeyCryptoEngine());
      WitnessCapsule witnessCapsule = witnessStore.get(witnessAddress);
      if (null == witnessCapsule) {
        logger.warn("Witness {} is not in witnessStore.", Hex.toHexString(witnessAddress));
      }
      Miner miner = param.new Miner(privateKey, ByteString.copyFrom(privateKeyAddress),
          ByteString.copyFrom(witnessAddress));
      miners.add(miner);
    }

    param.setMiners(miners);
    param.setBlockHandle(blockHandle);
    param.setPbftInterface(pbftBaseImpl);
    consensus.start(param);
    logger.info("consensus service start success");
  }

  public void stop() {
    consensus.stop();
  }

}
