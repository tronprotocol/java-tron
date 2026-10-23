package org.tron.core.consensus;

import static org.tron.common.utils.ByteArray.fromHexString;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.SignUtils;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.Metrics;
import org.tron.common.prometheus.SRMetrics;
import org.tron.common.utils.StringUtil;
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
        byte[] privateKeyAddress = SignUtils.fromPrivate(privateKey).getAddress();
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
    } else if (privateKeys.size() == 1) {
      byte[] privateKey =
          fromHexString(Args.getLocalWitnesses().getPrivateKey());
      byte[] privateKeyAddress = SignUtils.fromPrivate(privateKey).getAddress();
      byte[] witnessAddress = Args.getLocalWitnesses().getWitnessAccountAddress();
      WitnessCapsule witnessCapsule = witnessStore.get(witnessAddress);
      if (null == witnessCapsule) {
        logger.warn("Witness {} is not in witnessStore.", Hex.toHexString(witnessAddress));
      }
      // In multi-signature mode, the address derived from the private key may differ from
      // witnessAddress.
      Miner miner = param.new Miner(privateKey, ByteString.copyFrom(privateKeyAddress),
          ByteString.copyFrom(witnessAddress));
      miners.add(miner);
    }

    param.setMiners(miners);
    recordWitnessAddresses(miners);
    recordIsWitness();
    param.setBlockHandle(blockHandle);
    param.setPbftInterface(pbftBaseImpl);
    consensus.start(param);
    logger.info("consensus service start success");
  }

  private void recordWitnessAddresses(List<Miner> miners) {
    if (!Metrics.enabled() || miners == null) {
      return;
    }
    for (Miner miner : miners) {
      ByteString witnessAddress = miner.getWitnessAddress();
      try {
        String address = StringUtil.encode58Check(witnessAddress.toByteArray());
        SRMetrics.recordWitnessAddress(address);
      } catch (Exception e) {
        logger.warn("Failed to record witness address metrics.", e);
      }
    }
  }

  /**
   * Publishes tron:is_witness once at consensus startup; 0 = not witness, 1 = witness. It is
   * published independently of the witness-address publication, and even when no miner is
   * configured, so the flag series always exists. Catches its own failure so a metrics
   * problem can neither skip the witness publication nor abort consensus startup.
   */
  private void recordIsWitness() {
    if (!Metrics.enabled()) {
      return;
    }
    try {
      SRMetrics.recordIsWitness(Args.getInstance().isWitness()
          ? SRMetrics.WITNESS : SRMetrics.NOT_WITNESS);
    } catch (Exception e) {
      logger.warn("Failed to record is-witness metrics.", e);
    }
  }

  public void stop() {
    logger.info("consensus service closed start.");
    consensus.stop();
    logger.info("consensus service closed successfully.");
  }

}
